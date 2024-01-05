package plugins.nate.smp.listeners.enchantments;

import com.sk89q.worldguard.protection.flags.StateFlag;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import plugins.nate.smp.SMP;
import plugins.nate.smp.managers.EnchantmentManager;
import plugins.nate.smp.utils.SMPUtils;
import plugins.nate.smp.utils.WorldGuardUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


public class TimberListener implements Listener {

    private static final int MAX_BLOCKS = 192;


    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (!itemInHand.containsEnchantment(EnchantmentManager.ENCHANTMENTS.get("timber"))) {
            return;
        }

        if (!player.isSneaking()) {
            return;
        }

        if (!(player.getGameMode() == GameMode.SURVIVAL)) {
            return;
        }

        Block block = event.getBlock();
        Material type = block.getType();
      
        if (!isLog(type)) {
            return;
        }

        List<ItemStack> drops = new ArrayList<>();
        AtomicInteger blocksDestroyed = new AtomicInteger(0);
        Set<Block> checkedBlocks = new HashSet<>();
        destroyTree(block, drops, blocksDestroyed, checkedBlocks, player);

        Map<Material, Integer> consolidatedDrops = drops.stream()
                .collect(Collectors.groupingBy(ItemStack::getType, Collectors.summingInt(ItemStack::getAmount)));

        consolidatedDrops.forEach((material, amount) -> {
            player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(material, amount));
        });
    }

    private boolean isLog(Material type) {
        return type.name().contains("LOG") || type.name().contains("WOOD") || type.name().contains("STEM");
    }

    private void destroyTree(Block block, List<ItemStack> drops, AtomicInteger blocksDestroyed, Set<Block> checkedBlocks, Player player) {
        if (WorldGuardUtils.hasFlag(block.getLocation(), WorldGuardUtils.DISABLE_TIMBER, StateFlag.State.DENY)) {
            return;
        }

        if (blocksDestroyed.get() >= MAX_BLOCKS || !isLog(block.getType()) || block.getType() == Material.AIR || checkedBlocks.contains(block)) {
            return;
        }

        checkedBlocks.add(block);

        addCoreProtectLog(block, player);

        drops.addAll(block.getDrops());
        block.setType(Material.AIR);
        blocksDestroyed.incrementAndGet();

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        destroyTree(block.getRelative(x, y, z), drops, blocksDestroyed, checkedBlocks, player);
                    }
                }
            }
        }
    }

    private void addCoreProtectLog(Block block, Player player) {
        boolean success = SMP.getCoreProtect().logRemoval(player.getName(), block.getLocation(), block.getType(), block.getBlockData());
        if (!success) {
            SMPUtils.log("Failed to log block removal with CoreProtect!");
        }
    }
}
