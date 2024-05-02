package com.th3hero.eventbot.utils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter(AccessLevel.PRIVATE)
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

    public static String create(DiscordTimestamp style, LocalDateTime date) {
        return "<t:%d%s>".formatted(
                date.atZone(ZoneId.of("America/New_York")).toEpochSecond(),
                style.getStyle()
        );
    }
}
