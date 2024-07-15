create table student_completed_events (
    student_jpa_id bigint not null,
    completed_events_id bigint not null,
    constraint fk_student_id foreign key (student_jpa_id)
        references student(id),
    constraint fk_event_id foreign key (completed_events_id)
        references event(id),
    constraint student_completed_events_pk primary key (student_jpa_id, completed_events_id)
);
create index student_completed_events_student_id_index on student_completed_events(student_jpa_id);
create index student_completed_events_event_id_index on student_completed_events(completed_events_id);