package plugins.nate.smp.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.joml.Math;
import plugins.nate.smp.SMP;
import plugins.nate.smp.guis.TellerDepositGUI;
import plugins.nate.smp.utils.SMPUtils;
import plugins.nate.smp.utils.TellerUtils;
import plugins.nate.smp.utils.VaultUtils;

import java.util.HashMap;
import java.util.Optional;

import static plugins.nate.smp.utils.ChatUtils.*;
import static plugins.nate.smp.utils.TellerUtils.CURRENCY_SYMBOL;


public class TellerTradeListener implements Listener {
    private enum SOUND_EFFECTS { SUCCESS, ERROR, NEUTRAL }
    
    @EventHandler
    public void onTellerInteract(PlayerInteractEntityEvent event) {
        Entity clickedEntity = event.getRightClicked();
        if (clickedEntity.getType() != EntityType.VILLAGER || !TellerUtils.isTeller(clickedEntity)) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        String tellerKey = TellerUtils.getTellerKey(clickedEntity);

        if (tellerKey == null || tellerKey.equals("")) {
            SMPUtils.severe(player.getName() + " tried to interact with a null teller at location X: " +
                    clickedEntity.getLocation().getX() + " Y: " + clickedEntity.getLocation().getY() + " Z: " + clickedEntity.getLocation().getZ());
            sendMessage(player, PREFIX + "&cAn error has occured. Please contact staff or make a ticket in the discord: discord.gg/coolment");
            return;
        }

        switch (tellerKey.toLowerCase()) {
            case "withdraw" -> handleWithdraw(player);
            case "deposit" -> handleDeposit(player);
            default -> sendMessage(player, PREFIX + "&cThis teller does not have a valid TELLER_KEY. Contact an admin.");
        }
    }

    // Remove the text display linked with the teller
    @EventHandler
    public void onTellerDeath(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.VILLAGER) {
            return;
        }

        LivingEntity teller = event.getEntity();
        if (!teller.getPersistentDataContainer().has(TellerUtils.TELLER_TYPE_KEY, PersistentDataType.STRING)) {
            return;
        };
        
        Optional<Entity> textDisplay = teller.getWorld().getNearbyEntities(teller.getLocation().add(0, 2.4, 0), .1, .1, .1)
            .stream()
            .filter(entity -> entity instanceof TextDisplay)
            .filter(entity -> entity.getPersistentDataContainer().has(TellerUtils.PARENT_TELLER_KEY, PersistentDataType.STRING))
            .filter(entity -> entity.getPersistentDataContainer().get(TellerUtils.PARENT_TELLER_KEY, PersistentDataType.STRING).equals(teller.getUniqueId().toString()))
            .findFirst();

        if (textDisplay.isPresent()) {
            textDisplay.get().remove();
        }
    }

    /*
    * Handlers player action bar when moving within a region with the bank allow flag
    * */

    @EventHandler
    public void onPlayerMoveIntoBankRegion(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (event.getFrom().getBlock().equals(event.getTo().getBlock())) {
            return;
        }

        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(player.getWorld()));

        if (regionManager == null) {
            return;
        }

        Location loc = player.getLocation();
        BlockVector3 position = BlockVector3.at(loc.getX(), loc.getY(), loc.getZ());

        ApplicableRegionSet regionSet = regionManager.getApplicableRegions(position);
        boolean isBankRegion = regionSet.queryState(null, SMP.BANK_FLAG) == StateFlag.State.ALLOW;

        if (isBankRegion) {
            double balance = VaultUtils.econ.getBalance(player);
            String formattedBalance = String.format("%.2f", balance);

            createActionBar(player, "&7Balance: &a" + formattedBalance + CURRENCY_SYMBOL);
        } else {
            player.sendTitle("", "", 0, 0, 0);
        }
    }

    /*
     * Methods for handling deposits
     * */

    private void handleDeposit(Player player) {
        player.openInventory(new TellerDepositGUI().getInventory());
    }

    @EventHandler
    private void onDepositGUIClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof TellerDepositGUI)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();

        double totalValue = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item == null) {
                continue;
            }

            if (item.getType() == Material.DIAMOND) {
                totalValue += item.getAmount();
            } else if (item.getType() == Material.DIAMOND_BLOCK) {
                totalValue += item.getAmount() * 9;
            }
        }

        // Nothing was deposited
        if (inventory.isEmpty()) {
            return;
        }

        boolean success = VaultUtils.deposit(player, totalValue);
        if (success) {
            sendMessage(player, PREFIX + "&aSuccessfully deposited " + (int) totalValue + CURRENCY_SYMBOL);
            inventory.clear();
        } else {
            sendMessage(player, PREFIX + "&cTransaction failed!");
            returnItemsToPlayer(inventory, player);
        }
    }

    @EventHandler
    public void onDepositGUIClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Inventory clickedInventory = event.getView().getTopInventory();

        if (clickedInventory.getHolder() instanceof TellerDepositGUI) {
            Player player = (Player) event.getWhoClicked();

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.getType() != Material.DIAMOND && clickedItem.getType() != Material.DIAMOND_BLOCK) {
                event.setCancelled(true);
                sendMessage(player, PREFIX + "&cYou can only deposit diamonds or diamond blocks.");
                playSound(player, SOUND_EFFECTS.ERROR);
            }
        }
    }

    /*
    * Methods for handling withdraws
    * */

    private void handleWithdraw(Player player) {
        double playerBalance = VaultUtils.getBalance(player);

        if (playerBalance < 1.0) {
            sendMessage(player, PREFIX + "&cYou have no funds to withdraw!");
            playSound(player, SOUND_EFFECTS.ERROR);
            return;
        }

        double amountToWithdraw;
        ItemStack withdrawalItem;

        if (player.isSneaking()) {
            if (playerBalance >= 9.0) {
                int maxBlocks = (int) Math.min(playerBalance / 9.0, 64);
                withdrawalItem = new ItemStack(Material.DIAMOND_BLOCK, maxBlocks);
                amountToWithdraw = maxBlocks * 9.0;
            } else {
                int maxDiamonds = (int) Math.min(playerBalance, 64);
                withdrawalItem = new ItemStack(Material.DIAMOND, maxDiamonds);
                amountToWithdraw = maxDiamonds;
            }
        } else {
            if (playerBalance < 9.0) {
                withdrawalItem = new ItemStack(Material.DIAMOND);
                amountToWithdraw = 1.0;
            } else {
                withdrawalItem = new ItemStack(Material.DIAMOND_BLOCK);
                amountToWithdraw = 9.0;
            }
        }

        if (!player.getInventory().addItem(withdrawalItem).isEmpty()) {
            sendMessage(player, PREFIX + "&cYou don't have enough inventory space.");
            playSound(player, SOUND_EFFECTS.ERROR);
            return;
        }

        boolean success = VaultUtils.withdraw(player, amountToWithdraw);
        if (success) {
            sendMessage(player, PREFIX + "&aSuccessfully withdrew " + (int) amountToWithdraw + CURRENCY_SYMBOL);
        } else {
            sendMessage(player, PREFIX + "&cAn error has occurred");
        }
    }

    /*
    * Helper methods
    * */

    private void playSound(Player player, SOUND_EFFECTS sound) {
        if (sound == SOUND_EFFECTS.SUCCESS) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.5f, 0f);
        } else if (sound == SOUND_EFFECTS.NEUTRAL) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.5f, 0f);
        } else if (sound == SOUND_EFFECTS.ERROR) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 0.5f, 0f);
        }
    }

    private static void returnItemsToPlayer(Inventory inv, Player player) {
        HashMap<Integer, ItemStack> notReturned = player.getInventory().addItem(inv.getContents());
        inv.clear();

        if (!notReturned.isEmpty()) {
            notReturned.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            sendMessage(player, PREFIX + "&cNot all items could be returned to your inventory. Dropping them nearby.");
        }
    }
}
