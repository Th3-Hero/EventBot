package com.th3hero.eventbot.formatting;

import com.th3hero.eventbot.commands.actions.DiscordActionArguments;
import com.th3hero.eventbot.exceptions.ArgumentMappingException;
import com.th3hero.eventbot.exceptions.EventParsingException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.NONE)
public class InteractionArguments {
    public static final String DRAFT_ID = "draft_id";
    public static final String EVENT_ID = "event_id";

    public static String createInteractionIdString(DiscordActionArguments enumValue, Long id) {
        if (enumValue.getRequestKeys().size() != 1) {
            throw new IllegalArgumentException("Enum value must have exactly one request key");
        }
        return "%s-%s".formatted(enumValue, id);
    }
    public static String createInteractionIdString(DiscordActionArguments enumValue, List<Long> ids) {
        if (enumValue.getRequestKeys().size() != ids.size()) {
            throw new IllegalArgumentException("Enum value must have exactly one request key");
        }
        String joinedIds = ids.stream()
                .map(Objects::toString)
                .collect(Collectors.joining("-"));
        return "%s-%s".formatted(enumValue.name(), joinedIds);
    }


//    public static <T extends Enum<T>> String createInteractionIdString(T enumValue, Long id) {
//        return "%s-%s".formatted(enumValue.name(), id);
//    }
//    public static <T extends Enum<T>> String createInteractionIdString(T enumValue, List<Long> ids) {
//        String joinedIds = ids.stream()
//                .map(Objects::toString)
//                .collect(Collectors.joining("-"));
//        return "%s-%s".formatted(enumValue.name(), joinedIds);
//    }

    public static List<Long> parseLongs(List<String> list) {
        return list.stream()
                .map(Long::parseLong)
                .toList();
    }

    public static Map<String, Long> parseArguments(final DiscordActionArguments action, final List<String> idArguments) {
        if (idArguments.size() != action.getRequestKeys().size()) {
            throw new ArgumentMappingException("Number of request arguments does not match expected");
        }

        List<Long> args;
        try {
            args = parseLongs(idArguments);
        } catch (NumberFormatException e) {
            throw new EventParsingException("Failed to parse arguments");
        }

        Map<String, Long> mappedArgs = new HashMap<>();
        for (int i = 0; i < action.getRequestKeys().size(); i++) {
            mappedArgs.put(action.getRequestKeys().get(i), args.get(i));
        }
        return mappedArgs;
    }
}
