package plugins.nate.smp.guis;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import plugins.nate.smp.SMP;
import plugins.nate.smp.interfaces.CustomGUI;

public class TellerDepositGUI implements CustomGUI {
    private final Inventory inventory;
    private final NamespacedKey key;

    public TellerDepositGUI() {
        this.key = new NamespacedKey(SMP.getPlugin(), "deposit_gui");
        this.inventory = Bukkit.createInventory(this, 27, "Deposit Diamonds");
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
