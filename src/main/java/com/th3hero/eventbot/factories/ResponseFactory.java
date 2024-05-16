package com.th3hero.eventbot.factories;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.Collection;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ResponseFactory {

    public static MessageCreateData createResponse(MessageEmbed embed, ItemComponent... components) {
        return new MessageCreateBuilder()
            .addEmbeds(embed)
            .addActionRow(components)
            .build();
    }

    public static MessageCreateData createResponse(Collection<MessageEmbed> embeds, ItemComponent... components) {
        return new MessageCreateBuilder()
            .addEmbeds(embeds)
            .addActionRow(components)
            .build();
    }

}
