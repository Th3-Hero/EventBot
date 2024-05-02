package com.th3hero.eventbot.commands;

import lombok.Getter;

@Getter
public enum ButtonAction {
    EDIT_DRAFT_DETAILS,
    EDIT_DRAFT_COURSES,
    DELETE_DRAFT,
    CONFIRM_DRAFT,
    EDIT_EVENT,
    EDIT_EVENT_DETAILS,
    EDIT_EVENT_COURSES,
    DELETE_EVENT,
    MARK_COMPLETE;
}
