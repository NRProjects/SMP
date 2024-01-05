package plugins.nate.smp.utils;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChatUtils {

    public static final String PREFIX = "&8[&a&lSMP&8] &r";
    public static final String DEV_PREFIX = "&8[&3&lDEV&8] &r";
    public static final String SERVER_PREFIX = "&8&l[&c&lSERVER&8&l] &c";
    public static final String DENIED_COMMAND = "&cYou do not have access to this command";

    public static void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    public static void createActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.translateAlternateColorCodes('&', message)));
    }
}
