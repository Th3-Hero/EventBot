package com.th3hero.eventbot.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Getter
@Setter
@Entity
@Builder
@ToString(exclude = {"courses", "completedEvents"})
@Table(name = "student")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentJpa implements Serializable {

    @Id
    @NonNull
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Long id;

    @Setter(AccessLevel.NONE)
    @Builder.Default
    @OrderBy("code ASC")
    @ManyToMany
    private List<CourseJpa> courses = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    @Builder.Default
    @ManyToMany
    private List<EventJpa> completedEvents = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "student_reminder_offsets", joinColumns = @JoinColumn(name = "student_id"))
    @Column(name = "reminder_offset_time")
    private List<Integer> reminderOffsetTimes = new ArrayList<>();

    public static StudentJpa create(
        Long studentId
    ) {
        return StudentJpa.builder()
            .id(studentId)
            .courses(List.of())
            .reminderOffsetTimes(List.of(24, 72))
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
