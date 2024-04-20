package com.th3hero.eventbot.dto;

import com.th3hero.eventbot.entities.EventJpa;
import lombok.NonNull;

import java.util.Date;
import java.util.UUID;

public record Event(
        @NonNull UUID id,
        @NonNull String title,
        String description,
        @NonNull Date date,
        @NonNull Course course,
        @NonNull EventJpa.EventType type
) {}
