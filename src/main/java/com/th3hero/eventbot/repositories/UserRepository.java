package com.th3hero.eventbot.repositories;

import com.th3hero.eventbot.entities.UserJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserJpa, String> {

}
