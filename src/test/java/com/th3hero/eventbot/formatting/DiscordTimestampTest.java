package com.th3hero.eventbot.formatting;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiscordTimestampTest {
    private static final LocalDateTime DATE = LocalDateTime.of(2024, 5, 13, 21, 33);
    private static final Long TIMESTAMP = DATE.atZone(DateFormatter.ZONE_ID).toEpochSecond();

    @ParameterizedTest
    @EnumSource(DiscordTimestamp.class)
    void create(DiscordTimestamp style) {
        String expected = "<t:%d%s>".formatted(TIMESTAMP, style.getStyle());
        String actual = DiscordTimestamp.create(style, DATE);
        assertEquals(expected, actual);
    }

}
