package com.th3hero.eventbot.dto.config;

import lombok.NonNull;


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
    @NonNull Integer id,
    @NonNull Long eventChannel,
    @NonNull Long botOwnerId,
    @NonNull Integer deletedEventCleanupDelay,
    @NonNull Integer draftCleanupDelay
) {
}
