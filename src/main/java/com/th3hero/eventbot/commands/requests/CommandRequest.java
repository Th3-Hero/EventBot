package com.th3hero.eventbot.commands.requests;

import com.kseth.development.util.EnumUtils;
import com.th3hero.eventbot.commands.actions.Command;
import com.th3hero.eventbot.exceptions.IllegalInteractionException;
import com.th3hero.eventbot.exceptions.UnsupportedInteractionException;
import com.th3hero.eventbot.utils.DiscordActionUtils;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Getter
public class CommandRequest extends InteractionRequest {
    @NotNull
    private final Command command;
    @NonNull
    private final SlashCommandInteractionEvent event;
    @NonNull
    private final Map<String, String> arguments;

    private static final String SENT_TO_EVENT_CHANNEL = "Result has been sent to the event channel %s";

    private CommandRequest(
            Command command,
            SlashCommandInteractionEvent event,
            Member requester,
            Guild server,
            Map<String, String> arguments
    ) {
        super(requester, server);
        this.command = command;
        this.event = event;
        this.arguments = arguments;
    }

    /**
     * Create a command request from a slash command interaction event
     *
     * @param event The event to create the command request from
     * @return The created command request
     * @throws UnsupportedInteractionException If the interaction is not supported
     * @see Command Command for supported interactions
     */
    public static CommandRequest fromInteraction(@NonNull final SlashCommandInteractionEvent event) throws UnsupportedInteractionException {
        final Command command = EnumUtils.valueOf(
                Command.class,
                event.getName(),
                new UnsupportedInteractionException("Unsupported interaction with command %s".formatted(event.getName()))
        );

        Map<String, String> arguments = parseOptions(event.getOptions());
        if (event.getSubcommandName() != null) {
            arguments.put("sub_command", event.getSubcommandName().toLowerCase());
        }

        return new CommandRequest(command, event, event.getMember(), event.getGuild(), arguments);
    }

    private static Map<String, String> parseOptions(final List<OptionMapping> optionMappings) {
        final Map<String, String> arguments = new HashMap<>();
        for (final var option : optionMappings) {
            arguments.put(option.getName().toLowerCase(), option.getAsString());
        }
        return arguments;
    }


    /**
     * Send a response back to the user that made the request. <br>
     * Supported response types:<br>
     * - String <br>
     * - MessageEmbed <br>
     * - Modal <br>
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
            case Modal modal -> sendModalResponse(modal);
            case MessageCreateData createData -> sendMessageCreateData(createData, mode, success);
            default -> throw new UnsupportedInteractionException("Unable to process response of type '%s' for slash commands".formatted(response.getClass().getSimpleName()));
        }
    }


    /**
     * Sends a deferred reply to discord
     *
     * @param mode The mode used when later sending the response
     */
    @Override
    public void deferReply(final MessageMode mode) {
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
        log.debug("Text response sent for slash command {}", command);
    }

    private void sendEmbedResponse(final MessageEmbed embed, final MessageMode mode, final Consumer<Message> success) {
        if (mode.equals(MessageMode.EVENT_CHANNEL)) {
            MessageChannel channel = getEventChannel();
            channel.sendMessageEmbeds(embed).queue(success);
            DiscordActionUtils.textResponse(event, SENT_TO_EVENT_CHANNEL.formatted(channel.getAsMention()), true);
        } else {
            DiscordActionUtils.embedResponse(event, embed, true);
        }
        log.debug("Embed response sent for slash command {}", command);
    }

    private void sendModalResponse(final Modal modal) {
        if (event.isAcknowledged()) {
            throw new IllegalInteractionException("Events that have been acknowledged cannot be responded to with an exception");
        }
        DiscordActionUtils.modalResponse(event, modal);
        log.debug("Modal response sent for slash command {}", command);
    }

    private void sendMessageCreateData(final MessageCreateData createData, final MessageMode mode, final Consumer<Message> success) {
        if (mode.equals(MessageMode.EVENT_CHANNEL)) {
            MessageChannel channel = getEventChannel();
            channel.sendMessage(createData).queue(success);
            DiscordActionUtils.textResponse(event, SENT_TO_EVENT_CHANNEL.formatted(channel.getAsMention()), true);
        } else {
            DiscordActionUtils.messageCreateDataResponse(event, createData, true);
        }
        log.debug("MessageCreateData response sent for slash command {}", command);
    }

}