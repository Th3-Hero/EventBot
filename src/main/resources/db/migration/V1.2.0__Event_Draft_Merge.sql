
-- Add the new columns for drafts
alter table event
    add column creation_date timestamp not null;
alter table event
    add column status text not null;

-- Change deleted to be a status enum
update event
    set status = case when deleted = true then 'DELETED' else 'ACTIVE' end;
alter table event
    drop column deleted;

-- Remove the not null constraint on title
alter table event
    alter column title drop not null;

-- insert into event (author_id, title, note, event_date, type, creation_date, status)
--     select author_id, title, note, event_date, type, draft_creation_date, "ACTIVE"
--     from event_draft;
-- insert into event_courses (event_jpa_id, courses_id)
--     select event_draft_jpa_id, courses_id
--     from event_draft_courses;
--
drop table if exists event_draft_courses;
drop table if exists event_draft;
drop sequence if exists seq_event_draft_id;
drop index if exists idx_event_draft_courses_event_draft_jpa_id;
drop index if exists idx_event_draft_courses_courses_id;