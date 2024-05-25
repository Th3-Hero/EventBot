package com.th3hero.eventbot.listeners.handlers;

import com.th3hero.eventbot.repositories.StudentRepository;
import com.th3hero.eventbot.services.SchedulingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MemberHandler {
    private final SchedulingService schedulingService;
    private final StudentRepository studentRepository;

    public void handleRemovedMember(GuildMemberRemoveEvent event) {
        User user = event.getUser();
        schedulingService.removeAllEventReminderTriggers(user.getIdLong());
        studentRepository.deleteById(user.getIdLong());
        log.info("User {} has left the server. Remove all reminders.", event.getUser().getName());
    }
}
