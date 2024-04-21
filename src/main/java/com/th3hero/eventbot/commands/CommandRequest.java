package com.th3hero.eventbot.commands;

import com.kseth.development.util.EnumUtils;
import com.th3hero.eventbot.exceptions.UnsupportedInteractionException;
import lombok.Builder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Builder
public record CommandRequest(
        SlashCommandInteractionEvent event,
        User requester,
        Guild server,
        Command command,
        Map<String, String> arguments) {
    public static CommandRequest create(final SlashCommandInteractionEvent event) {
        Command command = EnumUtils.valueOf(
                Command.class,
                event.getName(),
                new UnsupportedInteractionException("Unsupported interaction with command %s".formatted(event.getName()))
        );

        Map<String, String> arguments = parseOptions(event.getOptions());
        if (event.getSubcommandName() != null) {
            arguments.put("sub_command", event.getSubcommandName().toLowerCase());
        }

        return CommandRequest.builder()
                .event(event)
                .requester(event.getUser())
                .server(event.getGuild())
                .command(command)
                .arguments(arguments)
                .build();
    }

    private static Map<String, String> parseOptions(final List<OptionMapping> optionMappings) {
        final Map<String, String> arguments = new HashMap<>();
        for (final var option : optionMappings) {
            arguments.put(option.getName().toLowerCase(), option.getAsString());
        }
        return arguments;
    }
}
