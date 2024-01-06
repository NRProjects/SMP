package plugins.nate.smp;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import plugins.nate.smp.managers.ElytraGlidingTracker;
import plugins.nate.smp.managers.EnchantmentManager;
import plugins.nate.smp.managers.RecipeManager;
import plugins.nate.smp.managers.TrustManager;
import plugins.nate.smp.utils.*;

import java.io.File;
import java.util.logging.Logger;

public final class SMP extends JavaPlugin {
    private static SMP plugin;
    private static CoreProtectAPI coreProtect;

    public static StateFlag WITHER_EXPLOSIONS;
    public static StateFlag BANK_FLAG;

    public static final Logger logger = Logger.getLogger("Minecraft");
    public final File prefixesFile = new File(getDataFolder() + "/prefixes.yml");
    public FileConfiguration prefixes;


    @Override
    public void onEnable() {
        super.onEnable();
        plugin = this;
        coreProtect = SMPUtils.loadCoreProtect();

        TrustManager.init(this.getDataFolder());

        DependencyUtils.checkDependencies();
        EventRegistration.registerEvents(this);
        VaultUtils.setupEconomy(this);
        CommandRegistration.registerCommands(this);
        EnchantmentManager.registerEnchants();
        RecipeManager.registerRecipes();
        ElytraGlidingTracker.startTracking();
    }

    @Override
    public void onLoad() {
        super.onLoad();

        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            StateFlag witherExplosionsFlag = new StateFlag("wither-explosions", true);
            StateFlag bankFlag = new StateFlag("bank", false);

            registry.register(witherExplosionsFlag);
            registry.register(bankFlag);

            WITHER_EXPLOSIONS = witherExplosionsFlag;
            BANK_FLAG = bankFlag;
        } catch (FlagConflictException ignored) {}
    }

    public void onDisable() {
        for(Player p : plugin.getServer().getOnlinePlayers()) {
            // Prevents bug regarding players interacting with tellers during shutdown
            p.closeInventory();
        }
    }

    public static SMP getPlugin() {
        return plugin;
    }

    public static CoreProtectAPI getCoreProtect() {
        return coreProtect;
    }


}

