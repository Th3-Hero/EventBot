package com.th3hero.eventbot.dto.management;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record Announcement(
    @NotNull String title,
    String description,
    List<AnnouncementField> fields
) {
}
