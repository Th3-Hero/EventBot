package com.th3hero.eventbot.dto.config;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;


/**
 * Represents the configuration returned by the rest api
 *
 * @param id
 * @param serverId id of the server the event channel is within
 * @param eventChannel id of the discord event channel
 * @param deletedEventCleanupDelay delay in hours before deleted events are removed from the database
 * @param draftCleanupDelay delay in hours before drafts are removed from the database
 */
@Schema(
    name = "Config",
    description = "Represents the configuration returned by the API"
)
public record Config(
    @NotNull Integer id,
    @Schema(description = "The id of the server the event channel is within") @NotNull Long serverId,
    @Schema(description = "The id for the discord channel in which the bot will post events") @NotNull Long eventChannel,
    @Schema(description = "The delay in which events will be deleted and unrecoverable after") @NotNull Integer deletedEventCleanupDelay,
    @Schema(description = "The delay in which unconfirmed drafts will be cleaned up") @NotNull Integer draftCleanupDelay
) {
}
