package com.th3hero.eventbot.commands;

import com.kseth.development.util.EnumUtils;
import com.th3hero.eventbot.exceptions.UnsupportedInteractionException;
import lombok.Builder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectInteraction;

@Builder
public record SelectionRequest(
        StringSelectInteractionEvent event,
        StringSelectInteraction interaction,
        User requester,
        Guild server,
        Selection selection
) {
    public static SelectionRequest create(final StringSelectInteractionEvent event) {
        Selection selection = EnumUtils.valueOf(
                Selection.class,
                event.getSelectMenu().getId(),
                new UnsupportedInteractionException("Unsupported interaction with menu %s".formatted(event.getComponentId()))
        );

        return SelectionRequest.builder()
                .event(event)
                .interaction(event.getInteraction())
                .requester(event.getUser())
                .server(event.getGuild())
                .selection(selection)
                .build();
    }
}
