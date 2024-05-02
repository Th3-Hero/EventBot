package com.th3hero.eventbot.commands;

import lombok.Getter;

@Getter
public enum Selection {
    SELECT_COURSES,
    DRAFT_CREATION,
    EDIT_DRAFT;

    public String getDisplayName() {
        return this.toString().toLowerCase();
    }
}
