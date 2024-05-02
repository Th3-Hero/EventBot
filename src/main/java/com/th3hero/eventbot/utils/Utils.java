package com.th3hero.eventbot.utils;

import com.th3hero.eventbot.exceptions.EventParsingException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.NONE)
public class Utils {

    public static String joinedIds(List<Long> ids) {
        return ids.stream()
                .map(Objects::toString)
                .collect(Collectors.joining("-"));
    }
    public static List<String> splitIds(String s) {
        return Arrays.stream(s.split("-"))
                .toList();
    }
    public static List<Long> splitNumericalIds(String s) {
        return Arrays.stream(s.split("-"))
                .map(Long::parseLong)
                .toList();
    }
    public static <T extends Enum<T>> String createInteractionIdString(T enumValue, List<Long> ids) {
        return enumValue.name() + "-" + joinedIds(ids);
    }
    public static <T extends Enum<T>> String createInteractionIdString(T enumValue, Long id) {
        return enumValue.name() + "-" + id;
    }

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
        List<String> acceptedDateFormats = List.of(
                "yyyy-M-d H:mm",
                "yyyy/M/d H:mm"
        );

        for (String format : acceptedDateFormats) {
            try {
                String combinedDateTime = "%s %S".formatted(dateString, timeString);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                return LocalDateTime.parse(combinedDateTime, formatter);
            } catch (Exception e) {
                throw new EventParsingException("Unable to parse date and time");
            }
        }
        throw new EventParsingException("Unable to parse date and time");
    }
}

