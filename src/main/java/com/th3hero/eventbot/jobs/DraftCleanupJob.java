package com.th3hero.eventbot.jobs;

import com.th3hero.eventbot.repositories.EventDraftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;

@Slf4j
@RequiredArgsConstructor
public class DraftCleanupJob implements Job {
    public static final JobKey JOB_KEY = JobKey.jobKey("draft_cleanup");

    private final EventDraftRepository eventDraftRepository;

    @Override
    public void execute(JobExecutionContext executionContext) {
        Long draftId = Long.parseLong(executionContext.getTrigger().getKey().getName());
        if (!eventDraftRepository.existsById(draftId)) {
            log.error("No existing draft for cleanup job. Draft id: %s".formatted(draftId));
            return;
        }

        eventDraftRepository.deleteById(draftId);
        log.info("Cleaned up draft with id %d".formatted(draftId));
    }
}
