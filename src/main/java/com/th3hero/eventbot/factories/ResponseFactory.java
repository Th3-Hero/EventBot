package com.th3hero.eventbot.factories;

import com.th3hero.eventbot.entities.EventDraftJpa;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

@NoArgsConstructor(access = AccessLevel.NONE)
public final class ResponseFactory {

    /**
     * Create a response with the given embed and components
     * @param embed The embed which will be displayed
     * @param components The components attached to the message
     * @return The message data
     */
    public static MessageCreateData createResponse(MessageEmbed embed, ItemComponent... components) {
        return new MessageCreateBuilder()
            .addEmbeds(embed)
            .addActionRow(components)
            .build();
    }

    public static MessageCreateData editOptionsResponse(Long eventId) {
        return new MessageCreateBuilder()
            .addComponents(ButtonFactory.editEventButtons(eventId))
            .build();
    }

    public static MessageCreateData draftPost(EventDraftJpa draft, int cleanupDelay, String author) {
        return new MessageCreateBuilder()
            .addEmbeds(EmbedBuilderFactory.displayEventDraft(draft, cleanupDelay, author))
            .addComponents(ButtonFactory.draftButtons(draft.getId()))
            .build();
    }

}
