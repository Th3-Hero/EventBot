package com.th3hero.eventbot.services;

import com.th3hero.eventbot.commands.ButtonAction;
import com.th3hero.eventbot.commands.ButtonRequest;
import com.th3hero.eventbot.entities.CourseJpa;
import com.th3hero.eventbot.entities.EventDraftJpa;
import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.exceptions.ConfigErrorException;
import com.th3hero.eventbot.repositories.EventDraftRepository;
import com.th3hero.eventbot.repositories.EventRepository;
import com.th3hero.eventbot.utils.EmbedBuilderFactory;
import com.th3hero.eventbot.utils.Utils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Service;

import java.util.Optional;

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


}
