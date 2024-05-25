package com.th3hero.eventbot.controllers.discord;

import com.th3hero.eventbot.commands.actions.Command;
import com.th3hero.eventbot.services.CourseService;
import com.th3hero.eventbot.services.StudentService;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoCompleteControllerTest {
    @Mock
    private StudentService studentService;
    @Mock
    private CourseService courseService;

    @InjectMocks
    private AutoCompleteController autoCompleteController;

    private final CommandAutoCompleteInteractionEvent event = mock(CommandAutoCompleteInteractionEvent.class);

    @Test
    void onCommandAutoCompleteInteraction_reminderOffsets() {
        when(event.getName())
            .thenReturn(Command.REMINDER_OFFSETS_CONFIG.name());

        autoCompleteController.onCommandAutoCompleteInteraction(event);

        verify(studentService).reminderOffsetAutoComplete(event);
        verify(courseService, never()).autoCompleteCourseOptions(event);
    }

    @Test
    void onCommandAutoCompleteInteraction_viewEvents() {
        when(event.getName())
            .thenReturn(Command.VIEW_EVENTS.name());

        autoCompleteController.onCommandAutoCompleteInteraction(event);

        verify(courseService).autoCompleteCourseOptions(event);
        verify(studentService, never()).reminderOffsetAutoComplete(event);
    }

    @Test
    void onCommandAutoCompleteInteraction_unsupportedAutocomplete() {
        when(event.getName())
            .thenReturn(Command.MY_COURSES.name());

        autoCompleteController.onCommandAutoCompleteInteraction(event);

        verify(courseService, never()).autoCompleteCourseOptions(event);
        verify(studentService, never()).reminderOffsetAutoComplete(event);
    }
}