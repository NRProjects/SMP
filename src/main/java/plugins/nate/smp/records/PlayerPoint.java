package plugins.nate.smp.records;

import org.bukkit.Location;

/**
 * Used for Elytra tracking to store where a player was at a given time
 * @param location Player's location at the captured time
 * @param time Epoch timestamp at time of capture
 */
public record PlayerPoint(Location location, long time) {}
