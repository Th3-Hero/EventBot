package com.th3hero.eventbot.listeners;

import com.th3hero.eventbot.listeners.handlers.MemberHandler;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberListener extends ListenerAdapter {
    private final MemberHandler memberHandler;

    @Override
    public void onGuildMemberRemove(@NonNull GuildMemberRemoveEvent event) {
        memberHandler.handleRemovedMember(event);
    }
}
