package com.th3hero.eventbot.commands;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

@Getter
public enum Command {
    HELP;

    public String toString() {
        return StringUtils.lowerCase(super.toString());
    }

    public static final Map<String, String> DESCRIPTIONS = Map.ofEntries(
            Map.entry(HELP.toString(), "Displays help embed")
    );
}
