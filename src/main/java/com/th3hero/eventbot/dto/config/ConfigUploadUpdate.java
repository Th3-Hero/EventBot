package com.th3hero.eventbot.dto.config;


/**
 * Represents the configuration send to the rest api when wanting to update the configuration
 *
 * @param eventChannel id of the discord event channel
 * @param botOwnerId id of the bot owner
 * @param deletedEventCleanupDelay delay in hours before deleted events are removed from the database
 * @param draftCleanupDelay delay in hours before drafts are removed from the database
 */
public record ConfigUploadUpdate(
    Long eventChannel,
    Long botOwnerId,
    Integer deletedEventCleanupDelay,
    Integer draftCleanupDelay
) {
}
