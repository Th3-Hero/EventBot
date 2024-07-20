package com.th3hero.eventbot.dto.config;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Represents the configuration uploaded to the rest api
 *
 * @param serverId id of the server the event channel is within
 * @param eventChannel id of the discord event channel
 * @param deletedEventCleanupDelay delay in hours before deleted events are removed from the database
 * @param draftCleanupDelay delay in hours before drafts are removed from the database
 */

@Schema(
    name = "ConfigUpload",
    description = "Represents the configuration uploaded when creating a new configuration for the bot. If optional fields are not provided, defaults will be used."
)
public record ConfigUpload(
    @Schema(description = "The id of the server the event channel is within") @NotNull Long serverId,
    @Schema(description = "The id for the discord channel in which the bot will post events (Required)") @NotNull Long eventChannel,
    @Schema(description = "The delay in which events will be deleted and unrecoverable after", defaultValue = "72") Integer deletedEventCleanupDelay,
    @Schema(description = "The delay in which unconfirmed drafts will be cleaned up", defaultValue = "24") Integer draftCleanupDelay
) {}
