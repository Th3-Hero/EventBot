package com.th3hero.eventbot.controllers.discord;

import com.th3hero.eventbot.commands.SelectionRequest;
import com.th3hero.eventbot.services.CourseService;
import com.th3hero.eventbot.services.EventDraftService;
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
    private final EventDraftService eventDraftService;

    @Override
    public void onStringSelectInteraction(@NonNull StringSelectInteractionEvent event) {
        try {
            final SelectionRequest request = SelectionRequest.create(event);
            selectionHandler(request);
        } catch (Exception e) {
            log.error("onStringSelectInteraction", e);
            event.reply(e.getMessage()).setEphemeral(true).queue();
            throw e;
        }
    }

    public void selectionHandler(@NotNull final SelectionRequest request) {
        try {
            switch (request.selection()) {
                case SELECT_COURSES -> courseService.processCourseSelection(request);
                case DRAFT_CREATION, EDIT_DRAFT -> eventDraftService.addCoursesToDraft(request);
            }
        } catch (Exception e) {
            request.interaction().reply(e.getMessage()).setEphemeral(true).queue();
            log.error("Selection Handler", e);
            throw e;
        }
    }
}
