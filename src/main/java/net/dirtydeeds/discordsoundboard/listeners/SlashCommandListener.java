package net.dirtydeeds.discordsoundboard.listeners;

import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.groupingBy;

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
            event.reply("Playing sound: " + soundName).setEphemeral(true).queue();
            bot.playForUser(soundName, event.getUser().getName(), 1, null);
        } else if (event.getName().equals("listsounds")) {
            Set<Map.Entry<String, SoundFile>> entrySet = bot.getAvailableSoundFiles().entrySet();

            ReplyCallbackAction msg = event.reply("Click on a sound to play it").setEphemeral(true);

            AtomicInteger counter = new AtomicInteger();
            entrySet.stream()
                    .collect(groupingBy(x->counter.getAndIncrement() / 5))
                    .values()
                    .stream().limit(5)
                    .forEach(soundsRow -> {
                        List<Button> btnRow = soundsRow.stream().map(
                                sound -> Button.primary("sound_" + sound.getKey(), sound.getKey())
                        ).toList();
                        msg.addActionRow(btnRow);
                    });

            msg.queue();
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getComponentId().startsWith("sound_")) {
            String soundName = event.getComponentId().split("sound_", 2)[1];
            event.deferEdit().queue();
            bot.playForUser(soundName, event.getUser().getName(), 1, null);
        }
    }
}
