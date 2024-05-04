package com.th3hero.eventbot.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Builder
@ToString
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
    private LocalDateTime datetime;

    @OrderBy("code ASC")
    @ManyToMany
    private List<CourseJpa> courses;

    @NonNull
    @Enumerated(EnumType.STRING)
    @Column
    private EventType type;

    @NonNull
    @Builder.Default
    @Column
    private Boolean isDeleted = false;

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
                .datetime(draftJpa.getDatetime())
                .courses(new ArrayList<>(draftJpa.getCourses()))
                .type(draftJpa.getType())
                .build();
    }
}
