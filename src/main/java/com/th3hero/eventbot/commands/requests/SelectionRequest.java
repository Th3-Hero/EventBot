package com.th3hero.eventbot.commands.requests;

import com.kseth.development.util.EnumUtils;
import com.th3hero.eventbot.commands.actions.SelectionAction;
import com.th3hero.eventbot.exceptions.IllegalInteractionException;
import com.th3hero.eventbot.formatting.InteractionArguments;
import com.th3hero.eventbot.utils.DiscordActionUtils;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Getter
public class SelectionRequest extends InteractionRequest {
    @NonNull
    private final SelectionAction action;
    @NonNull
    private final StringSelectInteractionEvent event;
    @NonNull
    private final Map<String, Long> arguments;

    private SelectionRequest(
        @NonNull SelectionAction action,
        @NonNull StringSelectInteractionEvent event,
        @NonNull Member requester,
        @NonNull Guild server,
        @NonNull Map<String, Long> arguments
    ) {
        super(requester, server);
        this.action = action;
        this.event = event;
        this.arguments = arguments;
    }

    /**
     * Create a selection request from a selection interaction event
     *
     * @param event The event to create the selection request from
     * @return The created selection request
     * @throws IllegalInteractionException If the interaction is not supported
     * @see SelectionAction SelectionAction for supported interactions
     */
    public static SelectionRequest fromInteraction(@NonNull final StringSelectInteractionEvent event) {
        final List<String> selectionIdSplits = List.of(event.getSelectMenu().getId().split("-"));
        final String selectionActionString = selectionIdSplits.getFirst();
        final List<String> idArguments = selectionIdSplits.subList(1, selectionIdSplits.size());
        final SelectionAction action = EnumUtils.valueOf(
            SelectionAction.class,
            selectionActionString,
            new IllegalInteractionException("Unsupported interaction with selection menu %s".formatted(event.getSelectMenu().getId()))
        );

        return new SelectionRequest(
            action,
            event,
            event.getMember(),
            event.getGuild(),
            InteractionArguments.parseArguments(action, idArguments)
        );
    }

    /**
     * Send a response back to the user that made the request. <br>
     * Supported response types:
     * <ul>
     *     <li>String</li>
     *     <li>MessageEmbed</li>
     *     <li>Modal</li>
     *     <li>MessageCreateData</li>
     * </ul>
     *
     * @param response The response to send
     * @param mode The mode used when sending the response
     * @param success Successful response callback
     * @throws IllegalInteractionException If given unsupported interaction
     */
    @Override
    public void sendResponse(@NonNull Object response, MessageMode mode, Consumer<Message> success) {
        switch (response) {
            case String text -> sendTextResponse(text, mode, success);
            case MessageEmbed embed -> sendEmbedResponse(embed, mode, success);
            case Modal modal -> sendModalResponse(modal);
            case MessageCreateData createData -> sendMessageCreateData(createData, mode, success);
            default ->
                throw new IllegalInteractionException("Unsupported selection event response type %s".formatted(response.getClass().getSimpleName()));
        }
    }

    /**
     * Sends a deferred reply to discord
     *
     * @param mode The mode used when later sending the response
     */
    @Override
    public void deferReply(MessageMode mode) {
        DiscordActionUtils.deferResponse(event, MessageMode.USER == mode);
    }

    private void sendTextResponse(final String text, final MessageMode mode, final Consumer<Message> success) {
        if (mode.equals(MessageMode.EVENT_CHANNEL)) {
            MessageChannel channel = getEventChannel();
            channel.sendMessage(text).queue(success);
            DiscordActionUtils.textResponse(event, SENT_TO_EVENT_CHANNEL.formatted(channel.getAsMention()), true);
        } else {
            DiscordActionUtils.textResponse(event, text, true);
        }
        log.debug("Text response sent for selection event {}", action);
    }

    private void sendEmbedResponse(final MessageEmbed embed, final MessageMode mode, final Consumer<Message> success) {
        if (mode.equals(MessageMode.EVENT_CHANNEL)) {
            MessageChannel channel = getEventChannel();
            channel.sendMessageEmbeds(embed).queue(success);
            DiscordActionUtils.textResponse(event, SENT_TO_EVENT_CHANNEL.formatted(channel.getAsMention()), true);
        } else {
            DiscordActionUtils.embedResponse(event, embed, true);
        }
        log.debug("Embed response sent for selection event {}", action);
    }

    private void sendModalResponse(final Modal modal) {
        if (event.isAcknowledged()) {
            throw new IllegalInteractionException("Cannot send a modal response to an acknowledged interaction");
        }
        DiscordActionUtils.modalResponse(event, modal);
        log.debug("Modal response sent for selection event {}", action);
    }

    private void sendMessageCreateData(final MessageCreateData createData, final MessageMode mode, final Consumer<Message> success) {
        if (mode.equals(MessageMode.EVENT_CHANNEL)) {
            MessageChannel channel = getEventChannel();
            channel.sendMessage(createData).queue(success);
            DiscordActionUtils.textResponse(event, SENT_TO_EVENT_CHANNEL.formatted(channel.getAsMention()), true);
        } else {
            DiscordActionUtils.messageCreateDataResponse(event, createData, true);
        }
        log.debug("MessageCreateData response sent for selection event {}", action);
    }
}
