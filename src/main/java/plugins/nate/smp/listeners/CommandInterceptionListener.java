package plugins.nate.smp.listeners;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import plugins.nate.smp.SMP;
import plugins.nate.smp.utils.ChatUtils;
import plugins.nate.smp.utils.NametagManager;

import java.util.Arrays;
import java.util.List;

public class CommandInterceptionListener implements Listener {

    @EventHandler
    public void onCommandUser(PlayerCommandPreprocessEvent event) {
        if (event.getMessage().startsWith("/lp user ") && event.getMessage().contains("group set")) {
            Bukkit.getScheduler().runTaskLater(SMP.getPlugin(), () -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    NametagManager.updateNametag(p);
                }
            }, 1);
            return;
        }

        Player p = event.getPlayer();
        List<String> commands = Arrays.asList(
                "pl",
                "bukkit:plugins",
                "plugins"
        );

        commands.forEach(all -> {
            if (event.getMessage().toLowerCase().equalsIgnoreCase("/" + all.toLowerCase())) {
                event.setCancelled(true);
                ChatUtils.sendMessage(p, "&rPlugins (1): &aSMP");
            }
        });
    }
}
