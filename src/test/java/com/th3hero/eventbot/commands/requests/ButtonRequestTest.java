package com.th3hero.eventbot.commands.requests;

import com.th3hero.eventbot.TestEntities;
import com.th3hero.eventbot.commands.actions.ButtonAction;
import com.th3hero.eventbot.exceptions.IllegalInteractionException;
import com.th3hero.eventbot.formatting.InteractionArguments;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ButtonRequestTest {

    @Test
    void fromInteraction() {
        final var interactionEvent = mock(ButtonInteractionEvent.class);
        final var button = mock(Button.class);
        final var member = mock(Member.class);
        final var guild = TestEntities.guild();

        when(interactionEvent.getButton())
            .thenReturn(button);
        when(button.getId())
            .thenReturn("TOGGLE_COMPLETED-1234");
        when(interactionEvent.getMember())
            .thenReturn(member);
        when(interactionEvent.getGuild())
            .thenReturn(guild);

        final var request = ButtonRequest.fromInteraction(interactionEvent);

        assertThat(request.getEvent()).isEqualTo(interactionEvent);
        assertThat(request.getRequester()).isEqualTo(member);
        assertThat(request.getServer()).isEqualTo(guild);
        assertThat(request.getAction()).isEqualTo(ButtonAction.TOGGLE_COMPLETED);
        assertThat(request.getArguments()).containsEntry(InteractionArguments.EVENT_ID, 1234L);
    }

    @Test
    void fromInteraction_unsupportedAction() {
        final var interactionEvent = mock(ButtonInteractionEvent.class);
        final var button = mock(Button.class);

        when(interactionEvent.getButton())
            .thenReturn(button);
        when(button.getId())
            .thenReturn("INVALID_ACTION-1234");

        assertThatExceptionOfType(IllegalInteractionException.class)
            .isThrownBy(() -> ButtonRequest.fromInteraction(interactionEvent));
    }

    @Test
    void fromInteraction_invalidButtonArgs() {
        final var interactionEvent = mock(ButtonInteractionEvent.class);
        final var button = mock(Button.class);
        final var member = mock(Member.class);
        final var guild = TestEntities.guild();

        when(interactionEvent.getButton())
            .thenReturn(button);
        when(button.getId())
            .thenReturn("TOGGLE_COMPLETED-not_a_number");
        when(interactionEvent.getMember())
            .thenReturn(member);
        when(interactionEvent.getGuild())
            .thenReturn(guild);

        assertThatExceptionOfType(IllegalInteractionException.class)
            .isThrownBy(() -> ButtonRequest.fromInteraction(interactionEvent))
            .withMessage("Failed to parse arguments");
    }
}