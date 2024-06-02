package com.th3hero.eventbot.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Entity
@Builder
@ToString(exclude = "courses")
@Table(name = "event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class EventJpa implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_event_id_generator")
    @SequenceGenerator(name = "seq_event_id_generator", sequenceName = "seq_event_id", allocationSize = 1)
    @Setter(AccessLevel.NONE)
    @Column(name = "id")
    private Long id;

    @NonNull
    @Column
    private Long authorId;

    @Column
    private Long messageId;

    @NotNull
    @Column
    private String title;

    @Column
    private String note;

    @NonNull
    @Column
    private LocalDateTime eventDate;

    @Setter(AccessLevel.NONE)
    @Builder.Default
    @OrderBy("code ASC")
    @ManyToMany
    private List<CourseJpa> courses = new ArrayList<>();

    @NonNull
    @Enumerated(EnumType.STRING)
    @Column
    private EventType type;

    @NonNull
    @Builder.Default
    @Column
    private Boolean deleted = false;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        EventJpa eventJpa = (EventJpa) o;
        return id.equals(eventJpa.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public enum EventType {
        ASSIGNMENT,
        LAB,
        MIDTERM,
        EXAM,
        OTHER;

        public String displayName() {
            return this.name().substring(0, 1).toUpperCase() + this.name().substring(1).toLowerCase();
        }
    }

    public static EventJpa create(EventDraftJpa draftJpa) {
        return EventJpa.builder()
            .authorId(draftJpa.getAuthorId())
            .title(draftJpa.getTitle())
            .note(draftJpa.getNote())
            .eventDate(draftJpa.getEventDate())
            .courses(new ArrayList<>(draftJpa.getCourses()))
            .type(draftJpa.getType())
            .build();
    }
}
