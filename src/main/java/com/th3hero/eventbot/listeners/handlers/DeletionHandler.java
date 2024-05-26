package com.th3hero.eventbot.listeners.handlers;


import com.th3hero.eventbot.commands.actions.ButtonAction;
import com.th3hero.eventbot.entities.ConfigJpa;
import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.exceptions.ConfigErrorException;
import com.th3hero.eventbot.factories.ButtonFactory;
import com.th3hero.eventbot.factories.EmbedBuilderFactory;
import com.th3hero.eventbot.formatting.InteractionArguments;
import com.th3hero.eventbot.repositories.EventRepository;
import com.th3hero.eventbot.services.ConfigService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DeletionHandler {
    private final EventRepository eventRepository;
    private final ConfigService configService;

    public void handleDeletedMessage(MessageDeleteEvent event) {
        // We can't stop users with permission from deleting messages, so we just repost the message

        Long deletedMessageId = event.getMessageIdLong();
        ConfigJpa configJpa = configService.getConfigJpa();

        // If the message isn't in the event channel, ignore it. No point searching all events in the database
        if (!configJpa.getEventChannel().equals(event.getChannel().getIdLong())) {
            return;
        }

        Optional<EventJpa> eventJpa = eventRepository.findByMessageId(deletedMessageId);
        if (eventJpa.isEmpty()) {
            // The message wasn't an event message, so ignore it
            return;
        }
        Long eventChannelId = configJpa.getEventChannel();
        TextChannel channel = Optional.ofNullable(event.getJDA().getTextChannelById(eventChannelId))
            .orElseThrow(() -> new ConfigErrorException("Configured event channel does not exist. Make sure config is setup correctly"));

        // If the user has left the server we can't get their mention
        String author = Optional.ofNullable(event.getJDA().getUserById(eventJpa.get().getAuthorId()))
            .map(IMentionable::getAsMention)
            .orElse(MarkdownUtil.italics("Unknown User"));

        channel.sendMessageEmbeds(EmbedBuilderFactory.eventEmbed(eventJpa.get(), author))
            .addComponents(ButtonFactory.eventButtons(eventJpa.get().getId()))
            .queue(success -> {
                // Save the new message id to the event
                eventJpa.get().setMessageId(success.getIdLong());
                eventRepository.save(eventJpa.get());
            });
        channel.sendMessage("Event messages cannot be deleted directly. You must use the delete button on the message.").queue();
        log.info("Reposted deleted event message for event {}", eventJpa.get().getId());
    }

    public void handleDeletedChannel(ChannelDeleteEvent event) {
        // We can't stop users with permission from deleting channels, so we just recreate the channel

        ConfigJpa configJpa = configService.getConfigJpa();

        Long eventChannelId = configJpa.getEventChannel();
        Long deletedChannelId = event.getChannel().getIdLong();
        if (!eventChannelId.equals(deletedChannelId)) {
            return;
        }

        Set<Permission> allow = EnumSet.of(
            Permission.VIEW_CHANNEL,
            Permission.MESSAGE_HISTORY,
            Permission.MESSAGE_ADD_REACTION,
            Permission.MESSAGE_SEND_IN_THREADS,
            Permission.CREATE_PUBLIC_THREADS
        );

        Set<Permission> deny = EnumSet.of(
            Permission.MESSAGE_SEND
        );

        // Updating the event channel here reposts the events
        log.info("Recreating event channel");
        event.getGuild().createTextChannel(event.getChannel().getName())
            .setTopic("Event Channel")
            .addRolePermissionOverride(event.getGuild().getPublicRole().getIdLong(), allow, deny)
            .queue(success -> configService.updateEventChannel(success.getIdLong()));
    }
}
