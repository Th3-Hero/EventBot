package com.th3hero.eventbot.dto.config;

import com.th3hero.eventbot.entities.ConfigJpa;
import jakarta.validation.constraints.NotNull;

/**
 * Represents the configuration uploaded to the rest api
 *
 * @param eventChannel id of the discord event channel
 * @param botOwnerId id of the bot owner
 * @param deletedEventCleanupDelay delay in hours before deleted events are removed from the database
 * @param draftCleanupDelay delay in hours before drafts are removed from the database
 */
public record ConfigUpload(
    @NotNull Long eventChannel,
    @NotNull Long botOwnerId,
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
