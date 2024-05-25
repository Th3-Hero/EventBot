package com.th3hero.eventbot.formatting;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.NONE)
public final class DateFormatter {

    public static final String DATE_FORMAT_EXAMPLE = "Event dates are formatted as year, month, day. Eg. 2025-4-24 or 2025/4/24.";
    public static final String TIME_FORMAT_EXAMPLE = "Event times are in 24 hour time. Eg. 14:30.";

    public static final ZoneId ZONE_ID = ZoneId.of("America/New_York");

    private static final List<DateTimeFormatter> FORMATTERS = List.of(
        DateTimeFormatter.ofPattern("yyyy-M-d H:mm"),
        DateTimeFormatter.ofPattern("yyyy/M/d H:mm")
    );

    /**
     * Converts a LocalDateTime to a Date.
     *
     * @param dateTime The LocalDateTime to convert.
     * @return The converted Date.
     */
    public static Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(ZONE_ID).toInstant());
    }

    /**
     * Formats a LocalDateTime to a String with the pattern "yyyy-MM-dd HH:mm".
     *
     * @param dateTime The LocalDateTime to format.
     * @return The formatted String.
     */
    public static String formattedDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    /**
     * Formats a LocalDateTime to a String with the pattern "yyyy-MM-dd".
     *
     * @param dateTime The LocalDateTime to format.
     * @return The formatted String.
     */
    public static String formattedDate(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }


    /**
     * Formats a LocalDateTime to a String with the pattern "HH:mm".
     *
     * @param dateTime The LocalDateTime to format.
     * @return The formatted String.
     */
    public static String formattedTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    /**
     * Formats a LocalDateTime to a String with a discord timestamp.
     *
     * @param dateTime The LocalDateTime to format.
     * @return The formatted String with timestamp.
     */
    public static String formattedDateTimeWithTimestamp(LocalDateTime dateTime) {
        return "%s (%s)".formatted(
            formattedDateTime(dateTime),
            TimeFormat.RELATIVE.format(dateTime.atZone(ZONE_ID))
        );
    }

    /**
     * Parses a date and time String to a LocalDateTime. Supported formats:
     * <ul>
     *     <li>yyyy-M-d H:mm</li>
     *     <li>yyyy/M/d H:mm</li>
     * </ul>
     *
     * @param dateString The date String to parse.
     * @param timeString The time String to parse.
     * @return LocalDateTime if the date and time Strings are valid, otherwise null.
     */
    public static LocalDateTime parseDate(String dateString, String timeString) {
        String combinedDateTime = "%s %s".formatted(dateString, timeString);

        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDateTime.parse(combinedDateTime, formatter);
            } catch (Exception e) {
                // Do nothing
            }
        }
        return null;
    }
}
