package plugins.nate.smp.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashSet;
import java.util.Set;

public class WorldGuardUtils {
    public static final FlagRegistry flagRegistry = WorldGuard.getInstance().getFlagRegistry();
    public static StateFlag WITHER_EXPLOSIONS;
    public static StateFlag BANK_FLAG;
    public static StateFlag DISABLE_TIMBER;

    public static final Set<StateFlag> antiBuildFlags = new HashSet<>();
    static {
        antiBuildFlags.add(Flags.BUILD);
        antiBuildFlags.add(Flags.BLOCK_BREAK);
    }

    public static boolean hasFlag(Location location, StateFlag flag, StateFlag.State state) {
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(location.getWorld()));
        BlockVector3 position = BukkitAdapter.asBlockVector(location);

        if(regionManager == null) {
            return false;
        }

        ApplicableRegionSet regionSet = regionManager.getApplicableRegions(position);

        return regionSet.queryState(null, flag) == state;
    }

    public static boolean isFlagAllowedAtLocation(StateFlag flag, Location location) {
        World world = location.getWorld();
        if (world == null) { return false; }

        RegionManager firstRegionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(location.getWorld()));
        if (firstRegionManager == null) { return false; }

        ApplicableRegionSet firstSet = firstRegionManager.getApplicableRegions(BukkitAdapter.asBlockVector(location));
        return firstSet.testState(null, flag);
    }

    public static void registerFlags() {
        try {
            StateFlag witherExplosionsFlag = new StateFlag("wither-explosions", true);
            StateFlag bankFlag = new StateFlag("bank", false);
            StateFlag disableTimberFlag = new StateFlag("disable-timber", false);

            flagRegistry.register(witherExplosionsFlag);
            flagRegistry.register(bankFlag);
            flagRegistry.register(disableTimberFlag);

            WITHER_EXPLOSIONS = witherExplosionsFlag;
            BANK_FLAG = bankFlag;
            DISABLE_TIMBER = disableTimberFlag;
        } catch (FlagConflictException ignored) {}
    }
}
