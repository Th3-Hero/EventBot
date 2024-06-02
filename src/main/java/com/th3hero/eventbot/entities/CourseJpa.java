package com.th3hero.eventbot.entities;

import com.th3hero.eventbot.dto.course.Course;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Getter
@Setter
@Entity
@Builder
@ToString(exclude = "students")
@Table(name = "course")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseJpa implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_course_id_generator")
    @SequenceGenerator(name = "seq_course_id_generator", sequenceName = "seq_course_id", allocationSize = 1)
    @Setter(AccessLevel.NONE)
    @Column(name = "id")
    private Long id;

    @NonNull
    @Column(unique = true)
    private String code;

    @NonNull
    @Column
    private String name;

    @Setter(AccessLevel.NONE)
    @Builder.Default
    @ManyToMany(mappedBy = "courses")
    private List<StudentJpa> students = new ArrayList<>();

    public Course toDto() {
        return new Course(
            this.getId(),
            this.getCode(),
            this.getName()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        CourseJpa courseJpa = (CourseJpa) o;
        return courseJpa.getCode().equals(code);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }
}
