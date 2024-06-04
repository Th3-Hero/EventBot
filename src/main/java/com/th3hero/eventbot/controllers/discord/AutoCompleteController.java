package com.th3hero.eventbot.controllers.discord;

import com.th3hero.eventbot.commands.actions.Command;
import com.th3hero.eventbot.commands.actions.DiscordActionArguments;
import com.th3hero.eventbot.exceptions.IllegalInteractionException;
import com.th3hero.eventbot.services.CourseService;
import com.th3hero.eventbot.services.StudentService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AutoCompleteController extends ListenerAdapter {
    private final StudentService studentService;
    private final CourseService courseService;

    @Override
    public void onCommandAutoCompleteInteraction(@NonNull CommandAutoCompleteInteractionEvent event) {
        final Command command = DiscordActionArguments.from(Command.class, event.getName())
            .orElseThrow(() ->  new IllegalInteractionException("Failed to parse auto complete interaction: %s".formatted(event.getName())));

        switch (command) {
            case REMINDER_OFFSETS_CONFIG -> studentService.reminderOffsetAutoComplete(event);
            case VIEW_EVENTS -> courseService.autoCompleteCourseOptions(event);
            default -> log.error("Received autocomplete event of unsupported type %s from command %s".formatted(event.getName(), event.getFullCommandName()));
        }
    }
}
