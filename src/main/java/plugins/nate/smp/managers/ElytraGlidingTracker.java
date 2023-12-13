package plugins.nate.smp.managers;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import plugins.nate.smp.records.PlayerPoint;
import plugins.nate.smp.SMP;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Purpose of this class:
 * Minecraft has a bug in the game where if you if are gliding at an angle steeper than ~36.9 degrees, you will
 * take damage based on the height you jumped from. If you had a superflat world and teleported 120 blocks above
 * your spawn, then glided down at a 36.8 degree angle, you would live and take no damage. If you repeated this,
 * instead going at a 36.9 degree angle, you would die.
 * <p>
 * This tracker takes the player's last position and
 * their current position, and calculates a speed. Then, using fall speed where damage starts and where it becomes
 * lethal, extrapolates that into a damage scale that's comparable for Elytras.
 * <p>
 * This results in a 43.8 degree angle being the first damaging angle for Elytras, and a 62.7 degree angle being the
 * new point of lethality. This is affected by feather falling as well, and the new lethal angle becomes ~75 degrees.
 * <p>
 * <a href="https://bugs.mojang.com/browse/MC-210371">See more about this bug</a>
 */
public class ElytraGlidingTracker {
    public static Set<Player> gliding = new HashSet<>();
    public static HashMap<Player, Double> calculatedDamageMap = new HashMap<>();
    public static Map<Player, PlayerPoint> lastLocationMap = new HashMap<>();

    public static void startTracking() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : gliding) {
                    if (!lastLocationMap.containsKey(player)) {
                        //If we don't have a last location, we can't calculate the speed over time.
                        //So, just insert their current location and move on.
                        lastLocationMap.put(player, new PlayerPoint(player.getLocation(), System.currentTimeMillis()));
                        continue;
                    }

                    PlayerPoint playerPoint = lastLocationMap.get(player);

                    //Gets how long since we last calculated damage between two locations.
                    long elapsedTime = System.currentTimeMillis() - playerPoint.time();
                    if (elapsedTime == 0) {
                        return;
                    }

                    Location lastLocation = playerPoint.location();
                    double distance = lastLocation.distance(player.getLocation());
                    if (distance == 0) {
                        return;
                    }

                    //Change in Y between player's current location and their "last" location
                    //(About a tick, but really varies between 49-51ms)
                    double deltaY = Math.abs(lastLocation.getY() - player.getLocation().getY());
                    double verticalSpeed = deltaY / elapsedTime;

                    //Derived from fall damage values.
                    //A 4 block fall (the first one that damages you) has a vertical speed of ~0.013
                    //A 24 block fall (first lethal fall height) has a vertical speed of ~0.032
                    //Extrapolating between these two points, you can create the slope formula
                    //y = 1052.63x - 12.68 where x is vertical speed.
                    double calculatedDamage = 1052.63 * verticalSpeed - 12.68;

                    calculatedDamageMap.put(player, calculatedDamage);
                    lastLocationMap.put(player, new PlayerPoint(player.getLocation(), System.currentTimeMillis()));
                }
            }
        }.runTaskTimer(SMP.getPlugin(), 0L, 1L);
    }
}
