package plugins.nate.smp.objects;

import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class Claim {
    private final Location[] points;
    private final World world;
    private final UUID owner;

    public Claim(@NotNull Location[] points, UUID owner) {
        if (points == null || points.length != 2) {
            throw new IllegalArgumentException("You must provide exactly two valid locations.");
        }

        this.points = points;
        this.world = points[0].getWorld();
        this.owner = owner;
    }

    public Location getPos1() {
        return points[0];
    }

    public Location getPos2() {
        return points[1];
    }

    public World getWorld() {
        return world;
    }

    public UUID getOwner() {
        return owner;
    }

    public Location[] getPoints() {
        return points;
    }
}
