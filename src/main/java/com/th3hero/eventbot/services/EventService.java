package com.th3hero.eventbot.services;

import com.th3hero.eventbot.commands.actions.ButtonAction;
import com.th3hero.eventbot.commands.actions.Command;
import com.th3hero.eventbot.commands.actions.SelectionAction;
import com.th3hero.eventbot.commands.requests.*;
import com.th3hero.eventbot.commands.requests.InteractionRequest.MessageMode;
import com.th3hero.eventbot.entities.*;
import com.th3hero.eventbot.exceptions.ConfigErrorException;
import com.th3hero.eventbot.exceptions.EventParsingException;
import com.th3hero.eventbot.factories.EmbedBuilderFactory;
import com.th3hero.eventbot.factories.ModalFactory;
import com.th3hero.eventbot.factories.ResponseFactory;
import com.th3hero.eventbot.formatting.DateFormatting;
import com.th3hero.eventbot.formatting.InteractionArguments;
import com.th3hero.eventbot.repositories.EventDraftRepository;
import com.th3hero.eventbot.repositories.EventRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.th3hero.eventbot.config.DiscordFieldsConfig.*;
import static com.th3hero.eventbot.formatting.InteractionArguments.EVENT_ID;

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

    private static final String UPDATED_EVENT_MESSAGE = "The event has been updated. %s";

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean verifyAdmin(InteractionRequest request) {
        if (!request.getRequester().hasPermission(Permission.ADMINISTRATOR)) {
            request.sendResponse("This action requires administrator permissions", MessageMode.USER);
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

        //TODO figure out how to move this to new request system

        Long eventChannel = configService.getConfigJpa().getEventChannel();
        Optional<TextChannel> channel = Optional.ofNullable(request.getEvent().getJDA().getTextChannelById(eventChannel));
        if (channel.isEmpty()) {
            throw new ConfigErrorException("The event channel could not be found. Please contact a bot administrator.");
        }

        channel.get().sendMessageEmbeds(EmbedBuilderFactory.eventEmbed(eventJpa, request.getRequester().getAsMention()))
                .addActionRow(
                        Button.success(InteractionArguments.createInteractionIdString(ButtonAction.MARK_COMPLETE, eventJpa.getId()), "Mark Complete"),
                        Button.primary(InteractionArguments.createInteractionIdString(ButtonAction.EDIT_EVENT, eventJpa.getId()), "Edit Event"),
                        Button.danger(InteractionArguments.createInteractionIdString(ButtonAction.DELETE_EVENT, eventJpa.getId()), "Delete Event")
                )
                .queue(success -> {
                    eventJpa.setMessageId(success.getIdLong());
                    eventRepository.save(eventJpa);
                    request.sendResponse("Event has been posted to the event channel. %s".formatted(success.getJumpUrl()), MessageMode.USER);
                    log.info("New event published (id:%d) in channel %s".formatted(eventJpa.getId(), success.getChannel().getName()));
                });
    }

    public void sendDeleteConformation(ButtonRequest request) {
        if (!verifyAdmin(request)) {
            return;
        }

        EventJpa eventJpa = eventRepository.findById(request.getArguments().get(EVENT_ID))
                .orElseThrow(() -> new EntityNotFoundException("Failed to find event in the database."));

        request.sendResponse(ModalFactory.deleteDraftReasonModal(eventJpa), MessageMode.USER);
    }

    public void deleteEvent(ModalRequest request) {
        EventJpa eventJpa = eventRepository.findById(request.getArguments().get(EVENT_ID))
                .orElseThrow(() -> new EntityNotFoundException("Failed to find an event within the database from modal %s".formatted(request.getEvent().getModalId())));

        if (eventJpa.getDeleted()) {
            request.sendResponse("Event is already deleted.", MessageMode.USER);
            return;
        }

        //TODO figure out how to move this to new request system

        String reason = Optional.ofNullable(request.getEvent().getValue(REASON_ID))
                .map(ModalMapping::getAsString)
                .orElseThrow(() -> new EventParsingException("Failed to parse field from modal"));

        int deletedEventCleanupDelay = configService.getConfigJpa().getDeletedEventCleanupDelay();

        request.getEvent().getChannel().retrieveMessageById(eventJpa.getMessageId()).queue(message -> {
            String jumpUrl = message.getJumpUrl();

            schedulingService.stripReminderTriggers(eventJpa.getId());
            eventJpa.setDeleted(true);
            eventRepository.save(eventJpa);

            request.getEvent().getChannel().sendMessageEmbeds(
                            EmbedBuilderFactory.deleteEvent(
                                    reason,
                                    jumpUrl,
                                    request.getRequester().getAsMention(),
                                    deletedEventCleanupDelay
                            )
                    ).addActionRow(
                            Button.primary(InteractionArguments.createInteractionIdString(ButtonAction.UNDO_EVENT_DELETION, eventJpa.getId()), "Recover Event")
                    )
                    .queue(success -> schedulingService.addDeletedEventCleanupTrigger(
                                eventJpa.getId(),
                                success.getIdLong(),
                                LocalDateTime.now().plusHours(deletedEventCleanupDelay)
                    ));

            request.sendResponse("Event has been deleted. %s".formatted(jumpUrl), MessageMode.USER);
        }, new ErrorHandler().handle(ErrorResponse.UNKNOWN_MESSAGE, e -> {
            request.sendResponse("Failed to retrieve message tied to event.", MessageMode.USER);
        }));
    }

    public void undoEventDeletion(ButtonRequest request) {
        EventJpa eventJpa = eventRepository.findById(request.getArguments().get(EVENT_ID))
                .orElseThrow(() -> new EntityNotFoundException("Failed to find event in the database. Event id: %s".formatted(request.getArguments().get(EVENT_ID))));
        eventJpa.setDeleted(false);
        schedulingService.removeDeletedEventCleanupTrigger(eventJpa.getId());

        for (CourseJpa courseJpa : eventJpa.getCourses()) {
            courseService.scheduleEventForCourse(eventJpa, courseJpa);
        }


        request.sendResponse("Event has been restored.", MessageMode.USER);

        //TODO figure out if this needs to change further

        request.getEvent().getMessage().delete().queue();

        request.getEvent().getChannel().retrieveMessageById(eventJpa.getMessageId())
                        .queue(message ->
                                message.replyEmbeds(EmbedBuilderFactory.eventRestored(request.getRequester().getAsMention())).queue()
                        );

    }

    public void markEventComplete(ButtonRequest request) {
        Long eventId = request.getArguments().get(EVENT_ID);
        if (!eventRepository.existsById(eventId)) {
            request.sendResponse("Failed to find event in the database", MessageMode.USER);
            return;
        }
        studentService.unscheduleStudentForEvent(request, eventId);
    }

    public void sendEventEditOptions(ButtonRequest request) {
        if (!verifyAdmin(request)) {
            return;
        }

        EventJpa eventJpa = eventRepository.findById(request.getArguments().get(EVENT_ID))
                .orElseThrow(() -> new EntityNotFoundException("Failed to find event in the database"));

        request.sendResponse(
                ActionRow.of(
                        Button.primary(InteractionArguments.createInteractionIdString(ButtonAction.EDIT_EVENT_DETAILS, eventJpa.getId()), "Edit Details"),
                        Button.primary(InteractionArguments.createInteractionIdString(ButtonAction.EDIT_EVENT_COURSES, eventJpa.getId()), "Edit Courses")
                ),
                MessageMode.USER
        );
    }

    public void sendEditEventDetails(ButtonRequest request) {
        EventJpa eventJpa = eventRepository.findById(request.getArguments().get(EVENT_ID))
                .orElseThrow(() -> new EntityNotFoundException("Failed to find event in the database"));

        request.sendResponse(ModalFactory.editDetailsModal(eventJpa), MessageMode.USER);
    }

    public void sendEventEditCourses(ButtonRequest request) {
        EventJpa eventJpa = eventRepository.findById(request.getArguments().get(EVENT_ID))
                .orElseThrow(() -> new EntityNotFoundException("Failed to find event in the database"));

        request.sendResponse(
                ResponseFactory.createResponse(
                        EmbedBuilderFactory.coursePicker("Select the courses for the event."),
                        courseService.createCourseSelector(
                                InteractionArguments.createInteractionIdString(SelectionAction.EDIT_EVENT_COURSES, eventJpa.getId()),
                                eventJpa.getCourses()
                        )
                ),
                MessageMode.USER
        );
    }

    public void editEventDetails(ModalRequest request) {
        EventJpa eventJpa = eventRepository.findById(request.getArguments().get(EVENT_ID))
                .orElseThrow(() -> new EntityNotFoundException("Failed to find event in the database"));

        String title = Optional.ofNullable(request.getEvent().getValue(TITLE_ID))
                .map(ModalMapping::getAsString)
                .orElseThrow(() -> new EventParsingException("Failed to parse title from modal"));

        String note = Optional.ofNullable(request.getEvent().getValue(NOTE_ID))
                .map(ModalMapping::getAsString)
                .orElse(null);

        String dateString = Optional.ofNullable(request.getEvent().getValue(DATE_ID))
                .map(ModalMapping::getAsString)
                .orElseThrow(() -> new EventParsingException("Failed to parse date from modal"));

        String timeString = Optional.ofNullable(request.getEvent().getValue(TIME_ID))
                .map(ModalMapping::getAsString)
                .orElseThrow(() -> new EventParsingException("Failed to parse time from modal"));

        Optional<LocalDateTime> eventDate = DateFormatting.parseDate(dateString, timeString);
        if (eventDate.isEmpty()) {
            request.sendResponse("Failed to parse date and time", MessageMode.USER);
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
        if (!eventDate.get().equals(eventJpa.getDatetime())) {
            embedBuilder.addField(
                    "Original Date",
                    DateFormatting.formattedDateTime(eventJpa.getDatetime()),
                    false
            ).addField(
                    "Updated Date",
                    DateFormatting.formattedDateTime(eventDate.get()),
                    false
            );
            eventJpa.setDatetime(eventDate.get());
            schedulingService.stripReminderTriggers(eventJpa.getId());
            for (CourseJpa courseJpa : eventJpa.getCourses()) {
                courseService.scheduleEventForCourse(eventJpa, courseJpa);
            }
        }

        eventJpa = eventRepository.save(eventJpa);

        EventJpa finalEventJpa = eventJpa;
        request.getEvent().getChannel().retrieveMessageById(eventJpa.getMessageId()).queue(
                message -> {
                    message.editMessageEmbeds(EmbedBuilderFactory.eventEmbed(finalEventJpa, request.getRequester().getAsMention())).queue();
                    message.replyEmbeds(embedBuilder.build()).queue();
                    request.sendResponse(UPDATED_EVENT_MESSAGE.formatted(message.getJumpUrl()), MessageMode.USER);
                }
        );
    }

    public void editEventCourses(SelectionRequest request) {
        EventJpa eventJpa = eventRepository.findById(request.getArguments().get(EVENT_ID))
                .orElseThrow(() -> new EntityNotFoundException("Failed to find event in the database"));

        List<CourseJpa> selectedCourses = courseService.coursesFromSelectionMenuValues(request.getEvent().getValues());

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

        request.getEvent().getChannel().retrieveMessageById(eventJpa.getMessageId()).queue(
                message -> {
                    message.editMessageEmbeds(EmbedBuilderFactory.eventEmbed(eventJpa, request.getRequester().getAsMention())).queue();
                    message.replyEmbeds(embedBuilder.build()).queue();
                    request.sendResponse(UPDATED_EVENT_MESSAGE.formatted(message.getJumpUrl()), MessageMode.USER);
                }
        );
    }

    public void listEvents(CommandRequest request) {
        StudentJpa studentJpa = studentService.fetchStudent(request.getRequester().getIdLong());
        if (studentJpa.getCourses().isEmpty()) {
            request.sendResponse(
                    "You are not signed up for any courses with the bot. Please use `%s`".formatted(Command.SELECT_COURSES.getDisplayName()),
                    MessageMode.USER
            );
            return;
        }

        request.deferReply(MessageMode.USER);

        String courseField = request.getArguments().get(COURSE_ID);

        Integer timePeriodField = null;
        if (request.getArguments().get(TIME_PERIOD_ID) != null) {
            timePeriodField = Integer.parseInt(request.getArguments().get(TIME_PERIOD_ID));
        }

        Integer upcomingField = null;
        if (request.getArguments().get(UPCOMING_ID) != null) {
            upcomingField = Integer.parseInt(request.getArguments().get(UPCOMING_ID));
        }

        List<CourseJpa> courses;
        if (courseField == null) {
            courses = studentJpa.getCourses();
        } else {
            Optional<CourseJpa> courseJpa = courseService.parseCourseCodeToCourseJpa(courseField);
            if (courseJpa.isEmpty()) {
                request.sendResponse("Unable to find course matching '%s'".formatted(courseField), MessageMode.USER);
                return;
            }

            courses = List.of(courseJpa.get());
        }

        List<EventJpa> events = sortEvents(eventRepository.findAllByCourse(courses));
        if (events.isEmpty()) {
            request.sendResponse("There are no events scheduled for your courses.", MessageMode.USER);
            return;
        }

        if (timePeriodField != null) {
            LocalDateTime minTimePeriod = LocalDateTime.now();
            LocalDateTime maxTimePeriod = minTimePeriod.plusDays(timePeriodField);
            events = events.stream()
                    .filter(event -> event.getDatetime().isAfter(minTimePeriod) && event.getDatetime().isBefore(maxTimePeriod))
                    .toList();
        }

        if (upcomingField != null) {
            events = sortEvents(events).subList(0, Math.min(upcomingField, events.size()));
        }

        ConfigJpa configJpa = configService.getConfigJpa();

        TextChannel channel = request.getEvent().getJDA().getTextChannelById(configJpa.getEventChannel());

        Map<EventJpa, CompletableFuture<Message>> futureMap = new LinkedHashMap<>();
        for (EventJpa event : events) {
            futureMap.put(event, channel.retrieveMessageById(event.getMessageId()).submit());
        }

        List<CompletableFuture<Message>> futures = events.stream()
                .map(eventJpa -> channel.retrieveMessageById(eventJpa.getMessageId()).submit())
                .toList();

        List<EventJpa> finalEvents = events;
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    Map<EventJpa, String> eventMessageMap = new LinkedHashMap<>();
                    for (EventJpa event : finalEvents) {
                        eventMessageMap.put(event, futureMap.get(event).join().getJumpUrl());
                    }

                    request.sendResponse(EmbedBuilderFactory.eventList(eventMessageMap), MessageMode.USER);
                });
    }

    public List<EventJpa> sortEvents(List<EventJpa> events) {
        if (events.isEmpty()) {
            return List.of();
        }
        LocalDateTime now = LocalDateTime.now();
        return events.stream()
                .sorted((event1, event2) -> {
                    long diff1 = ChronoUnit.SECONDS.between(now, event1.getDatetime());
                    long diff2 = ChronoUnit.SECONDS.between(now, event2.getDatetime());
                    return Long.compare(Math.abs(diff1), Math.abs(diff2));
                })
                .toList();
    }

}
