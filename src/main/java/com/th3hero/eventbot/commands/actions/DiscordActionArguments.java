package com.th3hero.eventbot.commands.actions;

import java.util.List;

public interface DiscordActionArguments {

    String name();

    /**
     * @return A list of the keys that the request should contain
     */
    List<String> getRequestKeys();

}
