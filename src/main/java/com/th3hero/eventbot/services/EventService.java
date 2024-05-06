package com.th3hero.eventbot.services;

import com.th3hero.eventbot.commands.*;
import com.th3hero.eventbot.entities.CourseJpa;
import com.th3hero.eventbot.entities.EventDraftJpa;
import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.exceptions.ConfigErrorException;
import com.th3hero.eventbot.exceptions.EventParsingException;
import com.th3hero.eventbot.repositories.EventDraftRepository;
import com.th3hero.eventbot.repositories.EventRepository;
import com.th3hero.eventbot.utils.EmbedBuilderFactory;
import com.th3hero.eventbot.utils.ModalFactory;
import com.th3hero.eventbot.utils.Utils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.th3hero.eventbot.config.DiscordFieldsConfig.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final CourseService courseService;
    private final ConfigService configService;
    private final EventDraftRepository eventDraftRepository;
    private final SchedulingService schedulingService;
    private final StudentService studentService;

    @Value("${app.config.deleted-event-cleanup-delay}")
    private int deletedEventCleanupDelay;

    private boolean verifyArgs(ButtonRequest request) {
        if (request.idArguments().isEmpty()) {
            request.buttonInteraction().reply("Failed to parse identifier from button").setEphemeral(true).queue();
            return false;
        }
        return true;
    }

    private boolean verifyArgs(ModalRequest request) {
        if (request.idArguments().isEmpty()) {
            request.interaction().reply("Failed to parse identifier from modal").setEphemeral(true).queue();
            return false;
        }
        return true;
    }

    private boolean verifyAdmin(ModalRequest request) {
        if (!request.interaction().getMember().hasPermission(Permission.ADMINISTRATOR)) {
            request.interaction().reply("You do not have permission to edit events.")
                    .setEphemeral(true)
                    .queue();
            return false;
        }
        return true;
    }

    private boolean verifyAdmin(ButtonRequest request) {
        if (!request.buttonInteractionEvent().getMember().hasPermission(Permission.ADMINISTRATOR)) {
            request.buttonInteraction().reply("You do not have permission to edit events.")
                    .setEphemeral(true)
                    .queue();
            return false;
        }
        return true;
    }

    private boolean verifyArgs(SelectionRequest request) {
        if (request.idArguments().isEmpty()) {
            request.interaction().reply("Failed to parse identifier from selector").setEphemeral(true).queue();
            return false;
        }
        return true;
    }

    private boolean verifyAdmin(SelectionRequest request) {
        if (!request.interaction().getMember().hasPermission(Permission.ADMINISTRATOR)) {
            request.interaction().reply("You do not have permission to edit events.")
                    .setEphemeral(true)
                    .queue();
            return false;
        }
        return true;
    }

    public void publishEvent(ButtonRequest request, EventDraftJpa draftJpa) {
        publishEvent(request, eventRepository.save(EventJpa.create(draftJpa)));
        eventDraftRepository.deleteById(draftJpa.getId());
        schedulingService.removeDraftCleanupTrigger(draftJpa.getId());
    }

    public void publishEvent(ButtonRequest request, EventJpa eventJpa) {
        for (CourseJpa courseJpa : eventJpa.getCourses()) {
            courseService.scheduleEventForCourse(eventJpa, courseJpa);
        }

        Long eventChannel = configService.getConfigJpa().getEventChannel();
        Optional<TextChannel> channel = Optional.ofNullable(request.buttonInteractionEvent().getJDA().getTextChannelById(eventChannel));
        if (channel.isEmpty()) {
            throw new ConfigErrorException("The event channel could not be found. Please contact a bot administrator.");
        }

        channel.get().sendMessageEmbeds(EmbedBuilderFactory.eventEmbed(eventJpa, request.requester().getAsMention()))
                .addActionRow(
                        Button.success(Utils.createInteractionIdString(ButtonAction.MARK_COMPLETE, eventJpa.getId()), "Mark Complete"),
                        Button.primary(Utils.createInteractionIdString(ButtonAction.EDIT_EVENT, eventJpa.getId()), "Edit Event"),
                        Button.danger(Utils.createInteractionIdString(ButtonAction.DELETE_EVENT, eventJpa.getId()), "Delete Event")
                )
                .queue(success -> {
                    eventJpa.setMessageId(success.getIdLong());
                    eventRepository.save(eventJpa);
                    request.buttonInteractionEvent().reply("Event has been posted to the event channel. %s".formatted(success.getJumpUrl()))
                            .setEphemeral(true)
                            .queue();
                    log.info("New event published (id:%d) in channel %s".formatted(eventJpa.getId(), success.getChannel().getName()));
                });
    }

    public void sendDeleteConformation(ButtonRequest request) {
        if (!verifyArgs(request) && !verifyAdmin(request)) {
            return;
        }

        EventJpa eventJpa = eventRepository.findById(Long.valueOf(request.idArguments().get(0)))
                .orElseThrow(() -> new EntityNotFoundException("Failed to find event in the database."));

        request.buttonInteraction()
                .replyModal(ModalFactory.deleteDraftReasonModal(eventJpa))
                .queue();
    }

    public void deleteEvent(ModalRequest request) {
        if (!verifyArgs(request) && !verifyAdmin(request)) {
            return;
        }

        EventJpa eventJpa = eventRepository.findById(Long.parseLong(request.idArguments().get(0)))
                .orElseThrow(() -> new EntityNotFoundException("Failed to find an event within the database from modal %s".formatted(request.interaction().getModalId())));

        if (eventJpa.getIsDeleted()) {
            request.interaction().reply("Event is already deleted.").setEphemeral(true).queue();
            return;
        }

        String reason = Optional.ofNullable(request.interaction().getValue(REASON_ID))
                .map(ModalMapping::getAsString)
                .orElseThrow(() -> new EventParsingException("Failed to parse field from modal"));

        request.event().getChannel().retrieveMessageById(eventJpa.getMessageId()).queue(message -> {
            String jumpUrl = message.getJumpUrl();

            schedulingService.stripReminderTriggers(eventJpa.getId());
            eventJpa.setIsDeleted(true);
            eventRepository.save(eventJpa);

            request.interaction().getChannel().sendMessageEmbeds(
                            EmbedBuilderFactory.deleteEvent(
                                    reason,
                                    jumpUrl,
                                    request.requester().getAsMention(),
                                    deletedEventCleanupDelay
                            )
                    ).addActionRow(
                            Button.primary(Utils.createInteractionIdString(ButtonAction.UNDO_EVENT_DELETION, eventJpa.getId()), "Recover Event")
                    )
                    .queue(success -> schedulingService.addDeletedEventCleanupTrigger(
                                eventJpa.getId(),
                                success.getIdLong(),
                                LocalDateTime.now().plusHours(deletedEventCleanupDelay)
                    ));

            request.interaction().reply("Event has been deleted").setEphemeral(true).queue();
        }, new ErrorHandler().handle(ErrorResponse.UNKNOWN_MESSAGE, e -> {
            request.interaction().reply("Failed to retrieve message tied to event.").setEphemeral(true).queue();
        }));
    }

    public void undoEventDeletion(ButtonRequest request) {
        if (!verifyArgs(request)) {
            return;
        }

        EventJpa eventJpa = eventRepository.findById(Long.parseLong(request.idArguments().get(0)))
                .orElseThrow(() -> new EntityNotFoundException("Failed to find event in the database. Event id: %s".formatted(request.idArguments().get(0))));
        eventJpa.setIsDeleted(false);
        schedulingService.removeDeletedEventCleanupTrigger(eventJpa.getId());

        for (CourseJpa courseJpa : eventJpa.getCourses()) {
            courseService.scheduleEventForCourse(eventJpa, courseJpa);
        }

        request.buttonInteraction().reply("Event has been restored").setEphemeral(true).queue();
        request.buttonInteraction().getMessage().delete().queue();

        request.buttonInteraction().getChannel().retrieveMessageById(eventJpa.getMessageId())
                        .queue(message ->
                                message.replyEmbeds(EmbedBuilderFactory.eventRestored(request.requester().getAsMention())).queue()
                        );

    }

    public void markEventComplete(ButtonRequest request) {
        if (!verifyArgs(request)) {
            return;
        }

        Long eventId = Long.parseLong(request.idArguments().get(0));

        if (!eventRepository.existsById(eventId)) {
            request.buttonInteraction().reply("Failed to find event in the database").setEphemeral(true).queue();
            return;
        }

        studentService.unscheduleStudentForEvent(request, eventId);
    }

    public void sendEventEditOptions(ButtonRequest request) {
        if (!verifyArgs(request) && !verifyAdmin(request)) {
            return;
        }

        EventJpa eventJpa = eventRepository.findById(Long.parseLong(request.idArguments().get(0)))
                .orElseThrow(() -> new EntityNotFoundException("Failed to find event in the database"));

        request.buttonInteraction().replyComponents(
                ActionRow.of(
                    Button.primary(Utils.createInteractionIdString(ButtonAction.EDIT_EVENT_DETAILS, eventJpa.getId()), "Edit Details"),
                    Button.primary(Utils.createInteractionIdString(ButtonAction.EDIT_EVENT_COURSES, eventJpa.getId()), "Edit Courses")
                )
        ).setEphemeral(true).queue();
    }

    public void sendEditEventDetails(ButtonRequest request) {
        if (!verifyArgs(request) && !verifyAdmin(request)) {
            return;
        }

        EventJpa eventJpa = eventRepository.findById(Long.parseLong(request.idArguments().get(0)))
                .orElseThrow(() -> new EntityNotFoundException("Failed to find event in the database"));

        request.buttonInteraction().replyModal(ModalFactory.editDetailsModal(eventJpa)).queue();
    }

    public void sendEventEditCourses(ButtonRequest request) {
        if (!verifyArgs(request) && !verifyAdmin(request)) {
            return;
        }

        EventJpa eventJpa = eventRepository.findById(Long.parseLong(request.idArguments().get(0)))
                .orElseThrow(() -> new EntityNotFoundException("Failed to find event in the database"));

        request.buttonInteraction().replyEmbeds(EmbedBuilderFactory.coursePicker("Select the courses for the event."))
                .addActionRow(
                        courseService.createCourseSelector(
                                Utils.createInteractionIdString(Selection.EDIT_EVENT_COURSES, eventJpa.getId()),
                                eventJpa.getCourses()
                        )
                )
                .setEphemeral(true)
                .queue();
    }

    public void editEventDetails(ModalRequest request) {
        if (!verifyArgs(request) && !verifyAdmin(request)) {
            return;
        }

        EventJpa eventJpa = eventRepository.findById(Long.parseLong(request.idArguments().get(0)))
                .orElseThrow(() -> new EntityNotFoundException("Failed to find event in the database"));

        String title = Optional.ofNullable(request.interaction().getValue(TITLE_ID))
                .map(ModalMapping::getAsString)
                .orElseThrow(() -> new EventParsingException("Failed to parse title from modal"));

        String note = Optional.ofNullable(request.interaction().getValue(NOTE_ID))
                .map(ModalMapping::getAsString)
                .orElse(null);

        String dateString = Optional.ofNullable(request.interaction().getValue(DATE_ID))
                .map(ModalMapping::getAsString)
                .orElseThrow(() -> new EventParsingException("Failed to parse date from modal"));

        String timeString = Optional.ofNullable(request.interaction().getValue(TIME_ID))
                .map(ModalMapping::getAsString)
                .orElseThrow(() -> new EventParsingException("Failed to parse time from modal"));

        LocalDateTime eventDate;
        try {
            eventDate = Utils.parseDate(dateString, timeString);
        } catch (EventParsingException e) {
            request.event().reply(e.getMessage())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        EmbedBuilder embedBuilder = EmbedBuilderFactory.eventEditsStarter();

        if (!title.equals(eventJpa.getTitle())) {
            embedBuilder.addField(
                    "Original Title",
                    eventJpa.getTitle(),
                    false
            ).addField(
                    "Updated Title",
                    title,
                    false
            );
            eventJpa.setTitle(title);
        }
        if (!StringUtils.equals(note, eventJpa.getNote())) {
            if (StringUtils.isBlank(eventJpa.getNote()) && !StringUtils.isBlank(note)) {
                embedBuilder.addField(
                        "Added Note",
                        note,
                        false
                );
            } else {
                embedBuilder.addField(
                        "Original Note",
                        eventJpa.getNote(),
                        false
                ).addField(
                        "Updated Note",
                        StringUtils.isBlank(note) ? "*Note was removed*" : note,
                        false
                );
            }
            eventJpa.setNote(StringUtils.isBlank(note) ? null : note);
        }
        if (!eventDate.equals(eventJpa.getDatetime())) {
            embedBuilder.addField(
                    "Original Date",
                    Utils.formattedDateTime(eventJpa.getDatetime()),
                    false
            ).addField(
                    "Updated Date",
                    Utils.formattedDateTime(eventDate),
                    false
            );
            eventJpa.setDatetime(eventDate);
            schedulingService.stripReminderTriggers(eventJpa.getId());
            for (CourseJpa courseJpa : eventJpa.getCourses()) {
                courseService.scheduleEventForCourse(eventJpa, courseJpa);
            }
        }

        eventJpa = eventRepository.save(eventJpa);

        EventJpa finalEventJpa = eventJpa;
        request.interaction().getChannel().retrieveMessageById(eventJpa.getMessageId()).queue(
                message -> {
                    message.editMessageEmbeds(EmbedBuilderFactory.eventEmbed(finalEventJpa, request.requester().getAsMention())).queue();
                    message.replyEmbeds(embedBuilder.build()).queue();
                    request.interaction().reply("The event has been updated. %s".formatted(message.getJumpUrl())).setEphemeral(true).queue();
                }
        );
    }

    public void editEventCourses(SelectionRequest request) {
        if (!verifyArgs(request) && !verifyAdmin(request)) {
            return;
        }

        EventJpa eventJpa = eventRepository.findById(Long.parseLong(request.idArguments().get(0)))
                .orElseThrow(() -> new EntityNotFoundException("Failed to find event in the database"));

        List<CourseJpa> selectedCourses = courseService.coursesFromSelectionMenuValues(request.interaction().getValues());

        EmbedBuilder embedBuilder = EmbedBuilderFactory.eventEditsStarter();

        embedBuilder.addField(
                "Original Courses",
                eventJpa.getCourses().stream().map(CourseJpa::getCode).collect(Collectors.joining("\n")),
                false
        ).addField(
                "Updated Courses",
                selectedCourses.stream().map(CourseJpa::getCode).collect(Collectors.joining("\n")),
                false
        );

        eventJpa.getCourses().clear();
        eventJpa.setCourses(selectedCourses);

        schedulingService.stripReminderTriggers(eventJpa.getId());
        for (CourseJpa courseJpa : eventJpa.getCourses()) {
            courseService.scheduleEventForCourse(eventJpa, courseJpa);
        }

        request.interaction().getChannel().retrieveMessageById(eventJpa.getMessageId()).queue(
                message -> {
                    message.editMessageEmbeds(EmbedBuilderFactory.eventEmbed(eventJpa, request.requester().getAsMention())).queue();
                    message.replyEmbeds(embedBuilder.build()).queue();
                    request.interaction().reply("The event has been updated. %s".formatted(message.getJumpUrl())).setEphemeral(true).queue();
                }
        );
    }

}
