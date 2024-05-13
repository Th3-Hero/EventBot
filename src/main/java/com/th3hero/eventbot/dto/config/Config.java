package com.th3hero.eventbot.dto.config;

import lombok.NonNull;

public record Config(
        @NonNull Integer id,
        @NonNull Long eventChannel,
        @NonNull Long botOwnerId,
        @NonNull Integer deletedEventCleanupDelay,
        @NonNull Integer draftCleanupDelay
) {}
