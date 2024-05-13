package com.th3hero.eventbot.formatting;

import com.th3hero.eventbot.exceptions.EventParsingException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

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
    void parseDate() {
        final var targetDateTime = LocalDateTime.of(2024, 5, 20, 4, 30);
        var dateStringFormat1 = "2024-05-20";
        var dateStringFormat2 = "2024-5-20";
        var dateStringFormat3 = "2024/05/20";
        var dateStringFormat4 = "2024/5/20";
        var timeStringFormat1 = "04:30";
        var timeStringFormat2 = "4:30";

        var result = DateFormatting.parseDate(dateStringFormat1, timeStringFormat1);
        var result2 = DateFormatting.parseDate(dateStringFormat1, timeStringFormat2);
        var result3 = DateFormatting.parseDate(dateStringFormat2, timeStringFormat1);
        var result4 = DateFormatting.parseDate(dateStringFormat3, timeStringFormat1);
        var result5 = DateFormatting.parseDate(dateStringFormat4, timeStringFormat1);

        assertThat(result).isEqualTo(targetDateTime);
        assertThat(result2).isEqualTo(targetDateTime);
        assertThat(result3).isEqualTo(targetDateTime);
        assertThat(result4).isEqualTo(targetDateTime);
        assertThat(result5).isEqualTo(targetDateTime);
    }

    @Test
    void parseDate_invalidDate() {
        var dateString = "2024-555-3123";
        var timeString = "14:30";
        assertThatExceptionOfType(EventParsingException.class).isThrownBy(() -> DateFormatting.parseDate(dateString, timeString));
    }
}
