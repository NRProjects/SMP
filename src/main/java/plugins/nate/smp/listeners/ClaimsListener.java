package plugins.nate.smp.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import plugins.nate.smp.managers.ClaimsManager;

public class ClaimsListener implements Listener {
    @EventHandler
    public void onClaimToolUse(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();

        if (!ClaimsManager.isClaimTool(event.getPlayer().getInventory().getItemInMainHand())) {
            return;
        }

        if (block == null) {
            return;
        }

        if (block.getType() == Material.AIR) {
            return;
        }

        event.setCancelled(true);
        Location blockLocation = block.getLocation();
        Player player = event.getPlayer();
        Action action = event.getAction();

        if (action == Action.LEFT_CLICK_BLOCK) {
            ClaimsManager.setPoint(player, 1, blockLocation);
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            ClaimsManager.setPoint(player, 2, blockLocation);
        }

    }
}
