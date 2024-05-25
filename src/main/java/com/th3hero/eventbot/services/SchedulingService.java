package com.th3hero.eventbot.services;

import com.th3hero.eventbot.formatting.DateFormatter;
import com.th3hero.eventbot.jobs.DeletedEventCleanupJob;
import com.th3hero.eventbot.jobs.DraftCleanupJob;
import com.th3hero.eventbot.jobs.EventReminderJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.scheduling.SchedulingException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulingService {
    private final Scheduler scheduler;
    private final ConfigService configService;

    private static final String DRAFT_CLEANUP_GROUP = "DRAFT_CLEANUP";
    private static final String DELETE_EVENT_CLEANUP_GROUP = "DELETED_EVENT_CLEANUP";

    private static final String REMINDER_TRIGGER_GROUP_FORMAT = "%d-%d";

    public void addDraftCleanupTrigger(Long draftId, LocalDateTime draftCreationDate) {
        try {
            createJobIfNone(DraftCleanupJob.JOB_KEY, DraftCleanupJob.class, "Automated cleanup of abandoned jobs");

            TriggerKey key = TriggerKey.triggerKey(draftId.toString(), DRAFT_CLEANUP_GROUP);
            Trigger cleanupTrigger = TriggerBuilder.newTrigger()
                .withIdentity(key)
                .forJob(DraftCleanupJob.JOB_KEY)
                .usingJobData(DraftCleanupJob.JOB_DRAFT_KEY, draftId)
                .startAt(DateFormatter.toDate(draftCreationDate.plusHours(configService.getConfigJpa().getDraftCleanupDelay())))
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
            scheduler.unscheduleJob(TriggerKey.triggerKey(draftId.toString(), DRAFT_CLEANUP_GROUP));
            log.debug("Removed cleanup trigger for draft: %s".formatted(draftId));
        } catch (SchedulerException e) {
            log.error("Failed to remove cleanup trigger for draft: %d".formatted(draftId), e);
        }
    }

    public void addEventReminderTrigger(Long eventId, Long studentId, Integer offset, LocalDateTime eventTime) {
        try {
            createJobIfNone(EventReminderJob.JOB_KEY, EventReminderJob.class, "Reminder notifications for events");

            String groupKey = REMINDER_TRIGGER_GROUP_FORMAT.formatted(eventId, studentId);
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
                .startAt(DateFormatter.toDate(eventTime))
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

    public boolean removeEventReminderTriggers(Long eventId, Long studentId) {
        try {
            Set<TriggerKey> keys = scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(REMINDER_TRIGGER_GROUP_FORMAT.formatted(eventId, studentId)));
            if (keys.isEmpty()) {
                return false;
            }
            log.debug("Removed triggers on event %d for student %s".formatted(eventId, studentId));
            return scheduler.unscheduleJobs(new ArrayList<>(keys));
        } catch (SchedulerException e) {
            log.error("Failed to remove trigger for event %d for student %d".formatted(eventId, studentId));
            throw new SchedulingException("Failed to remove event reminders.");
        }
    }

    public void removeAllEventReminderTriggers(Long studentId) {
        try {
            Set<TriggerKey> keys = scheduler.getTriggerKeys(GroupMatcher.triggerGroupEndsWith("-%s".formatted(studentId)));
            if (keys.isEmpty()) {
                return;
            }
            log.debug("Removed all triggers for student %s".formatted(studentId));
            scheduler.unscheduleJobs(new ArrayList<>(keys));
        } catch (SchedulerException e) {
            log.error("Scheduling error when removing all triggers for student %s".formatted(studentId), e);
        }
    }

    public void removeReminderTriggers(Long eventId) {
        try {
            Set<TriggerKey> keys = scheduler.getTriggerKeys(GroupMatcher.triggerGroupStartsWith("%d-".formatted(eventId)));
            scheduler.unscheduleJobs(new ArrayList<>(keys));
        } catch (SchedulerException e) {
            log.error("Failed to remove reminder trigger for event: %d".formatted(eventId), e);
        }
    }

    public void addDeletedEventCleanupTrigger(Long eventId, Long cleanupMessageId, LocalDateTime cleanupTime) {
        try {
            createJobIfNone(DeletedEventCleanupJob.JOB_KEY, DeletedEventCleanupJob.class, "Cleanup of deleted events");

            TriggerKey key = TriggerKey.triggerKey(eventId.toString(), DELETE_EVENT_CLEANUP_GROUP);
            Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(key)
                .usingJobData(DeletedEventCleanupJob.DELETION_MESSAGE_ID, cleanupMessageId)
                .usingJobData(DeletedEventCleanupJob.EVENT_ID, eventId)
                .forJob(DeletedEventCleanupJob.JOB_KEY)
                .startAt(DateFormatter.toDate(cleanupTime))
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
            TriggerKey key = TriggerKey.triggerKey(eventId.toString(), DELETE_EVENT_CLEANUP_GROUP);
            scheduler.unscheduleJob(key);
            log.debug("Removed cleanup trigger for deleted event with id: %d".formatted(eventId));
        } catch (SchedulerException e) {
            log.error("Failed to remove cleanup trigger for deleted event id: %d".formatted(eventId), e);
        }
    }

    private <T extends Job> void createJobIfNone(JobKey jobKey, Class<T> jobClass, String description) throws SchedulerException {
        if (!scheduler.checkExists(jobKey)) {
            JobDetail job = JobBuilder.newJob(jobClass)
                .withIdentity(jobKey)
                .storeDurably()
                .withDescription(description)
                .build();
            scheduler.addJob(job, true);
            log.info("Added job: %s".formatted(jobKey.getName()));
        }
    }

}
