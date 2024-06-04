package com.th3hero.eventbot.services;

import com.th3hero.eventbot.commands.actions.SelectionAction;
import com.th3hero.eventbot.commands.requests.InteractionRequest;
import com.th3hero.eventbot.commands.requests.SelectionRequest;
import com.th3hero.eventbot.dto.course.Course;
import com.th3hero.eventbot.dto.course.CourseUpdate;
import com.th3hero.eventbot.dto.course.CourseUpload;
import com.th3hero.eventbot.entities.CourseJpa;
import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.entities.StudentJpa;
import com.th3hero.eventbot.factories.EmbedBuilderFactory;
import com.th3hero.eventbot.repositories.CourseRepository;
import com.th3hero.eventbot.repositories.EventRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;
    private final StudentService studentService;
    private final EventRepository eventRepository;
    private final SchedulingService schedulingService;

    private static final String MISSING_COURSE_WITH_ID = "Unable to find Course with provided id.";

    public Collection<Course> getAllCourses() {
        return courseRepository.findAll().stream()
            .map(CourseJpa::toDto)
            .toList();
    }

    public List<Course> createCourses(List<CourseUpload> courseUploads) {
        if (courseUploads.isEmpty()) {
            throw new IllegalArgumentException("No courses provided to create.");
        }
        List<CourseJpa> courses = courseUploads.stream()
            .map(CourseUpload::toJpa)
            .toList();

        log.debug("Saving courses:\n {}", courses.stream().map(CourseJpa::getCode).collect(Collectors.joining("\n")));
        return courseRepository.saveAll(courses).stream()
            .map(CourseJpa::toDto)
            .toList();
    }

    public Course updateCourse(Long courseId, CourseUpdate courseUpload) {
        CourseJpa courseJpa = courseRepository.findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(MISSING_COURSE_WITH_ID));

        if (courseUpload.code() != null) {
            courseJpa.setCode(courseUpload.code());
        }
        if (courseUpload.name() != null) {
            courseJpa.setName(courseUpload.name());
        }

        log.debug("Updating course: {}", courseId);
        return courseRepository.save(courseJpa).toDto();
    }

    public void deleteCourseById(Long courseId) {
        CourseJpa courseJpa = courseRepository.findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(MISSING_COURSE_WITH_ID));

        studentService.removeCourseFromAllStudents(courseJpa);

        courseRepository.deleteById(courseId);
        log.debug("Deleted course: {}", courseId);
    }

    /**
     * Creates a select menu populated with the given courses as options
     * @param selectMenuId The id of the select menu
     * @param defaultOptions The default preselected options
     * @return A select menu with all courses as options
     */
    public StringSelectMenu createCourseSelectionMenu(String selectMenuId, List<CourseJpa> defaultOptions) {
        List<SelectOption> options = selectOptionFromJpas(courseRepository.findAll());
        if (options.isEmpty()) {
            log.error("No courses are currently setup.");
            throw new EntityNotFoundException("No courses are currently setup. Contact the bot owner.");
        }
        return StringSelectMenu.create(selectMenuId)
            .setPlaceholder("Select Courses")
            .setMaxValues(options.size())
            .addOptions(options)
            .setDefaultValues(defaultOptions.stream().map(CourseJpa::getCode).toList())
            .build();
    }

    public void sendCourseSelectionMenu(InteractionRequest request) {
        MessageCreateData data = new MessageCreateBuilder()
            .addEmbeds(EmbedBuilderFactory.courseSelectionHeader("Select any courses you wish to receive notifications for."))
            .addActionRow(
                createCourseSelectionMenu(
                    SelectionAction.SELECT_COURSES.toString(),
                    studentService.fetchStudent(request.getRequester().getIdLong()).getCourses()
                )
            ).build();
        request.sendResponse(data, InteractionRequest.MessageMode.USER);
    }

    /**
     * Gets a list of courses from the database based on the provided course codes
     * @param values The course codes to search for
     * @return A list of courses
     * @throws EntityNotFoundException If any of the provided course codes are not found in the database
     */
    public List<CourseJpa> coursesFromCourseCodes(List<String> values) {
        List<CourseJpa> courses = courseRepository.findByCodeIn(values);
        if (courses.size() != values.size()) {
            String missingCourses = courses.stream()
                .map(CourseJpa::getCode)
                .filter(code -> !values.contains(code))
                .collect(Collectors.joining("\n"));
            log.warn("The following courses were not found in the database. This may be from an outdated dropdown menu:\n{}", missingCourses);
            throw new EntityNotFoundException("The following courses were not found in the database:\n%s".formatted(missingCourses));
        }

        return courses;
    }

    public void processStudentSelectedCourses(SelectionRequest request) {
        StudentJpa studentJpa = studentService.fetchStudent(request.getRequester().getIdLong());

        // Get the new list of selected course
        List<CourseJpa> updatedCourses = coursesFromCourseCodes(request.getEvent().getValues());
        List<CourseJpa> removedCourses = studentJpa.getCourses().stream()
            .filter(course -> !updatedCourses.contains(course))
            .toList();

        // Remove reminders for any courses that were removed
        List<EventJpa> eventsToRemove = eventRepository.findAllByCourse(removedCourses);
        for (EventJpa event : eventsToRemove) {
            schedulingService.removeEventReminderTriggers(event.getId(), studentJpa.getId());
        }

        studentJpa.getCourses().clear();
        studentJpa.getCourses().addAll(updatedCourses);

        // Add reminders for any new courses
        List<EventJpa> events = eventRepository.findAllByCourse(studentJpa.getCourses());
        for (EventJpa event : events) {
            studentService.scheduleStudentForEvent(event, studentJpa);
        }

        request.sendResponse(
            EmbedBuilderFactory.selectedCourses(studentJpa.getCourses()),
            InteractionRequest.MessageMode.USER
        );
    }

    public void autoCompleteCourseOptions(CommandAutoCompleteInteractionEvent event) {
        StudentJpa studentJpa = studentService.fetchStudent(event.getUser().getIdLong());

        // Based on the courses the student has, filter the current input
        List<Command.Choice> choices = studentJpa.getCourses().stream()
            .filter(course -> course.getCode().startsWith(event.getFocusedOption().getValue()))
            .map(course -> new Command.Choice(course.getCode(), course.getCode()))
            .toList();
        event.replyChoices(choices).queue();
    }

    private List<SelectOption> selectOptionFromJpas(List<CourseJpa> courses) {
        return courses.stream()
            .map(course -> SelectOption.of(course.getCode(), course.getCode()).withDescription(course.getName()))
            .toList();
    }
}
