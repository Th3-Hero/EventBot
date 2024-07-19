package com.th3hero.eventbot.jobs;

import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.entities.EventJpa.EventStatus;
import com.th3hero.eventbot.repositories.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class DraftCleanupJob implements Job {
    public static final JobKey JOB_KEY = JobKey.jobKey("draft_cleanup");
    public static final String DRAFT_ID = "draft_id";

    private final EventRepository eventRepository;

    @Override
    public void execute(JobExecutionContext executionContext) {
        Long draftId = executionContext.getTrigger().getJobDataMap().getLong(DRAFT_ID);
        Optional<EventJpa> eventDraft = eventRepository.findById(draftId);
        if (eventDraft.isEmpty()) {
            // Drafts deleted by the user should remove cleanup triggers so if we got here something went wrong
            throw new IllegalStateException("No existing draft for cleanup job. Draft id: %s".formatted(draftId));
        }

        if (!eventDraft.get().getStatus().equals(EventStatus.DRAFT)) {
            throw new IllegalStateException("Attempted to cleanup an event that was no longer a draft.(Cleanup trigger should have been removed) Event id: %s".formatted(draftId));
        }

        eventRepository.delete(eventDraft.get());

        log.info("Cleaned up draft with id {}", draftId);
    }
}
