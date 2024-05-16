package com.th3hero.eventbot.formatting;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.EnumUtils;

import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EnumParser {

    public static <T extends Enum<T>> Optional<T> parseEnum(final Class<T> enumType, final String value) {
        return Optional.ofNullable(EnumUtils.getEnumIgnoreCase(enumType, value));
    }
}
