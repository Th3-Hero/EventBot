package com.th3hero.eventbot.commands;

import com.kseth.development.util.EnumUtils;
import com.th3hero.eventbot.exceptions.EventParsingException;
import com.th3hero.eventbot.exceptions.UnsupportedInteractionException;
import lombok.AccessLevel;
import lombok.Builder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectInteraction;

import java.util.ArrayList;
import java.util.List;

@Builder(access = AccessLevel.PRIVATE)
public record SelectionRequest(
        StringSelectInteractionEvent event,
        StringSelectInteraction interaction,
        User requester,
        Guild server,
        Selection selection,
        List<String> idArguments
) {
    public static SelectionRequest create(final StringSelectInteractionEvent event) {
        if (event.getSelectMenu().getId() == null) {
            throw new EventParsingException("Select menu id is null. HOW DID WE GET HERE.");
        }
        List<String> selectionIdArguments = new ArrayList<>(List.of(event.getSelectMenu().getId().split("-")));
        Selection selection = EnumUtils.valueOf(
                Selection.class,
                selectionIdArguments.remove(0),
                new UnsupportedInteractionException("Unsupported interaction with menu %s".formatted(event.getComponentId()))
        );

        return SelectionRequest.builder()
                .event(event)
                .interaction(event.getInteraction())
                .requester(event.getUser())
                .server(event.getGuild())
                .selection(selection)
                .idArguments(selectionIdArguments)
                .build();
    }
}
