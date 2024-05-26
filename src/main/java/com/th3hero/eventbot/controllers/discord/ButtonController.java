package com.th3hero.eventbot.controllers.discord;

import com.th3hero.eventbot.commands.requests.ButtonRequest;
import com.th3hero.eventbot.commands.requests.InteractionRequest.MessageMode;
import com.th3hero.eventbot.exceptions.ConfigErrorException;
import com.th3hero.eventbot.exceptions.DataAccessException;
import com.th3hero.eventbot.exceptions.IllegalInteractionException;
import com.th3hero.eventbot.exceptions.UnsupportedResponseException;
import com.th3hero.eventbot.services.ConfigService;
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
    private final ConfigService configService;
    private final EventDraftService eventDraftService;
    private final EventService eventService;

    @Override
    public void onButtonInteraction(@NonNull ButtonInteractionEvent event) {
        try {
            final ButtonRequest request = ButtonRequest.fromInteraction(event);
            buttonHandler(request);
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
            log.error("ButtonId: {}", event.getButton().getId());
            DiscordActionUtils.textResponse(event, DiscordActionUtils.DEFAULT_ERROR_RESPONSE, true);
            throw e;
        }
    }

    public void buttonHandler(@NonNull final ButtonRequest request) {
        request.addEventChannel(configService.getConfigJpa().getEventChannel());

        switch (request.getAction()) {
            case EDIT_DRAFT_DETAILS, EDIT_DRAFT_COURSES, CONFIRM_DRAFT ->
                eventDraftService.handleEventDraftActions(request);
            case DELETE_DRAFT -> eventDraftService.deleteDraft(request);
            case EDIT_EVENT -> eventService.sendEventEditOptions(request);
            case EDIT_EVENT_DETAILS -> eventService.sendEditEventDetails(request);
            case EDIT_EVENT_COURSES -> eventService.sendEventEditCourses(request);
            case DELETE_EVENT -> eventService.sendDeleteConformation(request);
            case UNDO_EVENT_DELETION -> eventService.undoEventDeletion(request);
            case MARK_COMPLETE -> eventService.markEventComplete(request);
            default ->
                log.error("Received an unsupported button action: {}", request.getEvent().getButton().getId());
        }
    }
}
