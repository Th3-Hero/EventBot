package com.th3hero.eventbot.dto;

import lombok.NonNull;
import com.th3hero.eventbot.entities.EventJpa;

import java.util.Date;

public record EventUpload(
        @NonNull String title,
        String description,
        @NonNull Date date,
        @NonNull CourseUpload course,
        @NonNull EventJpa.EventType type
) {
    public String description() {
        return (this.description == null) ? "" : this.description;
    }

    public EventJpa toJpa() {
        return EventJpa.builder()
                .title(title)
                .description(this.description())
                .datetime(date)
                .course(course.toJpa())
                .type(type)
                .build();
    }
}
