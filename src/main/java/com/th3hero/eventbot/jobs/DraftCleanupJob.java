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
    public static final String DRAFT_ID = "draft_id";

    private final EventDraftRepository eventDraftRepository;

    @Override
    public void execute(JobExecutionContext executionContext) {
        Long draftId = executionContext.getTrigger().getJobDataMap().getLong(DRAFT_ID);
        if (!eventDraftRepository.existsById(draftId)) {
            // Drafts deleted by the user should remove cleanup triggers so if we got here something went wrong
            log.error("No existing draft for cleanup job. Draft id: %s".formatted(draftId));
            return;
        }

        eventDraftRepository.deleteById(draftId);
        log.info("Cleaned up draft with id %d".formatted(draftId));
    }
}
