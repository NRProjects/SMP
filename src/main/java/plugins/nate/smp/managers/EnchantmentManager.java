package plugins.nate.smp.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import plugins.nate.smp.enchantments.CustomEnchant;
import plugins.nate.smp.enchantments.TimberEnchant;
import plugins.nate.smp.enchantments.VeinMinerEnchant;

import java.lang.reflect.Field;
import java.util.*;

public class EnchantmentManager implements Listener {
    // Maps for storing custom enchantments and their corresponding lore.
    public static final Map<String, Enchantment> ENCHANTMENTS = new HashMap<>();
    public static final Map<Enchantment, String> LORE_MAP = new HashMap<>();

    // Static initializer to populate the enchantments and lore map
    static {
        // Initialize custom enchantments
        ENCHANTMENTS.put("vein_miner", new VeinMinerEnchant());
        ENCHANTMENTS.put("timber", new TimberEnchant());

        // Initialize lore for custom enchantments
        LORE_MAP.put(new VeinMinerEnchant(), ChatColor.GOLD + "Vein Miner");
        LORE_MAP.put(new TimberEnchant(), ChatColor.GOLD + "Timber");
    }

    /**
     * Registers the custom enchantments
     *
     * @param key The key of the custom enchantment to register.
     */
    public static void registerEnchantment(String key) {
        try {
            Field f = Enchantment.class.getDeclaredField("acceptingNew");
            f.setAccessible(true);
            f.set(null, true);
            Enchantment.registerEnchantment(ENCHANTMENTS.get(key));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves a custom enchantment by its key.
     *
     * @param key The key of the enchantment.
     * @return The enchantment associated with the given key.
     */
    public static Enchantment getEnchantment(String key) {
        return ENCHANTMENTS.get(key);
    }

    /**
     * Registers all custom enchantments.
     */
    public static void registerEnchants() {
        ENCHANTMENTS.keySet().forEach(EnchantmentManager::registerEnchantment);
    }

    /**
     * Event handler for the enchant item event.
     * Applies custom enchantments based on certain conditions.
     *
     * @param event The enchant item event.
     */
    @EventHandler
    public static void onEnchantItem(EnchantItemEvent event) {
        ItemStack item = event.getItem();

        ENCHANTMENTS.values().stream()
                .filter(enchantment -> enchantment instanceof CustomEnchant)
                .filter(enchantment -> event.getExpLevelCost() >= 25 && enchantment.canEnchantItem(item))
                .filter(enchantment -> Math.random() < ((CustomEnchant) enchantment).getChance())
                .forEach(enchantment -> {
                    ItemMeta meta = item.getItemMeta();
                    List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                    lore.add(((CustomEnchant) enchantment).getLore());
                    meta.setLore(lore);

                    if (item.getType() == Material.BOOK) {
                        event.setCancelled(true);

                        EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) Bukkit.getItemFactory().getItemMeta(Material.ENCHANTED_BOOK);
                        bookMeta.addStoredEnchant(enchantment, 1, false);

                        List<String> bookLore = new ArrayList<>();
                        bookLore.add(ChatColor.BLUE + "Ingredients");
                        bookLore.add(((CustomEnchant) enchantment).getLore());
                        bookMeta.setLore(bookLore);

                        ItemStack enchantedBook = new ItemStack(Material.ENCHANTED_BOOK);
                        enchantedBook.setItemMeta(bookMeta);

                        event.getInventory().setItem(0, enchantedBook);
                    } else {
                        meta.addEnchant(enchantment, 1, false);
                        item.setItemMeta(meta);
                    }

                });
    }

    /**
     * Handles the anvil events when working with custom enchantments. This method is called whenever items are placed in both slots of an anvil.
     * It manages the merging of enchantments from two items, ensuring custom enchantments are applied correctly and generating
     * the appropriate lore for the result item. This method also handles special cases, such as when an enchanted book is involved.
     *
     * @param event The PrepareAnvilEvent provided by the Minecraft server API.
     */
    @EventHandler
    public static void onAnvilPrepare(PrepareAnvilEvent event) {
        // Retrieve the items in the first and second slots of the anvil, and what the item will be renamed to.
        ItemStack firstItem = event.getInventory().getItem(0);
        ItemStack secondItem = event.getInventory().getItem(1);
        String renameText = event.getInventory().getRenameText();

        // Return early if the first slot is empty
        if (firstItem == null) {
            return;
        }

        if (secondItem == null) {
            Map<Enchantment, Integer> customEnchants = getCustomEnchants(firstItem);
            if (renameText != null && renameText.length() > 0 && hasCustomEnchant(firstItem)) {
                ItemStack result = event.getResult();
                if (result != null) {
                    result.addEnchantments(customEnchants);
                }
                event.setResult(result);
            }

            // Return early if there's no rename text.
            return;
        }

        // Returns early if the first item is an enchanted book.
        if (firstItem.getType() == Material.ENCHANTED_BOOK) {
            return;
        }

        // Prevent merging if both items are of different types, and the second item isn't an enchanted book.
        if (firstItem.getType() != secondItem.getType() && secondItem.getType() != Material.ENCHANTED_BOOK) {
            return;
        }

        // Prevent merging if both items have no custom enchants.
        if (!hasCustomEnchant(firstItem) && !hasCustomEnchant(secondItem)) {
            return;
        }

        // Merge the enchantments from both items.
        Map<Enchantment, Integer> mergedEnchantments = mergeEnchantments(firstItem.getEnchantments(), secondItem.getEnchantments());

        // Determine the result item. Clone the first item if the second item has a custom enchantment or if the default result is null.
        ItemStack resultItem = event.getResult();
        if (resultItem == null || hasCustomEnchant(secondItem)) {
            resultItem = firstItem.clone();
        }

        // Retrieve and prepare the metadata for the result item.
        ItemMeta resultMeta = resultItem.getItemMeta();

        // If the second item is an enchanted book with stored enchantments, merge these enchantments too.
        if (secondItem.getType() == Material.ENCHANTED_BOOK) {
            EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) secondItem.getItemMeta();
            if (bookMeta != null && bookMeta.hasStoredEnchants()) {
                mergedEnchantments = mergeEnchantments(mergedEnchantments, bookMeta.getStoredEnchants());
            }
        }

        // Apply all merged enchantments to the result item's metadata.
        mergedEnchantments.forEach((enchantment, level) -> resultMeta.addEnchant(enchantment, level, true));

        // Generate and set custom lore for the result item based on the merged enchantments.
        List<String> resultLore = getCustomLore(mergedEnchantments);
        resultMeta.setLore(resultLore);

        if (renameText != null && renameText.length() > 0) {
            resultMeta.setDisplayName(renameText);
        }

        // Finalize the result item by setting its metadata and setting it as the event's result.
        resultItem.setItemMeta(resultMeta);

        event.setResult(resultItem);
    }

    /**
     * Checks if an item has a custom enchantment.
     *
     * @param item The item to check for custom enchantments.
     * @return true if the item has a custom enchantment, false otherwise.
     */
    private static boolean hasCustomEnchant(@NotNull ItemStack item) {
        return !getCustomEnchants(item).isEmpty();
    }

    /**
     * Gets a list of the custom enchantments and their level on an item.
     *
     * @param item The item to check for custom enchantments.
     * @return map of custom enchantments on the item.
     */
    private static Map<Enchantment, Integer> getCustomEnchants(@NotNull ItemStack item) {
        Map<Enchantment, Integer> enchants = new HashMap<>(item.getEnchantments());
        List<Enchantment> toRemove = new ArrayList<>();
        for (Enchantment enchant : enchants.keySet()) {
            if (!ENCHANTMENTS.containsValue(enchant)) {
                toRemove.add(enchant);
            }
        }

        toRemove.forEach(enchants::remove);

        return enchants;
    }

    /**
     * Merges the enchantments from two maps, choosing the highest level of overlapping enchantments.
     *
     * @param firstItemEnchants  Enchantments from the first item.
     * @param secondItemEnchants Enchantments from the second item.
     * @return A map containing the merged enchantments.
     */
    private static Map<Enchantment, Integer> mergeEnchantments(Map<Enchantment, Integer> firstItemEnchants, Map<Enchantment, Integer> secondItemEnchants) {
        Map<Enchantment, Integer> mergedEnchantments = new HashMap<>(firstItemEnchants);

        secondItemEnchants.forEach((enchantment, level) -> {
            if (mergedEnchantments.containsKey(enchantment)) {
                int existingLevel = mergedEnchantments.get(enchantment);
                if (existingLevel == level) {
                    mergedEnchantments.put(enchantment, Math.min(existingLevel + 1, enchantment.getMaxLevel()));
                } else {
                    mergedEnchantments.put(enchantment, Math.max(existingLevel, level));
                }
            } else {
                mergedEnchantments.put(enchantment, level);
            }
        });

        return mergedEnchantments;
    }

    /**
     * Generates a list of custom lore based on the enchantments present.
     *
     * @param enchantments The enchantments to generate lore for.
     * @return A list of lore strings.
     */
    private static List<String> getCustomLore(Map<Enchantment, Integer> enchantments) {
        List<String> customLore = new ArrayList<>();

        for (Enchantment enchantment : enchantments.keySet()) {
            if (LORE_MAP.containsKey(enchantment)) {
                customLore.add(LORE_MAP.get(enchantment));
            }
        }

        return customLore;
    }
}


