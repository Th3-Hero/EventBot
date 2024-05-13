package com.th3hero.eventbot.formatting;

import com.th3hero.eventbot.exceptions.EventParsingException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DateFormatting {

    private static final List<DateTimeFormatter> FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-M-d H:mm"),
            DateTimeFormatter.ofPattern("yyyy/M/d H:mm")
    );

    public static Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(ZoneId.of("America/New_York")).toInstant());
    }

    public static String formattedDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
    public static String formattedDate(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
    public static String formattedTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public static LocalDateTime parseDate(String dateString, String timeString) throws EventParsingException {
        String combinedDateTime = "%s %S".formatted(dateString, timeString);

        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDateTime.parse(combinedDateTime, formatter);
            } catch (Exception e) {
                // Do nothing
            }
        }
        throw new EventParsingException("Unable to parse date and time");
    }
}
