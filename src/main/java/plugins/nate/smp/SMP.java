package plugins.nate.smp;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.plugin.java.JavaPlugin;
import plugins.nate.smp.managers.*;
import plugins.nate.smp.storage.SMPDatabase;
import plugins.nate.smp.utils.CommandRegistration;
import plugins.nate.smp.utils.DependencyUtils;
import plugins.nate.smp.utils.EventRegistration;
import plugins.nate.smp.utils.SMPUtils;

public final class SMP extends JavaPlugin {
    private static SMP plugin;
    private static CoreProtectAPI coreProtect;
    private static SMPDatabase database;

    public static StateFlag WITHER_EXPLOSIONS;


    @Override
    public void onEnable() {
        super.onEnable();
        plugin = this;
        coreProtect = SMPUtils.loadCoreProtect();

        database = new SMPDatabase();
        database.initialize();

        ClaimsManager.loadClaims();
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

    @Override
    public void onDisable() {
        if (database != null) {
            database.close();
        }
    }

    public static SMP getPlugin() {
        return plugin;
    }

    public static CoreProtectAPI getCoreProtect() {
        return coreProtect;
    }

    public static SMPDatabase getDatabase() {
        return database;
    }

    // TODO: Make max claim in one direction 256 blocks
    // TODO: Make minimum claim a 5x5x5 cube

}

