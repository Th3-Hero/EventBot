package com.th3hero.eventbot.dto;

import com.th3hero.eventbot.dto.course.Course;
import lombok.NonNull;
import com.th3hero.eventbot.entities.EventJpa;

import java.util.Date;

public record Event(
        @NonNull Integer id,
        @NonNull String title,
        String description,
        @NonNull Date date,
        @NonNull Course course,
        @NonNull EventJpa.EventType type
) {}
