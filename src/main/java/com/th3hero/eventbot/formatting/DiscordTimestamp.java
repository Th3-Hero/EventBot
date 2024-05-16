package com.th3hero.eventbot.formatting;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public enum DiscordTimestamp {
    DEFAULT(""),
    SHORT_TIME(":t"),
    LONG_TIME(":T"),
    SHORT_DATE(":d"),
    LONG_DATE(":D"),
    SHORT_DATE_TIME(":f"),
    LONG_DATE_TIME(":F"),
    RELATIVE(":R");

    private final String style;

    /**
     * @param style The style of the timestamp
     * @param date The date to format
     *
     * @return A formatted timestamp string
     * @see <a href="https://discord.com/developers/docs/reference#message-formatting-timestamp-styles">Discord Timestamp Styles</a>
     */
    public static String create(DiscordTimestamp style, LocalDateTime date) {
        return "<t:%d%s>".formatted(
            date.atZone(DateFormatting.ZONE_ID).toEpochSecond(),
            style.getStyle()
        );
    }
}
