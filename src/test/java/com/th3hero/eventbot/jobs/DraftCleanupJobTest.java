package com.th3hero.eventbot.jobs;

import com.th3hero.eventbot.TestEntities;
import com.th3hero.eventbot.entities.EventJpa;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DraftCleanupJobTest {
    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private DraftCleanupJob draftCleanupJob;

    @Test
    void execute() {
        final Trigger trigger = mock(Trigger.class);
        final var draft = TestEntities.eventJpaWithId(1);
        draft.setStatus(EventJpa.EventStatus.DRAFT);
        final JobExecutionContext executionContext = mock(JobExecutionContext.class);
        final JobDataMap dataMap = new JobDataMap();
        dataMap.put(DraftCleanupJob.DRAFT_ID, draft.getId());

        when(executionContext.getTrigger())
            .thenReturn(trigger);
        when(executionContext.getTrigger().getJobDataMap())
            .thenReturn(dataMap);
        when(eventRepository.findById(draft.getId()))
            .thenReturn(Optional.of(draft));

        draftCleanupJob.execute(executionContext);

        verify(eventRepository).delete(draft);
    }

    @Test
    void execute_noDraft() {
        final Trigger trigger = mock(Trigger.class);
        final Long draftId = 1234L;
        final JobExecutionContext executionContext = mock(JobExecutionContext.class);
        final JobDataMap dataMap = new JobDataMap();
        dataMap.put(DraftCleanupJob.DRAFT_ID, draftId);

        when(executionContext.getTrigger())
            .thenReturn(trigger);
        when(executionContext.getTrigger().getJobDataMap())
            .thenReturn(dataMap);
        when(eventRepository.findById(draftId))
            .thenReturn(Optional.empty());

        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> draftCleanupJob.execute(executionContext));

        verify(eventRepository, never()).delete(any(EventJpa.class));
    }

    @Test
    void execute_noLongerDraft() {
        final Trigger trigger = mock(Trigger.class);
        final var draft = TestEntities.eventJpaWithId(1);
        draft.setStatus(EventJpa.EventStatus.ACTIVE);
        final JobExecutionContext executionContext = mock(JobExecutionContext.class);
        final JobDataMap dataMap = new JobDataMap();
        dataMap.put(DraftCleanupJob.DRAFT_ID, draft.getId());

        when(executionContext.getTrigger())
            .thenReturn(trigger);
        when(executionContext.getTrigger().getJobDataMap())
            .thenReturn(dataMap);
        when(eventRepository.findById(draft.getId()))
            .thenReturn(Optional.of(draft));

       assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> draftCleanupJob.execute(executionContext));

        verify(eventRepository, never()).delete(any(EventJpa.class));
    }
}
