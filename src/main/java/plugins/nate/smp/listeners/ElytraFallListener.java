package plugins.nate.smp.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import plugins.nate.smp.managers.ElytraGlidingTracker;

public class ElytraFallListener implements Listener {
    @EventHandler
    public void onGlide(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (p.isGliding()) {
            ElytraGlidingTracker.gliding.add(p);
        } else {
            ElytraGlidingTracker.gliding.remove(p);
        }
    }

    //High priority may not be a requirement here, but I think it's fair to assume we should be pretty high up on
    //the list since we're rewriting a core mechanic.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) {
            return;
        }

        if (e.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }

        if (ElytraGlidingTracker.calculatedDamageMap.containsKey(player)) {
            double calculatedDamage = ElytraGlidingTracker.calculatedDamageMap.get(player);
            if (calculatedDamage <= 0) {
                e.setCancelled(true);
            }

            e.setDamage(calculatedDamage);
            ElytraGlidingTracker.calculatedDamageMap.remove(player);
            ElytraGlidingTracker.lastLocationMap.remove(player);
        }
    }
}