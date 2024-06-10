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

    /**
     * Adds a trigger to the scheduler to clean up a draft after a certain amount of time.
     * @param draftId The id of the draft to clean up
     * @param draftCreationDate The date the draft was created
     * @throws SchedulingException If the trigger cannot be added
     */
    public void addDraftCleanupTrigger(Long draftId, LocalDateTime draftCreationDate) {
        try {
            createJobIfNone(DraftCleanupJob.JOB_KEY, DraftCleanupJob.class, "Automated cleanup of abandoned jobs");

            TriggerKey key = TriggerKey.triggerKey(draftId.toString(), DRAFT_CLEANUP_GROUP);
            Trigger cleanupTrigger = TriggerBuilder.newTrigger()
                .withIdentity(key)
                .forJob(DraftCleanupJob.JOB_KEY)
                .usingJobData(DraftCleanupJob.DRAFT_ID, draftId)
                .startAt(DateFormatter.toDate(draftCreationDate.plusHours(configService.getConfigJpa().getDraftCleanupDelay())))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                    .withMisfireHandlingInstructionFireNow()
                    .withRepeatCount(0))
                .build();
            scheduler.scheduleJob(cleanupTrigger);
            log.debug("Added new draft cleanup trigger for draft: {}", draftId);
        } catch (SchedulerException e) {
            log.error("Failed to add trigger for draft cleanup of draft: {}", draftId, e);
            throw new SchedulingException("Failed to schedule draft cleanup.");
        }
    }

    /**
     * Removes the cleanup trigger for a draft.
     * @param draftId The id of the draft to remove the trigger for
     * @throws SchedulingException If the trigger cannot be removed
     */
    public void removeDraftCleanupTrigger(Long draftId) {
        try {
            scheduler.unscheduleJob(TriggerKey.triggerKey(draftId.toString(), DRAFT_CLEANUP_GROUP));
            log.debug("Removed cleanup trigger for draft: {}", draftId);
        } catch (SchedulerException e) {
            log.error("Failed to remove cleanup trigger for draft: {}", draftId, e);
        }
    }

    /**
     * Adds a trigger to the scheduler to send a reminder notification for an event.
     * @param eventId The id of the event to send a reminder for
     * @param studentId The id of the student to send the reminder to
     * @param offset The offset in hours from the event time to send the reminder
     * @param eventTime The date/time of the event
     * @throws SchedulingException If the trigger cannot be added
     */
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
            log.debug("Added new reminder trigger for event: {}", eventId);

        } catch (SchedulerException e) {
            log.error("Failed to add trigger for event reminder: {}", eventId, e);
            throw new SchedulingException("Failed to schedule event reminder.");
        }
    }

    /**
     * Removes a specific reminder trigger for a student on a specific event.
     * @param eventId The id of the event
     * @param studentId The id of the student
     * @param offset The offset of the reminder from the event date
     * @throws SchedulingException If the trigger cannot be removed
     */
    public void removeEventReminderTriggers(Long eventId, Long studentId, Integer offset) {
        try {
            String groupKey = REMINDER_TRIGGER_GROUP_FORMAT.formatted(eventId, studentId);
            scheduler.unscheduleJob(TriggerKey.triggerKey(offset.toString(), groupKey));
            log.debug("Removed trigger for event {} for student {} with offset {}", eventId, studentId, offset);
        } catch (SchedulerException e) {
            log.error("Failed to remove trigger for event {} for student {} with offset {}", eventId, studentId, offset);
            throw new SchedulingException("Failed to remove event reminder.");
        }
    }

    /**
     * Removes all reminder triggers for a student on a specific event.{}
, am evenId The id of the event
     * @param studentId The id of the student
     * @return True if triggers were removed, false if no triggers were found
     * @throws SchedulingException If the triggers cannot be removed
     */
    public boolean removeEventReminderTriggers(Long eventId, Long studentId) {
        try {
            Set<TriggerKey> keys = scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(REMINDER_TRIGGER_GROUP_FORMAT.formatted(eventId, studentId)));
            if (keys.isEmpty()) {
                return false;
            }
            log.debug("Removed triggers on event {} for student {}", eventId, studentId);
            return scheduler.unscheduleJobs(new ArrayList<>(keys));
        } catch (SchedulerException e) {
            log.error("Failed to remove trigger for event {} for student {}", eventId, studentId);
            throw new SchedulingException("Failed to remove event reminders.");
        }
    }

    /**
     * Removes all reminder triggers for a student from all events.
     * @param studentId The id of the student
     * @throws SchedulingException If the triggers cannot be removed
     */
    public void removeAllEventReminderTriggers(Long studentId) {
        try {
            Set<TriggerKey> keys = scheduler.getTriggerKeys(GroupMatcher.triggerGroupEndsWith("-%s".formatted(studentId)));
            if (keys.isEmpty()) {
                return;
            }
            log.debug("Removed all triggers for student {}", studentId);
            scheduler.unscheduleJobs(new ArrayList<>(keys));
        } catch (SchedulerException e) {
            log.error("Scheduling error when removing all triggers for student {}", studentId, e);
        }
    }

    /**
     * Removes all reminder triggers for an event from all students.
     * @param eventId The id of the event
     * @throws SchedulingException If the triggers cannot be removed
     */
    public void removeReminderTriggers(Long eventId) {
        try {
            Set<TriggerKey> keys = scheduler.getTriggerKeys(GroupMatcher.triggerGroupStartsWith("%d-".formatted(eventId)));
            if (keys.isEmpty()) {
                return;
            }
            scheduler.unscheduleJobs(new ArrayList<>(keys));
            log.debug("Removed all triggers for event: {}", eventId);
        } catch (SchedulerException e) {
            log.error("Failed to remove reminder trigger for event: {}", eventId, e);
        }
    }

    /**
     * Adds a trigger to the scheduler to clean up a deleted event after a certain amount of time.
     * @param eventId The id of the event to clean up
     * @param cleanupMessageId The id of the cleanup/recovery message
     * @param cleanupTime The date/time to clean up the event
     * @throws SchedulingException If the trigger cannot be added
     */
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
            log.debug("Added cleanup trigger for deleted event with id: {}", eventId);
        } catch (SchedulerException e) {
            log.error("Failed to add cleanup trigger for deleted event id: {}", eventId, e);
            throw new SchedulingException("Failed to schedule draft cleanup.");
        }
    }

    /**
     * Removes the cleanup trigger for a deleted event.
     * @param eventId The id of the event to remove the trigger for
     * @throws SchedulingException If the trigger cannot be removed
     */
    public void removeDeletedEventCleanupTrigger(Long eventId) {
        try {
            TriggerKey key = TriggerKey.triggerKey(eventId.toString(), DELETE_EVENT_CLEANUP_GROUP);
            scheduler.unscheduleJob(key);
            log.debug("Removed cleanup trigger for deleted event with id: {}", eventId);
        } catch (SchedulerException e) {
            log.error("Failed to remove cleanup trigger for deleted event id: {}", eventId, e);
        }
    }

    /**
     * Creates a quartz job if one does not already exist.
     * @param jobKey The key of the job
     * @param jobClass The class of the job
     * @param description The description of the job
     * @throws SchedulerException If the job cannot be created
     */
    private <T extends Job> void createJobIfNone(JobKey jobKey, Class<T> jobClass, String description) throws SchedulerException {
        if (!scheduler.checkExists(jobKey)) {
            JobDetail job = JobBuilder.newJob(jobClass)
                .withIdentity(jobKey)
                .storeDurably()
                .withDescription(description)
                .build();
            scheduler.addJob(job, true);
            log.info("Added job: {}", jobKey.getName());
        }
    }

}
