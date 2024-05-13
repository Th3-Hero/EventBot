package com.th3hero.eventbot.dto.config;

public record ConfigUploadUpdate(
        Long eventChannel,
        Long botOwnerId,
        Integer deletedEventCleanupDelay,
        Integer draftCleanupDelay
) {}
