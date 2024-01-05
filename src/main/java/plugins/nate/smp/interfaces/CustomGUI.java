package plugins.nate.smp.interfaces;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public interface CustomGUI extends InventoryHolder {
    Inventory getInventory();
    NamespacedKey getKey();

    default String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
