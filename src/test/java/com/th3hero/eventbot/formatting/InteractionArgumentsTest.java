package com.th3hero.eventbot.formatting;

import com.th3hero.eventbot.exceptions.ArgumentMappingException;
import com.th3hero.eventbot.exceptions.EventParsingException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class InteractionArgumentsTest {


    @Test
    void createInteractionIdString() {
        var draftId = 423L;
        var targetString = "TEST_INTERACTION_ONE-%s".formatted(draftId);
        var result = InteractionArguments.createInteractionIdString(TestInteractionEnum.TEST_INTERACTION_ONE, draftId);
        assertThat(result).isEqualTo(targetString);
    }

    @Test
    void createInteractionIdString_enumRequiresMultipleArgs() {
        var draftId = 423L;
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            InteractionArguments.createInteractionIdString(TestInteractionEnum.TEST_INTERACTION_TWO, draftId);
        });
    }

    @Test
    void createInteractionIdString_ids() {
        var draftId = 423L;
        var eventId = 123L;
        var targetString = "TEST_INTERACTION_TWO-%s-%s".formatted(eventId, draftId);
        var result = InteractionArguments.createInteractionIdString(TestInteractionEnum.TEST_INTERACTION_TWO, List.of(eventId, draftId));
        assertThat(result).isEqualTo(targetString);
    }

    @Test
    void createInteractionIdString_ids_numberOfArgsMissMatch() {
        List<Long> list = List.of(423L);
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            InteractionArguments.createInteractionIdString(TestInteractionEnum.TEST_INTERACTION_TWO, list);
        });
    }

    @Test
    void parseLongs() {
        List<String> list = List.of("1", "2", "3");
        List<Long> targetList = List.of(1L, 2L, 3L);
        var result = InteractionArguments.parseLongs(list);
        assertThat(result).isEqualTo(targetList);
    }

    @Test
    void parseLongs_nonLongs() {
        List<String> list = List.of("1", "2", "3", "a");
        assertThatExceptionOfType(NumberFormatException.class).isThrownBy(() -> {
            InteractionArguments.parseLongs(list);
        });
    }

    @Test
    void parseArguments() {
        var action = TestInteractionEnum.TEST_INTERACTION_TWO;
        List<String> idArguments = List.of("1", "2");
        var targetMap = Map.of(
            "event_id", 1L,
            "draft_id", 2L
        );
        var result = InteractionArguments.parseArguments(action, idArguments);
        assertThat(result).isEqualTo(targetMap);
    }

    @Test
    void parseArguments_argNumberMissMatch() {
        var action = TestInteractionEnum.TEST_INTERACTION_TWO;
        List<String> idArguments = List.of("1");
        assertThatExceptionOfType(ArgumentMappingException.class).isThrownBy(() -> {
            InteractionArguments.parseArguments(action, idArguments);
        });
    }

    @Test
    void parseArguments_nonLong() {
        var action = TestInteractionEnum.TEST_INTERACTION_TWO;
        List<String> idArguments = List.of("1", "a");
        assertThatExceptionOfType(EventParsingException.class).isThrownBy(() -> {
            InteractionArguments.parseArguments(action, idArguments);
        });
    }
}
