package com.th3hero.eventbot.commands;

import lombok.Getter;

@Getter
public enum Selection {
    SELECT_COURSES;

    public String getDisplayName() {
        return this.toString().toLowerCase();
    }
}
