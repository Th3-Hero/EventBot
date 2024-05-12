package com.th3hero.eventbot.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

@NoArgsConstructor(access = AccessLevel.NONE)
public final class DiscordActionUtils {

    public static <T extends IReplyCallback> void textResponse(
            final T event,
            final String text,
            final boolean isUserReply
    ) {
        if (event.isAcknowledged()) {
            event.getHook().sendMessage(text).setEphemeral(isUserReply).queue();
        } else {
            event.reply(text).setEphemeral(isUserReply).queue();
        }
    }

    public static <T extends IReplyCallback> void embedResponse(
            final T event,
            final MessageEmbed embed,
            final boolean isUserReply
    ) {
        if (event.isAcknowledged()) {
            event.getHook().sendMessageEmbeds(embed).setEphemeral(isUserReply).queue();
        } else {
            event.replyEmbeds(embed).setEphemeral(isUserReply).queue();
        }
    }

    public static <T extends IModalCallback> void modalResponse(final T event, final Modal modal) {
        event.replyModal(modal).queue();
    }

    public static <T extends IReplyCallback> void messageCreateDataResponse(
            final T event,
            final MessageCreateData data,
            final boolean isUserReply
    ) {
        if (event.isAcknowledged()) {
            event.getHook().sendMessage(data).setEphemeral(isUserReply).queue();
        } else {
            event.reply(data).setEphemeral(isUserReply).queue();
        }
    }

    public static <T extends IReplyCallback> void deferResponse(final T event, final boolean isUserReply) {
        if (!event.isAcknowledged()) {
            event.deferReply(isUserReply).queue();
        }
    }


}
