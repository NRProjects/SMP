package plugins.nate.smp.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.boss.DragonBattle;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class DragonEggRespawnListener implements Listener {
    @EventHandler
    public void onDragonDeath(EntityDeathEvent e) {
        if (e.getEntityType() != EntityType.ENDER_DRAGON) {
            return;
        }

        World world = e.getEntity().getWorld();
        DragonBattle dragonBattle = e.getEntity().getWorld().getEnderDragonBattle();
        if (dragonBattle == null || !dragonBattle.hasBeenPreviouslyKilled()) {
            return;
        }

        Location portalLocation = dragonBattle.getEndPortalLocation();
        if (portalLocation == null) {
            return;
        }

        Location eggLocation = portalLocation.add(0, 4, 0);
        world.getBlockAt(eggLocation).setType(Material.DRAGON_EGG);
    }
}