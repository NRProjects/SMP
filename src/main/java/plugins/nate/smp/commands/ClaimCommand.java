package plugins.nate.smp.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import plugins.nate.smp.managers.ClaimsManager;
import plugins.nate.smp.objects.Claim;

import java.util.List;

import static plugins.nate.smp.utils.ChatUtils.PREFIX;
import static plugins.nate.smp.utils.ChatUtils.sendMessage;

public class ClaimCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sendMessage(sender, "&cOnly players can use this command!");
            return true;
        }

        if (args.length == 0) {
            ClaimsManager.giveClaimTool(player);
            sendMessage(player, PREFIX + "&aGranted claim tool in inventory");

            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
            sendMessage(player, "Implement help menu");

            return true;
        }
        // /claim <showborder>
        else if (args.length == 1 && args[0].equalsIgnoreCase("showborder")) {
            Claim claim = ClaimsManager.getClaimAtLocation(player.getLocation());

            if (claim == null) {
                sendMessage(player, PREFIX + "&cYou must be standing in a claim to display its borders!");

                return true;
            }

            ClaimsManager.toggleClaimBorder(claim, player);

            return true;
        } else if (args[0].equalsIgnoreCase("create")) {
            if (ClaimsManager.hasNullSelectionPoint(player)) {
                sendMessage(player, PREFIX + "&cYou must select two points to make a claim!");

                return true;
            }

            if (args.length > 3 || args[1].equals("")) {
                sendMessage(player, PREFIX + "&&cUsage: /claim confirm <claim name>");

                return true;
            }

            String claimName = args[1];
            Claim claim = new Claim(ClaimsManager.getPoints(player), player.getUniqueId(), claimName);
            ClaimsManager.createClaim(claim);

            return true;
        } else if (args.length == 1 && args[0].equalsIgnoreCase("info")) {
            Claim claim = ClaimsManager.getClaimAtLocation(player.getLocation());

            if (claim == null) {
                sendMessage(player, PREFIX + "&cYou are not in a claim!");

                return true;
            }

            ClaimsManager.displayClaimBorder(claim, player);
            sendMessage(player, "&8&m------------------------&8&l[&a&lSMP&8&l]&8&m------------------------");
            sendMessage(player, "&7Owner: &a" + claim.getOwnerName());
            // TODO: Add claim settings GUI
            sendMessage(player, "&7Settings: &a[Click to change settings]");
            sendMessage(player, createPosTooltip(claim));
            sendMessage(player, toggleClaimBorder());
            sendMessage(player, "&7Members: &a");



            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return null;
    }

    private static BaseComponent[] createPosTooltip(Claim claim) {
        ComponentBuilder tooltip = new ComponentBuilder()
                .append(ChatColor.GREEN + "(X: " + claim.getMaxX() + " Y: " + claim.getMaxY() + " Z: " + claim.getMaxZ() + " || " +
                        "X: " + claim.getMinX() + " Y: " + claim.getMinY() + " Z: " + claim.getMinZ());

        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(tooltip.create()));

        ComponentBuilder chatMessage = new ComponentBuilder()
                .append(ChatColor.GRAY + "Positions: " + ChatColor.GREEN + "[Hover to show]")
                .event(hoverEvent);

        return chatMessage.create();
    }

    private static BaseComponent[] toggleClaimBorder() {
        ComponentBuilder tooltip = new ComponentBuilder()
                .append(ChatColor.GREEN + "Click to show the border of your claim!");

        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(tooltip.create()));
        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/claim showborder");

        ComponentBuilder chatMessage = new ComponentBuilder()
                .append(ChatColor.GRAY + "Border: " + ChatColor.GREEN + "[Click to show claim border]")
                .event(hoverEvent)
                .event(clickEvent);

        return chatMessage.create();
    }
}
