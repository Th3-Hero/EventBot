package com.th3hero.eventbot.formatting;

import com.th3hero.eventbot.commands.actions.DiscordActionArguments;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.th3hero.eventbot.formatting.InteractionArguments.*;

@Getter
@RequiredArgsConstructor
public enum TestInteractionEnum implements DiscordActionArguments {
    TEST_INTERACTION_ONE(List.of(DRAFT_ID)),
    TEST_INTERACTION_TWO(List.of(EVENT_ID, DRAFT_ID));

    private final List<String> requestKeys;
}
