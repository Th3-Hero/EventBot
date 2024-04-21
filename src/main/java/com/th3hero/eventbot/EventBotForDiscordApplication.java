package com.th3hero.eventbot;

import com.kseth.development.autoconfigure.discord.JdaConnectionHealthContributorAutoConfiguration;
import com.kseth.development.autoconfigure.jpa.DataJpaAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// put this exclude in everything going on unless mascot finds a fix for autoconfiguration not having a static com.kseth name
@SpringBootApplication(exclude = {DataJpaAutoConfiguration.class, JdaConnectionHealthContributorAutoConfiguration.class})
@EnableJpaRepositories
public class EventBotForDiscordApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventBotForDiscordApplication.class, args);
	}

}
