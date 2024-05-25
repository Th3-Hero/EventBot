package com.th3hero.eventbot.jobs;


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
public class EventReminderJob implements Job {
    public static final JobKey JOB_KEY = JobKey.jobKey("event_reminder");
    public static final String STUDENT_ID = "student_id";
    public static final String EVENT_ID = "event_id";
    public static final String OFFSET_ID = "offset_id";

    private final JDA jda;
    private final EventRepository eventRepository;
    private final ConfigService configService;

    @SneakyThrows
    @Override
    public void execute(JobExecutionContext executionContext) {
        Long eventId = executionContext.getTrigger().getJobDataMap().getLong(EVENT_ID);
        final EventJpa eventJpa = eventRepository.findById(eventId)
            .orElseThrow(() -> new EntityNotFoundException("Failed to find event in the database when trying to send a reminder. Event id: %d".formatted(eventId)));


        TextChannel channel = Optional.ofNullable(jda.getTextChannelById(configService.getConfigJpa().getEventChannel()))
            .orElseThrow(() -> new ConfigErrorException("Failed to find event channel in the database when trying to send a reminder. Make sure config is setup correctly"));


        Long userId = executionContext.getTrigger().getJobDataMap().getLong(STUDENT_ID);
        int offset = executionContext.getTrigger().getJobDataMap().getInt(OFFSET_ID);

        DiscordActionUtils.retrieveMessage(
            channel,
            eventJpa.getMessageId(),
            message -> sendUserReminder(userId, offset, message.getJumpUrl()),
            err -> log.warn("Failed to find message for event %d".formatted(eventId))
        );
    }

    private void sendUserReminder(Long userId, int offset, String jumpUrl) {
        Optional.ofNullable(jda.getUserById(userId)).ifPresentOrElse(
            user -> user.openPrivateChannel().queue(privateChannel ->
                privateChannel.sendMessage("%d hour reminder for event: %s".formatted(offset, jumpUrl)).queue(
                    message -> log.debug("Sent notification to user %d".formatted(userId)),
                    err -> log.warn("Cannot send private message to user %d".formatted(userId))
                )
            ),
            () -> log.warn("Failed to find user %d to message".formatted(userId))
        );
    }

}
