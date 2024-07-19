package com.th3hero.eventbot.services;

import com.th3hero.eventbot.TestEntities;
import com.th3hero.eventbot.formatting.DateFormatter;
import com.th3hero.eventbot.jobs.DeletedEventCleanupJob;
import com.th3hero.eventbot.jobs.DraftCleanupJob;
import com.th3hero.eventbot.jobs.EventReminderJob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.scheduling.SchedulingException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulingServiceTest {

    private static final String REMINDER_TRIGGER_GROUP_FORMAT = "%d-%d";

    @Mock
    private Scheduler scheduler;
    @Mock
    private ConfigService configService;

    @InjectMocks
    private SchedulingService schedulingService;

    @Captor
    private ArgumentCaptor<List<TriggerKey>> triggerKeysCaptor;

    @Test
    void addDraftCleanupTrigger() throws SchedulerException {
        final var draftId = 1L;
        final var draftCreationDate = LocalDateTime.now();
        final var config = TestEntities.configJpa();

        when(scheduler.checkExists(DraftCleanupJob.JOB_KEY))
            .thenReturn(true);
        when(configService.getConfigJpa())
            .thenReturn(config);

        schedulingService.addDraftCleanupTrigger(draftId, draftCreationDate);

        verify(scheduler).scheduleJob(any(Trigger.class));
        verify(scheduler).scheduleJob(argThat(trigger -> trigger.getJobDataMap().get(DraftCleanupJob.DRAFT_ID).equals(draftId)));
    }

    @Test
    void addDraftCleanupTrigger_schedulerFail() throws SchedulerException {
        final var draftId = 1L;
        final var draftCreationDate = LocalDateTime.now();
        final var config = TestEntities.configJpa();

        when(scheduler.checkExists(DraftCleanupJob.JOB_KEY))
            .thenReturn(true);
        when(configService.getConfigJpa())
            .thenReturn(config);
        when(scheduler.scheduleJob(any(Trigger.class)))
            .thenThrow(SchedulerException.class);

        assertThatExceptionOfType(SchedulingException.class)
            .isThrownBy(() -> schedulingService.addDraftCleanupTrigger(draftId, draftCreationDate));
    }

    @Test
    void removeDraftCleanupTrigger() throws SchedulerException {
        final Long draftId = 1L;

        schedulingService.removeDraftCleanupTrigger(draftId);

        verify(scheduler).unscheduleJob(argThat(triggerKey -> triggerKey.getName().equals(draftId.toString())));
    }

    @Test
    void removeDraftCleanupTrigger_schedulerFail() throws SchedulerException {
        final var draftId = 1L;

        when(scheduler.unscheduleJob(any()))
            .thenThrow(SchedulerException.class);

        assertThatNoException()
            .isThrownBy(() -> schedulingService.removeDraftCleanupTrigger(draftId));
    }

    @Test
    void addEventReminderTrigger() throws SchedulerException {
        final var eventId = 1L;
        final var studentId = 2L;
        final var offset = 3;
        final var eventEndTime = LocalDateTime.now();
        final var triggerTime = eventEndTime.minusHours(24);

        schedulingService.addEventReminderTrigger(eventId, studentId, offset, triggerTime, eventEndTime);

        verify(scheduler).scheduleJob(any(Trigger.class));
        verify(scheduler).scheduleJob(argThat(
            trigger ->
                trigger.getJobDataMap().get(EventReminderJob.STUDENT_ID).equals(studentId) &&
                    trigger.getJobDataMap().get(EventReminderJob.EVENT_ID).equals(eventId) &&
                    trigger.getJobDataMap().get(EventReminderJob.OFFSET_ID).equals(offset) &&
                    trigger.getEndTime().equals(DateFormatter.toDate(eventEndTime)) &&
                    trigger.getStartTime().equals(DateFormatter.toDate(triggerTime))
        ));
    }

    @Test
    void addEventReminderTrigger_existingTrigger() throws SchedulerException {
        final var eventId = 1L;
        final var studentId = 2L;
        final var offset = 3;
        final var eventEndTime = LocalDateTime.now();
        final var triggerTime = eventEndTime.minusHours(24);

        when(scheduler.checkExists(any(JobKey.class)))
            .thenReturn(true);
        when(scheduler.checkExists(any(TriggerKey.class)))
            .thenReturn(true);

        schedulingService.addEventReminderTrigger(eventId, studentId, offset, triggerTime, eventEndTime);

        verify(scheduler, never()).scheduleJob(any(Trigger.class));
    }

    @Test
    void addEventReminderTrigger_schedulerFail() throws SchedulerException {
        final var eventId = 1L;
        final var studentId = 2L;
        final var offset = 3;
        final var eventEndTime = LocalDateTime.now();
        final var triggerTime = eventEndTime.minusHours(24);

        when(scheduler.scheduleJob(any(Trigger.class)))
            .thenThrow(SchedulerException.class);

        assertThatExceptionOfType(SchedulingException.class)
            .isThrownBy(() -> schedulingService.addEventReminderTrigger(eventId, studentId, offset, triggerTime, eventEndTime));
    }

    @Test
    void removeEventReminderTriggers() throws SchedulerException {
        final var eventId = 1L;
        final var studentId = 2L;
        final Integer offset = 3;

        final var groupKey = REMINDER_TRIGGER_GROUP_FORMAT.formatted(eventId, studentId);
        final var triggerKey = TriggerKey.triggerKey(offset.toString(), groupKey);

        schedulingService.removeEventReminderTriggers(eventId, studentId, offset);

        verify(scheduler).unscheduleJob(triggerKey);
    }

    @Test
    void removeEventReminderTriggers_noTrigger() throws SchedulerException {
        final var eventId = 1L;
        final var studentId = 2L;
        final Integer offset = 3;

        final var groupKey = REMINDER_TRIGGER_GROUP_FORMAT.formatted(eventId, studentId);
        final var triggerKey = TriggerKey.triggerKey(offset.toString(), groupKey);

        when(scheduler.unscheduleJob(triggerKey))
            .thenThrow(SchedulerException.class);

        assertThatExceptionOfType(SchedulingException.class)
            .isThrownBy(() -> schedulingService.removeEventReminderTriggers(eventId, studentId, offset));
    }

    @Test
    void testRemoveEventReminderTriggers() throws SchedulerException {
        final var eventId = 1L;
        final var studentId = 2L;

        final var groupKey = REMINDER_TRIGGER_GROUP_FORMAT.formatted(eventId, studentId);
        final var triggerKey = TriggerKey.triggerKey("1", groupKey);

        when(scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(groupKey)))
            .thenReturn(Set.of(triggerKey));
        when(scheduler.unscheduleJobs(anyList()))
            .thenReturn(true);

        boolean result = schedulingService.removeEventReminderTriggers(eventId, studentId);

        assertThat(result).isTrue();
        verify(scheduler).unscheduleJobs(List.of(triggerKey));
    }

    @Test
    void testRemoveEventReminderTriggers_noTrigger() throws SchedulerException {
        final var eventId = 1L;
        final var studentId = 2L;

        final var groupKey = REMINDER_TRIGGER_GROUP_FORMAT.formatted(eventId, studentId);

        when(scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(groupKey)))
            .thenReturn(Set.of());

        boolean result = schedulingService.removeEventReminderTriggers(eventId, studentId);

        assertThat(result).isFalse();
        verify(scheduler, never()).unscheduleJobs(anyList());
    }

    @Test
    void testRemoveEventReminderTriggers_schedulerFail() throws SchedulerException {
        final var eventId = 1L;
        final var studentId = 2L;

        final var groupKey = REMINDER_TRIGGER_GROUP_FORMAT.formatted(eventId, studentId);

        when(scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(groupKey)))
            .thenThrow(SchedulerException.class);

        assertThatExceptionOfType(SchedulingException.class)
            .isThrownBy(() -> schedulingService.removeEventReminderTriggers(eventId, studentId));
    }

    @Test
    void removeAllEventReminderTriggers() throws SchedulerException {
        final var studentId = 2L;

        final var triggerKeyOne = TriggerKey.triggerKey("1", REMINDER_TRIGGER_GROUP_FORMAT.formatted(1L, studentId));
        final var triggerKeyTwo = TriggerKey.triggerKey("2", REMINDER_TRIGGER_GROUP_FORMAT.formatted(1L, studentId));

        when(scheduler.getTriggerKeys(GroupMatcher.triggerGroupEndsWith("-%s".formatted(studentId))))
            .thenReturn(Set.of(triggerKeyOne, triggerKeyTwo));
        when(scheduler.unscheduleJobs(anyList()))
            .thenReturn(true);

        schedulingService.removeAllEventReminderTriggers(studentId);

        verify(scheduler).getTriggerKeys(GroupMatcher.triggerGroupEndsWith("-%s".formatted(studentId)));

        verify(scheduler).unscheduleJobs(triggerKeysCaptor.capture());
        List<TriggerKey> triggerKeys = triggerKeysCaptor.getValue();
        assertThat(triggerKeys).containsExactlyInAnyOrder(triggerKeyOne, triggerKeyTwo);
    }

    @Test
    void removeAllEventReminderTriggers_noTriggers() throws SchedulerException {
        final var studentId = 2L;

        when(scheduler.getTriggerKeys(GroupMatcher.triggerGroupEndsWith("-%s".formatted(studentId))))
            .thenReturn(Set.of());

        schedulingService.removeAllEventReminderTriggers(studentId);

        verify(scheduler, never()).unscheduleJobs(anyList());
    }

    @Test
    void removeAllEventReminderTriggers_schedulerFail() throws SchedulerException {
        final var studentId = 2L;

        when(scheduler.getTriggerKeys(any()))
            .thenThrow(SchedulerException.class);

        schedulingService.removeAllEventReminderTriggers(studentId);

        verify(scheduler, never()).unscheduleJobs(anyList());
    }

    @Test
    void removeReminderTriggers() throws SchedulerException {
        final var eventId = 1L;

        final var triggerKeyOne = TriggerKey.triggerKey("1", REMINDER_TRIGGER_GROUP_FORMAT.formatted(eventId, 2L));
        final var triggerKeyTwo = TriggerKey.triggerKey("2", REMINDER_TRIGGER_GROUP_FORMAT.formatted(eventId, 3L));

        when(scheduler.getTriggerKeys(GroupMatcher.triggerGroupStartsWith("%d-".formatted(eventId))))
            .thenReturn(Set.of(triggerKeyOne, triggerKeyTwo));
        when(scheduler.unscheduleJobs(anyList()))
            .thenReturn(true);

        schedulingService.removeReminderTriggers(eventId);

        verify(scheduler).unscheduleJobs(triggerKeysCaptor.capture());
        List<TriggerKey> triggerKeys = triggerKeysCaptor.getValue();
        assertThat(triggerKeys).containsExactlyInAnyOrder(triggerKeyOne, triggerKeyTwo);
    }

    @Test
    void removeReminderTriggers_noTriggers() throws SchedulerException {
        final var eventId = 1L;

        when(scheduler.getTriggerKeys(GroupMatcher.triggerGroupStartsWith("%d-".formatted(eventId))))
            .thenReturn(Set.of());

        schedulingService.removeReminderTriggers(eventId);

        verify(scheduler, never()).unscheduleJobs(anyList());
    }

    @Test
    void removeReminderTriggers_schedulerFail() throws SchedulerException {
        final var eventId = 1L;

        when(scheduler.getTriggerKeys(GroupMatcher.triggerGroupStartsWith("%d-".formatted(eventId))))
            .thenThrow(SchedulerException.class);

        schedulingService.removeReminderTriggers(eventId);

        verify(scheduler, never()).unscheduleJobs(anyList());
    }

    @Test
    void addDeletedEventCleanupTrigger() throws SchedulerException {
        final var eventId = 1L;
        final var cleanupMessageId = 2L;
        final var cleanupTime = LocalDateTime.now();

        schedulingService.addDeletedEventCleanupTrigger(eventId, cleanupMessageId, cleanupTime);

        verify(scheduler).scheduleJob(any(Trigger.class));
        verify(scheduler).scheduleJob(argThat(trigger ->
            trigger.getJobDataMap().get(DeletedEventCleanupJob.DELETION_MESSAGE_ID).equals(cleanupMessageId) &&
            trigger.getJobDataMap().get(DeletedEventCleanupJob.EVENT_ID).equals(eventId)
        ));
    }

    @Test
    void addDeletedEventCleanupTrigger_schedulerFail() throws SchedulerException {
        final var eventId = 1L;
        final var cleanupMessageId = 2L;
        final var cleanupTime = LocalDateTime.now();

        when(scheduler.scheduleJob(any(Trigger.class)))
            .thenThrow(SchedulerException.class);

        assertThatExceptionOfType(SchedulingException.class)
            .isThrownBy(() -> schedulingService.addDeletedEventCleanupTrigger(eventId, cleanupMessageId, cleanupTime));
    }

    @Test
    void removeDeletedEventCleanupTrigger() throws SchedulerException {
        final Long eventId = 1L;

        schedulingService.removeDeletedEventCleanupTrigger(eventId);

        verify(scheduler).unscheduleJob(argThat(triggerKey -> triggerKey.getName().equals(eventId.toString())));
    }

    @Test
    void removeDeletedEventCleanupTrigger_schedulerFail() throws SchedulerException {
        final var eventId = 1L;

        when(scheduler.unscheduleJob(any()))
            .thenThrow(SchedulerException.class);

        assertThatNoException()
            .isThrownBy(() -> schedulingService.removeDeletedEventCleanupTrigger(eventId));
    }

    @Test
    void addEventCompletedTrigger() throws SchedulerException {
        final var eventId = 1L;
        final var completedTime = LocalDateTime.now();

        when(scheduler.checkExists(any(JobKey.class)))
            .thenReturn(false);
        when(scheduler.checkExists(any(TriggerKey.class)))
            .thenReturn(false);

        schedulingService.addEventCompletedTrigger(eventId, completedTime);

        verify(scheduler).scheduleJob(any(Trigger.class));
        verify(scheduler).scheduleJob(argThat(trigger ->
            trigger.getJobDataMap().get(DeletedEventCleanupJob.EVENT_ID).equals(eventId) &&
            trigger.getStartTime().equals(DateFormatter.toDate(completedTime))
        ));
    }

    @Test
    void addEventCompletedTrigger_existingTrigger() throws SchedulerException {
        final var eventId = 1L;
        final var completedTime = LocalDateTime.now();

        when(scheduler.checkExists(any(JobKey.class)))
            .thenReturn(true);
        when(scheduler.checkExists(any(TriggerKey.class)))
            .thenReturn(true);

        schedulingService.addEventCompletedTrigger(eventId, completedTime);

        verify(scheduler, never()).scheduleJob(any());
    }

    @Test
    void addEventCompletedTrigger_schedulerFail() throws SchedulerException {
        final var eventId = 1L;
        final var completedTime = LocalDateTime.now();

        when(scheduler.scheduleJob(any()))
            .thenThrow(SchedulerException.class);

        assertThatExceptionOfType(SchedulingException.class)
            .isThrownBy(() -> schedulingService.addEventCompletedTrigger(eventId, completedTime));
    }

    @Test
    void removeEventCompletedTrigger() throws SchedulerException {
        final Long eventId = 1L;

        schedulingService.removeEventCompleteTrigger(eventId);

        verify(scheduler).unscheduleJob(argThat(triggerKey -> triggerKey.getName().equals(eventId.toString())));
    }

    @Test
    void removeEventCompletedTrigger_schedulerFail() throws SchedulerException {
        final Long eventId = 1L;

        when(scheduler.unscheduleJob(any()))
            .thenThrow(SchedulerException.class);

        assertThatExceptionOfType(SchedulingException.class)
            .isThrownBy(() -> schedulingService.removeEventCompleteTrigger(eventId));
    }
}