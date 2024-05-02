package com.th3hero.eventbot.services;

import com.th3hero.eventbot.jobs.DraftCleanupJob;
import com.th3hero.eventbot.jobs.EventReminderJob;
import com.th3hero.eventbot.utils.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.SchedulingException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulingService {
    private final Scheduler scheduler;

    @Value("${app.config.draft-cleanup-delay}")
    private int draftCleanupDelay;

    public void addDraftCleanupTrigger(Long draftId, LocalDateTime draftCreationDate) {
        try {
            if (!scheduler.checkExists(DraftCleanupJob.JOB_KEY)) {
                JobDetail cleanupJob = JobBuilder.newJob(DraftCleanupJob.class)
                        .withIdentity(DraftCleanupJob.JOB_KEY)
                        .withDescription("Automated cleanup of abandoned jobs")
                        .storeDurably()
                        .build();
                scheduler.addJob(cleanupJob, true);
                log.info("Added cleanup job");
            }
            TriggerKey key = TriggerKey.triggerKey(draftId.toString());
            Trigger cleanupTrigger = TriggerBuilder.newTrigger()
                    .withIdentity(key)
                    .forJob(DraftCleanupJob.JOB_KEY)
                    .startAt(Utils.toDate(draftCreationDate.plusHours(draftCleanupDelay)))
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withMisfireHandlingInstructionFireNow()
                            .withRepeatCount(0))
                    .build();
            scheduler.scheduleJob(cleanupTrigger);
            log.info("Added new draft cleanup trigger for draft: %d".formatted(draftId));
        } catch (SchedulerException e) {
            log.error("Failed to add trigger for draft cleanup of draft: %d".formatted(draftId), e);
            throw new SchedulingException("Failed to schedule draft cleanup.");
        }
    }

    public void removeDraftCleanupTrigger(Long draftId) {
        try {
            scheduler.unscheduleJob(TriggerKey.triggerKey(draftId.toString()));
            log.info("Removed cleanup trigger for draft: %s".formatted(draftId));
        } catch (SchedulerException e) {
            log.error("Failed to remove cleanup trigger for draft: %d".formatted(draftId), e);
        }
    }

    public void addEventReminderTrigger(Long eventId, Long studentId, Integer additionalTriggerIdentifier, LocalDateTime eventTime) {
        try {
            if (!scheduler.checkExists(EventReminderJob.JOB_KEY)) {
                JobDetail reminderJob = JobBuilder.newJob(EventReminderJob.class)
                        .withIdentity(EventReminderJob.JOB_KEY)
                        .withDescription("Reminder notifications for events")
                        .storeDurably()
                        .build();
                scheduler.addJob(reminderJob, true);
                log.info("Added reminder job");
            }

            String groupKey = "%d-%d".formatted(eventId, studentId);
            TriggerKey triggerKey = TriggerKey.triggerKey(additionalTriggerIdentifier.toString(), groupKey);

            Trigger reminderTrigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .forJob(EventReminderJob.JOB_KEY)
                    .startAt(Utils.toDate(eventTime))
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withMisfireHandlingInstructionFireNow()
                            .withRepeatCount(0))
                    .build();
            scheduler.scheduleJob(reminderTrigger);
            log.info("Added new reminder trigger for event: %d".formatted(eventId));

        } catch (SchedulerException e) {
            log.error("Failed to add trigger for event reminder: %d".formatted(eventId), e);
            throw new SchedulingException("Failed to schedule event reminder.");
        }
    }

    public void removeEventReminderTriggers(Long eventId, Long studentId) {
        try {
            String groupKey = "%d-%d".formatted(eventId, studentId);
            Set<TriggerKey> triggers = scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(groupKey));
            for (TriggerKey triggerKey : triggers) {
                scheduler.unscheduleJob(triggerKey);
            }
            log.info("Removed all reminder triggers for event: %d".formatted(eventId));
        } catch (SchedulerException e) {
            log.error("Failed to remove reminder trigger for event: %d".formatted(eventId), e);
        }
    }
}
