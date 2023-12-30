package plugins.nate.smp.objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public class Claim {
    private final int maxX, maxY, maxZ;
    private final int minX, minY, minZ;
    private final World world;
    private final UUID owner;
    private final String claimName;

    public Claim(@NotNull Location[] points, @NotNull UUID owner, @NotNull String claimName) {
        if (points == null || points.length != 2) {
            throw new IllegalArgumentException("You must provide exactly two valid locations when creating a claim!");
        }

        this.world = points[0].getWorld();
        this.owner = owner;
        this.claimName = claimName;

        this.minX = Math.min(points[0].getBlockX(), points[1].getBlockX());
        this.maxX = Math.max(points[0].getBlockX(), points[1].getBlockX());
        this.minY = Math.min(points[0].getBlockY(), points[1].getBlockY());
        this.maxY = Math.max(points[0].getBlockY(), points[1].getBlockY());
        this.minZ = Math.min(points[0].getBlockZ(), points[1].getBlockZ());
        this.maxZ = Math.max(points[0].getBlockZ(), points[1].getBlockZ());
    }

    public int getMinX() { return minX; }
    public int getMaxX() { return maxX; }
    public int getMinY() { return minY; }
    public int getMaxY() { return maxY; }
    public int getMinZ() { return minZ; }
    public int getMaxZ() { return maxZ; }

    public String getClaimName() {
        return claimName;
    }

    public World getWorld() {
        return world;
    }

    public UUID getOwner() {
        return owner;
    }

    public OfflinePlayer getOwnerPlayer() {
        return Bukkit.getOfflinePlayer(owner);
    }

    public String getOwnerName() {
        return Bukkit.getPlayer(owner).getName();
    }

    public boolean isInside(Location location) {
        if (!location.getWorld().equals(this.world)) return false;
        int x = location.getBlockX(), y = location.getBlockY(), z = location.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Claim claim = (Claim) obj;
        return Objects.equals(getClaimName(), claim.getClaimName()); // Assuming claimName is unique
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClaimName());
    }
}
