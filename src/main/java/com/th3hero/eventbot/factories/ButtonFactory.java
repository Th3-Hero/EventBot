package com.th3hero.eventbot.factories;

import com.th3hero.eventbot.commands.actions.ButtonAction;
import com.th3hero.eventbot.formatting.InteractionArguments;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

@NoArgsConstructor(access = AccessLevel.NONE)
public final class ButtonFactory {

    private static final String EDIT_DETAILS = "Edit Details";
    private static final String EDIT_COURSES = "Edit Courses";

    /**
     * Creates buttons for a draft letting the user edit, delete, and confirm the draft
     * @param draftId The id of the draft to create buttons for
     * @return An ActionRow with the buttons
     */
    public static ActionRow draftButtons(Long draftId) {
        return ActionRow.of(
            Button.primary(InteractionArguments.createInteractionIdString(ButtonAction.EDIT_DRAFT_DETAILS, draftId), EDIT_DETAILS),
            Button.primary(InteractionArguments.createInteractionIdString(ButtonAction.EDIT_DRAFT_COURSES, draftId), EDIT_COURSES),
            Button.danger(InteractionArguments.createInteractionIdString(ButtonAction.DELETE_DRAFT, draftId), "Delete Draft"),
            Button.success(InteractionArguments.createInteractionIdString(ButtonAction.CONFIRM_DRAFT, draftId), "Confirm Draft")
        );
    }

    /**
     * Creates buttons for an event letting the user mark the event as complete, edit the event, and delete the event
     * @param eventId The id of the event to create buttons for
     * @return An ActionRow with the buttons
     */
    public static ActionRow eventButtons(Long eventId) {
        return ActionRow.of(
            Button.success(InteractionArguments.createInteractionIdString(ButtonAction.TOGGLE_COMPLETED, eventId), "Toggle Completed"),
            Button.primary(InteractionArguments.createInteractionIdString(ButtonAction.EDIT_EVENT, eventId), "Edit Event"),
            Button.danger(InteractionArguments.createInteractionIdString(ButtonAction.DELETE_EVENT, eventId), "Delete Event")
        );
    }

    /**
     * Creates buttons for an event letting the user edit the event details and courses
     * @param eventId The id of the event to create buttons for
     * @return An ActionRow with the buttons
     */
    public static ActionRow editEventButtons(Long eventId) {
        return ActionRow.of(
            Button.primary(InteractionArguments.createInteractionIdString(ButtonAction.EDIT_EVENT_DETAILS, eventId), EDIT_DETAILS),
            Button.primary(InteractionArguments.createInteractionIdString(ButtonAction.EDIT_EVENT_COURSES, eventId), EDIT_COURSES)
        );
    }
}
