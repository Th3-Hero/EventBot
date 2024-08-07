package com.th3hero.eventbot.jobs;


import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.repositories.EventRepository;
import com.th3hero.eventbot.services.ConfigService;
import com.th3hero.eventbot.utils.DiscordUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
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

        if (!eventJpa.getStatus().equals(EventJpa.EventStatus.ACTIVE)) {
            throw new IllegalStateException("Attempted to send a reminder for an event that is not active. Event id: %s, Status: %s".formatted(eventId, eventJpa.getStatus().toString()));
        }

        Long userId = executionContext.getTrigger().getJobDataMap().getLong(STUDENT_ID);
        int offset = executionContext.getTrigger().getJobDataMap().getInt(OFFSET_ID);

        String eventJumpUrl = DiscordUtils.generateJumpUrl(configService.getConfigJpa(), eventJpa.getMessageId());

        sendUserReminder(userId, offset, eventJumpUrl);
    }

    private void sendUserReminder(Long userId, int offset, String jumpUrl) {
        Optional.ofNullable(jda.getUserById(userId)).ifPresentOrElse(
            user -> user.openPrivateChannel().queue(privateChannel ->
                privateChannel.sendMessage("%d hour reminder for event: %s".formatted(offset, jumpUrl)).queue(
                    message -> log.debug("Sent notification to user {}", userId),
                    err -> log.warn("Cannot send private message to user {}", userId)
                )
            ),
            () -> log.debug("Failed to message user {}", userId)
        );
    }

}
