package com.th3hero.eventbot.commands;

import lombok.Getter;

import java.util.Map;

@Getter
public enum Command {
    HELP,
    SELECT_COURSES,
    MY_COURSES,
    CREATE_EVENT,
    REMINDER_OFFSETS_CONFIG;

    public String getDisplayName() {
        return this.toString().toLowerCase();
    }

    public static final Map<String, String> DESCRIPTIONS = Map.ofEntries(
            Map.entry(HELP.getDisplayName(), "Displays this help embed"),
            Map.entry(SELECT_COURSES.getDisplayName(), "Select courses you wish to be notified for"),
            Map.entry(MY_COURSES.getDisplayName(), "Displays all courses you are currently subscribed to for notification"),
            Map.entry(CREATE_EVENT.getDisplayName(), "Create a new event"),
            Map.entry(REMINDER_OFFSETS_CONFIG.getDisplayName(), "List, Add, and Remove reminder offsets(how many hours before an event ou are notified)")
    );
}
