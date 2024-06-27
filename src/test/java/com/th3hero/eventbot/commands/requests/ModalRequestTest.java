package com.th3hero.eventbot.commands.requests;

import com.th3hero.eventbot.TestEntities;
import com.th3hero.eventbot.commands.actions.ModalAction;
import com.th3hero.eventbot.exceptions.IllegalInteractionException;
import com.th3hero.eventbot.formatting.InteractionArguments;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModalRequestTest {

    @Test
    void fromInteraction() {
        final var interactionEvent = mock(ModalInteractionEvent.class);
        final var member = mock(Member.class);
        final var guild = TestEntities.guild();

        when(interactionEvent.getModalId())
            .thenReturn("CREATE_DRAFT-1234");
        when(interactionEvent.getMember())
            .thenReturn(member);
        when(interactionEvent.getGuild())
            .thenReturn(guild);

        final var request = ModalRequest.fromInteraction(interactionEvent);

        assertThat(request.getEvent()).isEqualTo(interactionEvent);
        assertThat(request.getRequester()).isEqualTo(member);
        assertThat(request.getServer()).isEqualTo(guild);
        assertThat(request.getAction()).isEqualTo(ModalAction.CREATE_DRAFT);
        assertThat(request.getArguments()).containsEntry(InteractionArguments.DRAFT_ID, 1234L);
    }

    @Test
    void fromInteraction_unsupportedAction() {
        final var interactionEvent = mock(ModalInteractionEvent.class);

        when(interactionEvent.getModalId())
            .thenReturn("INVALID_ACTION-1234");

        assertThatExceptionOfType(IllegalInteractionException.class)
            .isThrownBy(() -> ModalRequest.fromInteraction(interactionEvent));
    }

    @Test
    void fromInteraction_invalidButtonArgs() {
        final var interactionEvent = mock(ModalInteractionEvent.class);
        final var member = mock(Member.class);
        final var guild = TestEntities.guild();

        when(interactionEvent.getModalId())
            .thenReturn("CREATE_DRAFT-invalid_id");
        when(interactionEvent.getMember())
            .thenReturn(member);
        when(interactionEvent.getGuild())
            .thenReturn(guild);

        assertThatExceptionOfType(IllegalInteractionException.class)
            .isThrownBy(() -> ModalRequest.fromInteraction(interactionEvent))
            .withMessage("Failed to parse arguments");
    }
}