package plugins.nate.smp.guis;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import plugins.nate.smp.SMP;
import plugins.nate.smp.interfaces.CustomGUI;

public class TellerWithdrawGUI implements CustomGUI {
    private final Inventory inventory;
    private final NamespacedKey key;

    public TellerWithdrawGUI() {
        this.key = new NamespacedKey(SMP.getPlugin(), "withdraw_gui");
        this.inventory = Bukkit.createInventory(this, 36, "Withdraw Diamonds");
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public NamespacedKey getKey() {
        return key;
    }
}
