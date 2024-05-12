package com.th3hero.eventbot.commands.actions;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.th3hero.eventbot.formatting.InteractionArguments.DRAFT_ID;
import static com.th3hero.eventbot.formatting.InteractionArguments.EVENT_ID;


@Getter
@RequiredArgsConstructor
public enum ButtonAction implements DiscordActionArguments {
    EDIT_DRAFT_DETAILS(List.of(DRAFT_ID)),
    EDIT_DRAFT_COURSES(List.of(DRAFT_ID)),
    DELETE_DRAFT(List.of(DRAFT_ID)),
    CONFIRM_DRAFT(List.of(DRAFT_ID)),
    EDIT_EVENT(List.of(EVENT_ID)),
    EDIT_EVENT_DETAILS(List.of(EVENT_ID)),
    EDIT_EVENT_COURSES(List.of(EVENT_ID)),
    DELETE_EVENT(List.of(EVENT_ID)),
    UNDO_EVENT_DELETION(List.of(EVENT_ID)),
    MARK_COMPLETE(List.of(EVENT_ID));

    private final List<String> requestKeys;

}