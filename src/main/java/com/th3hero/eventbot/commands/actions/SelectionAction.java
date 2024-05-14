package com.th3hero.eventbot.commands.actions;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.th3hero.eventbot.formatting.InteractionArguments.DRAFT_ID;
import static com.th3hero.eventbot.formatting.InteractionArguments.EVENT_ID;

@Getter
@RequiredArgsConstructor
public enum SelectionAction implements DiscordActionArguments {
    SELECT_COURSES(List.of()),
    DRAFT_CREATION(List.of(DRAFT_ID)),
    EDIT_DRAFT_COURSES(List.of(DRAFT_ID)),
    EDIT_EVENT_COURSES(List.of(EVENT_ID));

    /**
     * The keys that the request should contain
     */
    private final List<String> requestKeys;
}
