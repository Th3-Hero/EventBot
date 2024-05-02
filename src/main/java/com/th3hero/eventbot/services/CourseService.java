package com.th3hero.eventbot.services;

import com.kseth.development.util.CollectionUtils;
import com.th3hero.eventbot.commands.CommandRequest;
import com.th3hero.eventbot.commands.Selection;
import com.th3hero.eventbot.commands.SelectionRequest;
import com.th3hero.eventbot.dto.course.Course;
import com.th3hero.eventbot.dto.course.CourseUpload;
import com.th3hero.eventbot.dto.course.CourseUploadUpdate;
import com.th3hero.eventbot.entities.CourseJpa;
import com.th3hero.eventbot.entities.EventDraftJpa;
import com.th3hero.eventbot.entities.EventJpa;
import com.th3hero.eventbot.entities.StudentJpa;
import com.th3hero.eventbot.repositories.CourseRepository;
import com.th3hero.eventbot.utils.EmbedBuilderFactory;
import com.th3hero.eventbot.utils.HttpErrorUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;
    private final StudentService studentService;

    public Collection<Course> getAllCourses() {
        return CollectionUtils.transform(
                courseRepository.findAll(),
                CourseJpa::toDto
        );
    }

    public List<Course> createCourses(List<CourseUpload> courseUploads) {
        return courseUploads.stream()
                .map(course -> courseRepository.save(course.toJpa()).toDto())
                .toList();
    }

    public Course updateCourse(Long courseId, CourseUploadUpdate courseUpload) {
        CourseJpa courseJpa = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException(HttpErrorUtil.MISSING_COURSE_WITH_ID));

        if (courseUpload.code() != null) {
            courseJpa.setCode(courseUpload.code());
        }
        if (courseUpload.name() != null) {
            courseJpa.setName(courseUpload.name());
        }
        if (courseUpload.nickname() != null) {
            courseJpa.setNickname(courseUpload.nickname());
        }

        return courseRepository.save(courseJpa).toDto();
    }

    public void deleteCourseById(Long courseId) {
        CourseJpa courseJpa = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException(HttpErrorUtil.MISSING_COURSE_WITH_ID));

        List<StudentJpa> students = studentService.fetchAllStudents();
        for (StudentJpa student : students) {
            student.getCourses().remove(courseJpa);
        }

        courseRepository.deleteById(courseId);
    }

    private List<SelectOption> selectOptionFromJpas(List<CourseJpa> courses) {
        return courses.stream()
                .map(course -> SelectOption.of(course.getCode(), course.getCode()).withDescription(course.getName()))
                .toList();
    }

    public StringSelectMenu createCourseSelector(String selectMenuId, List<CourseJpa> defaultOptions, String placeholder) {
        List<SelectOption> options = selectOptionFromJpas(courseRepository.findAll());
        if (options.isEmpty()) {
            throw new EntityNotFoundException("No courses are currently setup. Ask William.");
        }
        return StringSelectMenu.create(selectMenuId)
                .setPlaceholder(placeholder)
                .setMaxValues(options.size())
                .addOptions(options)
                .setDefaultValues(defaultOptions.stream().map(CourseJpa::getCode).toList())
                .build();
    }

    public StringSelectMenu createCourseSelector(String selectMenuId, List<CourseJpa> defaultOptions) {
        return createCourseSelector(selectMenuId, defaultOptions, "Select Courses");
    }

    public StringSelectMenu createStudentCourseSelector(String selectMenuId, Long studentId) {
        return createCourseSelector(
                selectMenuId,
                studentService.fetchStudent(studentId).getCourses()
        );
    }

    public void sendCourseSelectionMenu(CommandRequest request) {
        request.event().replyEmbeds(EmbedBuilderFactory.coursePicker("Select Any courses you wish to receive notifications for."))
                .addActionRow(
                        createStudentCourseSelector(
                                Selection.SELECT_COURSES.toString(),
                                request.requester().getIdLong()
                        )
                )
                .setEphemeral(true)
                .queue();
    }

    public List<CourseJpa> coursesFromSelectionMenuValues(List<String> values) {
        return values.stream()
                .map(course ->
                        courseRepository.findCourseJpaByCode(course).orElseThrow(() -> new EntityNotFoundException("One or more of the courses selected is not within the database."))
                )
                .toList();
    }

    public void processCourseSelection(SelectionRequest request) {
        StudentJpa studentJpa = studentService.fetchStudent(request.requester().getIdLong());

        studentJpa.getCourses().clear();
        studentJpa.getCourses().addAll(coursesFromSelectionMenuValues(request.interaction().getValues()));

        request.interaction().replyEmbeds(EmbedBuilderFactory.selectedCourses(studentJpa.getCourses()))
                .setEphemeral(true)
                .queue();
    }

    public void scheduleEventForCourse(EventJpa eventJpa, CourseJpa targetCourse) {
        List<StudentJpa> studentsWithCourse = studentService.fetchStudentsWithCourse(targetCourse);

        studentsWithCourse.forEach(student -> studentService.scheduleStudentForEvent(eventJpa, student));
    }

}
