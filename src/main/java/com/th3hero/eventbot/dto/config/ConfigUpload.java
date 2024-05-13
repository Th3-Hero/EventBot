package com.th3hero.eventbot.dto.config;

import com.th3hero.eventbot.entities.ConfigJpa;
import lombok.NonNull;

public record ConfigUpload(
        @NonNull Long eventChannel,
        @NonNull Long botOwnerId,
        Integer deletedEventCleanupDelay,
        Integer draftCleanupDelay
) {
    public ConfigJpa toJpa() {
        return ConfigJpa.builder()
                .eventChannel(eventChannel)
                .botOwnerId(botOwnerId)
                .deletedEventCleanupDelay(deletedEventCleanupDelay)
                .draftCleanupDelay(draftCleanupDelay)
                .build();
    }
}
