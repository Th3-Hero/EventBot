package com.th3hero.eventbot.config;

import com.kseth.development.discord.JdaFactory;
import com.kseth.development.discord.config.JdaProperties;
import lombok.NonNull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JdaFactoryImpl extends JdaFactory {
    public JdaFactoryImpl(JdaProperties jdaProperties) {
        super(jdaProperties);
    }

    public <T extends ListenerAdapter>JDA richJdaClient(@NonNull List<T> listeners) {
        return JDABuilder.createDefault(jdaProperties.token(), GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES)
                .setChunkingFilter(ChunkingFilter.ALL)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .addEventListeners(listeners.toArray())
                .setActivity(Activity.watching("For Events"))
                .disableCache(CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.SCHEDULED_EVENTS)
                .build();
    }
}
