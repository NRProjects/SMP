package plugins.nate.smp.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import plugins.nate.smp.SMP;

import static plugins.nate.smp.utils.ChatUtils.PREFIX;
import static plugins.nate.smp.utils.ChatUtils.sendMessage;

public class SMPUtils {
    public static final NamespacedKey OWNER_UUID_KEY = new NamespacedKey(SMP.getPlugin(), "ownerUUID");
    public static final NamespacedKey TRADE_LOCKED_KEY = new NamespacedKey(SMP.getPlugin(), "tradeLocked");

    public static void reloadPlugin(CommandSender sender) {
        SMP smp = SMP.getPlugin();
        smp.getLogger().info("[SMP] Reloading SMP plugin...");
        HandlerList.unregisterAll(smp);

        smp.reloadConfig();

        EventRegistration.registerEvents(smp);
        CommandRegistration.registerCommands(smp);

        sendMessage(sender, PREFIX + "&aPlugin reloaded");
        log("[SMP] Reloaded SMP v" + smp.getDescription().getVersion());
    }

    public static CoreProtectAPI loadCoreProtect() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("CoreProtect");
        if (!(plugin instanceof CoreProtect coreProtect)) {
            return null;
        }

        CoreProtectAPI api = coreProtect.getAPI();
        if (api.isEnabled() && api.APIVersion() >= 6) {
            return api;
        }

        return null;
    }

    public static void log(String log) {
        SMP.getPlugin().getLogger().info(log);
    }
    public static void severe(String log) { SMP.getPlugin().getLogger().severe(log); }

    public static boolean isFlagDisallowedAtLocation(StateFlag flag, Location location) {
        World world = location.getWorld();
        if (world == null) { return false; }

        RegionManager firstRegionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(location.getWorld()));
        if (firstRegionManager == null) { return false; }

        ApplicableRegionSet firstSet = firstRegionManager.getApplicableRegions(BukkitAdapter.asBlockVector(location));
        return firstSet.queryState(null, flag) == StateFlag.State.DENY;
    }

    public static boolean isPickaxe(Material material) {
        return material == Material.WOODEN_PICKAXE ||
                material == Material.STONE_PICKAXE ||
                material == Material.IRON_PICKAXE ||
                material == Material.GOLDEN_PICKAXE ||
                material == Material.DIAMOND_PICKAXE ||
                material == Material.NETHERITE_PICKAXE;
    }

    public static boolean isAxe(Material material) {
        return material == Material.WOODEN_AXE ||
                material == Material.STONE_AXE ||
                material == Material.IRON_AXE ||
                material == Material.GOLDEN_AXE ||
                material == Material.DIAMOND_AXE ||
                material == Material.NETHERITE_AXE;
    }
}
