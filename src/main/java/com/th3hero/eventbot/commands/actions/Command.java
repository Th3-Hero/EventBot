package com.th3hero.eventbot.commands.actions;

import lombok.Getter;

import java.util.Map;

@Getter
public enum Command {
    HELP,
    SELECT_COURSES,
    MY_COURSES,
    CREATE_EVENT,
    REMINDER_OFFSETS_CONFIG,
    VIEW_EVENTS,
    TEST_NOTIFICATION;

    public String getDisplayName() {
        return this.toString().toLowerCase();
    }

    public static final Map<String, String> DESCRIPTIONS = Map.ofEntries(
        Map.entry(HELP.getDisplayName(), "Displays this help embed"),
        Map.entry(SELECT_COURSES.getDisplayName(), "Select courses you wish to be notified for"),
        Map.entry(MY_COURSES.getDisplayName(), "Displays all courses you currently have selected to receive notifications for"),
        Map.entry(CREATE_EVENT.getDisplayName(), "Create a new event"),
        Map.entry(REMINDER_OFFSETS_CONFIG.getDisplayName(), "List, Add, and Remove reminder offsets(how many hours before an event you are notified)"),
        Map.entry(VIEW_EVENTS.getDisplayName(), "Get a list of upcoming events. Filter by course, number of events, and time period."),
        Map.entry(TEST_NOTIFICATION.getDisplayName(), "Test to make sure you are receiving notifications from the bot properly.")
    );
}
