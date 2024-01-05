package plugins.nate.smp.utils;

import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import java.util.Set;

public class WorldGuardUtils {
    public static boolean hasFlags(ProtectedRegion region, Set<StateFlag> flags) {
        return region.getFlags().keySet().stream().anyMatch(regionFlag -> flags.stream().anyMatch(flag -> flag.equals(regionFlag)));
    }


}
