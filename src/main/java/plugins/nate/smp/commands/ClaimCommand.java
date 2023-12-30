package plugins.nate.smp.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import plugins.nate.smp.managers.ClaimsManager;
import plugins.nate.smp.objects.Claim;
import plugins.nate.smp.storage.SMPDatabase;
import plugins.nate.smp.utils.DatabaseUtils;
import plugins.nate.smp.utils.SMPUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

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
            return handleHelpCommand(player, args);
        }

        return switch (args[0].toLowerCase()) {
            case "help" -> handleHelpCommand(player, args);
            case "tool" -> handleToolCommand(player);
            case "showborder" -> handleShowBorderCommand(player, args);
            case "create" -> handleCreateCommand(player, args);
            case "info" -> handleInfoCommand(player, args);
            case "list" -> handleListCommand(player);
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

//    private static BaseComponent[] listClaims() {
//        ComponentBuilder tooltip = new ComponentBuilder()
//                .append(ChatColor.GREEN + "Click to show info about this claim!");
//
//        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(tooltip.create()));
//        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/claim showborder");
//    }

    /*
    * Sub command methods below
    * */

    private static boolean handleIncorrectCommandUsage(Player player) {
        sendMessage(player, PREFIX + "&cUnknown command. Type /claim help for help.");
        return true;
    }

    private static boolean handleToolCommand(Player player) {
        Inventory inventory = player.getInventory();
        if (inventory.contains(ClaimsManager.claimTool())) {
            sendMessage(player, PREFIX + "&cYou already have a claim tool in your inventory!");
            return true;
        }

        ClaimsManager.giveClaimTool(player);
        sendMessage(player, PREFIX + "&aGranted claim tool in inventory");
        return true;
    }

    private static boolean handleHelpCommand(Player player, String[] args) {
        if (args.length > 1) {
            handleIncorrectCommandUsage(player);
            return true;
        }

        sendMessage(player, "&8&m------------------------&8&l[&a&lSMP&8&l]&8&m------------------------");
        sendMessage(player, "&a/claim help &7- Displays claim help menu");
        sendMessage(player, "&a/claim tool &7- Grants claim tool");
        sendMessage(player, "&a/claim create <claim name> &7- Creates a claim");
        sendMessage(player, "&a/claim invite <username> <claim name> &7- Invites a player to a specific claim");
        sendMessage(player, "&a/claim info &7- Shows info on claim you're standing in");
        sendMessage(player, "&a/claim showborder &7- Shows the border of the claim you're standing in");

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
        sendMessage(player, "&7Claim Name: &a" + claim.getClaimName());
        // TODO: Add claim settings GUI
        sendMessage(player, "&7Settings: &a[Click to change settings]");
        sendMessage(player, createPosTooltip(claim));
        sendMessage(player, toggleClaimBorder());
        sendMessage(player, "&7Members: &a");

        return true;
    }

    private static boolean handleInviteCommand(Player player, String[] args) {
        if (args.length != 3) {
            sendMessage(player, PREFIX + "&cUsage: /claim invite <username> <claim name>");
            sendMessage(player, PREFIX + "&cUsage: /claim invite <accept|decline> <claim name>");
            return true;
        }

        if (!ClaimsManager.hasClaim(player)) {
            sendMessage(player, PREFIX + "&cYou currently do not have any claims! Create one with /claim create");
            return true;
        }

        String invitedPlayerName = args[1];
        String claimName = args[2];

        Claim claim = ClaimsManager.getClaimFromName(claimName);
        if (claim == null) {
            sendMessage(player, PREFIX + "&cClaim \"" + claimName + "\" does not exist!");
            return true;
        }

        SMPUtils.log(claim.toString());

        if (args[1].equalsIgnoreCase("accept")) {
            // TODO: Implement
            ClaimsManager.acceptClaimInvite(player, claim);
            return true;
        } else if (args[1].equalsIgnoreCase("decline")) {
            // TODO: Implement
            ClaimsManager.declineClaimInvite(player, claim);
            return true;
        }

        if (!ClaimsManager.isOwnerOfClaim(player, claim)) {
            sendMessage(player, PREFIX + "&cYou are not the owner \"" + claimName + "\"! You cannot invite people to claims you don't own!");
            return true;
        }

        OfflinePlayer invitedPlayer = Bukkit.getOfflinePlayer(invitedPlayerName);

        if (!invitedPlayer.hasPlayedBefore()) {
            sendMessage(player, PREFIX + "&cPlayer \"" + invitedPlayerName + "\" must be online when inviting them" );
            return true;
        }

        if (!invitedPlayer.isOnline()) {
            sendMessage(player, PREFIX + "&c" + invitedPlayerName + " must be online to invite them!");
            return true;
        }

        // TODO: Implement
        ClaimsManager.sendClaimInvite(player, (Player) invitedPlayer, claim);
        sendMessage(player, PREFIX + "Implement successful invite");
        return true;
    }

    private static boolean handleListCommand(Player player) {
        ResultSet rs = SMPDatabase.queryDB("SELECT * FROM claims WHERE OwnerUUID = ?;", player.getUniqueId().toString());

        try {
            int rowCount = 0;
            while (rs.next()) {
                rowCount++;
            }

            rs.beforeFirst();

            if (rs.next()) {
                String claimName = rs.getString("ClaimName");
                UUID ownerUUID = UUID.fromString(rs.getString("OwnerUUID"));
                World world = Bukkit.getWorld(rs.getString("World"));
                Location[] points = DatabaseUtils.getClaimPointsFromDatabase(rs);

                Claim claim = new Claim(points, ownerUUID, claimName);

                if (rowCount == 1) {

                } else {

                }


                // User has claims
                sendMessage(player, "test");

            } else {
                // User has no claims
                sendMessage(player, PREFIX + "&cYou do not have any claims! Get started with /claim help");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return true;
    }
}
