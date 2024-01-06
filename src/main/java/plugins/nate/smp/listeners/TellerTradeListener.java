package plugins.nate.smp.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.joml.Math;
import plugins.nate.smp.SMP;
import plugins.nate.smp.guis.TellerDepositGUI;
import plugins.nate.smp.guis.TellerWithdrawGUI;
import plugins.nate.smp.utils.SMPUtils;
import plugins.nate.smp.utils.TellerUtils;
import plugins.nate.smp.utils.VaultUtils;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import static plugins.nate.smp.utils.ChatUtils.*;
import static plugins.nate.smp.utils.TellerUtils.CURRENCY_SYMBOL;


public class TellerTradeListener implements Listener {
    private enum SOUND_EFFECTS { SUCCESS, ERROR, NEUTRAL }
    private final HashMap<UUID, Integer> initialDiamonds = new HashMap<>();
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
            sendMessage(player, PREFIX + "&cAn error has occurred. Please contact staff or make a ticket in the discord: discord.gg/coolment");
            return;
        }

        switch (tellerKey.toLowerCase()) {
            case "withdraw" -> handleWithdrawGUI(player);
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
        }
        
        Optional<Entity> textDisplay = teller.getWorld().getNearbyEntities(teller.getLocation().add(0, 2.4, 0), .1, .1, .1)
            .stream()
            .filter(entity -> entity instanceof TextDisplay)
            .filter(entity -> entity.getPersistentDataContainer().has(TellerUtils.PARENT_TELLER_KEY, PersistentDataType.STRING))
            .filter(entity -> entity.getPersistentDataContainer().get(TellerUtils.PARENT_TELLER_KEY, PersistentDataType.STRING).equals(teller.getUniqueId().toString()))
            .findFirst();

        textDisplay.ifPresent(Entity::remove);
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
            displayBalance(player);
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
            displayBalance(player);
            inventory.clear();
        } else {
            sendMessage(player, PREFIX + "&cTransaction failed!");
            returnItemsToPlayer(inventory, player);
        }
    }

    @EventHandler
    public void onTellerGUIClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Inventory clickedInventory = event.getView().getTopInventory();

        if (clickedInventory.getHolder() instanceof TellerDepositGUI ||
            clickedInventory.getHolder() instanceof TellerWithdrawGUI) {
            Player player = (Player) event.getWhoClicked();

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.getType() != Material.DIAMOND && clickedItem.getType() != Material.DIAMOND_BLOCK) {
                event.setCancelled(true);
                sendMessage(player, PREFIX + "&cYou can only deposit diamonds or diamond blocks.");
                playSound(player, SOUND_EFFECTS.ERROR);
            }
        }
    }

    // Used to prevent a bug where players can drop items they dont have the funds for anymore
    @EventHandler 
    public void onTellerGUIToss(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!(hasTellerInventoryOpen(player))) {
            return; 
        }
        event.setCancelled(true);
        sendMessage(player, PREFIX + "&cYou can't drop items while interracting with a teller.");
        playSound(player, SOUND_EFFECTS.ERROR);
    }

    @EventHandler
    public void onWithdrawGUIClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof TellerWithdrawGUI)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        int currentDiamonds = countDiamonds(event.getView().getTopInventory().getContents());
        int diff = initialDiamonds.getOrDefault(event.getPlayer().getUniqueId(), 0) - currentDiamonds;

        if (diff == 0) {
            return;
        }

        if (diff > 0) {
            VaultUtils.withdraw(player, diff);
            sendMessage(player, PREFIX + "&aSuccessfully withdrew " + diff + CURRENCY_SYMBOL);
        } else {
            int deposited = Math.abs(diff);
            VaultUtils.deposit(player, deposited);
            sendMessage(player, PREFIX + "&aSuccessfully deposited " + deposited + CURRENCY_SYMBOL);
            displayBalance(player);
        }
    }

    /*
    * Methods for handling withdraws
    * */
    private void handleWithdrawGUI(Player player) {
        double playerBalance = VaultUtils.getBalance(player);

        Inventory tellerUI = new TellerWithdrawGUI().getInventory();

        int inventorySlots = tellerUI.getSize();
        ItemStack[] tellerItems = new ItemStack[inventorySlots];
        double balanceToAdd = playerBalance;
        for (int i = 0; i < tellerItems.length; i++) {
            ItemStack diamondStack = new ItemStack(Material.AIR);

            if (balanceToAdd >= 9) {
                diamondStack.setType(Material.DIAMOND_BLOCK);

                // 576 is the amount in a stack of blocks
                if (balanceToAdd > 576) {
                    diamondStack.setAmount(64);
                    balanceToAdd -= 576;
                } else {
                    double blocksToAdd = Math.floor(balanceToAdd / 9);
                    diamondStack.setAmount((int) blocksToAdd);
                    balanceToAdd -= blocksToAdd * 9;
                }
            } else if (balanceToAdd >= 1) {
                diamondStack.setType(Material.DIAMOND);
                diamondStack.setAmount((int) balanceToAdd);
                balanceToAdd = 0;
            }

            tellerItems[i] = diamondStack;
        }

        // Used for determining how many diamonds were pulled from the bank
        initialDiamonds.put(player.getUniqueId(), countDiamonds(tellerItems));

        tellerUI.addItem(tellerItems);

        player.openInventory(tellerUI);
    }

    /*
    * Helper methods
    * */

    private boolean hasTellerInventoryOpen(Player player) {
        if (player.getOpenInventory() == null) {
            return false;
        }
        Inventory topInventory = player.getOpenInventory().getTopInventory();
        if (topInventory.getHolder() instanceof TellerDepositGUI ||
            topInventory.getHolder() instanceof TellerWithdrawGUI) {
                return true;
        }
        return false;
    }


    /**
     * Creates an action bar to display a player their balance
     */
    private void displayBalance(Player player) {
        createActionBar(player, "&7Balance: &a" + String.format("%.2f", VaultUtils.econ.getBalance(player)) + CURRENCY_SYMBOL);
    }

    private int countDiamonds(ItemStack[] items) {
        int diamondCount = 0;
        for (ItemStack item: items) {
            if (item == null) {
                continue;
            }
            if (item.getType() == Material.DIAMOND_BLOCK) {
                diamondCount += item.getAmount() * 9;
            } else if (item.getType() == Material.DIAMOND) {
                diamondCount += item.getAmount();
            }
        }
        return diamondCount;
    }

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
