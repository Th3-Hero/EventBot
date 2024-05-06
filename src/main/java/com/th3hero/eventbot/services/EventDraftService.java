package com.th3hero.eventbot.services;

import com.kseth.development.util.EnumUtils;
import com.th3hero.eventbot.commands.*;
import com.th3hero.eventbot.entities.CourseJpa;
import com.th3hero.eventbot.entities.EventDraftJpa;
import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.exceptions.EventParsingException;
import com.th3hero.eventbot.repositories.EventDraftRepository;
import com.th3hero.eventbot.utils.EmbedBuilderFactory;
import com.th3hero.eventbot.utils.ModalFactory;
import com.th3hero.eventbot.utils.Utils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.th3hero.eventbot.config.DiscordFieldsConfig.*;

@Service
@Transactional
@RequiredArgsConstructor
public class EventDraftService {
    private final EventDraftRepository eventDraftRepository;
    private final CourseService courseService;
    private final SchedulingService schedulingService;
    private final EventService eventService;

    @Value("${app.config.draft-cleanup-delay}")
    private static int draftCleanupDelay;

    public void createEventDraft(CommandRequest request) {
        String dateString = request.arguments().get(DATE_ID);
        String timeString = request.arguments().get(TIME_ID);


        EventJpa.EventType eventType;
        try {
            eventType = EnumUtils.valueOf(
                    EventJpa.EventType.class,
                    request.arguments().get(TYPE_ID),
                    new IllegalArgumentException("Failed to parse event type.")
            );
        } catch (IllegalArgumentException e) {
            request.event().reply("Something unexpected went wrong. Failed to parse event type.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        LocalDateTime eventDate;
        try {
            eventDate = Utils.parseDate(dateString, timeString);
        } catch (EventParsingException e) {
            request.event().reply(e.getMessage())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (eventDate.isBefore(LocalDateTime.now())) {
            request.event().reply("Event date cannot be in the past.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        EventDraftJpa eventDraft = EventDraftJpa.create(
                request.requester().getIdLong(),
                eventDate,
                eventType
        );

        eventDraft = eventDraftRepository.save(eventDraft);

        schedulingService.addDraftCleanupTrigger(eventDraft.getId(), eventDraft.getDatetime());

        request.event().replyModal(
                ModalFactory.eventDraftCreationModal(eventDraft.getId())
        ).queue();
    }

    public void addTitleAndNote(ModalRequest request) {
        if (request.idArguments().isEmpty()) {
            throw new EventParsingException("Failed to parse identifier from modal");
        }

        EventDraftJpa eventDraftJpa = eventDraftRepository.findById(Long.parseLong(request.idArguments().get(0)))
                .orElseThrow(() -> new EventParsingException("Failed to find event draft for this modal"));

        String title = Optional.ofNullable(request.interaction().getValue(TITLE_ID))
                .map(ModalMapping::getAsString)
                .orElseThrow(() -> new EventParsingException("Failed to get title from modal."));

        if (StringUtils.isBlank(title)) {
            throw new EventParsingException("Failed to get title from modal. Title is required.");
        }

        String note = Optional.ofNullable(request.interaction().getValue(NOTE_ID))
                .map(ModalMapping::getAsString)
                .orElse(null);

        eventDraftJpa.setTitle(title);
        eventDraftJpa.setNote(StringUtils.isBlank(note) ? null : note);

        request.interaction().replyEmbeds(EmbedBuilderFactory.coursePicker("Select any courses the event is for(Eg. multiple sections)"))
                .addActionRow(
                        courseService.createCourseSelector(
                                Utils.createInteractionIdString(Selection.DRAFT_CREATION, eventDraftJpa.getId()),
                                eventDraftJpa.getCourses()
                        )
                )
                .setEphemeral(true)
                .queue();
    }

    public void addCoursesToDraft(SelectionRequest request) {
        if (request.idArguments().isEmpty()) {
            throw new EventParsingException("Failed to parse identifier from selection menu");
        }

        EventDraftJpa eventDraftJpa = eventDraftRepository.findById(Long.valueOf(request.idArguments().get(0)))
                .orElseThrow(() -> new EntityNotFoundException("Failed to find the course draft."));

        List<CourseJpa> selectedCourses = courseService.coursesFromSelectionMenuValues(request.interaction().getValues());

        eventDraftJpa.getCourses().clear();
        eventDraftJpa.setCourses(selectedCourses);

        request.interaction()
                .replyEmbeds(EmbedBuilderFactory.displayEventDraft(eventDraftJpa, draftCleanupDelay, request.requester().getAsMention()))
                .setEphemeral(true)
                .addActionRow(
                        Button.primary(Utils.createInteractionIdString(ButtonAction.EDIT_DRAFT_DETAILS, eventDraftJpa.getId()), "Edit Details"),
                        Button.primary(Utils.createInteractionIdString(ButtonAction.EDIT_DRAFT_COURSES, eventDraftJpa.getId()), "Edit Courses"),
                        Button.danger(Utils.createInteractionIdString(ButtonAction.DELETE_DRAFT, eventDraftJpa.getId()), "Delete Draft"),
                        Button.success(Utils.createInteractionIdString(ButtonAction.CONFIRM_DRAFT, eventDraftJpa.getId()), "Confirm Draft")
                )
                .queue();
    }

    public void sendEditDraftDetailsModal(ButtonRequest request) {
        if (request.idArguments().isEmpty()) {
            throw new EventParsingException("Failed to parse identifier from modal");
        }
        Long draftId = Long.parseLong(request.idArguments().get(0));
        EventDraftJpa draftJpa = eventDraftRepository.findById(draftId)
                .orElseThrow(() -> new EventParsingException("Failed to find draft from button identifier"));

        request.buttonInteraction().replyModal(
                ModalFactory.editDetailsModal(draftJpa)
        ).queue();
    }

    public void updateDraftDetails(ModalRequest request) {
        if (request.idArguments().isEmpty()) {
            throw new EventParsingException("Failed to parse identifier from modal");
        }

        EventDraftJpa eventDraftJpa = eventDraftRepository.findById(Long.parseLong(request.idArguments().get(0)))
                .orElseThrow(() -> new EventParsingException("Failed to find event draft for this modal"));

        String title = Optional.ofNullable(request.interaction().getValue(TITLE_ID))
                .map(ModalMapping::getAsString)
                .orElseThrow(() -> new EventParsingException("Failed to get title from modal."));

        if (StringUtils.isBlank(title)) {
            throw new EventParsingException("Failed to get title from modal. Title is required.");
        }

        String note = Optional.ofNullable(request.interaction().getValue(NOTE_ID))
                .map(ModalMapping::getAsString)
                .orElse(null);

        String dateString = Optional.ofNullable(request.interaction().getValue(DATE_ID))
                .map(ModalMapping::getAsString)
                        .orElseThrow(() -> new EventParsingException("Failed to get date from modal"));

        String timeString = Optional.ofNullable(request.interaction().getValue(TIME_ID))
                .map(ModalMapping::getAsString)
                .orElseThrow(() -> new EventParsingException("Failed to get time from modal"));

        LocalDateTime eventDate;
        try {
            eventDate = Utils.parseDate(dateString, timeString);
        } catch (EventParsingException e) {
            request.event().reply(e.getMessage())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        eventDraftJpa.setTitle(title);
        eventDraftJpa.setNote(StringUtils.isBlank(note) ? null : note);
        eventDraftJpa.setDatetime(eventDate);

        request.interaction()
                .replyEmbeds(EmbedBuilderFactory.displayEventDraft(eventDraftJpa, draftCleanupDelay, request.requester().getAsMention()))
                .setEphemeral(true)
                .addActionRow(
                        Button.primary(Utils.createInteractionIdString(ButtonAction.EDIT_DRAFT_DETAILS, eventDraftJpa.getId()), "Edit Details"),
                        Button.primary(Utils.createInteractionIdString(ButtonAction.EDIT_DRAFT_COURSES, eventDraftJpa.getId()), "Edit Courses"),
                        Button.danger(Utils.createInteractionIdString(ButtonAction.DELETE_DRAFT, eventDraftJpa.getId()), "Delete Draft"),
                        Button.success(Utils.createInteractionIdString(ButtonAction.CONFIRM_DRAFT, eventDraftJpa.getId()), "Confirm Draft")
                )
                .queue();
    }

    public void editDraftCourses(ButtonRequest request) {
        if (request.idArguments().isEmpty()) {
            throw new EventParsingException("Failed to parse identifier from button");
        }
        EventDraftJpa eventDraftJpa = eventDraftRepository.findById(Long.valueOf(request.idArguments().get(0)))
                .orElseThrow(() -> new EntityNotFoundException("Failed to find the course draft."));

        request.buttonInteraction().replyEmbeds(EmbedBuilderFactory.coursePicker("Select any courses the event is for(Eg. multiple sections)"))
                .addActionRow(
                        courseService.createCourseSelector(
                                Utils.createInteractionIdString(Selection.EDIT_DRAFT_COURSES, eventDraftJpa.getId()),
                                eventDraftJpa.getCourses()
                        )
                )
                .setEphemeral(true)
                .queue();
    }

    public void deleteDraft(ButtonRequest request) {
        if (request.idArguments().isEmpty()) {
            throw new EventParsingException("Failed to parse identifier from button");
        }
        Long draftId = Long.parseLong(request.idArguments().get(0));
        if (!eventDraftRepository.existsById(draftId)) {
            throw new EntityNotFoundException("Could not find an existing draft. Draft may have already been deleted.");
        }
        deleteDraft(draftId);
        request.buttonInteraction().reply("Draft has been deleted.")
                .setEphemeral(true)
                .queue();
    }

    public void deleteDraft(Long draftId) {
        eventDraftRepository.deleteById(draftId);
        schedulingService.removeDraftCleanupTrigger(draftId);
    }

    public void confirmDraft(ButtonRequest request) {
        if (request.idArguments().isEmpty()) {
            throw new EventParsingException("Failed to parse identifier from button");
        }
        EventDraftJpa eventDraftJpa = eventDraftRepository.findById(Long.parseLong(request.idArguments().get(0)))
                .orElseThrow(() -> new EntityNotFoundException("Failed to find draft."));

        eventService.publishEvent(request, eventDraftJpa);
    }
}
