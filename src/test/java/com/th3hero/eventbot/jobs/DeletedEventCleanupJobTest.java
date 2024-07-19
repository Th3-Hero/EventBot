package com.th3hero.eventbot.jobs;

import com.th3hero.eventbot.TestEntities;
import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.entities.EventJpa.EventStatus;
import com.th3hero.eventbot.exceptions.MissingEventChannelException;
import com.th3hero.eventbot.repositories.EventRepository;
import com.th3hero.eventbot.services.ConfigService;
import jakarta.persistence.EntityNotFoundException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;

import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeletedEventCleanupJobTest {
    @Mock
    private JDA jda;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private ConfigService configService;

    @InjectMocks
    private DeletedEventCleanupJob deletedEventCleanupJob;

    @SuppressWarnings("unchecked")
    @Test
    void execute() {
        final Trigger trigger = mock(Trigger.class);
        final var config = TestEntities.configJpa();
        final var event = TestEntities.eventJpaWithId(1);
        event.setStatus(EventStatus.DELETED);
        final JobExecutionContext executionContext = mock(JobExecutionContext.class);
        final var deletionMessageId = 5678L;
        final JobDataMap dataMap = new JobDataMap();
        dataMap.put(DeletedEventCleanupJob.EVENT_ID, event.getId());
        dataMap.put(DeletedEventCleanupJob.DELETION_MESSAGE_ID, deletionMessageId);
        final var channel = mock(TextChannel.class);
        final RestAction<Message> messageRestAction = mock(RestAction.class);
        final var eventMessage = mock(Message.class);
        final var recoveryMessage = mock(Message.class);
        final AuditableRestAction<Void> deleteRestAction = mock(AuditableRestAction.class);

        when(executionContext.getTrigger())
            .thenReturn(trigger);
        when(trigger.getJobDataMap())
            .thenReturn(dataMap);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.of(event));
        when(configService.getConfigJpa())
            .thenReturn(config);
        when(jda.getTextChannelById(config.getEventChannel()))
            .thenReturn(channel);
        when(channel.retrieveMessageById(event.getMessageId()))
            .thenReturn(messageRestAction);
        when(channel.retrieveMessageById(deletionMessageId))
            .thenReturn(messageRestAction);
        when(eventMessage.delete())
            .thenReturn(deleteRestAction);
        when(recoveryMessage.delete())
            .thenReturn(deleteRestAction);

        deletedEventCleanupJob.execute(executionContext);

        final ArgumentCaptor<Consumer<Message>> eventMessageCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(channel.retrieveMessageById(event.getMessageId()), times(2))
            .queue(eventMessageCaptor.capture(), any(Consumer.class));
        final Consumer<Message> eventMessageValue = eventMessageCaptor.getValue();
        eventMessageValue.accept(eventMessage);
        verify(eventMessage).delete();

        final ArgumentCaptor<Consumer<Message>> recoveryMessageCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(channel.retrieveMessageById(deletionMessageId), times(2))
            .queue(recoveryMessageCaptor.capture(), any(Consumer.class));
        final Consumer<Message> recoveryMessageValue = recoveryMessageCaptor.getValue();
        recoveryMessageValue.accept(recoveryMessage);
        verify(recoveryMessage).delete();
    }

    @Test
    void execute_missingEvent() {
        final Trigger trigger = mock(Trigger.class);
        final var eventId = 1234L;
        final JobExecutionContext executionContext = mock(JobExecutionContext.class);
        final var deletionMessageId = 5678L;
        final JobDataMap dataMap = new JobDataMap();
        dataMap.put(DeletedEventCleanupJob.EVENT_ID, eventId);
        dataMap.put(DeletedEventCleanupJob.DELETION_MESSAGE_ID, deletionMessageId);

        when(executionContext.getTrigger())
            .thenReturn(trigger);
        when(trigger.getJobDataMap())
            .thenReturn(dataMap);
        when(eventRepository.findById(eventId))
            .thenReturn(Optional.empty());

        assertThatExceptionOfType(EntityNotFoundException.class)
            .isThrownBy(() -> deletedEventCleanupJob.execute(executionContext));
    }

    @Test
    void execute_failedToGetEventChannel() {
        final Trigger trigger = mock(Trigger.class);
        final var config = TestEntities.configJpa();
        final var event = TestEntities.eventJpaWithId(1);
        event.setStatus(EventStatus.DELETED);
        final JobExecutionContext executionContext = mock(JobExecutionContext.class);
        final var deletionMessageId = 5678L;
        final JobDataMap dataMap = new JobDataMap();
        dataMap.put(DeletedEventCleanupJob.EVENT_ID, event.getId());
        dataMap.put(DeletedEventCleanupJob.DELETION_MESSAGE_ID, deletionMessageId);

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

        assertThatExceptionOfType(MissingEventChannelException.class)
            .isThrownBy(() -> deletedEventCleanupJob.execute(executionContext));
    }

    @Test
    void execute_eventNotDeleted() {
        final Trigger trigger = mock(Trigger.class);
        final var event = TestEntities.eventJpaWithId(1);
        event.setStatus(EventStatus.ACTIVE);
        final JobExecutionContext executionContext = mock(JobExecutionContext.class);
        final var deletionMessageId = 5678L;
        final JobDataMap dataMap = new JobDataMap();
        dataMap.put(DeletedEventCleanupJob.EVENT_ID, event.getId());
        dataMap.put(DeletedEventCleanupJob.DELETION_MESSAGE_ID, deletionMessageId);

        when(executionContext.getTrigger())
            .thenReturn(trigger);
        when(trigger.getJobDataMap())
            .thenReturn(dataMap);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.of(event));

        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> deletedEventCleanupJob.execute(executionContext));

        verify(eventRepository, never()).delete(any(EventJpa.class));
    }
}