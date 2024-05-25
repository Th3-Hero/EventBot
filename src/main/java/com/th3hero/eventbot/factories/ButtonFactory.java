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

    public static ActionRow draftButtons(Long draftId) {
        return ActionRow.of(
            Button.primary(InteractionArguments.createInteractionIdString(ButtonAction.EDIT_DRAFT_DETAILS, draftId), EDIT_DETAILS),
            Button.primary(InteractionArguments.createInteractionIdString(ButtonAction.EDIT_DRAFT_COURSES, draftId), EDIT_COURSES),
            Button.danger(InteractionArguments.createInteractionIdString(ButtonAction.DELETE_DRAFT, draftId), "Delete Draft"),
            Button.success(InteractionArguments.createInteractionIdString(ButtonAction.CONFIRM_DRAFT, draftId), "Confirm Draft")
        );
    }

    public static ActionRow eventButtons(Long eventId) {
        return ActionRow.of(
            Button.success(InteractionArguments.createInteractionIdString(ButtonAction.MARK_COMPLETE, eventId), "Mark Complete"),
            Button.primary(InteractionArguments.createInteractionIdString(ButtonAction.EDIT_EVENT, eventId), "Edit Event"),
            Button.danger(InteractionArguments.createInteractionIdString(ButtonAction.DELETE_EVENT, eventId), "Delete Event")
        );
    }

    public static ActionRow editEventButtons(Long eventId) {
        return ActionRow.of(
            Button.primary(InteractionArguments.createInteractionIdString(ButtonAction.EDIT_EVENT_DETAILS, eventId), EDIT_DETAILS),
            Button.primary(InteractionArguments.createInteractionIdString(ButtonAction.EDIT_EVENT_COURSES, eventId), EDIT_COURSES)
        );
    }
}
