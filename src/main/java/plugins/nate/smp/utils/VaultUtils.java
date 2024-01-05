package plugins.nate.smp.utils;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import plugins.nate.smp.SMP;

public class VaultUtils {
    public static Economy econ = null;

    public static void setupEconomy(SMP plugin) {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", plugin.getDescription().getName()));
            Bukkit.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().severe(String.format("[%s] - Disabled due to no Economy service found!", plugin.getDescription().getName()));
            Bukkit.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }

        econ = rsp.getProvider();
    }

    public static boolean deposit(Player player, double amount) {
        return econ.depositPlayer(player, amount).transactionSuccess();
    }

    public static boolean withdraw(Player player, double amount) {
        return econ.withdrawPlayer(player, amount).transactionSuccess();
    }

    public static double getBalance(Player player) {
        return econ.getBalance(player);
    }
}
