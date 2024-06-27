package com.th3hero.eventbot.jobs;

import com.th3hero.eventbot.TestEntities;
import com.th3hero.eventbot.exceptions.MissingEventChannelException;
import com.th3hero.eventbot.repositories.EventRepository;
import com.th3hero.eventbot.services.ConfigService;
import com.th3hero.eventbot.utils.DiscordUtils;
import jakarta.persistence.EntityNotFoundException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.CacheRestAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
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
class EventReminderJobTest {
    @Mock
    private JDA jda;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private ConfigService configService;

    @InjectMocks
    private EventReminderJob eventReminderJob;

    @SuppressWarnings("unchecked")
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
        final var user = mock(User.class);
        final CacheRestAction<PrivateChannel> privateChannelRestAction = mock(CacheRestAction.class);
        final var privateChannel = mock(PrivateChannel.class);
        final var messageCreateAction = mock(MessageCreateAction.class);

        when(executionContext.getTrigger())
            .thenReturn(trigger);
        when(trigger.getJobDataMap())
            .thenReturn(dataMap);
        when(eventRepository.findById(event.getId()))
            .thenReturn(Optional.of(event));
        when(configService.getConfigJpa())
            .thenReturn(config);
        when(jda.getUserById(studentId))
            .thenReturn(user);
        when(user.openPrivateChannel())
            .thenReturn(privateChannelRestAction);
        when(privateChannel.sendMessage(anyString()))
            .thenReturn(messageCreateAction);

        eventReminderJob.execute(executionContext);

        final ArgumentCaptor<Consumer<PrivateChannel>> privateChannelCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(privateChannelRestAction).queue(privateChannelCaptor.capture());
        final Consumer<PrivateChannel> privateChannelConsumer = privateChannelCaptor.getValue();
        privateChannelConsumer.accept(privateChannel);
        verify(privateChannel).sendMessage("%d hour reminder for event: %s".formatted(offset, DiscordUtils.generateJumpUrl(configService.getConfigJpa(), event.getMessageId())));
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
}