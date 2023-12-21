package plugins.nate.smp.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import plugins.nate.smp.managers.ClaimsManager;
import plugins.nate.smp.objects.Claim;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static plugins.nate.smp.utils.ChatUtils.PREFIX;
import static plugins.nate.smp.utils.ChatUtils.sendMessage;

public class ClaimsListener implements Listener {
    private static final Map<UUID, Claim> lastClaimMap = new HashMap<>();

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

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Early return if the player hasn't moved locations (blocks)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        Location location = player.getLocation();

        Claim currentClaim = ClaimsManager.getClaimAtLocation(location);
        Claim lastClaim = lastClaimMap.get(player.getUniqueId());

        if (currentClaim != null && !currentClaim.equals(lastClaim)) {
            sendMessage(player, PREFIX + "You have entered a claim owned by " + currentClaim.getOwnerName());
            lastClaimMap.put(playerUUID, currentClaim);
        } else if (currentClaim == null && lastClaim != null) {
            lastClaimMap.remove(playerUUID);
        }
    }
}
