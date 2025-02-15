package net.dirtydeeds.discordsoundboard.listeners;

import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class SlashCommandListener extends ListenerAdapter {

//    private static final Logger LOG = LoggerFactory.getLogger(SlashCommandListener.class);

    private final SoundPlayer bot;

    public SlashCommandListener(SoundPlayer bot) {
        this.bot = bot;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("play")) {
            String soundName = Objects.requireNonNull(event.getOption("name")).getAsString();
            event.reply("Playing sound: " + soundName).queue();
            bot.playForUser(soundName, event.getUser().getName(), 1, null);
        }
    }
}
