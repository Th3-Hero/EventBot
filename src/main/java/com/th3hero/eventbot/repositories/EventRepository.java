package com.th3hero.eventbot.repositories;

import com.th3hero.eventbot.entities.EventJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<EventJpa, Long> {

}
