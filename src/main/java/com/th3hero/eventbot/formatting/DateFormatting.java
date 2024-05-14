package com.th3hero.eventbot.formatting;

import com.th3hero.eventbot.exceptions.EventParsingException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DateFormatting {

    public static final ZoneId ZONE_ID = ZoneId.of("America/New_York");

    private static final List<DateTimeFormatter> FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-M-d H:mm"),
            DateTimeFormatter.ofPattern("yyyy/M/d H:mm")
    );

    public static Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(ZONE_ID).toInstant());
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

    public static String formattedDateTimeWithTimestamp(LocalDateTime dateTime) {
        return "%s (%s)".formatted(
                formattedDateTime(dateTime),
                DiscordTimestamp.create(DiscordTimestamp.RELATIVE, dateTime)
        );
    }

    public static Optional<LocalDateTime> parseDate(String dateString, String timeString){
        String combinedDateTime = "%s %S".formatted(dateString, timeString);

        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return Optional.of(LocalDateTime.parse(combinedDateTime, formatter));
            } catch (Exception e) {
                // Do nothing
            }
        }
        return Optional.empty();
    }
}
