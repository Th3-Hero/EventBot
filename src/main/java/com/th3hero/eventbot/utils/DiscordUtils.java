package com.th3hero.eventbot.utils;

import com.th3hero.eventbot.entities.ConfigJpa;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.NONE)
public final class DiscordUtils {

    public static String generateJumpUrl(final ConfigJpa config, final Long messageId) {
        return "https://discord.com/channels/%d/%d/%d".formatted(config.getServerId(), config.getEventChannel(), messageId);
    }

}
