package plugins.nate.smp.managers;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import plugins.nate.smp.SMP;
import plugins.nate.smp.objects.Claim;
import plugins.nate.smp.storage.SMPDatabase;
import plugins.nate.smp.utils.SMPUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static plugins.nate.smp.utils.ChatUtils.PREFIX;
import static plugins.nate.smp.utils.ChatUtils.sendMessage;
import static plugins.nate.smp.utils.SMPUtils.addItemLore;
import static plugins.nate.smp.utils.SMPUtils.setItemNBT;

public class ClaimsManager {
    public static List<Claim> claims = new ArrayList<>();
    private static final String CLAIM_TOOL_NBT = "claim_tool";
    private static final Map<Player, Location[]> playerSelections = new HashMap<>();
    private static final Map<UUID, Integer> borderDisplayTasks = new HashMap<>();


    public static void loadClaims() {
        ResultSet rs = SMPDatabase.queryDB("SELECT * FROM claims");

        try {
            while (rs.next()) {
                UUID owner = UUID.fromString(rs.getString("OwnerUUID"));
                World world = Bukkit.getWorld(rs.getString("World"));
                Location pos1 = new Location(world, rs.getInt("Pos1_X"), rs.getInt("Pos1_Y"), rs.getInt("Pos1_Z"));
                Location pos2 = new Location(world, rs.getInt("Pos2_X"), rs.getInt("Pos2_Y"), rs.getInt("Pos2_Z"));
                Location[] points = {pos1, pos2};

                claims.add(new Claim(points, owner));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void giveClaimTool(Player player) {
        ItemStack claimTool = new ItemStack(Material.GOLDEN_SHOVEL);
        ItemMeta claimToolMeta = claimTool.getItemMeta();

        SMPUtils.changeItemName(claimToolMeta, "&aClaim Tool");
        addItemLore(claimToolMeta, "");
        addItemLore(claimToolMeta, "&a&oSelect 2 points to create a claim");
        addItemLore(claimToolMeta, "&a&o/smp claim confirm to create your claim");
        setItemNBT(claimToolMeta, CLAIM_TOOL_NBT);

        claimTool.setItemMeta(claimToolMeta);
        player.getInventory().addItem(claimTool);
    }

    public static boolean isClaimTool(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return false;
        }

        NamespacedKey key = new NamespacedKey(SMP.getPlugin(), CLAIM_TOOL_NBT);
        PersistentDataContainer container = meta.getPersistentDataContainer();

        return container.has(key, PersistentDataType.BOOLEAN) && Boolean.TRUE.equals(container.get(key, PersistentDataType.BOOLEAN));
    }

    public static void setPoint(Player player, int posNumber, Location location) {
        Location[] points = playerSelections.computeIfAbsent(player, k -> new Location[2]);

        int otherPointIndex = (posNumber == 1) ? 1 : 0;
        Location otherPoint = points[otherPointIndex];

        if (otherPoint != null && !location.getWorld().equals(otherPoint.getWorld())) {
            sendMessage(player, PREFIX + "&cBoth points must be in the same world!");
            return;
        }

        points[posNumber - 1] = location;
        SMPUtils.log(Arrays.toString(playerSelections.get(player)));
        sendMessage(player, PREFIX + "&aSelected point " + posNumber + "! " +
                "&a(X: " + (int) location.getX() + " Y: " + (int) location.getY() + " Z: " + (int) location.getZ() + ")");
    }

    public static Location[] getPoints(Player player) {
        return playerSelections.get(player);
    }

    public static boolean hasNullSelectionPoint(Player player) {
        Location[] points = playerSelections.get(player);
        if (points == null) {
            return true;
        }

        return points[0] == null || points[1] == null;
    }

    public static void createClaim(Claim claim) {
        SMPDatabase.queryDB("INSERT INTO claims (OwnerUUID, World, Pos1_X, Pos1_Y, Pos1_Z, Pos2_X, Pos2_Y, Pos2_Z) VALUES (?, ?, ?, ?, ?, ?, ?, ?);",
                // Owner
                claim.getOwner().toString(),
                // Word
                claim.getWorld().getName(),
                // First position
                (int) claim.getPos1().getX(), (int) claim.getPos1().getY(), (int) claim.getPos1().getZ(),
                // Second position
                (int) claim.getPos2().getX(), (int) claim.getPos2().getY(), (int) claim.getPos2().getZ());

        claims.add(claim);

        sendMessage(Bukkit.getPlayer(claim.getOwner()), PREFIX + "&aYou have created your claim");
    }

    public static Claim getClaimAtLocation(Location location) {
        for (Claim claim : claims) {
            if (claim.isInside(location)) {
                return claim;
            }
        }

        return null;
    }

    public static void displayClaimBorder(Claim claim, Player player) {
        UUID playerUUID = player.getUniqueId();

        if (borderDisplayTasks.containsKey(playerUUID)) {
            int taskId = borderDisplayTasks.get(playerUUID);
            Bukkit.getScheduler().cancelTask(taskId);
            borderDisplayTasks.remove(playerUUID);
        } else {
            int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(SMP.getPlugin(), () -> {
                World world = claim.getWorld();
                Location pos1 = claim.getPos1();
                Location pos2 = claim.getPos2();

                int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
                int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
                int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
                int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
                int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
                int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

                double step = 0.5;

                for (double x = minX; x <= maxX; x += step) {
                    spawnParticle(world, x, minY, minZ, player);
                    spawnParticle(world, x, maxY, minZ, player);
                    spawnParticle(world, x, minY, maxZ, player);
                    spawnParticle(world, x, maxY, maxZ, player);
                }
                for (double y = minY; y <= maxY; y += step) {
                    spawnParticle(world, minX, y, minZ, player);
                    spawnParticle(world, maxX, y, minZ, player);
                    spawnParticle(world, minX, y, maxZ, player);
                    spawnParticle(world, maxX, y, maxZ, player);
                }
                for (double z = minZ; z <= maxZ; z += step) {
                    spawnParticle(world, minX, minY, z, player);
                    spawnParticle(world, maxX, minY, z, player);
                    spawnParticle(world, minX, maxY, z, player);
                    spawnParticle(world, maxX, maxY, z, player);
                }
            }, 0L, 10L);

            borderDisplayTasks.put(playerUUID, taskId);
        }


    }

    private static void spawnParticle(World world, double x, double y, double z, Player player) {
        Location location = new Location(world, x, y, z);
        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(66, 135, 245), 1.0F);

        player.spawnParticle(Particle.REDSTONE, location, 1, 0, 0, 0, 10, dustOptions);
    }
}
