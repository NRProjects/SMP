package plugins.nate.smp.managers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import plugins.nate.smp.SMP;
import plugins.nate.smp.utils.SMPUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static plugins.nate.smp.utils.ChatUtils.PREFIX;
import static plugins.nate.smp.utils.ChatUtils.sendMessage;
import static plugins.nate.smp.utils.SMPUtils.addItemLore;
import static plugins.nate.smp.utils.SMPUtils.setItemNBT;

public class ClaimsManager {
    private static final String CLAIM_TOOL_NBT = "claim_tool";
    private static final Map<Player, Location[]> playerSelections = new HashMap<>();

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

    public static void clearSelection(Player player) {
        playerSelections.remove(player);
    }

    public static boolean hasNullSelectionPoint(Player player) {
        Location[] points = playerSelections.get(player);
        if (points == null) {
            return true;
        }

        return points[0] == null || points[1] == null;
    }

    public static boolean pointsInSameWorld(Player player) {
        Location[] points = playerSelections.get(player);

        if (points == null || points[0] == null) {
            return false;
        }

        return points[0].getWorld().equals(points[1].getWorld());
    }
}
