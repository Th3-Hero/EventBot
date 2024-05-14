package com.th3hero.eventbot.formatting;

import com.th3hero.eventbot.exceptions.EventParsingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

class DateFormattingTest {


    @Test
    void toDate() {
        var dateTime = LocalDateTime.of(2024, 5, 20, 14, 30);
        var result = DateFormatting.toDate(dateTime);
        assertThat(result).isNotNull();
    }

    @Test
    void formattedDateTime() {
        var dateTime = LocalDateTime.of(2024, 5, 20, 14, 30);
        var result = DateFormatting.formattedDateTime(dateTime);
        assertThat(result).isEqualTo("2024-05-20 14:30");
    }

    @Test
    void formattedDate() {
        var dateTime = LocalDateTime.of(2024, 5, 20, 14, 30);
        var result = DateFormatting.formattedDate(dateTime);
        assertThat(result).isEqualTo("2024-05-20");
    }

    @Test
    void formattedTime() {
        var dateTime = LocalDateTime.of(2024, 5, 20, 14, 30);
        var result = DateFormatting.formattedTime(dateTime);
        assertThat(result).isEqualTo("14:30");
    }

    @Test
    void formattedDateTimeWithTimestamp() {
        var dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(1715649720L), ZoneId.of("America/New_York"));
        var result = DateFormatting.formattedDateTimeWithTimestamp(dateTime);
        assertThat(result).isEqualTo("2024-05-13 21:22 (<t:1715649720:R>)");
    }

    static Stream<Arguments> parseDateArguments() {
        final var targetDateTime = LocalDateTime.of(2024, 5, 20, 4, 30);
        return Stream.of(
                Arguments.of("2024-05-20", "04:30", targetDateTime),
                Arguments.of("2024-05-20", "4:30", targetDateTime),
                Arguments.of("2024-5-20", "04:30", targetDateTime),
                Arguments.of("2024/05/20", "04:30", targetDateTime),
                Arguments.of("2024/5/20", "04:30", targetDateTime)
        );
    }

    @ParameterizedTest
    @MethodSource("parseDateArguments")
    void parseDate(String date, String time, LocalDateTime expected) throws EventParsingException {
        var result = DateFormatting.parseDate(date, time);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void parseDate_invalidDate() {
        var dateString = "2024-555-3123";
        var timeString = "14:30";
        assertThatExceptionOfType(EventParsingException.class).isThrownBy(() -> DateFormatting.parseDate(dateString, timeString));
    }

}
