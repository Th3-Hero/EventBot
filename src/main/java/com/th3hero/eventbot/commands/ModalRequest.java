package com.th3hero.eventbot.commands;

import com.kseth.development.util.EnumUtils;
import com.th3hero.eventbot.exceptions.EventParsingException;
import com.th3hero.eventbot.exceptions.UnsupportedInteractionException;
import lombok.AccessLevel;
import lombok.Builder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.ModalInteraction;

import java.util.ArrayList;
import java.util.List;

@Builder(access = AccessLevel.PRIVATE)
public record ModalRequest(
        ModalInteractionEvent event,
        ModalInteraction interaction,
        User requester,
        Guild server,
        ModalType modalType,
        List<String> idArguments
) {
    public static ModalRequest create(final ModalInteractionEvent event) {
        List<String> modalIdArguments = new ArrayList<>(List.of(event.getModalId().split("-")));
        if (modalIdArguments.isEmpty()) {
            throw new EventParsingException("Failed to parse modal id %s".formatted(event.getModalId()));
        }

        ModalType modalType = EnumUtils.valueOf(
                ModalType.class,
                modalIdArguments.remove(0),
                new UnsupportedInteractionException("Unknown modal interaction %s".formatted(event.getModalId()))
        );

        return ModalRequest.builder()
                .event(event)
                .interaction(event.getInteraction())
                .server(event.getGuild())
                .requester(event.getUser())
                .modalType(modalType)
                .idArguments(modalIdArguments)
                .build();
    }
}
