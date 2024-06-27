package com.th3hero.eventbot.dto.management;

import jakarta.validation.constraints.NotNull;

public record AnnouncementField(
    @NotNull String title,
    @NotNull String content
) {
}
