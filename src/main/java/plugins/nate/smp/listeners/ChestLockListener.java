package plugins.nate.smp.listeners;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataType;
import plugins.nate.smp.managers.TrustManager;
import plugins.nate.smp.utils.SMPUtils;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static plugins.nate.smp.utils.ChatUtils.*;

public class ChestLockListener implements Listener {
    private static final String LOCKED_TAG = "[LockedV2]";
    private static final BlockFace[] CARDINAL_FACES = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
    private static final BlockFace[] FACES_TO_CHECK = { BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
    private static final Set<Material> STORAGE_CONTAINERS = EnumSet.of(Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL);

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Sign sign = (Sign) block.getState();
        World world = player.getWorld();
        Location signLocation = sign.getLocation();

        if (isLockedSign(sign) && !playerHasAccess(player, sign)) {
            sendMessageAndCancel(event, player, "&cYou cannot edit this locked sign!");
            return;
        }

        if (hasLockLine(event) && isLockableSign(block)) {
            // If attached block isn't a storage container
            if (!(isStorageContainer(getAttachedBlock(block).getType()))) {
                sendMessage(player, PREFIX + "&cMust be placed directly on a storage container!");
                return;
            }

            event.setLine(0, LOCKED_TAG);
            event.setLine(1, player.getName());
            event.setLine(2, "");
            event.setLine(3, "");
            sendMessage(player, PREFIX + "&aChest locked");

            signCreationParticles(world, signLocation, Particle.WAX_ON);
            player.playSound(signLocation, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.BLOCKS, 1.0f, 1.0f);
            sign.setWaxed(true);

            sign.getPersistentDataContainer().set(SMPUtils.OWNER_UUID_KEY, PersistentDataType.STRING, player.getUniqueId().toString());
            sign.update();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChestAccess(PlayerInteractEvent event) {
        // If right-clicked on storage container
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && isStorageContainer(event.getClickedBlock().getType())) {
            Sign attachedSign = getAttachedSign(event.getClickedBlock());
            // If valid sign and doesn't have access
            if (attachedSign != null && isLockedSign(attachedSign) && !playerHasAccess(event.getPlayer(), attachedSign)) {
                sendMessageAndCancel(event, event.getPlayer(), "&cThis chest is locked");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLockedChestOrSignBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (isStorageContainer(block.getType())) {
            processBlockBreak(event, player, getAttachedSign(block));
        } else if (block.getState() instanceof Sign sign && isLockedSign(sign)) {
            processBlockBreak(event, player, sign);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHopperPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.HOPPER) {
            return;
        }

        Player player = event.getPlayer();
        for (BlockFace face : FACES_TO_CHECK) {
            Block adjacentBlock = event.getBlockPlaced().getRelative(face);
            if (isStorageContainer(adjacentBlock.getType())) {
                Sign attachedSign = getAttachedSign(adjacentBlock);
                if (attachedSign != null && isLockedSign(attachedSign) && !playerCanInteract(player, attachedSign)) {
                    sendMessageAndCancel(event, player, "&cYou cannot place a hopper next to a locked container");
                    return;
                }
            }
        }
    }

    /**
    *   Spawn particles with random offsets, .25 blocks in variation
    */
    private void signCreationParticles(World world, Location location, Particle particle) {
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        world.spawnParticle(particle, x +.5, y + .5, z + .5, 10, .25, .25, .25);
    }

    /**
    *  sendMessageAndCancel if they are unable to break a sign or chest
    */
    private void processBlockBreak(BlockBreakEvent event, Player player, Sign sign) {
        if (sign != null && isLockedSign(sign) && !playerCanInteract(player, sign)) {
            sendMessageAndCancel(event, player, isStorageContainer(getAttachedBlock(sign.getBlock()).getType()) ? "&cThis chest is locked" : "&cYou cannot break a lock");
        }
    }

    /**
    * Checks if block is a WallSign
     */
    private boolean isLockableSign(Block block) {
        BlockData blockData = block.getBlockData();
        return blockData instanceof WallSign;
    }

    /**
     * Cancels an event and sends a message to the player */
    private void sendMessageAndCancel(Event event, Player player, String message) {
        sendMessage(player, PREFIX + message);
        if (event instanceof Cancellable c) {
            c.setCancelled(true);
        }
    }

    /**
    * If its a WallSign, gets the block its attached to. Otherwise, the block below it
     */
    private Block getAttachedBlock(Block block) {
        BlockData blockData = block.getBlockData();
        return blockData instanceof WallSign ? block.getRelative(((WallSign) blockData).getFacing().getOppositeFace()) : block.getRelative(BlockFace.DOWN);
    }

    /**
     * Grabs DoubleChestInventory from block and returns the opposite block in the double chest
    */
    private Block getOtherHalfOfChest(Block block) {
        // Checks if block isn't CHEST or TRAPPED_CHEST
        if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) {
            return null;
        }

        BlockState blockState = block.getState();
        if (!(blockState instanceof Chest chest)) {
            return null;
        }

        Inventory inventory = chest.getInventory();
        if (!(inventory instanceof DoubleChestInventory doubleChestInventory)) {
            return null;
        }

        Block leftSide = doubleChestInventory.getLeftSide().getLocation().getBlock();
        Block rightSide = doubleChestInventory.getRightSide().getLocation().getBlock();
        if (block.equals(leftSide)) {
            return rightSide;
        } else if (block.equals(rightSide)) {
            return leftSide;
        }

        return null;
    }

    /**
     * Checks CARDINAL_FACES around block and returns the first wall sign found
     */
    private Sign scanForAttachedSign(Block block) {

        for (BlockFace blockface : CARDINAL_FACES) {
            // Grabs block in the relative position
            Block otherblock = block.getRelative(blockface);
            // If its not a WallSign,
            if (!(otherblock.getBlockData() instanceof WallSign wallSign)) {
                continue;
            }

            // If the WallSign found is not facing the same direction as the face its on
            if (!(wallSign.getFacing().equals(blockface))) {
                continue;
            }

            Sign sign = (Sign) otherblock.getState();
            if (isLockedSign(sign)) {
                return sign;
            }
        }
        return null;
    }

    /**
     *  Checks sign attached to the current block or the other half of the chest
     */
    private Sign getAttachedSign(Block block) {
        Sign foundSign = scanForAttachedSign(block);
        if (foundSign != null) {
            return foundSign;
        }

        Block otherHalf = getOtherHalfOfChest(block);
        if (otherHalf != null) {
            return scanForAttachedSign(otherHalf);
        }

        return null;
    }

    /**
     * Gets block data of PublicBukkitValues.smp:owneruuid stored in NBT
     */
    private UUID getLockedSignOwner(Sign sign) {
        String ownerUUID = sign.getPersistentDataContainer().get(SMPUtils.OWNER_UUID_KEY, PersistentDataType.STRING);
        if (ownerUUID == null) {
            return null;
        }

        return UUID.fromString(ownerUUID);
    }

    /**
     * Checks if sign is valid and has ownerUUID data
     */
    private boolean isLockedSign(Sign sign) {
        return getLockedSignOwner(sign) != null;
    }

    /**
     * Returns true if player is the owner or is trusted by the owner of the sign
     */
    private boolean playerHasAccess(Player player, Sign attachedSign) {
        if (canPlayerBypass(player)) {
            return true;
        }

        UUID ownerUUID = getLockedSignOwner(attachedSign);

        Set<UUID> trustedPlayersUUID = TrustManager.getTrustedPlayers(ownerUUID);
        return player.getUniqueId().equals(ownerUUID) || trustedPlayersUUID.contains(player.getUniqueId());
    }

    /**
     * Compares UUID of the sign owner and a player UUID
     */
    private boolean isPlayerOwner(Player player, Sign attachedSign) {
        UUID ownerUUID = getLockedSignOwner(attachedSign);
        return player.getUniqueId().equals(ownerUUID);
    }

    /**
     * Checking if player has admin permissions to bypass
     */
    private boolean canPlayerBypass(Player player) {
        return player.hasPermission("smp.bypasslocks") || player.isOp();
    }

    /**
     * Returns if player is sign owner or admin
     */
    private boolean playerCanInteract(Player player, Sign attachedSign) {
        return isPlayerOwner(player, attachedSign) || canPlayerBypass(player);
    }

    /**
     * Returns if block is an item from the STORAGE_CONTAINERS set
     */
    private boolean isStorageContainer(Material material) {
        return STORAGE_CONTAINERS.contains(material);
    }

    /**
     * Checks if sign has [Lock] on any line
     */
    private boolean hasLockLine(SignChangeEvent event) {
        return Arrays.stream(event.getLines())
                .anyMatch("[Lock]"::equalsIgnoreCase);
    }
}
