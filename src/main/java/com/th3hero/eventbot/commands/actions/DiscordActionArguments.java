package com.th3hero.eventbot.commands.actions;

import org.apache.commons.lang3.EnumUtils;

import java.util.List;
import java.util.Optional;

public interface DiscordActionArguments {

    String name();

    /**
     * @return A list of the keys that the request should contain
     */
    List<String> getRequestKeys();

    static <T extends Enum<T>> Optional<T> actionFrom(Class<T> enumType, String name) {
        return Optional.ofNullable(EnumUtils.getEnumIgnoreCase(enumType, name));
    }
}
