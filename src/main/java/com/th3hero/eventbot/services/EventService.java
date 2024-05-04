package com.th3hero.eventbot.services;

import com.th3hero.eventbot.commands.ButtonAction;
import com.th3hero.eventbot.commands.ButtonRequest;
import com.th3hero.eventbot.commands.ModalRequest;
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
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.RestAction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.th3hero.eventbot.config.DiscordFieldsConfig.REASON_ID;

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

    @Value("${app.config.deleted-event-cleanup-delay}")
    private int deletedEventCleanupDelay;

    public void eventFromDraft(ButtonRequest request, EventDraftJpa draftJpa) {
        EventJpa eventJpa = eventRepository.save(EventJpa.create(draftJpa));

        for (CourseJpa courseJpa : eventJpa.getCourses()) {
            courseService.scheduleEventForCourse(eventJpa, courseJpa);
        }

        Long eventChannel = configService.getConfigJpa().getEventChannel();
        Optional<TextChannel> channel = Optional.ofNullable(request.buttonInteractionEvent().getJDA().getTextChannelById(eventChannel));
        if (channel.isEmpty()) {
            throw new ConfigErrorException("The event channel could not be found. Please contact an administrator.");
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
                    eventDraftRepository.deleteById(draftJpa.getId());
                    schedulingService.removeDraftCleanupTrigger(draftJpa.getId());
                    log.info("New event published (id:%d) in channel %s".formatted(eventJpa.getId(), success.getChannel().getName()));
                });
    }

    public void sendDeleteConformation(ButtonRequest request) {
        if (!request.buttonInteractionEvent().getMember().hasPermission(Permission.ADMINISTRATOR)) {
            request.buttonInteractionEvent().reply("You do not have permission to delete events.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (request.idArguments().isEmpty()) {
            throw new EventParsingException("Failed to parse identifier from button");
        }

        EventJpa eventJpa = eventRepository.findById(Long.valueOf(request.idArguments().get(0)))
                .orElseThrow(() -> new EntityNotFoundException("Failed to find event in the database."));

        request.buttonInteraction()
                .replyModal(ModalFactory.deleteDraftReasonModal(eventJpa))
                .queue();
    }

    public void deleteEvent(ModalRequest request) {
        if (request.idArguments().isEmpty()) {
            request.interaction().reply("Failed to parse identifier from modal").setEphemeral(true).queue();
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

}
