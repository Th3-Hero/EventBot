package com.th3hero.eventbot.dto.config;

import jakarta.validation.constraints.NotNull;


/**
 * Represents the configuration returned by the rest api
 *
 * @param id
 * @param eventChannel id of the discord event channel
 * @param botOwnerId id of the bot owner
 * @param deletedEventCleanupDelay delay in hours before deleted events are removed from the database
 * @param draftCleanupDelay delay in hours before drafts are removed from the database
 */
public record Config(
    @NotNull Integer id,
    @NotNull Long eventChannel,
    @NotNull Long botOwnerId,
    @NotNull Integer deletedEventCleanupDelay,
    @NotNull Integer draftCleanupDelay
) {
}
