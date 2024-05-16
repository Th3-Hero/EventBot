package com.th3hero.eventbot.dto.course;

import lombok.NonNull;

public record Course(
    @NonNull Long id,
    @NonNull String code,
    @NonNull String name,
    @NonNull String nickname
) {
}
