package plugins.nate.smp.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Arrays;
import java.util.List;

import static plugins.nate.smp.utils.ChatUtils.sendMessage;

public class CommandInterceptionListener implements Listener {

    @EventHandler
    public void onCommandUser(PlayerCommandPreprocessEvent event) {
        Player p = event.getPlayer();
        List<String> commands = Arrays.asList(
                "pl",
                "bukkit:plugins",
                "plugins"
        );

        commands.forEach(all -> {
            if (event.getMessage().toLowerCase().equalsIgnoreCase("/" + all.toLowerCase())) {
                event.setCancelled(true);
                sendMessage(p, "&rPlugins (1): &aSMP");
            }
        });
    }
}
