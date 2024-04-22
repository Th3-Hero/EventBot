package com.th3hero.eventbot.repositories;

import com.th3hero.eventbot.entities.StudentJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<StudentJpa, Long> {

}
