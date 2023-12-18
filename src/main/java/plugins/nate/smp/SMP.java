package plugins.nate.smp;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import plugins.nate.smp.managers.ElytraGlidingTracker;
import plugins.nate.smp.managers.EnchantmentManager;
import plugins.nate.smp.managers.RecipeManager;
import plugins.nate.smp.managers.TrustManager;
import plugins.nate.smp.utils.CommandRegistration;
import plugins.nate.smp.utils.DependencyUtils;
import plugins.nate.smp.utils.EventRegistration;
import plugins.nate.smp.utils.SMPUtils;

import java.io.File;
import java.util.logging.Logger;

public final class SMP extends JavaPlugin {
    private static SMP plugin;
    private static CoreProtectAPI coreProtect;

    public static StateFlag WITHER_EXPLOSIONS;

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
            registry.register(witherExplosionsFlag);

            WITHER_EXPLOSIONS = witherExplosionsFlag;
        } catch (FlagConflictException ignored) {}
    }

    public static SMP getPlugin() {
        return plugin;
    }

    public static CoreProtectAPI getCoreProtect() {
        return coreProtect;
    }

}

