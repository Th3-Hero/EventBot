package com.th3hero.eventbot.jobs;

import com.th3hero.eventbot.TestEntities;
import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.entities.EventJpa.EventStatus;
import com.th3hero.eventbot.repositories.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompleteEventsJobTest {
    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private CompleteEventsJob completeEventsJob;

    @Test
    void execute() {
        final var event = TestEntities.eventJpaWithId(1);
        final Trigger trigger = mock(Trigger.class);
        final JobExecutionContext executionContext = mock(JobExecutionContext.class);
        final JobDataMap dataMap = new JobDataMap();
        dataMap.put(CompleteEventsJob.EVENT_ID, event.getId());

        when(executionContext.getTrigger())
            .thenReturn(trigger);
        when(trigger.getJobDataMap())
            .thenReturn(dataMap);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.of(event));

        completeEventsJob.execute(executionContext);

        assertEquals(EventStatus.COMPLETED, event.getStatus());
    }

    @Test
    void execute_missingEvent() {
        final Trigger trigger = mock(Trigger.class);
        final JobExecutionContext executionContext = mock(JobExecutionContext.class);
        final JobDataMap dataMap = new JobDataMap();
        dataMap.put(CompleteEventsJob.EVENT_ID, 1234L);

        when(executionContext.getTrigger())
            .thenReturn(trigger);
        when(trigger.getJobDataMap())
            .thenReturn(dataMap);
        when(eventRepository.findById(anyLong()))
            .thenReturn(Optional.empty());

        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> completeEventsJob.execute(executionContext));
    }

    @Test
    void execute_deletedEvent() {
        final var event = TestEntities.eventJpaWithId(1);
        event.setStatus(EventStatus.DELETED);
        final Trigger trigger = mock(Trigger.class);
        final JobExecutionContext executionContext = mock(JobExecutionContext.class);
        final JobDataMap dataMap = new JobDataMap();
        dataMap.put(CompleteEventsJob.EVENT_ID, event.getId());

        when(executionContext.getTrigger())
            .thenReturn(trigger);
        when(trigger.getJobDataMap())
            .thenReturn(dataMap);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.of(event));

        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> completeEventsJob.execute(executionContext));
    }
}