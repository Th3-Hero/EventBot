package com.th3hero.eventbot.commands.requests;

import com.kseth.development.util.EnumUtils;
import com.th3hero.eventbot.commands.actions.ButtonAction;
import com.th3hero.eventbot.exceptions.IllegalInteractionException;
import com.th3hero.eventbot.exceptions.UnsupportedResponseException;
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
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Getter
public class ButtonRequest extends InteractionRequest {
    @NonNull
    private final ButtonAction action;
    @NonNull
    private final ButtonInteractionEvent event;
    @NonNull
    private final Map<String, Long> arguments;

    private ButtonRequest(
        @NonNull ButtonAction action,
        @NonNull ButtonInteractionEvent event,
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
     * Create a button request from a button interaction event
     *
     * @param event The event to create the button request from
     * @return The created button request
     * @throws IllegalInteractionException If the interaction is not supported
     * @see ButtonAction ButtonAction for supported interactions
     */
    public static ButtonRequest fromInteraction(@NonNull final ButtonInteractionEvent event) throws IllegalInteractionException {
        final List<String> buttonIdSplits = List.of(event.getButton().getId().split("-"));
        final String buttonActionString = buttonIdSplits.subList(0, 1).getFirst();
        final List<String> idArguments = buttonIdSplits.subList(1, buttonIdSplits.size());
        final ButtonAction action = EnumUtils.valueOf(ButtonAction.class, buttonActionString, new IllegalInteractionException("Unsupported interaction with button %s".formatted(event.getButton().getId())));

        return new ButtonRequest(action, event, event.getMember(), event.getGuild(), InteractionArguments.parseArguments(action, idArguments));
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
     * @throws UnsupportedResponseException If given unsupported interaction
     */
    @Override
    public void sendResponse(@NonNull Object response, MessageMode mode, Consumer<Message> success) {
        switch (response) {
            case String text -> sendTextResponse(text, mode, success);
            case MessageEmbed embed -> sendEmbedResponse(embed, mode, success);
            case Modal modal -> sendModalResponse(modal);
            case MessageCreateData createData -> sendMessageCreateData(createData, mode, success);
            default ->
                throw new UnsupportedResponseException("Unsupported button event response type %s".formatted(response.getClass().getSimpleName()));
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
        log.debug("Text response sent for button event {}", action);
    }

    private void sendEmbedResponse(final MessageEmbed embed, final MessageMode mode, final Consumer<Message> success) {
        if (mode.equals(MessageMode.EVENT_CHANNEL)) {
            MessageChannel channel = getEventChannel();
            channel.sendMessageEmbeds(embed).queue(success);
            DiscordActionUtils.textResponse(event, SENT_TO_EVENT_CHANNEL.formatted(channel.getAsMention()), true);
        } else {
            DiscordActionUtils.embedResponse(event, embed, true);
        }
        log.debug("Embed response sent for button event {}", action);
    }

    private void sendModalResponse(final Modal modal) {
        if (event.isAcknowledged()) {
            throw new IllegalInteractionException("Cannot send a modal response to an acknowledged event");
        }
        DiscordActionUtils.modalResponse(event, modal);
        log.debug("Modal response sent for button event {}", action);
    }

    private void sendMessageCreateData(final MessageCreateData createData, final MessageMode mode, final Consumer<Message> success) {
        if (mode.equals(MessageMode.EVENT_CHANNEL)) {
            MessageChannel channel = getEventChannel();
            channel.sendMessage(createData).queue(success);
            DiscordActionUtils.textResponse(event, SENT_TO_EVENT_CHANNEL.formatted(channel.getAsMention()), true);
        } else {
            DiscordActionUtils.messageCreateDataResponse(event, createData, true);
        }
        log.debug("MessageCreateData response sent for button event {}", action);
    }
}
