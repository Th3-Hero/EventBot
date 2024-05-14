package com.th3hero.eventbot.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Provides a set of keys to be used for field identification in Discord messages.
 * Along with a set of constraints for field sizes and values.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DiscordFieldsConfig {
    public static final String TITLE_ID = "title";
    public static final String NOTE_ID = "note";
    public static final String DATE_ID = "date";
    public static final String TIME_ID = "time";
    public static final String TYPE_ID = "type";
    public static final String OFFSET_ID = "offset";
    public static final String REASON_ID = "reason";
    public static final String UPCOMING_ID = "upcoming";
    public static final String COURSE_ID = "course";
    public static final String TIME_PERIOD_ID = "time_period";

    public static final int MIN_TITLE_LENGTH = 4;
    public static final int MAX_TITLE_LENGTH = 128;
    public static final int MAX_NOTE_LENGTH = 1024;
    public static final int MIN_DATE_LENGTH = 8;
    public static final int MAX_DATE_LENGTH = 10;
    public static final int MIN_TIME_LENGTH = 4;
    public static final int MAX_TIME_LENGTH = 5;
    public static final int MIN_REASON_LENGTH = 5;
    public static final int MAX_REASON_LENGTH = 256;

    public static final int MIN_OFFSET_VALUE = 0;
    public static final int MIN_FILTER_VALUE = 1;
}
