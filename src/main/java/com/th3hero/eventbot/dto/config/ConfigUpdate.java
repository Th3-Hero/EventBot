package com.th3hero.eventbot.dto.config;


import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents the configuration send to the rest api when wanting to update the configuration
 *
 * @param eventChannel id of the discord event channel
 * @param deletedEventCleanupDelay delay in hours before deleted events are removed from the database
 * @param draftCleanupDelay delay in hours before drafts are removed from the database
 */
@Schema(
    name = "ConfigUpdate",
    description = "Represents the configuration uploaded when updating the configuration for the bot. Any blank fields will not be updated."
)
public record ConfigUpdate(
    @Schema(description = "The id for the discord channel in which the bot will post events") Long eventChannel,
    @Schema(description = "The delay in which events will be deleted and unrecoverable after") Integer deletedEventCleanupDelay,
    @Schema(description = "The delay in which unconfirmed drafts will be cleaned up") Integer draftCleanupDelay
) {}
