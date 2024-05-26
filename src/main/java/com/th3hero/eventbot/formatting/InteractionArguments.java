package com.th3hero.eventbot.formatting;

import com.th3hero.eventbot.commands.actions.DiscordActionArguments;
import com.th3hero.eventbot.exceptions.DataAccessException;
import com.th3hero.eventbot.exceptions.IllegalInteractionException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.NONE)
public final class InteractionArguments {
    public static final String DRAFT_ID = "draft_id";
    public static final String EVENT_ID = "event_id";

    /**
     * Creates an interaction ID string for a DiscordActionArguments enum value and a single ID.
     *
     * @param enumValue The DiscordActionArguments enum value.
     * @param id The ID to include in the interaction ID string.
     * @return The created interaction ID string to be used in discord interactions.
     */
    public static String createInteractionIdString(DiscordActionArguments enumValue, Long id) {
        // currently there is only requests with one key, later on this may change and another method can be made for a collection of longs
        if (enumValue.getRequestKeys().size() != 1) {
            throw new IllegalArgumentException("Enum value must have exactly one request key");
        }
        return "%s-%s".formatted(enumValue, id);
    }

    /**
     * Parses a list of string arguments into a map of request keys and long values.
     * The DiscordActionArguments enum value provides the expected request keys.
     * The number of arguments must match the number of expected request keys.
     *
     * @param action The DiscordActionArguments enum value providing the expected request keys.
     * @param idArguments The list of string arguments to parse.
     * @return A map of request keys and long values.
     * @throws DataAccessException If the number of arguments does not match the number of request keys.
     * @throws IllegalInteractionException If any argument cannot be parsed to a long.
     */
    public static Map<String, Long> parseArguments(final DiscordActionArguments action, final List<String> idArguments) {
        if (idArguments.size() != action.getRequestKeys().size()) {
            throw new DataAccessException("Number of request arguments(%d) does not match expected(%d) for action %s".formatted(action.getRequestKeys().size(), idArguments.size(), action.name()));
        }

        List<Long> args;
        try {
            args = idArguments.stream()
                .map(Long::parseLong)
                .toList();
        } catch (NumberFormatException e) {
            throw new IllegalInteractionException("Failed to parse arguments");
        }

        Map<String, Long> mappedArgs = new HashMap<>();
        for (int i = 0; i < action.getRequestKeys().size(); i++) {
            mappedArgs.put(action.getRequestKeys().get(i), args.get(i));
        }
        return mappedArgs;
    }
}
