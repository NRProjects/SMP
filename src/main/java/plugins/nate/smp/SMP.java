package plugins.nate.smp;

import net.coreprotect.CoreProtectAPI;
import org.bukkit.configuration.file.FileConfiguration;
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

        WorldGuardUtils.registerFlags();
    }

    public static SMP getPlugin() {
        return plugin;
    }

    public static CoreProtectAPI getCoreProtect() {
        return coreProtect;
    }

}

