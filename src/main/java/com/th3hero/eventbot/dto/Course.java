package com.th3hero.eventbot.dto;

import lombok.NonNull;

import java.util.UUID;

public record Course(
        @NonNull UUID id,
        @NonNull String code,
        @NonNull String name,
        @NonNull String nickname
) {}
