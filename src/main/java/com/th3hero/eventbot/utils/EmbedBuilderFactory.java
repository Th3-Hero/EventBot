package com.th3hero.eventbot.utils;

import com.th3hero.eventbot.commands.Command;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class EmbedBuilderFactory {
    private static final Color BLUE = new Color(3, 123, 252);

    public static MessageEmbed help() {
        EmbedBuilder embedBuilder =  new EmbedBuilder()
                .setColor(BLUE)
                .setTitle("Help:");

        for (Command command : Command.values()) {
            embedBuilder.addField(
                    command.name(),
                    Command.DESCRIPTIONS.get(command.toString()),
                    false
            );
        }

        return embedBuilder.build();
    }
}
