package com.th3hero.eventbot.services;

import com.th3hero.eventbot.commands.requests.ButtonRequest;
import com.th3hero.eventbot.commands.requests.CommandRequest;
import com.th3hero.eventbot.commands.requests.InteractionRequest;
import com.th3hero.eventbot.commands.requests.InteractionRequest.MessageMode;
import com.th3hero.eventbot.entities.CourseJpa;
import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.entities.StudentJpa;
import com.th3hero.eventbot.exceptions.IllegalInteractionException;
import com.th3hero.eventbot.factories.EmbedBuilderFactory;
import com.th3hero.eventbot.repositories.EventRepository;
import com.th3hero.eventbot.repositories.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.th3hero.eventbot.utils.DiscordFieldsUtils.OFFSET;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class StudentService {
    private final StudentRepository studentRepository;
    private final SchedulingService schedulingService;
    private final EventRepository eventRepository;

    /**
     * Fetches a student based on the provided studentId or creates a new student if one does not exist.
     *
     * @param studentId The ID of the student to fetch.
     * @return The StudentJpa object corresponding to the provided studentId.
     */
    public StudentJpa fetchStudent(Long studentId) {
        return studentRepository.findById(studentId)
            .orElseGet(() -> studentRepository.save(StudentJpa.create(studentId)));
    }

    public void listStudentCourses(InteractionRequest request) {
        StudentJpa studentJpa = studentRepository.findById(request.getRequester().getIdLong())
            .orElseThrow(() -> new EntityNotFoundException("Student with ID %d not found".formatted(request.getRequester().getIdLong())));

        if (studentJpa.getCourses().isEmpty()) {
            request.sendResponse("You currently have no selected courses. Please use the /select_courses command.", MessageMode.USER);
            return;
        }

        request.sendResponse(EmbedBuilderFactory.selectedCourses(studentJpa.getCourses()), MessageMode.USER);
    }

    /**
     * Handles the command auto complete event for the offset times of a student.
     *
     * @param event the command auto complete event
     */
    public void reminderOffsetAutoComplete(CommandAutoCompleteInteractionEvent event) {
        StudentJpa studentJpa = fetchStudent(event.getUser().getIdLong());
        List<Command.Choice> choices = studentJpa.getReminderOffsetTimes().stream()
            .filter(offset -> offset.toString().startsWith(event.getFocusedOption().getValue()))
            .map(offset -> new Command.Choice(offset.toString(), offset))
            .toList();
        event.replyChoices(choices).queue();
    }

    /**
     * Handles the reminder offset command and distributing the subcommand.
     *
     * @param request the command request
     */
    public void reminderOffsetSubcommandHandler(CommandRequest request) {
        ReminderConfigOptions option = Optional.ofNullable(EnumUtils.getEnumIgnoreCase(
                ReminderConfigOptions.class,
                request.getArguments().get("sub_command"))
            )
            .orElseThrow(() -> new IllegalInteractionException("Unknown sub command %s".formatted(request.getArguments().get("sub_command"))));

        StudentJpa studentJpa = fetchStudent(request.getRequester().getIdLong());

        switch (option) {
            case LIST -> listReminderOffsets(request, studentJpa);
            case ADD -> addReminderOffsets(request, studentJpa);
            case REMOVE -> removeReminderOffsets(request, studentJpa);
        }
    }

    /**
     * Schedules a student for reminders on an event.
     *
     * @param eventJpa the event to schedule the student for
     * @param studentJpa the student to schedule reminders for
     */
    public void scheduleStudentForEvent(EventJpa eventJpa, StudentJpa studentJpa) {
        for (Integer offset : studentJpa.getReminderOffsetTimes()) {
            if (eventJpa.getEventDate().minusHours(offset).isBefore(LocalDateTime.now())) {
                continue;
            }
            schedulingService.addEventReminderTrigger(
                eventJpa.getId(),
                studentJpa.getId(),
                offset,
                eventJpa.getEventDate().minusHours(offset),
                eventJpa.getEventDate()
            );
        }
    }

    public void unscheduleStudentRemindersForEvent(ButtonRequest request, Long eventId) {
        StudentJpa studentJpa = fetchStudent(request.getRequester().getIdLong());
        boolean removed = schedulingService.removeEventReminderTriggers(eventId, studentJpa.getId());
        String message = removed ?
            "All reminders have been removed for this event." :
            "You have no reminders on this event.";

        request.sendResponse(message, MessageMode.USER);
    }

    /**
     * Removes a course from all students.
     *
     * @param courseJpa the course to remove from all students
     */
    public void removeCourseFromAllStudents(CourseJpa courseJpa) {
        studentRepository.findAllByCoursesContains(courseJpa).forEach(studentJpa -> studentJpa.getCourses().remove(courseJpa));
    }

    public void notificationTest(CommandRequest request) {
        request.getRequester().getUser().openPrivateChannel().queue(
            channel -> channel.sendMessage("This is a test notification. You're all good to receive reminders from the bot for your selected courses.").queue(
                success -> request.sendResponse("Test notification sent.", MessageMode.USER),
                error -> request.sendResponse("Failed to send test notification. Make sure your discord settings allow direct messages from server members.", MessageMode.USER)
            )
        );
    }

    public enum ReminderConfigOptions {
        LIST,
        ADD,
        REMOVE;

        @Override
        public String toString() {
            return StringUtils.capitalize(toLower());
        }

        public String toLower() {
            return StringUtils.lowerCase(super.toString());
        }
    }

    private void listReminderOffsets(CommandRequest request, StudentJpa studentJpa) {
        studentJpa.getReminderOffsetTimes().sort(null);
        request.sendResponse(EmbedBuilderFactory.reminderOffsets(studentJpa.getReminderOffsetTimes()), MessageMode.USER);
    }

    private void addReminderOffsets(CommandRequest request, StudentJpa studentJpa) {
        Integer newOffset = Integer.parseInt(request.getArguments().get(OFFSET));
        if (studentJpa.getReminderOffsetTimes().contains(newOffset)) {
            request.sendResponse("You already have an offset for %d".formatted(newOffset), MessageMode.USER);
            return;
        }
        studentJpa.getReminderOffsetTimes().add(newOffset);
        eventRepository.findAllByCourse(studentJpa.getCourses())
            .forEach(event -> schedulingService.addEventReminderTrigger(
                event.getId(),
                studentJpa.getId(),
                newOffset,
                event.getEventDate().minusHours(newOffset),
                event.getEventDate()
            ));
        request.sendResponse("You will now be reminded %d hours before an event.".formatted(newOffset), MessageMode.USER);
    }

    private void removeReminderOffsets(CommandRequest request, StudentJpa studentJpa) {
        Integer targetOffset = Integer.parseInt(request.getArguments().get(OFFSET));
        if (!studentJpa.getReminderOffsetTimes().contains(targetOffset)) {
            request.sendResponse("You have no offset for %d".formatted(targetOffset), MessageMode.USER);
            return;
        }
        studentJpa.getReminderOffsetTimes().remove(targetOffset);
        eventRepository.findAllByCourse(studentJpa.getCourses())
            .forEach(event -> schedulingService.removeEventReminderTriggers(
                event.getId(),
                studentJpa.getId(),
                targetOffset
            ));
        request.sendResponse("You will no longer be reminded %d hours before an event.".formatted(targetOffset), MessageMode.USER);
    }
}
