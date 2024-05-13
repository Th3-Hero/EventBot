package com.th3hero.eventbot.commands.requests;

import com.kseth.development.util.EnumUtils;
import com.th3hero.eventbot.commands.actions.ModalAction;
import com.th3hero.eventbot.exceptions.UnsupportedInteractionException;
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
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Getter
public class ModalRequest extends InteractionRequest {
    @NonNull
    private final ModalAction action;
    @NonNull
    private final ModalInteractionEvent event;
    @NonNull
    private final Map<String, Long> arguments;

    private static final String SENT_TO_EVENT_CHANNEL = "Result has been sent to the event channel %s";

    private ModalRequest(
            @NonNull ModalAction action,
            @NonNull ModalInteractionEvent event,
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
     * Create a modal request from a modal interaction event
     *
     * @param event The event to create the modal request from
     * @return The created modal request
     * @throws UnsupportedInteractionException If the interaction is not supported
     * @see ModalAction ModalAction for supported interactions
     */
    public static ModalRequest fromInteraction(@NonNull final ModalInteractionEvent event) {
        final List<String> modalIdSplits = List.of(event.getModalId().split("-"));
        final String modalActionString = modalIdSplits.subList(0, 1).getFirst();
        final List<String> idArguments = modalIdSplits.subList(1, modalIdSplits.size());
        final ModalAction action = EnumUtils.valueOf(
                ModalAction.class,
                modalActionString,
                new UnsupportedInteractionException("Unsupported interaction with modal %s".formatted(event.getModalId()))
        );

        return new ModalRequest(
                action,
                event,
                event.getMember(),
                event.getGuild(),
                InteractionArguments.parseArguments(action, idArguments)
        );
    }

    /**
     * Send a response back to the user that made the request. <br>
     * Supported response types:<br>
     * - String <br>
     * - MessageEmbed <br>
     * - MessageCreateData <br>
     *
     * @param response The response to send
     * @param mode The mode used when sending the response
     * @param success Successful response callback
     * @throws UnsupportedInteractionException If given unsupported interaction
     */
    @Override
    public void sendResponse(@NonNull Object response, MessageMode mode, Consumer<Message> success) {
        switch (response) {
            case String text -> sendTextResponse(text, mode, success);
            case MessageEmbed embed -> sendEmbedResponse(embed, mode, success);
            case MessageCreateData createData -> sendMessageCreateData(createData, mode, success);
            default -> throw new UnsupportedInteractionException("Unable to process response of type '%s' for modal interactions".formatted(response.getClass().getSimpleName()));
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
        log.debug("Text response sent for modal event {}", action);
    }

    private void sendEmbedResponse(final MessageEmbed embed, final MessageMode mode, final Consumer<Message> success) {
        if (mode.equals(MessageMode.EVENT_CHANNEL)) {
            MessageChannel channel = getEventChannel();
            channel.sendMessageEmbeds(embed).queue(success);
            DiscordActionUtils.textResponse(event, SENT_TO_EVENT_CHANNEL.formatted(channel.getAsMention()), true);
        } else {
            DiscordActionUtils.embedResponse(event, embed, true);
        }
    }

    private void sendMessageCreateData(final MessageCreateData createData, final MessageMode mode, final Consumer<Message> success) {
        if (mode.equals(MessageMode.EVENT_CHANNEL)) {
            MessageChannel channel = getEventChannel();
            channel.sendMessage(createData).queue(success);
            DiscordActionUtils.textResponse(event, SENT_TO_EVENT_CHANNEL.formatted(channel.getAsMention()), true);
        } else {
            DiscordActionUtils.messageCreateDataResponse(event, createData, true);
        }
    }
}
