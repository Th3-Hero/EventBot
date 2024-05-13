package com.th3hero.eventbot.services;


import com.th3hero.eventbot.commands.actions.ButtonAction;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DeletionHandler {
    private final EventRepository eventRepository;
    private final ConfigService configService;

    @Value("${app.config.bot-owner-id}")
    private static Long botOwnerId;

    public void handleDeletedMessage(MessageDeleteEvent event) {
        Long deletedMessageId = event.getMessageIdLong();

        if (!eventRepository.existsByMessageId(deletedMessageId)) {
            return;
        }

        EventJpa eventJpa = eventRepository.findEventJpaByMessageId(deletedMessageId)
                .orElseThrow(() -> new EntityNotFoundException("Event with deleted message was not found in the database. Message id: %d".formatted(deletedMessageId)));
        Long eventChannelId = configService.getConfigJpa().getEventChannel();
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
        Long eventChannelId = configService.getConfigJpa().getEventChannel();
        Long deletedChannelId = event.getChannel().getIdLong();
        if (!eventChannelId.equals(deletedChannelId)) {
            return;
        }

        Optional.ofNullable(event.getJDA().getUserById(botOwnerId)).ifPresentOrElse(
                user -> user.openPrivateChannel().queue(
                        channel -> channel.sendMessage("The event channel has been deleted. Please set a new event channel.").queue()
                ),
                () -> log.error("Failed to find bot owner with configured id. The event channel has been deleted.")
        );
    }
}
