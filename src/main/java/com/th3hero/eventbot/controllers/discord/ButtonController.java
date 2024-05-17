package com.th3hero.eventbot.controllers.discord;

import com.th3hero.eventbot.commands.requests.ButtonRequest;
import com.th3hero.eventbot.commands.requests.InteractionRequest.MessageMode;
import com.th3hero.eventbot.exceptions.DataAccessException;
import com.th3hero.eventbot.services.EventDraftService;
import com.th3hero.eventbot.services.EventService;
import com.th3hero.eventbot.utils.DiscordActionUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ButtonController extends ListenerAdapter {
    private final EventDraftService eventDraftService;
    private final EventService eventService;

    private static final String DEFAULT_ERROR_RESPONSE = "An unexpected error occurred";

    @Override
    public void onButtonInteraction(@NonNull ButtonInteractionEvent event) {
        try {
            final ButtonRequest request = ButtonRequest.fromInteraction(event);
            buttonHandler(request);
        } catch (Exception e) {
            log.error("onButtonInteraction", e);
            DiscordActionUtils.textResponse(event, e.getMessage(), true);
            throw e;
        }
    }

    public void buttonHandler(@NonNull final ButtonRequest request) {
        try {
            switch (request.getAction()) {
                case EDIT_DRAFT_DETAILS, EDIT_DRAFT_COURSES, CONFIRM_DRAFT ->
                    eventDraftService.eventDraftHandler(request);
                case DELETE_DRAFT -> eventDraftService.deleteDraft(request);
                case EDIT_EVENT -> eventService.sendEventEditOptions(request);
                case EDIT_EVENT_DETAILS -> eventService.sendEditEventDetails(request);
                case EDIT_EVENT_COURSES -> eventService.sendEventEditCourses(request);
                case DELETE_EVENT -> eventService.sendDeleteConformation(request);
                case UNDO_EVENT_DELETION -> eventService.undoEventDeletion(request);
                case MARK_COMPLETE -> eventService.markEventComplete(request);
                default ->
                    log.warn("Received an unsupported button action: {}", request.getEvent().getButton().getId());
            }
        } catch (EntityNotFoundException e) {
            request.sendResponse(e.getMessage(), MessageMode.USER);
            log.debug(e.getMessage());
        } catch (DataAccessException e) {
            request.sendResponse(e.getMessage(), MessageMode.USER);
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error("Button Handler", e);
            request.sendResponse(DEFAULT_ERROR_RESPONSE, MessageMode.USER);
            throw e;
        }
    }
}
