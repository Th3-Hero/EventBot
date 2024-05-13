package com.th3hero.eventbot;

import com.th3hero.eventbot.entities.ConfigJpa;
import com.th3hero.eventbot.entities.EventJpa;

import java.time.LocalDateTime;

public class TestEntities {

    public static EventJpa createEventJpa() {
        return EventJpa.builder()
                .id(1234L)
                .title("Test Event")
                .note("Test Note")
                .authorId(1234L)
                .datetime(LocalDateTime.now())
                .type(EventJpa.EventType.ASSIGNMENT)
                .build();
    }

    public static ConfigJpa createConfigJpa() {
        return ConfigJpa.builder()
                .id(1234)
                .eventChannel(1234L)
                .build();
    }
}
