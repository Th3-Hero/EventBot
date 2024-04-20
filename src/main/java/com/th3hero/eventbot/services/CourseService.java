package com.th3hero.eventbot.services;

import com.kseth.development.util.CollectionUtils;
import com.th3hero.eventbot.dto.Course;
import com.th3hero.eventbot.dto.CourseUpload;
import com.th3hero.eventbot.entities.CourseJpa;
import com.th3hero.eventbot.repositories.CourseRepository;
import com.th3hero.eventbot.utils.HttpErrorUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
@Transactional
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;

    public Collection<Course> getAllCourses() {
        return CollectionUtils.transform(
                courseRepository.findAll(),
                CourseJpa::toDto
        );
    }

    public Course createCourse(CourseUpload courseUpload) {
        return courseRepository.save(courseUpload.toJpa()).toDto();
    }

//    public Course updateCourse(Integer courseId, CourseUpload courseUpload) {
//        CourseJpa courseJpa = courseRepository.findById(courseId)
//                .orElseThrow(() -> new EntityNotFoundException(HttpErrorUtil.MISSING_COURSE_WITH_ID));
//
//
//    }


    public void deleteCourseById(Integer courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new EntityNotFoundException(HttpErrorUtil.MISSING_COURSE_WITH_ID);
        }
        courseRepository.deleteById(courseId);
    }

}
