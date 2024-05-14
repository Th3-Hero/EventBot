package com.th3hero.eventbot.controllers.discord;

import com.th3hero.eventbot.TestEntities;
import com.th3hero.eventbot.commands.actions.ButtonAction;
import com.th3hero.eventbot.exceptions.UnsupportedInteractionException;
import com.th3hero.eventbot.formatting.InteractionArguments;
import com.th3hero.eventbot.services.EventDraftService;
import com.th3hero.eventbot.services.EventService;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ButtonControllerTest {
    @Mock
    private EventDraftService eventDraftService;
    @Mock
    private EventService eventService;

    @InjectMocks
    private ButtonController buttonController;

    private final ButtonInteractionEvent event = mock(ButtonInteractionEvent.class);

    @Test
    void onButtonInteraction_failedCommandRequest() {
        final var event = mock(ButtonInteractionEvent.class);

        when(event.getButton())
                .thenReturn(mock(Button.class));
        when(event.getButton().getId())
                .thenReturn("INVALID_BUTTON_ACTION-4234");
        when(event.isAcknowledged())
                .thenReturn(false);
        when(event.reply(anyString()))
                .thenReturn(mock(ReplyCallbackAction.class));
        when(event.reply(anyString()).setEphemeral(anyBoolean()))
                .thenReturn(mock(ReplyCallbackAction.class));

        assertThrows(UnsupportedInteractionException.class, () -> buttonController.onButtonInteraction(event));
    }

    @Test
    void buttonHandler() {

    }

    private static ButtonInteractionEvent event(
            final ButtonAction action
    ) {
        final var event = mock(ButtonInteractionEvent.class);
        final var guild = TestEntities.guild();
        final var member = TestEntities.member(guild);

        when(event.getButton())
                .thenReturn(mock(Button.class));
        when(event.getButton().getId())
                .thenReturn(InteractionArguments.createInteractionIdString(action, 1234L));

        when(event.getMember())
                .thenReturn(member);
        when(event.getGuild())
                .thenReturn(guild);

        return event;
    }

}