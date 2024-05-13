package com.th3hero.eventbot.jobs;

import com.th3hero.eventbot.repositories.EventDraftRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DraftCleanupJobTest {
    @Mock
    private EventDraftRepository eventDraftRepository;

    @Mock
    private Trigger trigger;

    @InjectMocks
    private DraftCleanupJob draftCleanupJob;

    @Test
    void execute() {
        final Long draftId = 1234L;
        final JobExecutionContext executionContext = mock(JobExecutionContext.class);
        final JobDataMap dataMap = new JobDataMap();
        dataMap.put(DraftCleanupJob.JOB_DRAFT_KEY, draftId);

        when(executionContext.getTrigger())
            .thenReturn(trigger);
        when(executionContext.getTrigger().getJobDataMap())
            .thenReturn(dataMap);
        when(eventDraftRepository.existsById(draftId))
            .thenReturn(true);

        draftCleanupJob.execute(executionContext);

        verify(eventDraftRepository, times(1)).deleteById(draftId);
    }

    @Test
    void execute_noDraft() {
        final Long draftId = 1234L;
        final JobExecutionContext executionContext = mock(JobExecutionContext.class);
        final JobDataMap dataMap = new JobDataMap();
        dataMap.put(DraftCleanupJob.JOB_DRAFT_KEY, draftId);

        when(executionContext.getTrigger())
            .thenReturn(trigger);
        when(executionContext.getTrigger().getJobDataMap())
            .thenReturn(dataMap);
        when(eventDraftRepository.existsById(draftId))
            .thenReturn(false);

        draftCleanupJob.execute(executionContext);

        verify(eventDraftRepository, never()).deleteById(draftId);
    }
}