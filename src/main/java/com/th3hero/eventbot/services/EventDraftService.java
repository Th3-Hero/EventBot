package com.th3hero.eventbot.services;

import com.th3hero.eventbot.commands.actions.SelectionAction;
import com.th3hero.eventbot.commands.requests.*;
import com.th3hero.eventbot.commands.requests.InteractionRequest.MessageMode;
import com.th3hero.eventbot.entities.CourseJpa;
import com.th3hero.eventbot.entities.EventDraftJpa;
import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.exceptions.DataAccessException;
import com.th3hero.eventbot.exceptions.IllegalInteractionException;
import com.th3hero.eventbot.factories.EmbedBuilderFactory;
import com.th3hero.eventbot.factories.ModalFactory;
import com.th3hero.eventbot.factories.ResponseFactory;
import com.th3hero.eventbot.formatting.DateFormatter;
import com.th3hero.eventbot.formatting.InteractionArguments;
import com.th3hero.eventbot.repositories.EventDraftRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.th3hero.eventbot.formatting.InteractionArguments.DRAFT_ID;
import static com.th3hero.eventbot.utils.DiscordFieldsUtils.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EventDraftService {
    private final EventDraftRepository eventDraftRepository;
    private final CourseService courseService;
    private final SchedulingService schedulingService;
    private final EventService eventService;
    private final ConfigService configService;


    public void handleEventDraftActions(ButtonRequest request) {
        EventDraftJpa draftJpa = fetchDraft(request.getArguments().get(DRAFT_ID));

        switch (request.getAction()) {
            case EDIT_DRAFT_DETAILS -> sendEditDraftDetailsModal(request, draftJpa);
            case EDIT_DRAFT_COURSES -> editDraftCourses(request, draftJpa);
            case CONFIRM_DRAFT -> eventService.publishEvent(request, draftJpa);
            default -> throw new IllegalInteractionException("Unexpected action %s".formatted(request.getAction()));
        }
    }

    public void createEventDraft(CommandRequest request) {
        String dateString = request.getArguments().get(DATE);
        String timeString = request.getArguments().get(TIME);

        EventJpa.EventType eventType = EnumUtils.getEnumIgnoreCase(
            EventJpa.EventType.class,
            request.getArguments().get(TYPE)
        );
        if (eventType == null) {
            request.sendResponse("Something unexpected went wrong. Failed to parse event type.", MessageMode.USER);
            log.error("Failed to parse event type {} when creating draft.", request.getArguments().get(TYPE));
            return;
        }

        LocalDateTime eventDate = DateFormatter.parseDate(dateString, timeString);
        if (eventDate == null) {
            request.sendResponse(
                """
                    Failed to parse date and time. Reminder:
                    %s
                    %s
                    """.formatted(DateFormatter.DATE_FORMAT_EXAMPLE, DateFormatter.TIME_FORMAT_EXAMPLE
                ),
                MessageMode.USER
            );
            return;
        }

        if (eventDate.isBefore(LocalDateTime.now())) {
            request.sendResponse("Event date cannot be in the past.", MessageMode.USER);
            return;
        }

        EventDraftJpa eventDraft = EventDraftJpa.create(
            request.getRequester().getIdLong(),
            eventDate,
            eventType
        );

        eventDraft = eventDraftRepository.save(eventDraft);

        schedulingService.addDraftCleanupTrigger(eventDraft.getId(), eventDraft.getEventDate());

        request.sendResponse(
            ModalFactory.draftCreationData(eventDraft.getId()),
            MessageMode.USER
        );
        log.debug("Created new event draft {}", eventDraft.getId());
    }

    public void addDraftDetails(ModalRequest request) {
        EventDraftJpa eventDraftJpa = fetchDraft(request.getArguments().get(DRAFT_ID));

        String title = Optional.ofNullable(request.getEvent().getValue(TITLE))
            .map(ModalMapping::getAsString)
            .filter(StringUtils::isNoneBlank)
            .orElseThrow(() -> new DataAccessException("Failed to get title from modal."));

        String note = Optional.ofNullable(request.getEvent().getValue(NOTE))
            .map(ModalMapping::getAsString)
            .filter(StringUtils::isNotBlank)
            .orElse(null);

        eventDraftJpa.setTitle(title);
        eventDraftJpa.setNote(note);

        request.sendResponse(
            ResponseFactory.createResponse(
                EmbedBuilderFactory.courseSelectionHeader("Select any courses the event is for(Eg. multiple sections)"),
                courseService.createCourseSelectionMenu(
                    InteractionArguments.createInteractionIdString(SelectionAction.DRAFT_CREATION, eventDraftJpa.getId()),
                    eventDraftJpa.getCourses()
                )
            ),
            MessageMode.USER
        );
        log.debug("Added details to draft {}", eventDraftJpa.getId());
    }

    public void setCoursesOnDraft(SelectionRequest request) {
        Optional<EventDraftJpa> eventDraftJpa = eventDraftRepository.findById(request.getArguments().get(DRAFT_ID));
        if (eventDraftJpa.isEmpty()) {
            request.sendResponse("Failed to find draft. It may have already been deleted.", MessageMode.USER);
            request.getEvent().getMessage().delete().queue();
            log.warn("Failed to find draft with id {} when choosing courses for the draft", request.getArguments().get(DRAFT_ID));
            return;
        }

        List<CourseJpa> selectedCourses = courseService.coursesFromCourseCodes(request.getEvent().getValues());

        eventDraftJpa.get().getCourses().clear();
        eventDraftJpa.get().getCourses().addAll(selectedCourses);

        sendDraft(request, eventDraftJpa.get());
        log.debug("Added courses to draft {}", eventDraftJpa.get().getId());
    }

    public void updateDraftDetails(ModalRequest request) {
        EventDraftJpa eventDraftJpa = fetchDraft(request.getArguments().get(DRAFT_ID));

        String title = Optional.ofNullable(request.getEvent().getValue(TITLE))
            .map(ModalMapping::getAsString)
            .filter(StringUtils::isNoneBlank)
            .orElseThrow(() -> new DataAccessException("Failed to get title from modal."));

        String note = Optional.ofNullable(request.getEvent().getValue(NOTE))
            .map(ModalMapping::getAsString)
            .orElse(null);

        String dateString = Optional.ofNullable(request.getEvent().getValue(DATE))
            .map(ModalMapping::getAsString)
            .filter(StringUtils::isNoneBlank)
            .orElseThrow(() -> new DataAccessException("Failed to get date from modal"));

        String timeString = Optional.ofNullable(request.getEvent().getValue(TIME))
            .map(ModalMapping::getAsString)
            .filter(StringUtils::isNoneBlank)
            .orElseThrow(() -> new DataAccessException("Failed to get time from modal"));

        LocalDateTime eventDate = DateFormatter.parseDate(dateString, timeString);
        if (eventDate == null) {
            request.sendResponse("Failed to parse date and time.", MessageMode.USER);
            return;
        }

        eventDraftJpa.setTitle(title);
        eventDraftJpa.setNote(StringUtils.isBlank(note) ? null : note);
        eventDraftJpa.setEventDate(eventDate);

        sendDraft(request, eventDraftJpa);
        log.debug("Updated details for draft {}", eventDraftJpa.getId());
    }

    public void deleteDraft(ButtonRequest request) {
        Long draftId = request.getArguments().get(DRAFT_ID);
        if (!eventDraftRepository.existsById(draftId)) {
            throw new EntityNotFoundException("Could not find an existing draft. Draft may have already been deleted.");
        }

        eventDraftRepository.deleteById(draftId);
        schedulingService.removeDraftCleanupTrigger(draftId);

        request.sendResponse("Draft has been deleted.", MessageMode.USER);
        request.getEvent().getMessage().delete().queue();
        log.debug("Deleted draft {}", draftId);
    }

    private EventDraftJpa fetchDraft(Long draftId) {
        return eventDraftRepository.findById(draftId)
            .orElseThrow(() -> new EntityNotFoundException("Failed to find draft %s".formatted(draftId)));
    }

    private void sendDraft(InteractionRequest request, EventDraftJpa eventDraftJpa) {
        request.sendResponse(
            ResponseFactory.draftPost(
                eventDraftJpa,
                configService.getConfigJpa().getDraftCleanupDelay(),
                request.getRequester().getAsMention()
            ),
            MessageMode.USER
        );
    }

    private void sendEditDraftDetailsModal(ButtonRequest request, EventDraftJpa draftJpa) {
        request.sendResponse(ModalFactory.editDetails(draftJpa), MessageMode.USER);
    }

    private void editDraftCourses(ButtonRequest request, EventDraftJpa draftJpa) {
        request.sendResponse(
            ResponseFactory.createResponse(
                EmbedBuilderFactory.courseSelectionHeader("Select any courses the event is for(Eg. multiple sections)"),
                courseService.createCourseSelectionMenu(
                    InteractionArguments.createInteractionIdString(SelectionAction.EDIT_DRAFT_COURSES, draftJpa.getId()),
                    draftJpa.getCourses()
                )
            ),
            MessageMode.USER
        );
    }
}
