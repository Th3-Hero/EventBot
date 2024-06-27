package com.th3hero.eventbot.controllers.rest;

import com.th3hero.eventbot.TestEntities;
import com.th3hero.eventbot.dto.management.Announcement;
import com.th3hero.eventbot.dto.management.AnnouncementField;
import com.th3hero.eventbot.exceptions.MissingEventChannelException;
import com.th3hero.eventbot.factories.EmbedBuilderFactory;
import com.th3hero.eventbot.services.ConfigService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagementControllerTest {
    @Mock
    private ConfigService configService;
    @Mock
    private JDA jda;

    @InjectMocks
    private ManagementController managementController;

    @Test
    void createAnnouncement() {
        final var config = TestEntities.configJpa();
        final var channel = mock(TextChannel.class);
        final var messageCreateAction = mock(MessageCreateAction.class);

        final var fieldOne = new AnnouncementField("Title1", "Content1");
        final var fieldTwo = new AnnouncementField("Title2", "Content2");
        final var announcement = new Announcement("Title", "Description", List.of(fieldOne, fieldTwo));
        final var expectedEmbed = EmbedBuilderFactory.announcementEmbed(announcement);

        when(configService.getConfigJpa())
            .thenReturn(config);
        when(jda.getTextChannelById(config.getEventChannel()))
            .thenReturn(channel);
        when(channel.sendMessageEmbeds(any(MessageEmbed.class)))
            .thenReturn(messageCreateAction);

        managementController.createAnnouncement(announcement);

        verify(channel).sendMessageEmbeds(expectedEmbed);
    }

    @Test
    void createAnnouncement_failedToFindChannel() {
        final var config = TestEntities.configJpa();

        final var fieldOne = new AnnouncementField("Title1", "Content1");
        final var fieldTwo = new AnnouncementField("Title2", "Content2");
        final var announcement = new Announcement("Title", "Description", List.of(fieldOne, fieldTwo));

        when(configService.getConfigJpa())
            .thenReturn(config);
        when(jda.getTextChannelById(config.getEventChannel()))
            .thenReturn(null);

        assertThatExceptionOfType(MissingEventChannelException.class)
            .isThrownBy(() -> managementController.createAnnouncement(announcement));

    }
}
