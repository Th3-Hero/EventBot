package com.th3hero.eventbot.dto.course;

import jakarta.validation.constraints.NotNull;

public record Course(
    @NotNull Long id,
    @NotNull String code,
    @NotNull String name
) {
}
