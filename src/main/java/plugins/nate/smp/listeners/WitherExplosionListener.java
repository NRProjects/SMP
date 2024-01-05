package plugins.nate.smp.listeners;

import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import plugins.nate.smp.utils.WorldGuardUtils;

import java.util.List;
import java.util.Objects;

public class WitherExplosionListener implements Listener {
    @EventHandler
    public void onWitherExplosion(EntityExplodeEvent event) {
        if (event.getEntityType() == EntityType.WITHER || event.getEntityType() == EntityType.WITHER_SKULL) {
            List<Block> toRemove = event.blockList().stream()
                    .filter(Objects::nonNull)
                    .filter(block -> !WorldGuardUtils.isFlagAllowedAtLocation(WorldGuardUtils.WITHER_EXPLOSIONS, block.getLocation()))
                    .toList();

            event.blockList().removeAll(toRemove);
        }
    }
}
