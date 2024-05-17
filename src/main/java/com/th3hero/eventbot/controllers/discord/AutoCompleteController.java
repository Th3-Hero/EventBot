package com.th3hero.eventbot.controllers.discord;

import com.kseth.development.util.EnumUtils;
import com.th3hero.eventbot.commands.actions.Command;
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
        Command command = EnumUtils.valueOf(
            Command.class,
            event.getName(),
            new IllegalInteractionException("Failed to parse auto complete interaction: %s".formatted(event.getName()))
        );

        switch (command) {
            case REMINDER_OFFSETS_CONFIG -> studentService.offsetAutoComplete(event);
            case VIEW_EVENTS -> courseService.autoCompleteCourseOptions(event);
            default -> log.warn("Received autocomplete event of unsupported type %s".formatted(event.getName()));
        }
    }
}
