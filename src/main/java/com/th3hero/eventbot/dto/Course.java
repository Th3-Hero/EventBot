package com.th3hero.eventbot.dto;

import lombok.NonNull;

public record Course(
        @NonNull Integer id,
        @NonNull String code,
        @NonNull String name,
        @NonNull String nickname
) {}
