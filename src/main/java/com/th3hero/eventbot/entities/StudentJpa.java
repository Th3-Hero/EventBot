package com.th3hero.eventbot.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Slf4j
@Getter
@Setter
@Entity
@Builder
@ToString
@Table(name = "student")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentJpa implements Serializable {

    @Id
    @NonNull
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Long id;

    @OrderBy("code ASC")
    @ManyToMany
    private List<CourseJpa> courses;

    @ElementCollection
    @CollectionTable(name = "student_reminder_offsets", joinColumns = @JoinColumn(name = "student_id"))
    @Column(name = "offsets_times")
    private List<Integer> offsetTimes;

    public static StudentJpa create(
            Long studentId
    ) {
        return StudentJpa.builder()
                .id(studentId)
                .courses(List.of())
                .offsetTimes(List.of(24, 72))
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }

        StudentJpa that = (StudentJpa) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
