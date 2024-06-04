package com.th3hero.eventbot.jobs;

import com.th3hero.eventbot.TestEntities;
import com.th3hero.eventbot.exceptions.ConfigErrorException;
import com.th3hero.eventbot.repositories.EventRepository;
import com.th3hero.eventbot.services.ConfigService;
import jakarta.persistence.EntityNotFoundException;
import net.dv8tion.jda.api.JDA;
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
class EventReminderJobTest {
    @Mock
    private JDA jda;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private ConfigService configService;

    @InjectMocks
    private EventReminderJob eventReminderJob;

    @Test
    void execute() {
        final Trigger trigger = mock(Trigger.class);
        final var event = TestEntities.eventJpaWithId(1);
        final var config = TestEntities.configJpa();
        final Long studentId = 5678L;
        final int offset = 24;
        final JobExecutionContext executionContext = mock(JobExecutionContext.class);
        final JobDataMap dataMap = new JobDataMap();
        dataMap.put(EventReminderJob.EVENT_ID, event.getId());
        dataMap.put(EventReminderJob.STUDENT_ID, studentId);
        dataMap.put(EventReminderJob.OFFSET_ID, offset);

        when(executionContext.getTrigger())
            .thenReturn(trigger);
        when(trigger.getJobDataMap())
            .thenReturn(dataMap);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.of(event));
        when(configService.getConfigJpa())
            .thenReturn(config);

        // NOTE: Not really sure how to test the rest of this due to the callbacks
    }

    @Test
    void execute_missingEvent() {
        final Trigger trigger = mock(Trigger.class);
        final Long eventId = 1234L;
        final JobExecutionContext executionContext = mock(JobExecutionContext.class);
        final JobDataMap dataMap = new JobDataMap();
        dataMap.put(EventReminderJob.EVENT_ID, eventId);

        when(executionContext.getTrigger())
            .thenReturn(trigger);
        when(trigger.getJobDataMap())
            .thenReturn(dataMap);
        when(eventRepository.findById(eventId))
            .thenReturn(Optional.empty());

        assertThatExceptionOfType(EntityNotFoundException.class)
            .isThrownBy(() -> eventReminderJob.execute(executionContext));
    }

    @Test
    void execute_missingEventChannel() {
        final Trigger trigger = mock(Trigger.class);
        final var event = TestEntities.eventJpaWithId(1);
        final var config = TestEntities.configJpa();
        final JobExecutionContext executionContext = mock(JobExecutionContext.class);
        final JobDataMap dataMap = new JobDataMap();
        dataMap.put(EventReminderJob.EVENT_ID, event.getId());

        when(executionContext.getTrigger())
            .thenReturn(trigger);
        when(trigger.getJobDataMap())
            .thenReturn(dataMap);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.of(event));
        when(configService.getConfigJpa())
            .thenReturn(config);
        when(jda.getTextChannelById(config.getEventChannel()))
            .thenReturn(null);

        assertThatExceptionOfType(ConfigErrorException.class)
            .isThrownBy(() -> eventReminderJob.execute(executionContext));
    }
}