package com.th3hero.eventbot.commands.requests;

import com.th3hero.eventbot.TestEntities;
import com.th3hero.eventbot.commands.actions.SelectionAction;
import com.th3hero.eventbot.exceptions.IllegalInteractionException;
import com.th3hero.eventbot.formatting.InteractionArguments;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SelectionRequestTest {

    @Test
    void fromInteraction() {
        final var interactionEvent = mock(StringSelectInteractionEvent.class);
        final var member = mock(Member.class);
        final var guild = TestEntities.guild();
        final var menu = mock(StringSelectMenu.class);

        when(interactionEvent.getSelectMenu())
            .thenReturn(menu);
        when(menu.getId())
            .thenReturn("DRAFT_CREATION-1234");
        when(interactionEvent.getMember())
            .thenReturn(member);
        when(interactionEvent.getGuild())
            .thenReturn(guild);

        final var request = SelectionRequest.fromInteraction(interactionEvent);

        assertThat(request.getEvent()).isEqualTo(interactionEvent);
        assertThat(request.getRequester()).isEqualTo(member);
        assertThat(request.getServer()).isEqualTo(guild);
        assertThat(request.getAction()).isEqualTo(SelectionAction.DRAFT_CREATION);
        assertThat(request.getArguments()).containsEntry(InteractionArguments.DRAFT_ID, 1234L);
    }

    @Test
    void fromInteraction_unsupportedAction() {
        final var interactionEvent = mock(StringSelectInteractionEvent.class);
        final var menu = mock(StringSelectMenu.class);

        when(interactionEvent.getSelectMenu())
            .thenReturn(menu);
        when(menu.getId())
            .thenReturn("INVALID_ACTION-1234");

        assertThatExceptionOfType(IllegalInteractionException.class)
            .isThrownBy(() -> SelectionRequest.fromInteraction(interactionEvent));
    }

    @Test
    void fromInteraction_invalidButtonArgs() {
        final var interactionEvent = mock(StringSelectInteractionEvent.class);
        final var member = mock(Member.class);
        final var guild = TestEntities.guild();
        final var menu = mock(StringSelectMenu.class);

        when(interactionEvent.getSelectMenu())
            .thenReturn(menu);
        when(menu.getId())
            .thenReturn("DRAFT_CREATION-invalid_id");
        when(interactionEvent.getMember())
            .thenReturn(member);
        when(interactionEvent.getGuild())
            .thenReturn(guild);

        assertThatExceptionOfType(IllegalInteractionException.class)
            .isThrownBy(() -> SelectionRequest.fromInteraction(interactionEvent))
            .withMessage("Failed to parse arguments");
    }
}