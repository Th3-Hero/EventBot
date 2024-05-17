package com.th3hero.eventbot.formatting;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.EnumUtils;

import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.NONE)
public final class EnumParser {

    /**
     * Parses a string value into an enum of the specified type.
     *
     * @param enumType the class of the enum to parse into
     * @param value the string value to parse
     * @return an {@link Optional} containing the parsed enum value if successful, or an empty Optional if not
     */
    public static <T extends Enum<T>> Optional<T> parseEnum(final Class<T> enumType, final String value) {
        return Optional.ofNullable(EnumUtils.getEnumIgnoreCase(enumType, value));
    }
}
