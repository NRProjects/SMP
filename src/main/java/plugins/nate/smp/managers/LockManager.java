package plugins.nate.smp.managers;

import static plugins.nate.smp.utils.ChatUtils.PREFIX;
import static plugins.nate.smp.utils.ChatUtils.sendMessage;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import plugins.nate.smp.utils.SMPUtils;

public class LockManager {
    public static final Map <BlockFace,Transformation> lockIconMap = new HashMap<BlockFace, Transformation>();
    {
        lockIconMap.put(BlockFace.NORTH, new Transformation(new Vector3f(-.31f, -.19f, .499f), new Quaternionf(0f, 0f, 0f, 1f), new Vector3f(0.5f, 0.5f, 0.0001f), new Quaternionf()));
        lockIconMap.put(BlockFace.EAST, new Transformation(new Vector3f(-.499f, -.19f, -.31f), new Quaternionf(0f, -.71f, 0f, .71f), new Vector3f(0.5f, 0.5f, 0.0001f), new Quaternionf()));
        lockIconMap.put(BlockFace.SOUTH, new Transformation(new Vector3f(.31f, -.19f, -.499f), new Quaternionf(0f, 1f, 0f, 0f), new Vector3f(0.5f, 0.5f, 0.0001f), new Quaternionf()));
        lockIconMap.put(BlockFace.WEST, new Transformation(new Vector3f(.499f, -.19f, .31f), new Quaternionf(0f, .71f, 0f, .71f), new Vector3f(0.5f, 0.5f, 0.0001f), new Quaternionf()));
        lockIconMap.put(BlockFace.UP, new Transformation(new Vector3f(-.31f, -.499f, -.19f), new Quaternionf(.71f, 0f, 0f, .71f), new Vector3f(0.5f, 0.5f, 0.0001f), new Quaternionf()));
        lockIconMap.put(BlockFace.DOWN, new Transformation(new Vector3f( .31f, .499f, -.44f), new Quaternionf(-.71f, 0f, 0f, .71f), new Vector3f(0.5f, 0.5f, 0.0001f), new Quaternionf()));
    }
    public static final BlockFace[] CARDINAL_FACES = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
    public static final BlockFace[] FACES_TO_CHECK = { BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
    public static final Material[] LOCK_ITEMS = { Material.IRON_NUGGET, Material.TRIPWIRE_HOOK };

    /**
     * Puts player UUID inside container NBT. <p>
     * If container instanceof DoubleChest, this puts the UUID in the other side of DChest aswell 
     * @param uuid              UUID of the player to put into the NBT of the container
     * @param container         The container to put the NBT into
     * @param particleLocation  Surface of the container that is being locked, used to display particles 
     * @param particleWorld     Used to display particles in the correct world
     * @param lockFace          Surface of the block that is being locked, to determine where the lock icon will be.
     */
    public static void lockContainer(UUID uuid, Container container, @Nullable Location particleLocation, @Nullable World particleWorld, @Nullable BlockFace lockFace) {
        // Locks current container
        container.getPersistentDataContainer().set(SMPUtils.OWNER_UUID_KEY, PersistentDataType.STRING, uuid.toString());
        container.update();
        if (particleLocation != null && particleWorld != null) {
            lockCreationParticles(particleWorld, particleLocation, Particle.WAX_ON);
        }

        if (lockFace != null) {
            createLockIcon(uuid, particleLocation, lockFace);
        }
        
        if (!(container instanceof Chest)) {
            return;
        }
        
        // Locks other half of chest
        getOtherHalfOfChest(container.getBlock());
        Block otherHalfOfChest = getOtherHalfOfChest(container.getBlock());
        if (otherHalfOfChest.getState() instanceof Container otherChestContainer) {
            otherChestContainer.getPersistentDataContainer().set(SMPUtils.OWNER_UUID_KEY, PersistentDataType.STRING, uuid.toString());
            otherChestContainer.update();
        }
    }

    /**
     * Puts player UUID inside container NBT. <p>
     * If container instanceof DoubleChest, this puts the UUID in the other side of DChest aswell 
     * @param uuid      UUID of the player to put into the NBT of the container
     * @param container The container to put the NBT into
     */
    public static void lockContainer(UUID uuid, Container container) {
        lockContainer(uuid, container, null, null, null);
    }

    /**
     * Puts player UUID inside container NBT, and returns the popped UUID
     * @param container The container to put the NBT into
     */
    public static UUID unlockContainer(Container container) {
        UUID playerUUID = UUID.fromString(container.getPersistentDataContainer().get(SMPUtils.OWNER_UUID_KEY, PersistentDataType.STRING));
        container.getPersistentDataContainer().remove(SMPUtils.OWNER_UUID_KEY);
        container.update();
        return playerUUID;
    }
    
    /**
     * Moves UUID stored in sign to container and other side of double chest, then removes the sign
     */
    public static void migrateSignLock(Container container, Sign sign) {
        Block signBlock = sign.getBlock();

        UUID signOwner = UUID.fromString(sign.getPersistentDataContainer().get(SMPUtils.OWNER_UUID_KEY, PersistentDataType.STRING));
        
        lockContainer(signOwner, container);

        signBlock.breakNaturally();
    }

    /**
     * Returns true if player is right clicking with IRON_NUGGET or TRIPWIRE_HOOK
     */
    public static boolean isPlacingLock(PlayerInteractEvent event) {
        if (!(event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            return false;
        }
        if (event.getItem() != null) {
            Material heldItem = event.getItem().getType();
            for (Material item : LOCK_ITEMS) {
                if (item.equals(heldItem)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
    *   Spawn particles with random offsets, .25 blocks in variation
    */
    public static void lockCreationParticles(World world, Location location, Particle particle) {
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        world.spawnParticle(particle, x +.5, y + .5, z + .5, 10, .25, .25, .25);
    }

    /**
    *  sendMessageAndCancel if they dont have permission to break container
    */
    public static void handleBreakContainer(BlockBreakEvent event, Player player, Container container) {
        // If not owner or admin, cancel break
        if (container != null && containerOrAttachedSignHasLock(container) && !(isPlayerContainerOwner(player, container) || canPlayerBypass(player))) {
            sendMessageAndCancel(event, player, "&cThis container is locked");
        }
    }
    /**
     * sendMessageAndCancel if they dont have permission to break container
     */
    public static void handleBreakSign(BlockBreakEvent event, Player player, Sign sign) {
        // If not owner or admin, cancel break
        if (sign != null && getLockedSignOwner(sign) != null && !(isPlayerSignOwner(player, sign) || canPlayerBypass(player))) {
            sendMessageAndCancel(event, player, "&cThis container is locked");
        }
        
    }

    /**
     * Cancels an event and sends a message to the player 
     */
    public static void sendMessageAndCancel(Event event, Player player, String message) {
        sendMessage(player, PREFIX + message);
        if (event instanceof Cancellable c) {
            c.setCancelled(true);
        }
    }

    /**
     * Returns opposite block in a double chest, if its an instanceof DoubleChestInventory
    */
    public static Block getOtherHalfOfChest(Block block) {
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
    public static DoubleChestInventory getDChestInventory(Block block) {   
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
     * Checks CARDINAL_FACES around block and returns the first locked wall sign  found
     */
    public static Sign scanForAttachedSign(Block block) {

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

    public static Block getAttachedTo(Sign sign) {
        
        if (!(sign.getBlockData() instanceof WallSign wallSign)) {
            return null;
        }
        for (BlockFace blockFace : CARDINAL_FACES) {
            Block relativeBlock = sign.getBlock().getRelative(blockFace);
            if (blockFace == wallSign.getFacing().getOppositeFace()) {
                Bukkit.broadcastMessage(blockFace.toString());
                
                return relativeBlock;
            }
        }

        return null;
    }

    /**
     *  Returns sign attached to the current block or the other half of the chest
     */
    public static Sign getAttachedSign(Block block) {
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
    public static UUID getLockedSignOwner(Sign sign) {
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
    public static UUID getLockedContainerOwner(Container container) {
        String ownerUUID = container.getPersistentDataContainer().get(SMPUtils.OWNER_UUID_KEY, PersistentDataType.STRING);
        if (ownerUUID == null) {
            return null;
        }

        return UUID.fromString(ownerUUID);
    }

    /**
     * Returns if container or sign attached to container has a player UUID stored in it 
     */
    public static boolean containerOrAttachedSignHasLock( Container container){
        Sign attachedSign = getAttachedSign(container.getBlock());

        return getLockedContainerOwner(container) != null 
            || getLockedSignOwner(attachedSign) != null;
    }

    /**
     * Returns if container has UUID stored in it 
     */
    public static boolean containerHasLock(Container container) {
        return getLockedContainerOwner(container) != null;
    }

    /**
     * Returns if sign attached to container has a play  er UUID stored in it 
     */
    public static boolean containerHasLockSign(Container container) {
        Sign attachedSign = getAttachedSign(container.getBlock());
        return getLockedSignOwner(attachedSign) != null;
    }

    /**
     * Compares UUID stored in container and a player UUID
     */
    public static boolean isPlayerContainerOwner(Player player, Container container) {
        UUID ownerUUID = getLockedContainerOwner(container);
        return player.getUniqueId().equals(ownerUUID);
    }

    /**
     * Compares UUID stored in sign and a player UUID
     */
    public static boolean isPlayerSignOwner(Player player, Sign sign) {
        UUID ownerUUID = getLockedSignOwner(sign);
        return player.getUniqueId().equals(ownerUUID);
    }
    
    /**
     * Checking if player has admin permissions to bypass
     */
    public static boolean canPlayerBypass(Player player) {
        return player.hasPermission("smp.bypasslocks") || player.isOp();
    }

    /**
     * Returns if player is container owner, trusted by owner. <p> 
     * Optional: allow admins bypass in the check
     */
    public static boolean hasAccessToContainerOrLock(Player player, Container container, @Nullable boolean canAdminBypass) {
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
        
        if (!(ownerUUID == null)) {
            Bukkit.broadcastMessage("OwnerUUID: " + ownerUUID.toString());
        }

        Set<UUID> trustedPlayersUUID = TrustManager.getTrustedPlayers(ownerUUID);

        return player.getUniqueId().equals(ownerUUID) || (trustedPlayersUUID != null ? trustedPlayersUUID.contains(player.getUniqueId()) : false);
    }

    /**
     * Returns if player is container owner, trusted by owner, or admin
     */
    public static boolean hasAccessToContainerOrLock(Player player, Container container) {
        return hasAccessToContainerOrLock(player, container, true);
    }

    /**
     * Checks if sign has [Lock] on any line
     */
    public static boolean hasLockLine(SignChangeEvent event) {
        return Arrays.stream(event.getLines())
                .anyMatch("[Lock]"::equalsIgnoreCase);
    }

    /**
     * Spawns an item_display of a player's head at a location, facing a certain direction
     * @param playerIconUUID    UUID of the player who's skin that is going to represented by the icon
     * @param iconLocation      Location of the block where the entity is spawned
     * @param facing            Direction the icon is facing
     * @return                  UUID of the entity spawned
     */
    public static UUID createLockIcon(UUID playerIconUUID, Location iconLocation, BlockFace facing) {
        Bukkit.broadcastMessage("Creating lock icon");
        World world = iconLocation.getWorld();
        // Grabbing center of block location
        iconLocation = new Location(world, Math.ceil(iconLocation.getX()) -.5, Math.ceil(iconLocation.getY()) + .5, Math.ceil(iconLocation.getZ()) - .5);
        
        // Create and set player_head to the player's skin.
        ItemStack skullStack = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta skullMeta = (SkullMeta) skullStack.getItemMeta();
        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(playerIconUUID));
        skullStack.setItemMeta(skullMeta);

        Bukkit.broadcastMessage("Facing: " + facing.toString());
        Bukkit.broadcastMessage("Location: " + iconLocation.toString());
        
        // Bukkit.broadcastMessage("Mapping: " + lockIconMap.get(facing).toString());

        for (Map.Entry<BlockFace, Transformation> entry : lockIconMap.entrySet()) {
            Bukkit.broadcastMessage("1");
        }
        // Spawning item display
        UUID iconUUID =  iconLocation.getWorld().spawn(iconLocation, ItemDisplay.class, (itemDisplay) -> {
            Bukkit.broadcastMessage("Test1");
            itemDisplay.setItemStack(skullStack);
            Bukkit.broadcastMessage("Test2");
            itemDisplay.setTransformation(lockIconMap.get(facing));
            Bukkit.broadcastMessage("Test3");
        }).getUniqueId();
        Bukkit.broadcastMessage("iconUUID: " + iconUUID.toString());
        return iconUUID;
    }
}
