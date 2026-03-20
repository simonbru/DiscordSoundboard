package net.dirtydeeds.discordsoundboard.listeners;

import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.groupingBy;


public class SlashCommandListener extends ListenerAdapter {

//    private static final Logger LOG = LoggerFactory.getLogger(SlashCommandListener.class);

    private static final int MAX_SOUNDS_PER_ROW = 5;
    private static final int MAX_SOUNDS_ROWS = 4;

    private static final int MAX_SOUNDS_PER_PAGE = MAX_SOUNDS_ROWS * MAX_SOUNDS_PER_ROW;


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
            event.reply(buildListSoundsMessage(1)).setEphemeral(true).queue();
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        if (componentId.startsWith("sound_")) {
            String soundName = componentId.split("sound_", 2)[1];
            event.deferEdit().queue();
            bot.playForUser(soundName, event.getUser().getName(), 1, null);
        } else if (componentId.startsWith("page_")) {
            int page = Integer.parseInt(componentId.split("page_", 2)[1]);
            MessageCreateData msg = buildListSoundsMessage(page);
            event.editMessage(MessageEditData.fromCreateData(msg)).queue();
        } else if (componentId.startsWith("disconnect")) {
            bot.disconnectFromChannel(event.getGuild());
            event.deferEdit().queue();
        }
    }

    private MessageCreateData buildListSoundsMessage(int page) {
        MessageCreateBuilder msg = new MessageCreateBuilder();
        Map<String, SoundFile> soundFiles = bot.getAvailableSoundFiles();


        int nbPages = (int) Math.ceil((double) soundFiles.size() / MAX_SOUNDS_PER_PAGE);
        // Clamp `page` between 1 and `nbPages`
        // TODO: rotate instead of clamping ?
        page = Math.min(page, nbPages);
        page = Math.max(page, 1);

        msg.setContent(String.format("Page %s/%s", page, nbPages));

        AtomicInteger counter = new AtomicInteger();
        soundFiles.keySet().stream()
                .skip((long) (page - 1) * MAX_SOUNDS_PER_PAGE)
                .limit(MAX_SOUNDS_PER_PAGE)
                .collect(groupingBy(x -> counter.getAndIncrement() / MAX_SOUNDS_PER_ROW))
                .values()
                .forEach(soundsRow -> {
                    List<Button> btnRow = soundsRow.stream().map(
                            sound -> Button.primary("sound_" + sound, sound)
                    ).toList();
                    msg.addComponents(ActionRow.of(btnRow));
                });

        msg.addComponents(ActionRow.of(
                Button.secondary("page_" + (page - 1), "Previous").withDisabled(page == 1),
                Button.secondary("page_" + (page + 1), "Next").withDisabled(page == nbPages),
                Button.danger("disconnect", "Disconnect")
        ));
        return msg.build();
    }
}
