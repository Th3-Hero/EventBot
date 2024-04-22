package com.th3hero.eventbot.controllers.discord;

import com.th3hero.eventbot.commands.SelectionRequest;
import com.th3hero.eventbot.services.CourseService;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SelectionController extends ListenerAdapter {
    private final CourseService courseService;

    @Override
    public void onStringSelectInteraction(@NonNull StringSelectInteractionEvent event) {
        try {
            final SelectionRequest request = SelectionRequest.create(event);
            selectionHandler(request);
        } catch (Exception e) {
            log.error("onStringSelectInteraction", e);
        }
    }

    public void selectionHandler(@NotNull final SelectionRequest request) {
        switch (request.selection()) {
            case SELECT_COURSES -> courseService.processCourseSelection(request);
        }
    }
}
