package com.th3hero.eventbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories
@SpringBootApplication
public class EventBotForDiscordApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventBotForDiscordApplication.class, args);
	}

}
