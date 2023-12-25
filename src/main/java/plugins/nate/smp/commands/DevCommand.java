package plugins.nate.smp.commands;

import static plugins.nate.smp.utils.ChatUtils.sendMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import plugins.nate.smp.SMP;
import plugins.nate.smp.enchantments.CustomEnchant;
import plugins.nate.smp.managers.EnchantmentManager;
import plugins.nate.smp.utils.ChatUtils;

public class DevCommand implements CommandExecutor, TabCompleter {
    private static final Set<String> VALID_SUBCOMMANDS = Set.of(
        "setdurability", 
        "forcerestart", 
        "nextrestart", 
        "customenchant", 
        "findenchant", 
        "lockicon",
        "givehead",
        "spawnlockicon");

    private static final List<UUID> AUTHORIZED_UUIDS = Arrays.asList(
            // NitrogenAtom
            UUID.fromString("38ee2126-4d91-4dbe-86fe-2e8c94320056"),

            // Doogar
            UUID.fromString("b42a0052-0760-49b8-bf22-af5016994822"),

            // SwiftVines
            UUID.fromString("7b280144-0fc2-4517-a7dd-a28b8be293c2")
    );

    public static final Map <BlockFace,Transformation> lockIconMap = new HashMap<BlockFace, Transformation>();
    {
        lockIconMap.put(BlockFace.NORTH, new Transformation(new Vector3f(-.31f, -.19f, .499f), new Quaternionf(0f, 0f, 0f, 1f), new Vector3f(0.5f, 0.5f, 0.0001f), new Quaternionf()));
        lockIconMap.put(BlockFace.EAST, new Transformation(new Vector3f(-.499f, -.19f, -.31f), new Quaternionf(0f, -.71f, 0f, .71f), new Vector3f(0.5f, 0.5f, 0.0001f), new Quaternionf()));
        lockIconMap.put(BlockFace.SOUTH, new Transformation(new Vector3f(.31f, -.19f, -.499f), new Quaternionf(0f, 1f, 0f, 0f), new Vector3f(0.5f, 0.5f, 0.0001f), new Quaternionf()));
        lockIconMap.put(BlockFace.WEST, new Transformation(new Vector3f(.499f, -.19f, .31f), new Quaternionf(0f, .71f, 0f, .71f), new Vector3f(0.5f, 0.5f, 0.0001f), new Quaternionf()));
        lockIconMap.put(BlockFace.UP, new Transformation(new Vector3f(-.31f, -.499f, -.19f), new Quaternionf(.71f, 0f, 0f, .71f), new Vector3f(0.5f, 0.5f, 0.0001f), new Quaternionf()));
        lockIconMap.put(BlockFace.DOWN, new Transformation(new Vector3f( .31f, .499f, -.44f), new Quaternionf(-.71f, 0f, 0f, .71f), new Vector3f(0.5f, 0.5f, 0.0001f), new Quaternionf()));
    }
    // PacketPlayOutSpawnEntity
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be executed by a player.");
            return true;
        }
        if (sender.isOp() == false) {
            sender.sendMessage("You must be an operator to send this command.");
        }
        if (!AUTHORIZED_UUIDS.contains(player.getUniqueId())) {
            sendMessage(player, ChatUtils.DEV_PREFIX + ChatUtils.DENIED_COMMAND);
            return true;
        }

        if (args.length == 0) {
            sendMessage(player, ChatUtils.DEV_PREFIX + "This command is used to for development testing");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "setdurability": {
                if (args.length == 1) {
                    sendMessage(player, ChatUtils.DEV_PREFIX + "&cUsage: /dev setdurability <amount>");
                    return true;
                }

                int durability;
                try {
                    durability = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sendMessage(player, ChatUtils.DEV_PREFIX + "&cDurability must be a number");
                    return true;
                }

                ItemStack item = player.getInventory().getItemInMainHand();
                ItemMeta meta = item.getItemMeta();
                if (!(meta instanceof Damageable damageable)) {
                    sendMessage(player, ChatUtils.DEV_PREFIX + "You must be holding an item with durability");
                    return true;
                }

                damageable.setDamage(item.getType().getMaxDurability() - durability);
                item.setItemMeta(damageable);
                sendMessage(player, ChatUtils.DEV_PREFIX + "Durability set to " + durability + ".");
                return true;
            }
            case "customenchant": {
                if (args.length == 1) {
                    sendMessage(player, ChatUtils.DEV_PREFIX + "&cUsage: /dev customenchant <enchantname>");
                    return true;
                }

                String enchantName = args[1].toLowerCase();
                Enchantment enchantment = EnchantmentManager.getEnchantment(enchantName);
                if (!(enchantment instanceof CustomEnchant)) {
                    sendMessage(player, ChatUtils.DEV_PREFIX + "&cUnknown enchantment.");
                    return true;
                }

                ItemStack heldItem = player.getInventory().getItemInMainHand();
                if (heldItem.getType() == Material.AIR) {
                    sendMessage(player, ChatUtils.DEV_PREFIX + "You need to be holding an item to enchant.");
                    return true;
                }

                if (!enchantment.canEnchantItem(heldItem)) {
                    sendMessage(player, ChatUtils.DEV_PREFIX + "&cThis item cannot be enchanted with the given enchantment.");
                    return true;
                }

                ItemMeta meta = heldItem.getItemMeta();

                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add(((CustomEnchant) enchantment).getLore());
                meta.setLore(lore);

                heldItem.setItemMeta(meta);
                heldItem.addUnsafeEnchantment(enchantment, 1);

                sendMessage(player, ChatUtils.DEV_PREFIX + "Successfully added " + enchantName + " enchantment to your held item!");
                return true;
            }
            case "findenchant": {
                if (args.length == 1) {
                    sendMessage(player, ChatUtils.DEV_PREFIX + "&cUsage: /dev findenchant <enchantkey>");
                    return true;
                }

                String keyString = args[1].toLowerCase();
                NamespacedKey key = NamespacedKey.fromString(keyString, SMP.getPlugin());

                if (key == null) {
                    sendMessage(player, ChatUtils.DEV_PREFIX + "Key was null");
                    return true;
                }

                Enchantment enchantment = Enchantment.getByKey(key);
                if (enchantment == null) {
                    sendMessage(player, ChatUtils.DEV_PREFIX + "No enchantment found for key " + keyString);
                } else {
                    sendMessage(player, ChatUtils.DEV_PREFIX + "Enchantment: " + enchantment.getName());
                }
                return true;
            }
            case "forcerestart":{
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "restart");
                break;
            }
            case "lockicon": {

                if (!(player.getInventory().getItemInMainHand().getType().equals(Material.PLAYER_HEAD))) {
                    sendMessage(sender, ChatUtils.DEV_PREFIX + "Not a skull.");
                    return true;
                } else {
                    sendMessage(sender, ChatUtils.DEV_PREFIX + "Is a skull");
                    
                    SkullMeta skullMeta = (SkullMeta) player.getInventory().getItemInMainHand().getItemMeta();
                    sendMessage(sender, ChatUtils.DEV_PREFIX + "GetOwningPlayer Name: " + skullMeta.getOwningPlayer().getName());
                    
                    ItemStack skullStack = new ItemStack(Material.PLAYER_HEAD);
                    skullStack.setItemMeta(skullMeta);

                    // Vector3f translation = new Vector3f();
                    // Vector3f scale = new Vector3f(0.5f, 0.5f, 0.5f);
                    // Quaternionf leftRotation = new Quaternionf();
                    // Quaternionf rightRotation = new Quaternionf();
                    
                    Transformation transformation = lockIconMap.get(BlockFace.NORTH);
                    
                    try {
                        // leftRotation = new Quaternionf(Float.parseFloat(args[1]), Float.parseFloat(args[2]), Float.parseFloat(args[3]), Float.parseFloat(args[4]));
                        // rightRotation = new Quaternionf(Float.parseFloat(args[1]), Float.parseFloat(args[2]), Float.parseFloat(args[3]), Float.parseFloat(args[4]));
                        Bukkit.broadcastMessage(args[1].toUpperCase());
                        transformation = lockIconMap.get(BlockFace.valueOf(args[1].toUpperCase()));
                    } catch (Exception e) {
                        Bukkit.broadcastMessage("shit aint work");
                    }
                    

                    Location pLocation = player.getLocation();
                    // Grabs only xyz, and truncates down to place at origin of block
                    pLocation = new Location(player.getWorld(), Math.ceil(pLocation.getX()) -.5, Math.ceil(pLocation.getY()) + .5, Math.ceil(pLocation.getZ()) - .5);
                    Bukkit.broadcastMessage("Placing at: " + pLocation.toString());

                    final Transformation transformFinal = transformation;

                    player.getWorld().spawn(pLocation, ItemDisplay.class, (itemDisplay) -> {
                        itemDisplay.setItemStack(skullStack);
                        itemDisplay.setTransformation(transformFinal);
                    });
                }
                return true;
        
            }
            case "givehead": {
                OfflinePlayer offlinePlayer = (OfflinePlayer) player;
                if (args.length >= 2) {
                    offlinePlayer = Bukkit.getOfflinePlayer(args[1]);
                    if (offlinePlayer == null) {
                        ChatUtils.sendMessage(sender, ChatUtils.DEV_PREFIX + "&cNot a valid player.");
                        return true;
                    };
                }

                ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1);
                SkullMeta skullMeta = (SkullMeta) item.getItemMeta();

                skullMeta.setOwningPlayer(offlinePlayer);
                item.setItemMeta(skullMeta);

                player.getInventory().addItem(item);
                return true;
            }
            case "spawnlockicon": {
                Location iconLocation = player.getLocation();
                
                // Grabbing center of block location
                iconLocation = new Location(player.getWorld(), Math.ceil(iconLocation.getX()) -.5, Math.ceil(iconLocation.getY()) + .5, Math.ceil(iconLocation.getZ()) - .5);
        
                // Create and set player_head to the player's skin.
                ItemStack skullStack = new ItemStack(Material.PLAYER_HEAD, 1);
                SkullMeta skullMeta = (SkullMeta) skullStack.getItemMeta();
                skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(player.getUniqueId()));
                skullStack.setItemMeta(skullMeta);

                // Spawning item display
                String displayUUID = iconLocation.getWorld().spawn(iconLocation, ItemDisplay.class, (itemDisplay) -> {
                    itemDisplay.setItemStack(skullStack);
                    itemDisplay.setTransformation(lockIconMap.get(BlockFace.valueOf(args[1])));
                }).getUniqueId().toString();
                Bukkit.broadcastMessage("UUID of item display: " + displayUUID);
                return true;
            }
            default: {
                sendMessage(player, ChatUtils.DEV_PREFIX + "&cUnknown sub-command.");
                break;
            }
        }        
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!(sender instanceof Player player) || !AUTHORIZED_UUIDS.contains(player.getUniqueId())) {
            return null;
        }

        if (args.length == 1) {
            return VALID_SUBCOMMANDS.stream()
                    .filter(subcommand -> subcommand.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        return null;
    }
}
