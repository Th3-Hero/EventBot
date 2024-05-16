package com.th3hero.eventbot.controllers.rest;

import com.th3hero.eventbot.dto.course.Course;
import com.th3hero.eventbot.dto.course.CourseUpload;
import com.th3hero.eventbot.dto.course.CourseUploadUpdate;
import com.th3hero.eventbot.services.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/course")
@Tag(name = "Course Controller", description = "Handles admin operations regarding Courses")
public class CourseController {
    private final CourseService courseService;

    @GetMapping
    @Operation(summary = "Returns a list of all courses")
    public Collection<Course> listCourses() {
        return courseService.getAllCourses();
    }

    @PostMapping("/create")
    @Operation(summary = "Create multiple courses at once")
    @ResponseStatus(HttpStatus.CREATED)
    public List<Course> createCourses(
        @RequestBody @NotNull List<CourseUpload> courseUploads
    ) {
        return courseService.createCourses(courseUploads);
    }

    @PostMapping("/{courseId}")
    @Operation(summary = "Update the information of an course")
    public Course updateCourse(
        @PathVariable @NotNull Long courseId,
        @RequestBody @NotNull CourseUploadUpdate courseUploadUpdate
    ) {
        return courseService.updateCourse(courseId, courseUploadUpdate);
    }

    @DeleteMapping("/{courseId}")
    @Operation(summary = "Delete a Course by its id")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCourse(
        @PathVariable @NotNull Long courseId
    ) {
        courseService.deleteCourseById(courseId);
    }
}
