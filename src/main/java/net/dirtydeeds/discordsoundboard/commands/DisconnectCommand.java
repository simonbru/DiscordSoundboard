package net.dirtydeeds.discordsoundboard.commands;

import net.dirtydeeds.discordsoundboard.SoundPlayer;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

/**
 * @author Dave Furrer
 * <p>
 * Command to disconnect the bot
 */
public class DisconnectCommand extends Command {

    private final SoundPlayer soundPlayer;

    public DisconnectCommand(SoundPlayer soundPlayer) {
        this.soundPlayer = soundPlayer;
        this.name = "disconnect";
        this.help = "Disconnect the bot from voice channels";
    }

    @Override
    protected void execute(CommandEvent event) {
        GenericMessageEvent msgEvent = event.getMessageReceivedEvent();
        soundPlayer.disconnectFromChannel(
                msgEvent.isFromGuild() ? msgEvent.getGuild() : null
        );
    }
}