package com.th3hero.eventbot.services;

import com.th3hero.eventbot.jobs.DeletedEventCleanupJob;
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
import java.util.ArrayList;
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
            log.debug("Added new draft cleanup trigger for draft: %d".formatted(draftId));
        } catch (SchedulerException e) {
            log.error("Failed to add trigger for draft cleanup of draft: %d".formatted(draftId), e);
            throw new SchedulingException("Failed to schedule draft cleanup.");
        }
    }

    public void removeDraftCleanupTrigger(Long draftId) {
        try {
            scheduler.unscheduleJob(TriggerKey.triggerKey(draftId.toString()));
            log.debug("Removed cleanup trigger for draft: %s".formatted(draftId));
        } catch (SchedulerException e) {
            log.error("Failed to remove cleanup trigger for draft: %d".formatted(draftId), e);
        }
    }

    public void addEventReminderTrigger(Long eventId, Long studentId, Integer offset, LocalDateTime eventTime) {
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
            TriggerKey triggerKey = TriggerKey.triggerKey(offset.toString(), groupKey);

            if (scheduler.checkExists(triggerKey)) {
                return;
            }

            Trigger reminderTrigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .usingJobData(EventReminderJob.STUDENT_ID, studentId)
                    .usingJobData(EventReminderJob.EVENT_ID, eventId)
                    .usingJobData(EventReminderJob.OFFSET_ID, offset)
                    .forJob(EventReminderJob.JOB_KEY)
                    .startAt(Utils.toDate(eventTime))
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withMisfireHandlingInstructionFireNow()
                            .withRepeatCount(0))
                    .build();
            scheduler.scheduleJob(reminderTrigger);
            log.debug("Added new reminder trigger for event: %d".formatted(eventId));

        } catch (SchedulerException e) {
            log.error("Failed to add trigger for event reminder: %d".formatted(eventId), e);
            throw new SchedulingException("Failed to schedule event reminder.");
        }
    }

    public boolean removeEventReminderTriggersForStudent(Long eventId, Long studentId) {
        try {
            Set<TriggerKey> keys = scheduler.getTriggerKeys(GroupMatcher.triggerGroupEndsWith(studentId.toString()));
            if (keys.isEmpty()) {
                return false;
            }
            scheduler.unscheduleJobs(new ArrayList<>(keys));
            log.debug("Removed triggers on event %d for student %s".formatted(eventId, studentId));
            return true;
        } catch (SchedulerException e) {
            log.error("Failed to remove trigger for event %d for student %d".formatted(eventId, studentId));
            throw new SchedulingException("Failed to remove event reminders.");
        }
    }

    public void stripReminderTriggers(Long eventId) {
        try {
            Set<TriggerKey> keys = scheduler.getTriggerKeys(GroupMatcher.triggerGroupStartsWith(eventId.toString()));
            scheduler.unscheduleJobs(new ArrayList<>(keys));
        } catch (SchedulerException e) {
            log.error("Failed to remove reminder trigger for event: %d".formatted(eventId), e);
        }
    }

    public void addDeletedEventCleanupTrigger(Long eventId, Long cleanupMessageId, LocalDateTime cleanupTime) {
        try {
            if (!scheduler.checkExists(DeletedEventCleanupJob.JOB_KEY)) {
                JobDetail cleanupJob = JobBuilder.newJob(DeletedEventCleanupJob.class)
                        .withIdentity(DeletedEventCleanupJob.JOB_KEY)
                        .withDescription("Cleanup of deleted events")
                        .storeDurably()
                        .build();
                scheduler.addJob(cleanupJob, true);
                log.info("Added deleted event cleanup job");
            }

            TriggerKey key = TriggerKey.triggerKey(eventId.toString());
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(key)
                    .usingJobData(DeletedEventCleanupJob.DELETION_MESSAGE_ID, cleanupMessageId)
                    .forJob(DeletedEventCleanupJob.JOB_KEY)
                    .startAt(Utils.toDate(cleanupTime))
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withMisfireHandlingInstructionFireNow()
                            .withRepeatCount(0))
                    .build();

            scheduler.scheduleJob(trigger);
            log.debug("Added cleanup trigger for deleted event with id: %d".formatted(eventId));
        } catch (SchedulerException e) {
            log.error("Failed to add cleanup trigger for deleted event id: %d".formatted(eventId), e);
            throw new SchedulingException("Failed to schedule draft cleanup.");
        }
    }

    public void removeDeletedEventCleanupTrigger(Long eventId) {
        try {
            TriggerKey key = TriggerKey.triggerKey(eventId.toString());
            scheduler.unscheduleJob(key);
            log.debug("Removed cleanup trigger for deleted event with id: %d".formatted(eventId));
        } catch (SchedulerException e) {
            log.error("Failed to remove cleanup trigger for deleted event id: %d".formatted(eventId), e);
        }
    }

}
