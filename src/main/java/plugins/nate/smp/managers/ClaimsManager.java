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
    public static final Map<Player, Location[]> playerSelections = new HashMap<>();
    private static final Map<UUID, Integer> borderDisplayTasks = new HashMap<>();
    private static final Map<Claim, Set<UUID>> claimInvites = new HashMap<>();


    public static void loadClaims() {
        ResultSet rs = SMPDatabase.queryDB("SELECT * FROM claims");
        try {
            while (rs.next()) {
                String claimName = rs.getString("ClaimName");
                UUID owner = UUID.fromString(rs.getString("OwnerUUID"));
                World world = Bukkit.getWorld(rs.getString("World"));
                Location pos1 = new Location(world, rs.getInt("MaxX"), rs.getInt("MaxY"), rs.getInt("MaxZ"));
                Location pos2 = new Location(world, rs.getInt("MinX"), rs.getInt("MinY"), rs.getInt("MinZ"));
                Location[] points = {pos1, pos2};

                claims.add(new Claim(points, owner, claimName));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void giveClaimTool(Player player) {
        ItemStack claimTool = new ItemStack(Material.GOLDEN_SHOVEL);
        ItemMeta claimToolMeta = claimTool.getItemMeta();

        if (claimToolMeta == null) {
            return;
        }

        SMPUtils.changeItemName(claimToolMeta, "&aClaim Tool");
        addItemLore(claimToolMeta, "");
        addItemLore(claimToolMeta, "&a&oSelect 2 points to create a claim");
        addItemLore(claimToolMeta, "&a&o/smp claim confirm to create your claim");
        setItemNBT(claimToolMeta, CLAIM_TOOL_NBT);

        claimTool.setItemMeta(claimToolMeta);
        player.getInventory().addItem(claimTool);
    }

    public static ItemStack claimTool() {
        ItemStack claimTool = new ItemStack(Material.GOLDEN_SHOVEL);
        ItemMeta claimToolMeta = claimTool.getItemMeta();

        SMPUtils.changeItemName(claimToolMeta, "&aClaim Tool");
        addItemLore(claimToolMeta, "");
        addItemLore(claimToolMeta, "&a&oSelect 2 points to create a claim");
        addItemLore(claimToolMeta, "&a&o/smp claim confirm to create your claim");
        setItemNBT(claimToolMeta, CLAIM_TOOL_NBT);

        claimTool.setItemMeta(claimToolMeta);
        return claimTool;
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

    public static boolean createClaim(Claim claim) {
        if (doesClaimOverlap(claim)) {
            sendMessage(Bukkit.getPlayer(claim.getOwner()), PREFIX + "&cFailed to create a claim. Part of your selection is already claimed!");
            return false;
        }

        SMPDatabase.queryDB("INSERT INTO claims (ClaimName, OwnerUUID, World, MaxX, MaxY, MaxZ, MinX, MinY, MinZ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);",
                // Claim name
                claim.getClaimName(),
                // Owner
                claim.getOwner().toString(),
                // Word
                claim.getWorld().getName(),
                // Maximum X, Y, Z
                claim.getMaxX(), claim.getMaxY(), claim.getMaxZ(),
                // Minimum X, Y, Z
                claim.getMinX(), claim.getMinY(), claim.getMinZ());

        claims.add(claim);
        return true;
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
        final int[] taskIdHolder = new int[1];

        UUID playerUUID = player.getUniqueId();
        double maxDistance = 64.0;

        if (borderDisplayTasks.containsKey(playerUUID)) {
            int taskId = borderDisplayTasks.get(playerUUID);
            Bukkit.getScheduler().cancelTask(taskId);
            borderDisplayTasks.remove(playerUUID);
        } else {
            taskIdHolder[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(SMP.getPlugin(), () -> {
                if (!player.isOnline()) {
                    Bukkit.getScheduler().cancelTask(taskIdHolder[0]);
                    borderDisplayTasks.remove(playerUUID);
                    return;
                }

                Location playerLocation = player.getLocation();
                Location nearestEdge = findNearestClaimEdge(playerLocation, claim);

                if (playerLocation.distance(nearestEdge ) > maxDistance) {
                    Bukkit.getScheduler().cancelTask(taskIdHolder[0]);
                    borderDisplayTasks.remove(playerUUID);
                    return;
                }

                World world = claim.getWorld();
                Location pos1 = new Location(claim.getWorld(), claim.getMaxX(), claim.getMaxY(), claim.getMaxZ());
                Location pos2 = new Location(claim.getWorld(), claim.getMinX(), claim.getMinY(), claim.getMinZ());

                // We add plus one to account for particle being offset when spawned
                int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
                int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX()) + 1;
                int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
                int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY()) + 1;
                int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
                int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ()) + 1;

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

            borderDisplayTasks.put(playerUUID, taskIdHolder[0]);
        }
    }

    private static void spawnParticle(World world, double x, double y, double z, Player player) {
        Location location = new Location(world, x, y, z);
        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(66, 135, 245), 1.0F);

        player.spawnParticle(Particle.REDSTONE, location, 1, 0, 0, 0, 10, dustOptions);
    }

    public static boolean doesClaimOverlap(Claim claim) {
        ResultSet rs = SMPDatabase.queryDB(
                "SELECT * FROM claims WHERE World = ? AND NOT (MaxX < ? OR MinX > ? OR MaxY < ? OR MinY > ? OR MaxZ < ? OR MinZ > ?)",
                claim.getWorld().getName(),
                claim.getMinX(), claim.getMaxX(),
                claim.getMinY(), claim.getMaxY(),
                claim.getMinZ(), claim.getMaxZ()
        );

        try {
            // If there are any entries in the result set there is an overlap
            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            SMPUtils.severe("Issue with the try-catch within doesClaimOverlap() method");
            e.printStackTrace();
        }

        return false;
    }

    public static void toggleClaimBorder(Claim claim, Player player) {
        UUID playerUUID = player.getUniqueId();

        if (borderDisplayTasks.containsKey(playerUUID)) {
            int taskId = borderDisplayTasks.get(playerUUID);
            Bukkit.getScheduler().cancelTask(taskId);
            borderDisplayTasks.remove(playerUUID);
            sendMessage(player, PREFIX + "&cClaim border display turned off");
        } else {
            displayClaimBorder(claim, player);
            sendMessage(player, PREFIX + "&aClaim border display turned on");
        }
    }

    private static Location findNearestClaimEdge(Location playerLocation, Claim claim) {
        World world = claim.getWorld();
        double nearestX = Math.max(claim.getMinX(), Math.min(claim.getMaxX(), playerLocation.getX()));
        double nearestY = Math.max(claim.getMinY(), Math.min(claim.getMaxY(), playerLocation.getY()));
        double nearestZ = Math.max(claim.getMinZ(), Math.min(claim.getMaxZ(), playerLocation.getZ()));

        return new Location(world, nearestX, nearestY, nearestZ);
    }

    public static boolean hasClaim(Player player) {
        ResultSet rs = SMPDatabase.queryDB("SELECT * FROM claims WHERE OwnerUUID = ?;", player.getUniqueId().toString());

        try {
            return rs.next();
        } catch (SQLException e) {
            SMPUtils.severe("Issue with the try-catch within hasClaim() method");
            e.printStackTrace();
        }

        return false;
    }

    public static boolean isOwnerOfClaim(Player player, Claim claim) {
        UUID ownerUUID = player.getUniqueId();
        ResultSet rs = SMPDatabase.queryDB("SELECT * FROM claims WHERE ClaimName = ? AND OwnerUUID = ?;", claim.getClaimName(), ownerUUID.toString());

        try {
            return rs.next();
        } catch (SQLException e) {
            SMPUtils.severe("Issue with the try-catch within isOwnerOfClaim() method");
            e.printStackTrace();
        }

        return false;
    }

    public static Claim getClaimFromName(String name) {
        ResultSet rs = SMPDatabase.queryDB("SELECT * FROM claims WHERE ClaimName = ?;", name);

        try{
            if (rs.next()) {
                String claimName = rs.getString("ClaimName");
                UUID ownerUUID = UUID.fromString(rs.getString("OwnerUUID"));
                World world = Bukkit.getServer().getWorld(rs.getString("World"));
                Location[] points = new Location[]{
                        new Location(world, rs.getInt("MaxX"), rs.getInt("MaxY"), rs.getInt("MaxZ")),
                        new Location(world, rs.getInt("MinX"), rs.getInt("MinY"), rs.getInt("MinZ"))
                };

                return new Claim(points, ownerUUID, claimName);
            }
        } catch (SQLException e) {
            SMPUtils.severe("Issue with the try-catch within getClaimFromName() method");
            e.printStackTrace();
        }

        return null;
    }

    public static boolean claimNameAlreadyExists(String claimName) {
        ResultSet rs = SMPDatabase.queryDB("SELECT * FROM claims WHERE ClaimName = ?", claimName);

        try {
             return rs.next();
        } catch (SQLException e) {
            SMPUtils.severe("Issue with the try-catch within claimNameAlreadyExists() method");
            e.printStackTrace();
        }

        return false;
    }

    public static void sendClaimInvite(Player claimOwner, Player invitedPlayer, Claim claim) {
//        ComponentBuilder tooltip = new ComponentBuilder()
//                .append(net.md_5.bungee.api.ChatColor.GREEN + "[Click to accept invite]");
//
//        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(tooltip.create()));
//        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/claim invite accept");
//
//        ComponentBuilder chatMessage = new ComponentBuilder()
//                .append(net.md_5.bungee.api.ChatColor.GRAY + "Positions: " + ChatColor.GREEN + "[Hover to show]")
//                .event(hoverEvent)
//                .event(clickEvent);



        // TODO: Specify which claim they invited the player to
        claimInvites.computeIfAbsent(claim, k -> new HashSet<>()).add(invitedPlayer.getUniqueId());
        sendMessage(claimOwner, PREFIX + "&7You have invited &a" + invitedPlayer.getName() + " &7to &a" + claim.getClaimName());
        sendMessage(invitedPlayer, PREFIX + "&a" + claimOwner.getName() + " &7invited you to &a" + claim.getClaimName());

        for (Map.Entry<Claim, Set<UUID>> entry : claimInvites.entrySet()) {
            SMPUtils.severe(entry.getKey().getClaimName());
        }
    }

    public static void acceptClaimInvite(Player player, Claim claim) {
        Set<UUID> invitedPlayers = claimInvites.getOrDefault(claim, Collections.emptySet());
        if (invitedPlayers.isEmpty()) {
            sendMessage(player, PREFIX + "empty map");
        }

        for (Map.Entry<Claim, Set<UUID>> entry : claimInvites.entrySet()) {
            SMPUtils.severe(entry.getKey().getClaimName());
        }
        if (invitedPlayers.remove(player.getUniqueId())) {
            // Player was in the invite list and accepted the invite
            sendMessage(player, "TEST1");
            // TODO: Implement
        } else {
            sendMessage(player, PREFIX + "&cYou were not invited to " + claim.getClaimName());
        }
    }

    public static void declineClaimInvite(Player player, Claim claim) {
        Set<UUID> invitedPlayers = claimInvites.getOrDefault(claim, Collections.emptySet());
        if (invitedPlayers.remove(player.getUniqueId())) {
            sendMessage(player, "TEST2");
            // Player was in the invite list and declined the invite
            // TODO: Implement
        } else {
            sendMessage(player, PREFIX + "&cYou were not invited to " + claim.getClaimName());
        }
    }

}
