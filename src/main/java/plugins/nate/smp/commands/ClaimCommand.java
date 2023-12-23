package plugins.nate.smp.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
            return handleDefaultCommand(player);
        }

        return switch (args[0].toLowerCase()) {
            case "help" -> handleHelpCommand(player, args);
            case "showborder" -> handleShowBorderCommand(player, args);
            case "create" -> handleCreateCommand(player, args);
            case "info" -> handleInfoCommand(player, args);
            case "invite" -> handleInviteCommand(player, args);
            default -> handleIncorrectCommandUsage(player);
        };
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
                .append(ChatColor.GRAY + "Border: " + ChatColor.GREEN + "[Click to toggle claim border]")
                .event(hoverEvent)
                .event(clickEvent);

        return chatMessage.create();
    }

    /*
    * Sub command methods below
    * */

    private static boolean handleIncorrectCommandUsage(Player player) {
        sendMessage(player, PREFIX + "&cUnknown command. Type /claim help for help.");
        return true;
    }

    private static boolean handleDefaultCommand(Player player) {
        ClaimsManager.giveClaimTool(player);
        sendMessage(player, PREFIX + "&aGranted claim tool in inventory");
        return true;
    }

    private static boolean handleHelpCommand(Player player, String[] args) {
        if (args.length > 1) {
            handleIncorrectCommandUsage(player);
            return true;
        }

        sendMessage(player, PREFIX + "/claim help - Displays claim help menu");
        sendMessage(player, PREFIX + "/claim create <claim name> - Creates a claim");
        sendMessage(player, PREFIX+ "/claim invite <username> <claim name> - Invites a player to a specific claim");
        sendMessage(player, PREFIX + "/claim info - Shows info on claim you're standing in");
        sendMessage(player, PREFIX + "/claim showborder - Shows the border of the claim you're standing in");

        return true;
    }

    private static boolean handleShowBorderCommand(Player player, String[] args) {
        if (args.length > 1) {
            handleIncorrectCommandUsage(player);
            return true;
        }

        Claim claim = ClaimsManager.getClaimAtLocation(player.getLocation());

        if (claim == null) {
            sendMessage(player, PREFIX + "&cYou must be standing in a claim to display its borders!");

            return true;
        }

        ClaimsManager.toggleClaimBorder(claim, player);
        return true;
    }

    private static boolean handleCreateCommand(Player player, String[] args) {
        if (args.length != 2) {
            sendMessage(player, PREFIX + "&&cUsage: /claim create <claim name>");

            return true;
        }

        if (ClaimsManager.hasNullSelectionPoint(player)) {
            sendMessage(player, PREFIX + "&cYou must select two points to make a claim!");

            return true;
        }

        String claimName = args[1];

        if (ClaimsManager.claimNameAlreadyExists(claimName)) {
            sendMessage(player, PREFIX + "&cClaim name is already taken!");
            return true;
        }

        Claim claim = new Claim(ClaimsManager.getPoints(player), player.getUniqueId(), claimName);
        ClaimsManager.createClaim(claim);

        return true;
    }

    private static boolean handleInfoCommand(Player player, String[] args) {
        if (args.length > 1) {
            handleIncorrectCommandUsage(player);
            return true;
        }

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

    private static boolean handleInviteCommand(Player player, String[] args) {
        if (args.length != 3) {
            sendMessage(player, PREFIX + "&cUsage: /claim invite <username>");
            return true;
        }

        if (!ClaimsManager.hasClaim(player)) {
            sendMessage(player, PREFIX + "&cYou currently do not have any claims! Create one with /claim create");
            return true;
        }

        Claim claim = ClaimsManager.getClaimFromName(args[2]);
        if (claim == null) {
            sendMessage(player, PREFIX + "&cClaim \"" + args[2] + "\" does not exist!");
            return true;
        }

        if (!ClaimsManager.isOwnerOfClaim(player, claim)) {
            sendMessage(player, PREFIX + "&cYou are not the owner \"" + args[2] + "\"! You cannot invite people to claims you don't own!");
            return true;
        }

        OfflinePlayer invitedPlayer = Bukkit.getOfflinePlayer(args[1]);

        if (!invitedPlayer.hasPlayedBefore()) {
            sendMessage(player, PREFIX + "&cPlayer \"" + args[1] + "\" must be online when inviting them" );
            return true;
        }

        sendMessage(player, PREFIX + "Implement successful invite");
        return true;
    }
}
