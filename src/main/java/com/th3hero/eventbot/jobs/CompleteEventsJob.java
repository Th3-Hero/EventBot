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
public class CompleteEventsJob implements Job {
    public static final JobKey JOB_KEY = JobKey.jobKey("complete_events");
    public static final String EVENT_ID = "event_id";

    private final EventRepository eventRepository;

    @Override
    public void execute(JobExecutionContext executionContext) {
        Long eventId = executionContext.getTrigger().getJobDataMap().getLong(EVENT_ID);
        Optional<EventJpa> event = eventRepository.findById(eventId);
        if (event.isEmpty()) {
            throw new IllegalStateException("No existing event for completion job. Event id: %s".formatted(eventId));
        }

        if (event.get().getStatus().equals(EventStatus.DELETED)) {
            throw new IllegalStateException("Attempted to complete a deleted event. Event id: %s".formatted(eventId));
        }

        event.get().setStatus(EventStatus.COMPLETED);
        eventRepository.save(event.get());

        log.info("Marked event as completed. id: {}", eventId);
    }
}
