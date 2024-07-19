package com.th3hero.eventbot.factories;

import com.th3hero.eventbot.commands.actions.ModalAction;
import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.formatting.DateFormatter;
import com.th3hero.eventbot.formatting.InteractionArguments;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.time.LocalDateTime;

import static com.th3hero.eventbot.utils.DiscordFieldsUtils.*;

@NoArgsConstructor(access = AccessLevel.NONE)
public final class ModalFactory {
    private static final String TITLE_LABEL = "Event Title";
    private static final String NOTE_LABEL = "Description/Note";
    private static final String DATE_LABEL = "Date";
    private static final String TIME_LABEL = "Time";

    private static final String TITLE_PLACEHOLDER = "Cornelius Samuelson III 113th birthday";
    private static final String NOTE_PLACEHOLDER = "Lunch with Cornelius";
    private static final String DATE_PLACEHOLDER = "2024-4-24";
    private static final String TIME_PLACEHOLDER = "14:30";


    public static Modal draftCreationData(Long eventDraftId) {
        TextInput title = TextInput.create(TITLE, TITLE_LABEL, TextInputStyle.SHORT)
            .setPlaceholder(TITLE_PLACEHOLDER)
            .setRequiredRange(MIN_TITLE_LENGTH, MAX_TITLE_LENGTH)
            .setRequired(true)
            .build();
        TextInput note = TextInput.create(NOTE, NOTE_LABEL, TextInputStyle.PARAGRAPH)
            .setPlaceholder(NOTE_PLACEHOLDER)
            .setMaxLength(MAX_NOTE_LENGTH)
            .setRequired(false)
            .build();

        return Modal.create(InteractionArguments.createInteractionIdString(ModalAction.CREATE_DRAFT, eventDraftId), "Event Creation")
            .addComponents(
                ActionRow.of(title),
                ActionRow.of(note)
            )
            .build();
    }

    public static Modal editDetails(EventJpa eventJpa, ModalAction action) {
        return editDetails(action, eventJpa.getId(), eventJpa.getTitle(), eventJpa.getNote(), eventJpa.getEventDate());
    }

    public static Modal deleteDraftReason(EventJpa eventJpa) {
        TextInput reason = TextInput.create(REASON, "Why are you deleting the event?", TextInputStyle.PARAGRAPH)
            .setPlaceholder("Assignment was cancelled")
            .setRequiredRange(MIN_REASON_LENGTH, MAX_REASON_LENGTH)
            .setRequired(true)
            .build();

        return Modal.create(InteractionArguments.createInteractionIdString(ModalAction.EVENT_DELETION_REASON, eventJpa.getId()), "Reason for event deletion")
            .addComponents(
                ActionRow.of(reason)
            )
            .build();
    }

    private static Modal editDetails(
        ModalAction modalType,
        Long id,
        String title,
        String note,
        LocalDateTime dateTime
    ) {
        TextInput titleInput = TextInput.create(TITLE, TITLE_LABEL, TextInputStyle.SHORT)
            .setPlaceholder(TITLE_PLACEHOLDER)
            .setValue(title)
            .setRequiredRange(MIN_TITLE_LENGTH, MAX_TITLE_LENGTH)
            .setRequired(true)
            .build();
        TextInput noteInput = TextInput.create(NOTE, NOTE_LABEL, TextInputStyle.PARAGRAPH)
            .setPlaceholder(NOTE_PLACEHOLDER)
            .setValue(note)
            .setMaxLength(MAX_NOTE_LENGTH)
            .setRequired(false)
            .build();
        TextInput dateInput = TextInput.create(DATE, DATE_LABEL, TextInputStyle.SHORT)
            .setPlaceholder(DATE_PLACEHOLDER)
            .setRequiredRange(MIN_DATE_LENGTH, MAX_DATE_LENGTH)
            .setValue(DateFormatter.formattedDate(dateTime))
            .setRequired(true)
            .build();
        TextInput timeInput = TextInput.create(TIME, TIME_LABEL, TextInputStyle.SHORT)
            .setPlaceholder(TIME_PLACEHOLDER)
            .setRequiredRange(MIN_TIME_LENGTH, MAX_TIME_LENGTH)
            .setValue(DateFormatter.formattedTime(dateTime))
            .setRequired(true)
            .build();

        return Modal.create(InteractionArguments.createInteractionIdString(modalType, id), "Edit Event Information")
            .addComponents(
                ActionRow.of(titleInput),
                ActionRow.of(noteInput),
                ActionRow.of(dateInput),
                ActionRow.of(timeInput)
            )
            .build();
    }
}
