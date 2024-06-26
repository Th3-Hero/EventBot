package com.th3hero.eventbot.controllers.discord;

import com.th3hero.eventbot.commands.requests.ModalRequest;
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
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Controller;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ModalController extends ListenerAdapter {
    private final ConfigService configService;
    private final EventDraftService eventDraftService;
    private final EventService eventService;

    @Override
    public void onModalInteraction(@NonNull ModalInteractionEvent event) {
        try {
            final ModalRequest request = ModalRequest.fromInteraction(event);
            commandHandler(request);
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
            log.error("ModalId: {}", event.getModalId());
            List<String> options = event.getValues().stream()
                .map(option -> "%s: %s".formatted(option.getId(), option.getAsString()))
                .toList();
            log.error("Options: {}", options);
            DiscordActionUtils.textResponse(event, DiscordActionUtils.DEFAULT_ERROR_RESPONSE, true);
            throw e;
        }
    }

    public void commandHandler(@NonNull final ModalRequest request) {
        request.addEventChannel(configService.getConfigJpa().getEventChannel());
        switch (request.getAction()) {
            case CREATE_DRAFT -> eventDraftService.addDraftDetails(request);
            case EDIT_DRAFT_DETAILS -> eventDraftService.updateDraftDetails(request);
            case EVENT_DELETION_REASON -> eventService.handleDeleteConformation(request);
            case EDIT_EVENT_DETAILS -> eventService.editEventDetails(request);
            default -> log.error("Received an unsupported modal type: {}", request.getEvent().getModalId());
        }
    }
}
