package com.th3hero.eventbot.controllers.discord;

import com.kseth.development.util.EnumUtils;
import com.th3hero.eventbot.exceptions.UnsupportedInteractionException;
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

    @Override
    public void onCommandAutoCompleteInteraction(@NonNull CommandAutoCompleteInteractionEvent event) {
        SupportedAutoCompleteInteractions interaction = EnumUtils.valueOf(
                SupportedAutoCompleteInteractions.class,
                event.getName(),
                new UnsupportedInteractionException("Failed to parse auto complete interaction: %s".formatted(event.getName()))
        );

        switch (interaction) {
            case REMINDER_OFFSETS_CONFIG -> studentService.offsetAutoComplete(event);
        }
    }


    public enum SupportedAutoCompleteInteractions {
        REMINDER_OFFSETS_CONFIG;
    }

}
