package com.th3hero.eventbot.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DiscordFieldsConfig {
    public static final String TITLE_ID = "title";
    public static final String NOTE_ID = "note";
    public static final String DATE_ID = "date";
    public static final String TIME_ID = "time";
    public static final String TYPE_ID = "type";
    public static final String OFFSET_ID = "offset";

    public static final int MIN_TITLE_LENGTH = 4;
    public static final int MAX_TITLE_LENGTH = 128;
    public static final int MAX_NOTE_LENGTH = 1024;
    public static final int MIN_DATE_LENGTH = 8;
    public static final int MAX_DATE_LENGTH = 10;
    public static final int MIN_TIME_LENGTH = 4;
    public static final int MAX_TIME_LENGTH = 5;

    public static final int MIN_OFFSET_VALUE = 0;
}
