package com.th3hero.eventbot.commands;

import com.kseth.development.util.EnumUtils;
import com.th3hero.eventbot.exceptions.EventParsingException;
import com.th3hero.eventbot.exceptions.UnsupportedInteractionException;
import lombok.AccessLevel;
import lombok.Builder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonInteraction;

import java.util.ArrayList;
import java.util.List;

@Builder(access = AccessLevel.PRIVATE)
public record ButtonRequest (
    ButtonInteractionEvent buttonInteractionEvent,
    ButtonInteraction buttonInteraction,
    User requester,
    ButtonAction action,
    List<String> idArguments
) {
    public static ButtonRequest create(final ButtonInteractionEvent event) {
        if (event.getButton().getId() == null)  {
            throw new EventParsingException("Button id is null. HOW DID WE GET HERE.");
        }
        List<String> buttonIdArguments = new ArrayList<>(List.of(event.getButton().getId().split("-")));
        ButtonAction action = EnumUtils.valueOf(
                ButtonAction.class,
                buttonIdArguments.remove(0),
                new UnsupportedInteractionException("Unsupported interaction with button %s".formatted(event.getButton().getId()))
        );

        return ButtonRequest.builder()
                .buttonInteractionEvent(event)
                .buttonInteraction(event.getInteraction())
                .requester(event.getUser())
                .action(action)
                .idArguments(buttonIdArguments)
                .build();
    }
}
