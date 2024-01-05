package plugins.nate.smp.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import plugins.nate.smp.SMP;

public class TellerUtils {
    public static final NamespacedKey TELLER_TYPE_KEY = new NamespacedKey(SMP.getPlugin(), "tellerType");
    public static final NamespacedKey PARENT_TELLER_KEY = new NamespacedKey(SMP.getPlugin(), "parentTeller");
    public static char CURRENCY_SYMBOL = '\u00D0';

    public static void createDepositTeller(Player player) {
        player.getWorld().spawn(player.getLocation(), Villager.class, (villager) -> {
            configureTeller(villager, "DEPOSIT", player);
            createTextDisplay(player, villager, "Deposit Diamonds (" + CURRENCY_SYMBOL + ")");
        });
    }

    public static void createWithdrawTeller(Player player) {
        player.getWorld().spawn(player.getLocation(), Villager.class, (villager) -> {
            configureTeller(villager, "WITHDRAW", player);
            createTextDisplay(player, villager, "Withdraw Diamonds (" + CURRENCY_SYMBOL + ")");
        });
    }

    private static void configureTeller(Villager villager, String type, Player player) {
        villager.getPersistentDataContainer().set(TELLER_TYPE_KEY, PersistentDataType.STRING, type);
        villager.setRotation(player.getLocation().getYaw(), player.getLocation().getPitch());
        villager.setInvulnerable(true);
        villager.setAI(false);
    }

    private static void createTextDisplay(Player player, Entity teller, String text) {
        player.getWorld().spawn(player.getLocation().add(0, 2.4, 0), TextDisplay.class, (displayText) -> {
            displayText.getPersistentDataContainer().set(PARENT_TELLER_KEY, PersistentDataType.STRING, teller.getUniqueId().toString());
            displayText.setBillboard(Display.Billboard.CENTER);
            displayText.setText(text);
        });
    }

    public static boolean isDepositTeller(Entity entity) {
        return "DEPOSIT".equals(entity.getPersistentDataContainer().get(TELLER_TYPE_KEY, PersistentDataType.STRING));
    }

    public static boolean isWithdrawTeller(Entity entity) {
        return "WITHDRAW".equals(entity.getPersistentDataContainer().get(TELLER_TYPE_KEY, PersistentDataType.STRING));
    }

    public static boolean isTeller(Entity entity) {
        return entity.getPersistentDataContainer().has(TellerUtils.TELLER_TYPE_KEY, PersistentDataType.STRING);
    }

    public static String getTellerKey(Entity entity) {
        return entity.getPersistentDataContainer().get(TellerUtils.TELLER_TYPE_KEY, PersistentDataType.STRING);
    }
}
