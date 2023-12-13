package plugins.nate.smp.listeners;

import org.bukkit.*;
import org.bukkit.block.*;
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
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import static plugins.nate.smp.utils.ChatUtils.*;

public class ChestLockListener implements Listener {
    private static final BlockFace[] CARDINAL_FACES = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
    private static final BlockFace[] FACES_TO_CHECK = { BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
  
    // TODO: autolock feature
    // TODO: admin command to force lock to be of a certain player

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSignChange(SignChangeEvent event) {
        // TODO: Allow signs to lock chests
        // Player player = event.getPlayer();
        // Block block = event.getBlock();
        // Sign sign = (Sign) block.getState();
        // World world = player.getWorld();
        // Location signLocation = sign.getLocation();

        // if (isLockedContainer(sign) && !playerHasAccess(player, sign)) {
        //     sendMessageAndCancel(event, player, "&cYou cannot edit this locked sign!");
        //     return;
        // }

        // if (hasLockLine(event) && isLockableSign(block)) {
        //     // If attached block isn't a storage container
        //     if (!(isStorageContainer(getAttachedBlock(block).getType()))) {
        //         sendMessage(player, PREFIX + "&cMust be placed directly on a storage container!");
        //         return;
        //     }

        //     event.setLine(0, LOCKED_TAG);
        //     event.setLine(1, player.getName());
        //     event.setLine(2, "");
        //     event.setLine(3, "");
        //     sendMessage(player, PREFIX + "&aChest locked");

        //     signCreationParticles(world, signLocation, Particle.WAX_ON);
        //     player.playSound(signLocation, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.BLOCKS, 1.0f, 1.0f);
        //     sign.setWaxed(true);

        //     sign.getPersistentDataContainer().set(SMPUtils.OWNER_UUID_KEY, PersistentDataType.STRING, player.getUniqueId().toString());
        //     sign.update();
        // }
    }
    // Used to restrict placement of chests next to a locked chest
    @EventHandler
    public void onChestPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (getDChestInventory(block) == null) {
            Bukkit.broadcastMessage("Not dChest");
            return;
        }
        Block otherHalfOfChest = getOtherHalfOfChest(block);
        Player player = event.getPlayer();
        Container otherChestContainer = (Container) otherHalfOfChest.getState();
        // If the owner of the other side of the double chest and the placer aren't the same, cancel
        if (!(hasAccessToContainerOrLock(player, otherChestContainer))) {
            sendMessageAndCancel(event, player, "&cYou cannot place a chest next to an owned chest!");
        }
        // TODO: import lock data from other chest
    }

    @EventHandler
    public void onContainerAccess(PlayerInteractEvent event) {
        // If not right clicking container
        if (!(event.getAction() == Action.RIGHT_CLICK_BLOCK) || !(event.getClickedBlock().getState() instanceof Container container)) {
            Bukkit.broadcastMessage("Not Container, or not right clicking");
            return;
        }
        
        if ((!containerOrAttachedSignHasLock(container) && isPlacingLock(event))) {
            Bukkit.broadcastMessage("Locking...");
            World world = event.getPlayer().getWorld();
            // Getting location of the surface of the block clicked
            Location lockSurface = event.getClickedBlock().getLocation().add(event.getBlockFace().getDirection());
            lockContainer(event.getPlayer().getUniqueId(), container,  lockSurface, world);
            
            return;
        }
        
        Player player = event.getPlayer();
        if (!(hasAccessToContainerOrLock(player, container))) {
            // TODO: Anvil sound effect iron bars (after making container lock)
            sendMessageAndCancel(event, player, "&cThis container is locked!");
        }
        
        // If theres a locked sign and player is an owner
        if (hasAccessToContainerOrLock(player, container, false) && containerHasLockSign(container)) {
            Sign sign = getAttachedSign(container.getBlock());
            migrateSignLock(container, sign);
        }
        // TODO: Place lock icon in the bottom right of opened surface        
    }

    @EventHandler
    public void onLockedContainerBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (block.getState() instanceof Container container) {
            handleBreakContainer(event, player, container);
        }
        if (block.getState() instanceof Sign sign) {
            handleBreakSign(event, player, sign);
        }
    }

    @EventHandler
    public void onHopperPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.HOPPER) {
            return;
        }

        Player player = event.getPlayer();
        for (BlockFace face : FACES_TO_CHECK) {
            Block adjacentBlock = event.getBlockPlaced().getRelative(face);
            if (adjacentBlock instanceof Container container) {
                if (containerOrAttachedSignHasLock(container) && !(isPlayerContainerOwner(player, container) || canPlayerBypass(player))) {
                    sendMessageAndCancel(event, player, "&cYou cannot place a hopper next to a locked container");
                    return;
                }
            }
        }
    }

    /**
     * Puts player UUID inside container NBT. <p>
     * If container instanceof DoubleChest, this puts the UUID in the other side of DChest aswell 
     * @param uuid UUID of the player to put into the NBT of the container
     * @param container The container to put the NBT into
     * @param location Surface of the container that is being locked, used to display particles 
     * @param world Used to display particles in the correct world
     */
    
    public static void lockContainer(UUID uuid, Container container, @Nullable Location location, @Nullable World world) {
        Bukkit.broadcastMessage("Locking...");
        // Locks current container
        container.getPersistentDataContainer().set(SMPUtils.OWNER_UUID_KEY, PersistentDataType.STRING, uuid.toString());
        container.update();
        if (location != null && world != null) {
            lockCreationParticles(world, location, Particle.WAX_ON);
        }

        if (!(container instanceof Chest)) {
            return;
        }
        // Locks other half of chest
        getOtherHalfOfChest(container.getBlock());
        Block otherHalfOfChest = getOtherHalfOfChest(container.getBlock());
        if (otherHalfOfChest != null) {
            Container otherChestContainer = (Container) otherHalfOfChest;
            otherChestContainer.getPersistentDataContainer().set(SMPUtils.OWNER_UUID_KEY, PersistentDataType.STRING, uuid.toString());
            otherChestContainer.update();
        }
    }

    /**
     * Puts player UUID inside container NBT. <p>
     * If container instanceof DoubleChest, this puts the UUID in the other side of DChest aswell 
     * @param uuid UUID of the player to put into the NBT of the container
     * @param container The container to put the NBT into
     */
    public static void lockContainer(UUID uuid, Container container) {
        lockContainer(uuid, container, null, null);
    }

    /**
     * Puts player UUID inside container NBT, and returns the popped UUID
     * @param container The container to put the NBT into
     */
    public static UUID unlockContainer(Container container) {
        Bukkit.broadcastMessage("Unlocking...");
        UUID playerUUID = UUID.fromString(container.getPersistentDataContainer().get(SMPUtils.OWNER_UUID_KEY, PersistentDataType.STRING));
        Bukkit.broadcastMessage(playerUUID.toString());
        container.getPersistentDataContainer().remove(SMPUtils.OWNER_UUID_KEY);
        container.update();
        return playerUUID;
    }
    
    /**
     * Moves UUID stored in sign to container and other side of double chest, then removes the sign
     */
    private static void migrateSignLock(Container container, Sign sign) {
        Block signBlock = sign.getBlock();

        UUID signOwner = UUID.fromString(sign.getPersistentDataContainer().get(SMPUtils.OWNER_UUID_KEY, PersistentDataType.STRING));
        
        lockContainer(signOwner, container);

        signBlock.breakNaturally();
    }

    /**
     * Returns true if player is right clicking with IRON_NUGGET or TRIPWIRE_HOOK
     */
    private static boolean isPlacingLock(PlayerInteractEvent event) {
        if (!(event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            return false;
        }
        if (event.getItem() != null) {
            Material heldItem = event.getItem().getType();
            if (heldItem == Material.IRON_NUGGET || heldItem == Material.TRIPWIRE_HOOK) {
                return true;
            }
        }
        return false;
    }

    /**
    *   Spawn particles with random offsets, .25 blocks in variation
    */
    private static void lockCreationParticles(World world, Location location, Particle particle) {
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        world.spawnParticle(particle, x +.5, y + .5, z + .5, 10, .25, .25, .25);
    }

    /**
    *  sendMessageAndCancel if they dont have permission to break container
    */
    private static void handleBreakContainer(BlockBreakEvent event, Player player, Container container) {
        // If not owner or admin, cancel break
        if (container != null && containerOrAttachedSignHasLock(container) && !(isPlayerContainerOwner(player, container) || canPlayerBypass(player))) {
            sendMessageAndCancel(event, player, "&cThis container is locked");
        }
    }
    /**
     * sendMessageAndCancel if they dont have permission to break container
     */
    private static void handleBreakSign(BlockBreakEvent event, Player player, Sign sign) {
        // If not owner or admin, cancel break
        if (sign != null && getLockedSignOwner(sign) != null && !(isPlayerSignOwner(player, sign) || canPlayerBypass(player))) {
            sendMessageAndCancel(event, player, "&cThis container is locked");
        }
        
    }

    /**
     * Cancels an event and sends a message to the player 
     */
    private static void sendMessageAndCancel(Event event, Player player, String message) {
        sendMessage(player, PREFIX + message);
        if (event instanceof Cancellable c) {
            c.setCancelled(true);
        }
    }

    /**
     * Returns opposite block in a double chest, if its an instanceof DoubleChestInventory
    */
    private static Block getOtherHalfOfChest(Block block) {
        // Checks if block isn't CHEST or TRAPPED_CHEST
        if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) {
            return null;
        }
        if (getDChestInventory(block) == null) {
            return null;
        }
        DoubleChestInventory doubleChestInventory = getDChestInventory(block);

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
     *  Returns DoubleChestInventory from a block, otherwise returns null
     */
    private static DoubleChestInventory getDChestInventory(Block block) {   
        BlockState blockState = block.getState();
        if (!(blockState instanceof Chest chest)) {
            return null;
        }
        
        Inventory inventory = chest.getInventory();
        if (!(inventory instanceof DoubleChestInventory doubleChestInventory)) {
            return null;
        }
        return doubleChestInventory;
    }

    /**
     * Checks CARDINAL_FACES around block and returns the first wall sign found
     */
    private static Sign scanForAttachedSign(Block block) {

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
            if (getLockedSignOwner(sign) != null) {
                return sign;
            }
        }
        return null;
    }

    /**
     *  Returns sign attached to the current block or the other half of the chest
     */
    private static Sign getAttachedSign(Block block) {
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
    private static UUID getLockedSignOwner(Sign sign) {
        if (sign == null) {
            return null;
        }
        String ownerUUID = sign.getPersistentDataContainer().get(SMPUtils.OWNER_UUID_KEY, PersistentDataType.STRING);
        if (ownerUUID == null) {
            return null;
        }
        return UUID.fromString(ownerUUID);
    }

    /**
     * Gets block data of PublicBukkitValues.smp:owneruuid stored in NBT
     */
    private static UUID getLockedContainerOwner(Container container) {
        String ownerUUID = container.getPersistentDataContainer().get(SMPUtils.OWNER_UUID_KEY, PersistentDataType.STRING);
        if (ownerUUID == null) {
            return null;
        }

        return UUID.fromString(ownerUUID);
    }

    /**
     * Returns if container or sign attached to container has a player UUID stored in it 
     * @deprecated Name scheme is poor.
     */
    private static boolean containerOrAttachedSignHasLock(Container container) {
        Sign attachedSign = getAttachedSign(container.getBlock());

        return getLockedContainerOwner(container) != null 
            || getLockedSignOwner(attachedSign) != null;
    }

    /**
     * Returns if container has UUID stored in it 
     */
    private static boolean containerHasLock(Container container) {
        return getLockedContainerOwner(container) != null;
    }

    /**
     * Returns if sign attached to container has a play  er UUID stored in it 
     */
    private static boolean containerHasLockSign(Container container) {
        Sign attachedSign = getAttachedSign(container.getBlock());
        return getLockedSignOwner(attachedSign) != null;
    }

    /**
     * Compares UUID stored in container and a player UUID
     */
    private static boolean isPlayerContainerOwner(Player player, Container container) {
        UUID ownerUUID = getLockedContainerOwner(container);
        return player.getUniqueId().equals(ownerUUID);
    }

    /**
     * Compares UUID stored in sign and a player UUID
     */
    private static boolean isPlayerSignOwner(Player player, Sign sign) {
        UUID ownerUUID = getLockedSignOwner(sign);
        return player.getUniqueId().equals(ownerUUID);
    }
    
    /**
     * Checking if player has admin permissions to bypass
     */
    private static boolean canPlayerBypass(Player player) {
        return player.hasPermission("smp.bypasslocks") || player.isOp();
    }

    /**
     * Returns if player is container owner, trusted by owner. <p> 
     * Optional: allow admins bypass in the check
     */
    private boolean hasAccessToContainerOrLock(Player player, Container container, @Nullable boolean canAdminBypass) {
        Bukkit.broadcastMessage("if its locked...");
        if (canAdminBypass && canPlayerBypass(player)) { 
            return true; 
        }
        
        if (containerOrAttachedSignHasLock(container) == false) {
            return true;
        }

        // If container isn't null, grab container owner. otherwise, grab sign owner
        UUID ownerUUID = getLockedContainerOwner(container) != null 
            ? getLockedContainerOwner(container) 
            : getLockedSignOwner(getAttachedSign(container.getBlock()));
        
        if (ownerUUID == null) {
            Bukkit.broadcastMessage("ownerUUID == NULL");
        } else {
            Bukkit.broadcastMessage("OwnerUUID: " + ownerUUID.toString());
        }

        Set<UUID> trustedPlayersUUID = TrustManager.getTrustedPlayers(ownerUUID);

        return player.getUniqueId().equals(ownerUUID) || (trustedPlayersUUID != null ? trustedPlayersUUID.contains(player.getUniqueId()) : false);
    }

    /**
     * Returns if player is container owner, trusted by owner, or admin
     */
    private boolean hasAccessToContainerOrLock(Player player, Container container) {
        return hasAccessToContainerOrLock(player, container, true);
    }

    /**
     * Checks if sign has [Lock] on any line
     */
    private boolean hasLockLine(SignChangeEvent event) {
        return Arrays.stream(event.getLines())
                .anyMatch("[Lock]"::equalsIgnoreCase);
    }
}