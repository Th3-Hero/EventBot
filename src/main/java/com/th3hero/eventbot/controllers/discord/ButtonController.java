package com.th3hero.eventbot.controllers.discord;

import com.th3hero.eventbot.commands.ButtonRequest;
import com.th3hero.eventbot.services.EventDraftService;
import com.th3hero.eventbot.services.EventService;
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

    public void onButtonInteraction(@NonNull ButtonInteractionEvent event) {
        try {
            final ButtonRequest request = ButtonRequest.create(event);
            buttonHandler(request);
        } catch (Exception e) {
            event.reply(e.getMessage()).setEphemeral(true).queue();
            log.error("onButtonInteraction", e);
            throw e;
        }
    }

    public void buttonHandler(@NonNull final ButtonRequest request) {
        try {
            switch (request.action()) {
                case EDIT_DRAFT_DETAILS -> eventDraftService.sendEditDraftDetailsModal(request);
                case EDIT_DRAFT_COURSES -> eventDraftService.editDraftCourses(request);
                case DELETE_DRAFT -> eventDraftService.deleteDraft(request);
                case CONFIRM_DRAFT -> eventDraftService.confirmDraft(request);
                case EDIT_EVENT -> eventService.sendEventEditOptions(request);
                case EDIT_EVENT_DETAILS -> eventService.sendEditEventDetails(request);
                case EDIT_EVENT_COURSES -> eventService.sendEventEditCourses(request);
                case DELETE_EVENT -> eventService.sendDeleteConformation(request);
                case UNDO_EVENT_DELETION -> eventService.undoEventDeletion(request);
                case MARK_COMPLETE -> eventService.markEventComplete(request);
            }
        } catch (Exception e) {
            request.buttonInteractionEvent().reply(e.getMessage()).setEphemeral(true).queue();
            log.error("Button Handler", e);
            throw e;
        }
    }
}
