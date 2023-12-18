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
}
