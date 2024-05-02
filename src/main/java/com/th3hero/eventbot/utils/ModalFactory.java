package com.th3hero.eventbot.utils;

import com.th3hero.eventbot.commands.ModalType;
import com.th3hero.eventbot.entities.EventDraftJpa;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

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

        return Modal.create(Utils.createInteractionIdString(ModalType.CREATE_EVENT_DRAFT, eventDraftId), "Event Creation")
                .addComponents(
                        ActionRow.of(title),
                        ActionRow.of(note)
                )
                .build();
    }

    public static Modal editDraftDetailsModal(EventDraftJpa draftJpa) {
        TextInput title = TextInput.create(TITLE_ID, TITLE_LABEL, TextInputStyle.SHORT)
                .setPlaceholder(TITLE_PLACEHOLDER)
                .setValue(draftJpa.getTitle())
                .setRequiredRange(MIN_TITLE_LENGTH, MAX_TITLE_LENGTH)
                .setRequired(true)
                .build();
        TextInput note = TextInput.create(NOTE_ID, NOTE_LABEL, TextInputStyle.PARAGRAPH)
                .setPlaceholder(NOTE_PLACEHOLDER)
                .setValue(draftJpa.getNote())
                .setMaxLength(MAX_NOTE_LENGTH)
                .setRequired(false)
                .build();
        TextInput date = TextInput.create(DATE_ID, DATE_LABEL, TextInputStyle.SHORT)
                .setPlaceholder(DATE_PLACEHOLDER)
                .setRequiredRange(MIN_DATE_LENGTH, MAX_DATE_LENGTH)
                .setValue(Utils.formattedDate(draftJpa.getDatetime()))
                .setRequired(true)
                .build();
        TextInput time = TextInput.create(TIME_ID, TIME_LABEL, TextInputStyle.SHORT)
                .setPlaceholder(TIME_PLACEHOLDER)
                .setRequiredRange(MIN_TIME_LENGTH, MAX_TIME_LENGTH)
                .setValue(Utils.formattedTime(draftJpa.getDatetime()))
                .setRequired(true)
                .build();

        return Modal.create(Utils.createInteractionIdString(ModalType.EDIT_EVENT_DRAFT, draftJpa.getId()), "Event Creation")
                .addComponents(
                        ActionRow.of(title),
                        ActionRow.of(note),
                        ActionRow.of(date),
                        ActionRow.of(time)
                )
                .build();
    }
}
