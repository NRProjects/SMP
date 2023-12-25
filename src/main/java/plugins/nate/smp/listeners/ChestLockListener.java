package plugins.nate.smp.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import plugins.nate.smp.managers.LockManager;
import plugins.nate.smp.utils.ChatUtils;

public class ChestLockListener implements Listener {

    public static final BlockFace[] FACES_TO_CHECK = { BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };

    // TODO: autolock feature
    // TODO: admin command to force lock to be of a certain player
    // TODO: Making a lock adds an entry to coreprotect (coreprotect integration)


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSignChange(SignChangeEvent event) {

        // TODO: Allow signs to lock chests
        if (LockManager.hasLockLine(event) == false) {
            return;
        }
        
        Block block = event.getBlock();
        Sign sign = (Sign) block.getState();
        
        Block attachedTo = LockManager.getAttachedTo(sign);
        if (!(attachedTo.getState() instanceof Container container)) {
            Bukkit.broadcastMessage(attachedTo.toString());
            return;
        } 

        Location signLocation = sign.getLocation();
        Player player = event.getPlayer();
        World world = player.getWorld();

        LockManager.lockContainer(player.getUniqueId(), container, signLocation, world, null);
        block.breakNaturally();

        ChatUtils.sendMessage(player, ChatUtils.PREFIX + "&aContainer locked!");

        ChatUtils.sendMessage(player, ChatUtils.PREFIX + "&7You no longer need to lock containers with a sign.");
        ChatUtils.sendMessage(player, ChatUtils.PREFIX + "&7You can instead use /lock while looking at a container.");

        // if (isLockedContainer(sign) && !playerHasAccess(player, sign)) {
        //     sendMessageAndCancel(event, player, "&cYou cannot edit this locked sign!");
        //     return;
        // }
        // if (hasLockLine(event) && isLockableSign(block) ) {
        //     // If attached block isn't a storage container
        //     if (!(isStorageContainer(getAttachedBlock(block).getType()))) {
        //         sendMessage(player, PREFIX + "&cMust be placed directly on a storage container!");
        //         return;
        //     }

        //     event.setLine(0, LOCKED_TAG);
        //     event.setLine(1, player.getName());
        //     event.setLine(2, "");
        //     event.setLine(3, "");

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
        if (LockManager.getDChestInventory(block) == null) {
            return;
        }
        Block otherHalfOfChest = LockManager.getOtherHalfOfChest(block);
        Player player = event.getPlayer();
        Container otherChestContainer = (Container) otherHalfOfChest.getState();
        // If the owner of the other side of the double chest and the placer aren't the same, cancel
        if (!(LockManager.hasAccessToContainerOrLock(player, otherChestContainer))) {
            LockManager.sendMessageAndCancel(event, player, "&cYou cannot place a chest next to an owned chest!");
        }

        // TODO: import lock data from other chest
    }

    @EventHandler
    public void onContainerAccess(PlayerInteractEvent event) {
        // If not right clicking container
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || !(event.getClickedBlock().getState() instanceof Container container)) {
            return;
        }

        if ((!LockManager.containerOrAttachedSignHasLock(container)) && LockManager.isPlacingLock(event)) {
            World world = event.getPlayer().getWorld();
            // Getting location of the surface of the block clicked
            Location lockSurface = event.getClickedBlock().getLocation().add(event.getBlockFace().getDirection());
            
            LockManager.lockContainer(event.getPlayer().getUniqueId(), container,  lockSurface, world, event.getBlockFace());
            ChatUtils.sendMessage(event.getPlayer(), ChatUtils.PREFIX + "&aContainer locked!");
            return;
        }
        
        Player player = event.getPlayer();
        if (!(LockManager.hasAccessToContainerOrLock(player, container))) {
            // TODO: Anvil sound effect iron bars (after making container lock)
            LockManager.sendMessageAndCancel(event, player, "&cThis container is locked!");
        }
        
        if (LockManager.hasAccessToContainerOrLock(player, container, false) && LockManager.containerHasLockSign(container)) {
            Sign sign = LockManager.getAttachedSign(container.getBlock());
            
            LockManager.migrateSignLock(container, sign);
        }
        // TODO: Place lock icon in the bottom right of opened surface        
    }

    @EventHandler
    public void onLockedContainerBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (block.getState() instanceof Container container) {
            LockManager.handleBreakContainer(event, player, container);
        }
        if (block.getState() instanceof Sign sign) {
            LockManager.handleBreakSign(event, player, sign);
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
                if (!(LockManager.isPlayerContainerOwner(player, container) 
                        || !LockManager.isPlayerSignOwner(player, LockManager.getAttachedSign(container.getBlock())) 
                        || LockManager.canPlayerBypass(player))) {
                    LockManager.sendMessageAndCancel(event, player, "&cYou cannot place a hopper next to a locked container");
                    return;
                }
            }
        }
    }
}