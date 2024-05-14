package com.th3hero.eventbot;

import com.th3hero.eventbot.entities.ConfigJpa;
import com.th3hero.eventbot.entities.CourseJpa;
import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.entities.StudentJpa;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    public static CourseJpa courseJpa(int seed) {
        return CourseJpa.builder()
                .code("TEST%s".formatted(seed))
                .name("Test Course%s".formatted(seed))
                .nickname("Test%s".formatted(seed))
                .build();
    }

    public static EventJpa eventJpa(int seed, List<CourseJpa> courses) {
        return EventJpa.builder()
                .authorId(1234L+seed)
                .messageId(1234L+seed)
                .title("Test Event%s".formatted(seed))
                .note("Test Note%s".formatted(seed))
                .datetime(LocalDateTime.of(2025, 1, 1, 1, 1, 1))
                .type(EventJpa.EventType.ASSIGNMENT)
                .courses(new ArrayList<>(courses))
                .build();
    }

    public static StudentJpa studentJpa(int seed, List<CourseJpa> courses) {
        return StudentJpa.builder()
                .id(1234L+seed)
                .offsetTimes(List.of(24, 72))
                .courses(new ArrayList<>(courses))
                .build();
    }
}
