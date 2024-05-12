package com.th3hero.eventbot.controllers.discord;

import com.th3hero.eventbot.commands.requests.InteractionRequest.MessageMode;
import com.th3hero.eventbot.commands.requests.SelectionRequest;
import com.th3hero.eventbot.services.CourseService;
import com.th3hero.eventbot.services.EventDraftService;
import com.th3hero.eventbot.services.EventService;
import com.th3hero.eventbot.utils.DiscordActionUtils;
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
    private final EventService eventService;

    @Override
    public void onStringSelectInteraction(@NonNull StringSelectInteractionEvent event) {
        try {
            final SelectionRequest request = SelectionRequest.fromInteraction(event);
            selectionHandler(request);
        } catch (Exception e) {
            log.error("onStringSelectInteraction", e);
            DiscordActionUtils.textResponse(event, e.getMessage(), true);
            throw e;
        }
    }

    public void selectionHandler(@NotNull final SelectionRequest request) {
        try {
            switch (request.getAction()) {
                case SELECT_COURSES -> courseService.processCourseSelection(request);
                case DRAFT_CREATION, EDIT_DRAFT_COURSES -> eventDraftService.addCoursesToDraft(request);
                case EDIT_EVENT_COURSES -> eventService.editEventCourses(request);
                default -> log.warn("Received an unsupported selection type: {}", request.getEvent().getSelectMenu().getId());
            }
        } catch (Exception e) {
            log.error("Selection Handler", e);
            request.sendResponse(e.getMessage(), MessageMode.USER);
            throw e;
        }
    }
}
