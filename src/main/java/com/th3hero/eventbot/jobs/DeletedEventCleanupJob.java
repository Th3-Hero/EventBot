package com.th3hero.eventbot.jobs;

import com.th3hero.eventbot.entities.ConfigJpa;
import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.exceptions.ConfigErrorException;
import com.th3hero.eventbot.repositories.EventRepository;
import com.th3hero.eventbot.services.ConfigService;
import com.th3hero.eventbot.utils.DiscordActionUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class DeletedEventCleanupJob implements Job {
    public static final JobKey JOB_KEY = JobKey.jobKey("deleted_event_cleanup");

    public static final String DELETION_MESSAGE_ID = "deletion_message_id";
    public static final String EVENT_ID = "event_id";

    private final JDA jda;
    private final EventRepository eventRepository;
    private final ConfigService configService;

    @SneakyThrows
    @Override
    public void execute(JobExecutionContext executionContext) {
        Long eventId = executionContext.getTrigger().getJobDataMap().getLong(EVENT_ID);
        final EventJpa eventJpa = eventRepository.findById(eventId)
            .orElseThrow(() -> new EntityNotFoundException("Attempted to clean up deleted event that does not exist. Event id: %d".formatted(eventId)));

        final ConfigJpa config = configService.getConfigJpa();

        eventRepository.delete(eventJpa);

        TextChannel channel = Optional.ofNullable(jda.getTextChannelById(config.getEventChannel()))
            .orElseThrow(() -> new ConfigErrorException("Failed to get event channel. Make sure config is setup correctly. Channel id: %d".formatted(config.getEventChannel())));

        DiscordActionUtils.deleteMessage(
            channel,
            eventJpa.getMessageId(),
            success -> log.info("Cleaned up deleted event %d".formatted(eventId)),
            e -> log.warn("Failed to find event message for cleanup. The message may have already been deleted outside of the bot. Otherwise something has went wrong. Message id: %d".formatted(eventJpa.getMessageId()))
        );

        long deletionMessage = executionContext.getTrigger().getJobDataMap().getLongFromString(DELETION_MESSAGE_ID);

        DiscordActionUtils.deleteMessage(
            channel,
            deletionMessage,
            success -> log.info("Cleaned up deleted event %d".formatted(eventId)),
            e -> log.warn("Failed to find deletion message")
        );
    }
}
