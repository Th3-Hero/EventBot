package com.th3hero.eventbot.services;


import com.th3hero.eventbot.commands.actions.ButtonAction;
import com.th3hero.eventbot.entities.ConfigJpa;
import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.exceptions.ConfigErrorException;
import com.th3hero.eventbot.factories.EmbedBuilderFactory;
import com.th3hero.eventbot.formatting.InteractionArguments;
import com.th3hero.eventbot.repositories.EventRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DeletionHandler {
    private final EventRepository eventRepository;
    private final ConfigService configService;


    public void handleDeletedMessage(MessageDeleteEvent event) {
        Long deletedMessageId = event.getMessageIdLong();
        ConfigJpa configJpa = configService.getConfigJpa();

        // If the message isn't in the event channel, ignore it. No point searching all events in the database
        if (!configJpa.getEventChannel().equals(event.getChannel().getIdLong())) {
            return;
        }

        if (!eventRepository.existsByMessageId(deletedMessageId)) {
            return;
        }

        EventJpa eventJpa = eventRepository.findEventJpaByMessageId(deletedMessageId)
                .orElseThrow(() -> new EntityNotFoundException("Event with deleted message was not found in the database. Message id: %d".formatted(deletedMessageId)));
        Long eventChannelId = configJpa.getEventChannel();
        Optional<TextChannel> channel = Optional.ofNullable(event.getJDA().getTextChannelById(eventChannelId));
        if (channel.isEmpty()) {
            throw new ConfigErrorException("Configured event channel does not exist. Make sure config is setup correctly");
        }

        String author = event.getJDA().getUserById(eventJpa.getAuthorId()).getAsMention();

        channel.get().sendMessageEmbeds(EmbedBuilderFactory.eventEmbed(eventJpa, author))
                .addActionRow(
                        Button.success(InteractionArguments.createInteractionIdString(ButtonAction.MARK_COMPLETE, eventJpa.getId()), "Mark Complete"),
                        Button.primary(InteractionArguments.createInteractionIdString(ButtonAction.EDIT_EVENT, eventJpa.getId()), "Edit Event"),
                        Button.danger(InteractionArguments.createInteractionIdString(ButtonAction.DELETE_EVENT, eventJpa.getId()), "Delete Event")
                )
                .queue(success -> {
                    eventJpa.setMessageId(success.getIdLong());
                    eventRepository.save(eventJpa);
                });
        channel.get().sendMessage("Event messages cannot be deleted directly. You must use the delete button on the message.").queue();
    }

    public void handleDeletedChannel(ChannelDeleteEvent event) {
        ConfigJpa configJpa = configService.getConfigJpa();

        Long eventChannelId = configJpa.getEventChannel();
        Long deletedChannelId = event.getChannel().getIdLong();
        if (!eventChannelId.equals(deletedChannelId)) {
            return;
        }

        Optional.ofNullable(event.getJDA().getUserById(configJpa.getBotOwnerId())).ifPresentOrElse(
                user -> user.openPrivateChannel().queue(
                        channel -> channel.sendMessage("The event channel has been deleted. Please set a new event channel.").queue()
                ),
                () -> log.error("Failed to find bot owner with configured id. The event channel has been deleted.")
        );
    }
}
