package com.th3hero.eventbot.services;

import com.th3hero.eventbot.commands.actions.ButtonAction;
import com.th3hero.eventbot.commands.actions.SelectionAction;
import com.th3hero.eventbot.commands.requests.*;
import com.th3hero.eventbot.commands.requests.InteractionRequest.MessageMode;
import com.th3hero.eventbot.entities.CourseJpa;
import com.th3hero.eventbot.entities.EventDraftJpa;
import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.exceptions.InformationRetrievalException;
import com.th3hero.eventbot.factories.EmbedBuilderFactory;
import com.th3hero.eventbot.factories.ModalFactory;
import com.th3hero.eventbot.factories.ResponseFactory;
import com.th3hero.eventbot.formatting.DateFormatting;
import com.th3hero.eventbot.formatting.EnumParser;
import com.th3hero.eventbot.formatting.InteractionArguments;
import com.th3hero.eventbot.repositories.EventDraftRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.th3hero.eventbot.config.DiscordFieldsConfig.*;
import static com.th3hero.eventbot.formatting.InteractionArguments.DRAFT_ID;

@Service
@Transactional
@RequiredArgsConstructor
public class EventDraftService {
    private final EventDraftRepository eventDraftRepository;
    private final CourseService courseService;
    private final SchedulingService schedulingService;
    private final EventService eventService;
    private final ConfigService configService;

    private EventDraftJpa fetchDraft(Long draftId) {
        return eventDraftRepository.findById(draftId)
            .orElseThrow(() -> new EntityNotFoundException("Failed to find draft %s".formatted(draftId)));
    }

    public void eventDraftHandler(ButtonRequest request) {
        EventDraftJpa draftJpa = fetchDraft(request.getArguments().get(DRAFT_ID));

        switch (request.getAction()) {
            case EDIT_DRAFT_DETAILS -> sendEditDraftDetailsModal(request, draftJpa);
            case EDIT_DRAFT_COURSES -> editDraftCourses(request, draftJpa);
            case CONFIRM_DRAFT -> confirmDraft(request, draftJpa);
        }
    }

    public void createEventDraft(CommandRequest request) {
        String dateString = request.getArguments().get(DATE_ID);
        String timeString = request.getArguments().get(TIME_ID);

        Optional<EventJpa.EventType> eventType = EnumParser.parseEnum(
            EventJpa.EventType.class,
            request.getArguments().get(TYPE_ID)
        );
        if (eventType.isEmpty()) {
            request.sendResponse("Something unexpected went wrong. Failed to parse event type.", MessageMode.USER);
            return;
        }

        Optional<LocalDateTime> eventDate = DateFormatting.parseDate(dateString, timeString);
        if (eventDate.isEmpty()) {
            request.sendResponse("Failed to parse date and time.", MessageMode.USER);
            return;
        }

        if (eventDate.get().isBefore(LocalDateTime.now())) {
            request.sendResponse("Event date cannot be in the past.", MessageMode.USER);
            return;
        }

        EventDraftJpa eventDraft = EventDraftJpa.create(
            request.getRequester().getIdLong(),
            eventDate.get(),
            eventType.get()
        );

        eventDraft = eventDraftRepository.save(eventDraft);

        schedulingService.addDraftCleanupTrigger(eventDraft.getId(), eventDraft.getDatetime());

        request.sendResponse(
            ModalFactory.eventDraftCreationModal(eventDraft.getId()),
            MessageMode.USER
        );
    }

    public void addTitleAndNote(ModalRequest request) {
        EventDraftJpa eventDraftJpa = fetchDraft(request.getArguments().get(DRAFT_ID));

        String title = Optional.ofNullable(request.getEvent().getValue(TITLE_ID))
            .map(ModalMapping::getAsString)
            .orElseThrow(() -> new InformationRetrievalException("Failed to get title from modal."));

        if (StringUtils.isBlank(title)) {
            throw new InformationRetrievalException("Failed to get title from modal. Title is required.");
        }

        String note = Optional.ofNullable(request.getEvent().getValue(NOTE_ID))
            .map(ModalMapping::getAsString)
            .orElse(null);

        eventDraftJpa.setTitle(title);
        eventDraftJpa.setNote(StringUtils.isBlank(note) ? null : note);

        request.sendResponse(
            ResponseFactory.createResponse(
                EmbedBuilderFactory.coursePicker("Select any courses the event is for(Eg. multiple sections)"),
                courseService.createCourseSelector(
                    InteractionArguments.createInteractionIdString(SelectionAction.DRAFT_CREATION, eventDraftJpa.getId()),
                    eventDraftJpa.getCourses()
                )
            ),
            MessageMode.USER
        );
    }

    private void sendEventResponse(InteractionRequest request, EventDraftJpa eventDraftJpa) {
        request.sendResponse(
            ResponseFactory.createResponse(
                EmbedBuilderFactory.displayEventDraft(eventDraftJpa, configService.getConfigJpa().getDraftCleanupDelay(), request.getRequester().getAsMention()),
                Button.primary(InteractionArguments.createInteractionIdString(ButtonAction.EDIT_DRAFT_DETAILS, eventDraftJpa.getId()), "Edit Details"),
                Button.primary(InteractionArguments.createInteractionIdString(ButtonAction.EDIT_DRAFT_COURSES, eventDraftJpa.getId()), "Edit Courses"),
                Button.danger(InteractionArguments.createInteractionIdString(ButtonAction.DELETE_DRAFT, eventDraftJpa.getId()), "Delete Draft"),
                Button.success(InteractionArguments.createInteractionIdString(ButtonAction.CONFIRM_DRAFT, eventDraftJpa.getId()), "Confirm Draft")
            ),
            MessageMode.USER
        );
    }

    public void addCoursesToDraft(SelectionRequest request) {
        EventDraftJpa eventDraftJpa = fetchDraft(request.getArguments().get(DRAFT_ID));

        List<CourseJpa> selectedCourses = courseService.coursesFromSelectionMenuValues(request.getEvent().getValues());

        eventDraftJpa.getCourses().clear();
        eventDraftJpa.setCourses(selectedCourses);

        sendEventResponse(request, eventDraftJpa);
    }

    public void sendEditDraftDetailsModal(ButtonRequest request, EventDraftJpa draftJpa) {
        request.sendResponse(ModalFactory.editDetailsModal(draftJpa), MessageMode.USER);
    }

    public void updateDraftDetails(ModalRequest request) {
        EventDraftJpa eventDraftJpa = fetchDraft(request.getArguments().get(DRAFT_ID));

        String title = Optional.ofNullable(request.getEvent().getValue(TITLE_ID))
            .map(ModalMapping::getAsString)
            .orElseThrow(() -> new InformationRetrievalException("Failed to get title from modal."));

        if (StringUtils.isBlank(title)) {
            throw new InformationRetrievalException("Failed to get title from modal. Title is required.");
        }

        String note = Optional.ofNullable(request.getEvent().getValue(NOTE_ID))
            .map(ModalMapping::getAsString)
            .orElse(null);

        String dateString = Optional.ofNullable(request.getEvent().getValue(DATE_ID))
            .map(ModalMapping::getAsString)
            .orElseThrow(() -> new InformationRetrievalException("Failed to get date from modal"));

        String timeString = Optional.ofNullable(request.getEvent().getValue(TIME_ID))
            .map(ModalMapping::getAsString)
            .orElseThrow(() -> new InformationRetrievalException("Failed to get time from modal"));

        Optional<LocalDateTime> eventDate = DateFormatting.parseDate(dateString, timeString);
        if (eventDate.isEmpty()) {
            request.sendResponse("Failed to parse date and time.", MessageMode.USER);
            return;
        }

        eventDraftJpa.setTitle(title);
        eventDraftJpa.setNote(StringUtils.isBlank(note) ? null : note);
        eventDraftJpa.setDatetime(eventDate.get());

        sendEventResponse(request, eventDraftJpa);
    }

    public void editDraftCourses(ButtonRequest request, EventDraftJpa draftJpa) {
        request.sendResponse(
            ResponseFactory.createResponse(
                EmbedBuilderFactory.coursePicker("Select any courses the event is for(Eg. multiple sections)"),
                courseService.createCourseSelector(
                    InteractionArguments.createInteractionIdString(SelectionAction.EDIT_DRAFT_COURSES, draftJpa.getId()),
                    draftJpa.getCourses()
                )
            ),
            MessageMode.USER
        );
    }

    public void deleteDraft(ButtonRequest request) {
        Long draftId = request.getArguments().get(DRAFT_ID);
        if (!eventDraftRepository.existsById(draftId)) {
            throw new EntityNotFoundException("Could not find an existing draft. Draft may have already been deleted.");
        }
        deleteDraft(draftId);

        request.sendResponse("Draft has been deleted.", MessageMode.USER);
    }

    public void deleteDraft(Long draftId) {
        eventDraftRepository.deleteById(draftId);
        schedulingService.removeDraftCleanupTrigger(draftId);
    }

    public void confirmDraft(ButtonRequest request, EventDraftJpa draftJpa) {
        eventService.publishEvent(request, draftJpa);
    }
}
