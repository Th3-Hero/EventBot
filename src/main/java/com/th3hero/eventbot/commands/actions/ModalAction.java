package com.th3hero.eventbot.commands.actions;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.th3hero.eventbot.formatting.InteractionArguments.DRAFT_ID;
import static com.th3hero.eventbot.formatting.InteractionArguments.EVENT_ID;

@Getter
@RequiredArgsConstructor
public enum ModalAction implements DiscordActionArguments{
    CREATE_DRAFT(List.of(DRAFT_ID)),
    EDIT_DRAFT_DETAILS(List.of(DRAFT_ID)),
    EVENT_DELETION_REASON(List.of(EVENT_ID)),
    EDIT_EVENT_DETAILS(List.of(EVENT_ID));

    private final List<String> requestKeys;
}
