package com.th3hero.eventbot.repositories;

import com.th3hero.eventbot.entities.EventDraftJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventDraftRepository extends JpaRepository<EventDraftJpa, Long> {

}
