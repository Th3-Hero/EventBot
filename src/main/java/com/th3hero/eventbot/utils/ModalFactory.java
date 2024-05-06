package com.th3hero.eventbot.utils;

import com.th3hero.eventbot.commands.ModalType;
import com.th3hero.eventbot.entities.EventDraftJpa;
import com.th3hero.eventbot.entities.EventJpa;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.time.LocalDateTime;

import static com.th3hero.eventbot.config.DiscordFieldsConfig.*;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ModalFactory {
    private static final String TITLE_LABEL = "Event Title";
    private static final String NOTE_LABEL = "Description/Note";
    private static final String DATE_LABEL = "Date";
    private static final String TIME_LABEL = "Time";

    private static final String TITLE_PLACEHOLDER = "Cornelius Samuelson III 113th birthday";
    private static final String NOTE_PLACEHOLDER = "Lunch with Cornelius";
    private static final String DATE_PLACEHOLDER = "2024-4-24";
    private static final String TIME_PLACEHOLDER = "14:30";


    public static Modal eventDraftCreationModal(Long eventDraftId) {
        TextInput title = TextInput.create(TITLE_ID, TITLE_LABEL, TextInputStyle.SHORT)
                .setPlaceholder(TITLE_PLACEHOLDER)
                .setRequiredRange(MIN_TITLE_LENGTH, MAX_TITLE_LENGTH)
                .setRequired(true)
                .build();
        TextInput note = TextInput.create(NOTE_ID, NOTE_LABEL, TextInputStyle.PARAGRAPH)
                .setPlaceholder(NOTE_PLACEHOLDER)
                .setMaxLength(MAX_NOTE_LENGTH)
                .setRequired(false)
                .build();

        return Modal.create(Utils.createInteractionIdString(ModalType.CREATE_DRAFT, eventDraftId), "Event Creation")
                .addComponents(
                        ActionRow.of(title),
                        ActionRow.of(note)
                )
                .build();
    }

    private static Modal editDetailsModal(
            String modalTitle,
            ModalType modalType,
            Long id,
            String title,
            String note,
            LocalDateTime dateTime
    ) {
        TextInput titleInput = TextInput.create(TITLE_ID, TITLE_LABEL, TextInputStyle.SHORT)
                .setPlaceholder(TITLE_PLACEHOLDER)
                .setValue(title)
                .setRequiredRange(MIN_TITLE_LENGTH, MAX_TITLE_LENGTH)
                .setRequired(true)
                .build();
        TextInput noteInput = TextInput.create(NOTE_ID, NOTE_LABEL, TextInputStyle.PARAGRAPH)
                .setPlaceholder(NOTE_PLACEHOLDER)
                .setValue(note)
                .setMaxLength(MAX_NOTE_LENGTH)
                .setRequired(false)
                .build();
        TextInput dateInput = TextInput.create(DATE_ID, DATE_LABEL, TextInputStyle.SHORT)
                .setPlaceholder(DATE_PLACEHOLDER)
                .setRequiredRange(MIN_DATE_LENGTH, MAX_DATE_LENGTH)
                .setValue(Utils.formattedDate(dateTime))
                .setRequired(true)
                .build();
        TextInput timeInput = TextInput.create(TIME_ID, TIME_LABEL, TextInputStyle.SHORT)
                .setPlaceholder(TIME_PLACEHOLDER)
                .setRequiredRange(MIN_TIME_LENGTH, MAX_TIME_LENGTH)
                .setValue(Utils.formattedTime(dateTime))
                .setRequired(true)
                .build();

        return Modal.create(Utils.createInteractionIdString(modalType, id), modalTitle)
                .addComponents(
                        ActionRow.of(titleInput),
                        ActionRow.of(noteInput),
                        ActionRow.of(dateInput),
                        ActionRow.of(timeInput)
                )
                .build();
    }

    public static Modal editDetailsModal(EventDraftJpa eventDraft) {
        return editDetailsModal("Edit Draft Information", ModalType.EDIT_DRAFT_DETAILS, eventDraft.getId(), eventDraft.getTitle(), eventDraft.getNote(), eventDraft.getDatetime());
    }

    public static Modal editDetailsModal(EventJpa eventJpa) {
        return editDetailsModal("Edit Event Information", ModalType.EDIT_EVENT_DETAILS, eventJpa.getId(), eventJpa.getTitle(), eventJpa.getNote(), eventJpa.getDatetime());
    }

    public static Modal deleteDraftReasonModal(EventJpa eventJpa) {
        TextInput reason = TextInput.create(REASON_ID, "Why are you deleting the event?", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Assignment was cancelled")
                .setRequiredRange(MIN_REASON_LENGTH, MAX_REASON_LENGTH)
                .setRequired(true)
                .build();

        return Modal.create(Utils.createInteractionIdString(ModalType.EVENT_DELETION_REASON, eventJpa.getId()), "Reason for event deletion")
                .addComponents(
                        ActionRow.of(reason)
                )
                .build();
    }
}
