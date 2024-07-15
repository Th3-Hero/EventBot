package com.th3hero.eventbot.services;

import com.th3hero.eventbot.commands.actions.ButtonAction;
import com.th3hero.eventbot.commands.actions.Command;
import com.th3hero.eventbot.commands.actions.SelectionAction;
import com.th3hero.eventbot.commands.requests.*;
import com.th3hero.eventbot.commands.requests.InteractionRequest.MessageMode;
import com.th3hero.eventbot.entities.*;
import com.th3hero.eventbot.exceptions.ConfigErrorException;
import com.th3hero.eventbot.exceptions.DataAccessException;
import com.th3hero.eventbot.factories.ButtonFactory;
import com.th3hero.eventbot.factories.EmbedBuilderFactory;
import com.th3hero.eventbot.factories.ModalFactory;
import com.th3hero.eventbot.factories.ResponseFactory;
import com.th3hero.eventbot.formatting.DateFormatter;
import com.th3hero.eventbot.formatting.InteractionArguments;
import com.th3hero.eventbot.repositories.EventDraftRepository;
import com.th3hero.eventbot.repositories.EventRepository;
import com.th3hero.eventbot.utils.DiscordActionUtils;
import com.th3hero.eventbot.utils.DiscordUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static com.th3hero.eventbot.formatting.InteractionArguments.EVENT_ID;
import static com.th3hero.eventbot.utils.DiscordFieldsUtils.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final CourseService courseService;
    private final ConfigService configService;
    private final EventDraftRepository eventDraftRepository;
    private final SchedulingService schedulingService;
    private final StudentService studentService;

    private static final String FAILED_TO_FIND_EVENT = "Failed to find event %s";
    private static final String UPDATED_EVENT_MESSAGE = "The event has been updated. %s";

    private static final String EVENT_DATE = "eventDate";

    public void publishEvent(ButtonRequest request, EventDraftJpa draftJpa) {
        // Publish event via draft conformation
        publishEvent(request, eventRepository.save(EventJpa.create(draftJpa)));
        eventDraftRepository.deleteById(draftJpa.getId());
        schedulingService.removeDraftCleanupTrigger(draftJpa.getId());
    }

    public void sendDeleteConformation(ButtonRequest request) {
        if (!requesterIsAdmin(request)) {
            return;
        }

        // we fetch deleted events as well so that we can handle responses differently
        // either by informing the user the event doesn't exist, or that it's already deleted
        EventJpa eventJpa = eventRepository.findById(request.getArguments().get(EVENT_ID))
            .orElseThrow(() -> new EntityNotFoundException(FAILED_TO_FIND_EVENT.formatted(request.getArguments().get(EVENT_ID))));

        if (eventDeleted(request, eventJpa)) {
            return;
        }

        request.sendResponse(ModalFactory.deleteDraftReason(eventJpa), MessageMode.USER);
    }

    public void handleDeleteConformation(ModalRequest request) {
        EventJpa eventJpa = eventRepository.findById(request.getArguments().get(EVENT_ID))
            .orElseThrow(() -> new EntityNotFoundException("Failed to find an event within the database from modal %s".formatted(request.getEvent().getModalId())));

        if (eventJpa.getDeleted()) {
            request.sendResponse("Event is already deleted.", MessageMode.USER);
            return;
        }

        String reason = Optional.ofNullable(request.getEvent().getValue(REASON))
            .map(ModalMapping::getAsString)
            .orElseThrow(() -> new DataAccessException("Failed to parse field from modal"));

        int deletedEventCleanupDelay = configService.getConfigJpa().getDeletedEventCleanupDelay();

        request.getEvent().getChannel().retrieveMessageById(eventJpa.getMessageId()).queue(
            message -> {
                // Get the message tied to the event and strip the buttons
                message.editMessageComponents().queue();
                // Send a message saying that the event has been deleted and giving the recovery option
                deleteEventConsumer(request, message, eventJpa, reason, deletedEventCleanupDelay);
            },
            new ErrorHandler().handle(ErrorResponse.UNKNOWN_MESSAGE, e -> {
                    log.error("Failed to retrieve message tied to event (id: {})", eventJpa.getId());
                    request.sendResponse("Failed to retrieve message tied to event.", MessageMode.USER);
                }
            ));
    }

    public void undoEventDeletion(ButtonRequest request) {
        EventJpa eventJpa = eventRepository.findById(request.getArguments().get(EVENT_ID))
            .orElseThrow(() -> new EntityNotFoundException(FAILED_TO_FIND_EVENT.formatted(request.getArguments().get(EVENT_ID))));
        eventJpa.setDeleted(false);
        schedulingService.removeDeletedEventCleanupTrigger(eventJpa.getId());

        scheduleEventReminders(eventJpa);

        request.sendResponse("Event has been restored.", MessageMode.USER);

        request.getEvent().getMessage().delete().queue();

        DiscordActionUtils.retrieveMessage(
            request.getEvent().getChannel(),
            eventJpa.getMessageId(),
            success -> {
                success.replyEmbeds(EmbedBuilderFactory.eventRestored(request.getRequester().getAsMention())).queue();
                success.editMessageComponents(ButtonFactory.eventButtons(eventJpa.getId())).queue();
            },
            e -> log.warn("Failed to retrieved deleted event message")
        );

    }

    public void toggleEventCompleted(ButtonRequest request) {
        StudentJpa studentJpa = studentService.fetchStudent(request.getRequester().getIdLong());
        EventJpa eventJpa = eventRepository.findById(request.getArguments().get(EVENT_ID))
            .orElseThrow(() -> new EntityNotFoundException(FAILED_TO_FIND_EVENT.formatted(request.getArguments().get(EVENT_ID))));
        if (eventJpa.getDeleted()) {
            request.sendResponse("This event has been deleted, actions cannot be preformed on it.", MessageMode.USER);
            log.error("User tried to mark a deleted event as complete (id: {})", eventJpa.getId());
            return;
        }

        if (studentJpa.getCompletedEvents().contains(eventJpa)) {
            request.sendResponse("Reminders have been re-enabled for this event.", MessageMode.USER);
            studentJpa.getCompletedEvents().remove(eventJpa);
            studentService.scheduleStudentForEvent(eventJpa, studentJpa);
            return;
        }

        studentService.unscheduleStudentRemindersForEvent(request, eventJpa.getId());
        studentJpa.getCompletedEvents().add(eventJpa);
    }

    public void sendEventEditOptions(ButtonRequest request) {
        if (!requesterIsAdmin(request)) {
            return;
        }

        EventJpa eventJpa = eventRepository.findById(request.getArguments().get(EVENT_ID))
            .orElseThrow(() -> new EntityNotFoundException(FAILED_TO_FIND_EVENT.formatted(request.getArguments().get(EVENT_ID))));

        if (eventDeleted(request, eventJpa)) {
            return;
        }

        request.sendResponse(
            ResponseFactory.editOptionsResponse(eventJpa.getId()),
            MessageMode.USER
        );
    }

    public void sendEditEventDetails(ButtonRequest request) {
        EventJpa eventJpa = eventRepository.findById(request.getArguments().get(EVENT_ID))
            .orElseThrow(() -> new EntityNotFoundException(FAILED_TO_FIND_EVENT.formatted(request.getArguments().get(EVENT_ID))));

        if (eventDeleted(request, eventJpa)) {
            return;
        }

        request.sendResponse(ModalFactory.editDetails(eventJpa), MessageMode.USER);
    }

    public void sendEventEditCourses(ButtonRequest request) {
        EventJpa eventJpa = eventRepository.findById(request.getArguments().get(EVENT_ID))
            .orElseThrow(() -> new EntityNotFoundException(FAILED_TO_FIND_EVENT.formatted(request.getArguments().get(EVENT_ID))));

        if (eventDeleted(request, eventJpa)) {
            return;
        }

        request.sendResponse(
            ResponseFactory.createResponse(
                EmbedBuilderFactory.courseSelectionHeader("Select the courses for the event."),
                courseService.createCourseSelectionMenu(
                    InteractionArguments.createInteractionIdString(SelectionAction.EDIT_EVENT_COURSES, eventJpa.getId()),
                    eventJpa.getCourses()
                )
            ),
            MessageMode.USER
        );
    }

    public void editEventDetails(ModalRequest request) {
        EventJpa eventJpa = eventRepository.findById(request.getArguments().get(EVENT_ID))
            .orElseThrow(() -> new EntityNotFoundException(FAILED_TO_FIND_EVENT.formatted(request.getArguments().get(EVENT_ID))));

        String title = Optional.ofNullable(request.getEvent().getValue(TITLE))
            .map(ModalMapping::getAsString)
            .orElseThrow(() -> new DataAccessException("Failed to parse title from modal"));

        String note = Optional.ofNullable(request.getEvent().getValue(NOTE))
            .map(ModalMapping::getAsString)
            .orElse(null);

        String dateString = Optional.ofNullable(request.getEvent().getValue(DATE))
            .map(ModalMapping::getAsString)
            .orElseThrow(() -> new DataAccessException("Failed to parse date from modal"));

        String timeString = Optional.ofNullable(request.getEvent().getValue(TIME))
            .map(ModalMapping::getAsString)
            .orElseThrow(() -> new DataAccessException("Failed to parse time from modal"));

        LocalDateTime eventDate = DateFormatter.parseDate(dateString, timeString);
        if (eventDate == null) {
            request.sendResponse("Failed to parse date and time", MessageMode.USER);
            return;
        }

        MessageEmbed embed = EmbedBuilderFactory.editedEventDetailsChangelog(title, eventJpa, note, eventDate);

        if (!eventDate.equals(eventJpa.getEventDate())) {
            eventJpa.setEventDate(eventDate);
            schedulingService.removeReminderTriggers(eventJpa.getId());
            scheduleEventReminders(eventJpa);
            log.debug("Event date was updated, rescheduling reminders for event (id: {})", eventJpa.getId());
        }

        eventJpa = eventRepository.save(eventJpa);

        updateMessage(
            request,
            request.getEvent().getChannel(),
            eventJpa,
            embed,
            request.getRequester().getAsMention()
        );
    }

    public void editEventCourses(SelectionRequest request) {
        EventJpa eventJpa = eventRepository.findById(request.getArguments().get(EVENT_ID))
            .orElseThrow(() -> new EntityNotFoundException(FAILED_TO_FIND_EVENT.formatted(request.getArguments().get(EVENT_ID))));

        List<CourseJpa> selectedCourses = courseService.coursesFromCourseCodes(request.getEvent().getValues());

        MessageEmbed embed = EmbedBuilderFactory.editedEventCoursesChangelog(eventJpa, selectedCourses);

        eventJpa.getCourses().clear();
        eventJpa.getCourses().addAll(selectedCourses);

        schedulingService.removeReminderTriggers(eventJpa.getId());
        scheduleEventReminders(eventJpa);

        updateMessage(
            request,
            request.getEvent().getChannel(),
            eventJpa,
            embed,
            request.getRequester().getAsMention()
        );

    }

    public void filterViewEvents(CommandRequest request) {
        StudentJpa studentJpa = studentService.fetchStudent(request.getRequester().getIdLong());
        if (studentJpa.getCourses().isEmpty()) {
            request.sendResponse(
                "You are not signed up for any courses with the bot. Please use `/%s`".formatted(Command.SELECT_COURSES.getDisplayName()),
                MessageMode.USER
            );
            return;
        }

        request.deferReply(MessageMode.USER);

        Integer timePeriodField = null;
        if (request.getArguments().get(TIME_PERIOD) != null) {
            timePeriodField = Integer.parseInt(request.getArguments().get(TIME_PERIOD));
        }

        Integer upcomingField = null;
        if (request.getArguments().get(UPCOMING) != null) {
            upcomingField = Integer.parseInt(request.getArguments().get(UPCOMING));
        }

        String courseField = request.getArguments().get(COURSE);

        // If they didn't specify a course, use the courses they are signed up for
        List<CourseJpa> courses;
        if (courseField == null) {
            courses = studentJpa.getCourses();
        } else {
            courses = courseService.coursesFromCourseCodes(List.of(courseField));
            if (courses.isEmpty()) {
                request.sendResponse("Unable to find course matching '%s'".formatted(courseField), MessageMode.USER);
                return;
            }
        }

        // We don't want to list events that have already passed and
        // if they haven't specified a time period we don't limit how far in the future we show events for
        LocalDateTime minDate = LocalDateTime.now();
        LocalDateTime maxDate = timePeriodField != null ? minDate.plusDays(timePeriodField) : null;

        Specification<EventJpa> spec = getEventsByCourseAndDateRange(courses, minDate, maxDate);
        int maxEvents = upcomingField != null ? upcomingField : MessageEmbed.MAX_FIELD_AMOUNT;

        List<EventJpa> events = eventRepository.findBy(
            spec,
            query -> query.sortBy(Sort.by(Sort.Direction.ASC, EVENT_DATE)).limit(maxEvents).all()
        );

        sendViewEvents(request, events);
    }

    public void sendAllEventsToEventChannel(JDA jda) {
        Long eventChannelId = configService.getConfigJpa().getEventChannel();
        TextChannel eventChannel = jda.getTextChannelById(eventChannelId);
        if (eventChannel == null) {
            log.error("Failed to find event channel with id {}", eventChannelId);
            return;
        }

        eventChannel.sendMessage("This channel is now the event channel. All events will be posted here going forward.").queue();

        List<EventJpa> events = eventRepository.findAllByDeletedIsFalse();
        for (EventJpa event : events) {
            String author = Optional.ofNullable(jda.getUserById(event.getAuthorId()))
                .map(User::getAsMention)
                .orElse(MarkdownUtil.italics("Unknown User"));
            repostEvent(eventChannel, event, author);
        }
        log.debug("All events reposted to event channel");
    }

    /**
     * Checks if the requester has administrator permissions and sends a response if they do not.
     *
     * @param request The request to check
     * @return True if the requester has administrator permissions, otherwise False
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean requesterIsAdmin(InteractionRequest request) {
        if (!request.getRequester().hasPermission(Permission.ADMINISTRATOR)) {
            request.sendResponse("This action requires administrator permissions", MessageMode.USER);
            log.debug("User {} attempted to preform an action that requires administrator permissions", request.getRequester().getAsMention());
            return false;
        }
        return true;
    }

    /**
     * Schedules reminders for all students in the courses associated with the event.
     *
     * @param eventJpa The event to schedule reminders for
     */
    private void scheduleEventReminders(EventJpa eventJpa) {
        for (CourseJpa courseJpa : eventJpa.getCourses()) {
            courseJpa.getStudents().forEach(student -> studentService.scheduleStudentForEvent(eventJpa, student));
        }
    }

    private static void updateMessage(InteractionRequest request, MessageChannel channel, EventJpa eventJpa, MessageEmbed embed, String requesterMention) {
        DiscordActionUtils.retrieveMessage(
            channel,
            eventJpa.getMessageId(),
            message -> {
                message.editMessageEmbeds(EmbedBuilderFactory.eventEmbed(eventJpa, requesterMention)).queue();
                message.replyEmbeds(embed).queue();
                request.sendResponse(UPDATED_EVENT_MESSAGE.formatted(message.getJumpUrl()), MessageMode.USER);
            },
            error -> request.sendResponse("Failed to retrieve message tied to event.", MessageMode.USER)
        );
    }

    private void deleteEventConsumer(ModalRequest request, Message message, EventJpa eventJpa, String reason, int deletedEventCleanupDelay) {
        String jumpUrl = message.getJumpUrl();

        // remove all the reminders and soft delete the event
        schedulingService.removeReminderTriggers(eventJpa.getId());
        eventJpa.setDeleted(true);
        eventRepository.save(eventJpa);

        // Send the recovery message
        request.getEvent().getChannel().sendMessageEmbeds(
                EmbedBuilderFactory.deletedEvent(
                    reason,
                    jumpUrl,
                    request.getRequester().getAsMention(),
                    deletedEventCleanupDelay
                )
            ).addActionRow(
                Button.primary(InteractionArguments.createInteractionIdString(ButtonAction.UNDO_EVENT_DELETION, eventJpa.getId()), "Recover Event")
            )
            .queue(success ->
                // Schedule the cleanup trigger for the deleted event after the recovery period ends
                schedulingService.addDeletedEventCleanupTrigger(
                    eventJpa.getId(),
                    success.getIdLong(),
                    LocalDateTime.now().plusHours(deletedEventCleanupDelay)
                ));

        request.sendResponse("Event has been deleted. %s".formatted(jumpUrl), MessageMode.USER);
    }

    private void repostEvent(TextChannel channel, EventJpa eventJpa, String author) {
        channel.sendMessageEmbeds(EmbedBuilderFactory.eventEmbed(eventJpa, author))
            .addComponents(ButtonFactory.eventButtons(eventJpa.getId()))
            .queue(success -> {
                    // Save the new message id to the event
                    eventJpa.setMessageId(success.getIdLong());
                    eventRepository.save(eventJpa);
                    log.info("Event reposted (id:{}) in channel {}", eventJpa.getId(), success.getChannel().getName());
                },
                error -> log.error("Failed to repost event (id:{}) in channel {}", eventJpa.getId(), channel.getName())
            );
    }

    private void publishEvent(ButtonRequest request, EventJpa eventJpa) {
        scheduleEventReminders(eventJpa);

        Long eventChannel = configService.getConfigJpa().getEventChannel();
        TextChannel channel = Optional.ofNullable(request.getEvent().getJDA().getTextChannelById(eventChannel))
            .orElseThrow(() -> new ConfigErrorException("The event channel could not be found. Please contact a bot administrator."));

        channel.sendMessageEmbeds(EmbedBuilderFactory.eventEmbed(eventJpa, request.getRequester().getAsMention()))
            .addComponents(ButtonFactory.eventButtons(eventJpa.getId()))
            .queue(success -> {
                // Save the new message id to the event
                eventJpa.setMessageId(success.getIdLong());
                eventRepository.save(eventJpa);
                request.sendResponse("Event has been posted to the event channel. %s".formatted(success.getJumpUrl()), MessageMode.USER);
                log.info("New event published (id:{}) in channel {}", eventJpa.getId(), success.getChannel().getName());
            });
    }

    private static boolean eventDeleted(ButtonRequest request, EventJpa eventJpa) {
        if (eventJpa.getDeleted()) {
            request.sendResponse("This event has been deleted, actions cannot be preformed on it.", MessageMode.USER);
            log.error("Attempted to preform actions (action: {}) on a deleted event (id: {}). Deleted events shouldn't have buttons to action even.", request.getAction(), eventJpa.getId());
            return true;
        }
        return false;
    }

    private static Specification<EventJpa> getEventsByCourseAndDateRange(List<CourseJpa> courses, LocalDateTime minDate, LocalDateTime maxDate) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (courses != null) {
                Join<EventJpa, CourseJpa> courseJoin = root.join("courses");
                predicates.add(courseJoin.in(courses));
            }
            if (minDate != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get(EVENT_DATE), minDate));
            }
            if (maxDate != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get(EVENT_DATE), maxDate));
            }
            predicates.add(builder.not(root.get("deleted")));
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void sendViewEvents(CommandRequest request, List<EventJpa> events) {
        if (events.isEmpty()) {
            request.sendResponse("No events found matching the criteria.", MessageMode.USER);
            return;
        }

        ConfigJpa configJpa = configService.getConfigJpa();

        Map<EventJpa, String> eventMessageMap = new LinkedHashMap<>();
        for (EventJpa event : events) {
            eventMessageMap.put(event, DiscordUtils.generateJumpUrl(configJpa, event.getMessageId()));
        }

        request.sendResponse(EmbedBuilderFactory.listEvents(eventMessageMap), MessageMode.USER);
    }
}
