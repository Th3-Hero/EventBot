package com.th3hero.eventbot.controllers.discord;

import com.th3hero.eventbot.commands.requests.InteractionRequest.MessageMode;
import com.th3hero.eventbot.commands.requests.SelectionRequest;
import com.th3hero.eventbot.exceptions.ConfigErrorException;
import com.th3hero.eventbot.exceptions.DataAccessException;
import com.th3hero.eventbot.exceptions.IllegalInteractionException;
import com.th3hero.eventbot.exceptions.UnsupportedResponseException;
import com.th3hero.eventbot.services.ConfigService;
import com.th3hero.eventbot.services.CourseService;
import com.th3hero.eventbot.services.EventDraftService;
import com.th3hero.eventbot.services.EventService;
import com.th3hero.eventbot.utils.DiscordActionUtils;
import jakarta.persistence.EntityNotFoundException;
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
    private final ConfigService configService;
    private final CourseService courseService;
    private final EventDraftService eventDraftService;
    private final EventService eventService;

    @Override
    public void onStringSelectInteraction(@NonNull StringSelectInteractionEvent event) {
        try {
            final SelectionRequest request = SelectionRequest.fromInteraction(event);
            selectionHandler(request);
        } catch (EntityNotFoundException e) {
            DiscordActionUtils.textResponse(event, e.getMessage(), true);
            log.debug(e.getMessage(), e);
        } catch (DataAccessException | IllegalInteractionException | ConfigErrorException e) {
            DiscordActionUtils.textResponse(event, e.getMessage(), true);
            log.error(e.getMessage(), e);
        } catch (UnsupportedResponseException e) {
            DiscordActionUtils.textResponse(event, "Failed to respond to the interaction.", true);
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error("SelectionMenuId: {}", event.getSelectMenu().getId());
            log.error("Options: {}", event.getValues());
            DiscordActionUtils.textResponse(event, e.getMessage(), true);
            throw e;
        }
    }

    public void selectionHandler(@NotNull final SelectionRequest request) {
        request.addEventChannel(configService.getConfigJpa().getEventChannel());
        switch (request.getAction()) {
            case SELECT_COURSES -> courseService.processStudentSelectedCourses(request);
            case DRAFT_CREATION, EDIT_DRAFT_COURSES -> eventDraftService.setCoursesOnDraft(request);
            case EDIT_EVENT_COURSES -> eventService.editEventCourses(request);
            default ->
                log.error("Received an unsupported selection type: {}", request.getEvent().getSelectMenu().getId());
        }
    }
}
