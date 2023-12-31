package plugins.nate.smp.enchantments;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import plugins.nate.smp.utils.SMPUtils;

public class TimberEnchant extends Enchantment implements CustomEnchant {

    public TimberEnchant() {
        super(NamespacedKey.minecraft("timber"));
    }

    @Override
    public @NotNull String getName() {
        return "Timber";
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public int getStartLevel() {
        return 1;
    }

    @Override
    public @NotNull EnchantmentTarget getItemTarget() {
        return EnchantmentTarget.TOOL;
    }

    @Override
    public boolean isTreasure() {
        return false;
    }

    @Override
    public boolean isCursed() {
        return false;
    }

    @Override
    public boolean conflictsWith(@NotNull Enchantment other) {
        return false;
    }

    @Override
    public boolean canEnchantItem(ItemStack item) {
        return SMPUtils.isAxe(item.getType()) || item.getType() == Material.BOOK;
    }

    @Override
    public double getChance() {
        return 0.05;
    }

    @Override
    public String getLore() {
        return ChatColor.GOLD + "Timber";
    }
}
