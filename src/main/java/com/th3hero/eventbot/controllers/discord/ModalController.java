package com.th3hero.eventbot.controllers.discord;

import com.th3hero.eventbot.commands.ModalRequest;
import com.th3hero.eventbot.services.EventDraftService;
import com.th3hero.eventbot.services.EventService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ModalController extends ListenerAdapter {
    private final EventDraftService eventDraftService;
    private final EventService eventService;

    @Override
    public void onModalInteraction(@NonNull ModalInteractionEvent event) {
        try {
            final ModalRequest request = ModalRequest.create(event);
            commandHandler(request);
        } catch (Exception e) {
            log.error("onModalInteraction", e);
            event.reply(e.getMessage()).setEphemeral(true).queue();
            throw e;
        }
    }

    public void commandHandler(@NonNull final ModalRequest request) {
        try {
            switch (request.modalType()) {
                case CREATE_DRAFT -> eventDraftService.addTitleAndNote(request);
                case EDIT_DRAFT_DETAILS -> eventDraftService.updateDraftDetails(request);
                case EVENT_DELETION_REASON -> eventService.deleteEvent(request);
                case EDIT_EVENT_DETAILS -> eventService.editEventDetails(request);
            }
        } catch (Exception e) {
            request.interaction().reply(e.getMessage()).setEphemeral(true).queue();
            log.error("Modal Handler", e);
            throw e;
        }
    }
}
