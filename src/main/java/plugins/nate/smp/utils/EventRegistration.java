package plugins.nate.smp.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import plugins.nate.smp.SMP;
import plugins.nate.smp.listeners.*;
import plugins.nate.smp.listeners.enchantments.TimberListener;
import plugins.nate.smp.listeners.enchantments.VeinMinerListener;
import plugins.nate.smp.managers.EnchantmentManager;

public class EventRegistration {
    public static void registerEvents(SMP plugin) {
        PluginManager pm = Bukkit.getPluginManager();

        pm.registerEvents(new CropReplanterListener(), plugin);
        pm.registerEvents(new CommandInterceptionListener(), plugin);
        pm.registerEvents(new ChestLockListener(), plugin);
        pm.registerEvents(new ConcreteWaterListener(), plugin);
        pm.registerEvents(new AnvilRepairCostListener(), plugin);
        pm.registerEvents(new WitherExplosionListener(), plugin);
        pm.registerEvents(new DragonEggRespawnListener(), plugin);
        pm.registerEvents(new VeinMinerListener(), plugin);
        pm.registerEvents(new EnchantmentManager(), plugin);
        pm.registerEvents(new TimberListener(), plugin);
        pm.registerEvents(new AntiEntityGriefListener(), plugin);
        pm.registerEvents(new ElytraFallListener(), plugin);
        pm.registerEvents(new ExpandedRocketCraftingListener(), plugin);
        pm.registerEvents(new TellerTradeListener(), plugin);
    }
}
