package com.th3hero.eventbot.jobs;

import com.th3hero.eventbot.TestEntities;
import com.th3hero.eventbot.entities.ConfigJpa;
import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.exceptions.ConfigErrorException;
import com.th3hero.eventbot.repositories.EventRepository;
import com.th3hero.eventbot.services.ConfigService;
import com.th3hero.eventbot.utils.DiscordActionUtils;
import jakarta.persistence.EntityNotFoundException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.RestAction;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;

import java.util.Optional;

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

    @Test
    void execute() {
        final Long eventId = 1234L;
        final Long deletionMessageId = 4321L;
        final JobExecutionContext executionContext = mock(JobExecutionContext.class);
        final JobDataMap dataMap = new JobDataMap();
        dataMap.put(DeletedEventCleanupJob.EVENT_ID, eventId);
        dataMap.put(DeletedEventCleanupJob.DELETION_MESSAGE_ID, deletionMessageId);

        final Message message = mock(Message.class);
        final MessageChannel channel = mock(MessageChannel.class);

        final EventJpa event = TestEntities.eventJpa(1);
        final ConfigJpa config = TestEntities.configJpa();

        when(eventRepository.findById(eventId))
            .thenReturn(Optional.of(event));
        when(configService.getConfigJpa())
            .thenReturn(config);
        when(jda.getTextChannelById(config.getEventChannel()))
            .thenReturn(mock(TextChannel.class));

        when(executionContext.getTrigger())
            .thenReturn(mock(Trigger.class));
        when(executionContext.getTrigger().getJobDataMap())
            .thenReturn(dataMap);

        when(channel.retrieveMessageById(deletionMessageId))
            .thenReturn((RestAction<Message>) mock(RestAction.class));



        deletedEventCleanupJob.execute(executionContext);

        Assertions.assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> deletedEventCleanupJob.execute(executionContext));
        Assertions.assertThatExceptionOfType(ConfigErrorException.class).isThrownBy(() -> deletedEventCleanupJob.execute(executionContext));

        verify(eventRepository).delete(event);
    }
}