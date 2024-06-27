package com.th3hero.eventbot.commands.requests;

import com.th3hero.eventbot.TestEntities;
import com.th3hero.eventbot.commands.actions.Command;
import com.th3hero.eventbot.exceptions.IllegalInteractionException;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommandRequestTest {

    @Test
    void fromInteraction() {
        final var interactionEvent = mock(SlashCommandInteractionEvent.class);
        final var member = mock(Member.class);
        final var guild = TestEntities.guild();

        when(interactionEvent.getName())
            .thenReturn("CREATE_EVENT");
        when(interactionEvent.getMember())
            .thenReturn(member);
        when(interactionEvent.getGuild())
            .thenReturn(guild);

        final var request = CommandRequest.fromInteraction(interactionEvent);

        assertThat(request.getEvent()).isEqualTo(interactionEvent);
        assertThat(request.getRequester()).isEqualTo(member);
        assertThat(request.getServer()).isEqualTo(guild);
        assertThat(request.getCommand()).isEqualTo(Command.CREATE_EVENT);
        assertThat(request.getArguments()).isEmpty();
    }

    @Test
    void fromInteraction_unsupportedCommand() {
        final var interactionEvent = mock(SlashCommandInteractionEvent.class);

        when(interactionEvent.getName())
            .thenReturn("NOT_A_COMMAND");

        assertThatExceptionOfType(IllegalInteractionException.class)
            .isThrownBy(() -> CommandRequest.fromInteraction(interactionEvent));
    }
}