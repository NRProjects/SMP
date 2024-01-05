package plugins.nate.smp.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import plugins.nate.smp.managers.TrustManager;
import plugins.nate.smp.utils.ChatUtils;
import plugins.nate.smp.utils.SMPUtils;
import plugins.nate.smp.utils.TellerUtils;

import java.util.*;
import java.util.stream.Collectors;

import static plugins.nate.smp.utils.ChatUtils.sendMessage;

public class SMPCommand implements CommandExecutor, TabCompleter {
    private static final Set<String> VALID_SUBCOMMANDS = Set.of("help", "features", "reload", "forcelock", "lockholder", "teller", "trust", "untrust", "trustlist", "lock", "unlock");
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sendMessage(sender, "&8&m------------------------&8&l[&a&lSMP&8&l]&8&m------------------------");
            sendMessage(sender, "&aWelcome to the CoolmentSMP! This server is driven by public");
            sendMessage(sender, "&aand private plugins to give the best experience possible.");
            sendMessage(sender, "&aThe SMP plugin is main driver of this server and is meant to");
            sendMessage(sender, "&aprovide a vanilla-esque experience. With additions in and out");
            sendMessage(sender, "&aof game, including QoL additions and Discord integration. ");
            sendMessage(sender, "&aIf you have any feature requests or issues contact staff");
            sendMessage(sender, "&aon our Discord. Thank you and have fun!");
            sendMessage(sender, "&8&m------------------------&8&l[&a&lSMP&8&l]&8&m------------------------");

            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("smp.reload")) {
                sendMessage(sender, ChatUtils.PREFIX + ChatUtils.DENIED_COMMAND);
                return true;
            }

            SMPUtils.reloadPlugin(sender);
        } else if (args[0].equalsIgnoreCase("forcelock")) {
            if (!(sender instanceof Player player)) {
                sendMessage(sender, "&cOnly players can use this command!");
                return true;
            }
            if (!sender.hasPermission("smp.forcelock")) {
                sendMessage(sender, ChatUtils.PREFIX + ChatUtils.DENIED_COMMAND);
                return true;
            }
            if (args.length == 1) {
                sendMessage(sender, ChatUtils.PREFIX + "&cUsage: /smp forcelock <username>");
                return true;
            }
            if (Bukkit.getOfflinePlayer(args[1]).hasPlayedBefore() == false) {
                sendMessage(sender, ChatUtils.PREFIX + "&c" + args[1] + " is not a valid player!");
                return true;
            }
            Block targetBlock = player.getTargetBlockExact( 5);
            if (!(targetBlock.getState() instanceof Sign sign)) {
                sendMessage(sender, ChatUtils.PREFIX + "&cMust be a targeting a sign.");
                return true;
            }
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[1]);
            sendMessage(sender, ChatUtils.PREFIX + "&7Locking sign for &a" + offlinePlayer.getName());
            sign.getPersistentDataContainer().set(SMPUtils.OWNER_UUID_KEY, PersistentDataType.STRING, offlinePlayer.getUniqueId().toString());
            sign.getSide(Side.FRONT).setLine(0, "[LockedV2]");
            sign.getSide(Side.FRONT).setLine(1, offlinePlayer.getName());
            sign.setWaxed(true);
            sign.update();
        } else if (args[0].equalsIgnoreCase("teller")) {
            if (!(sender instanceof Player player)) {
                sendMessage(sender, "&cOnly players can use this command!");
                return true;
            }

            if (!sender.hasPermission("smp.createteller")) {
                sendMessage(sender, ChatUtils.PREFIX + ChatUtils.DENIED_COMMAND);
                return true;
            }

            if (args.length == 1) {
                sendMessage(sender, ChatUtils.PREFIX + "&cUsage: /smp teller (deposit/withdraw)");
                return true;
            }

            if (args[1].equalsIgnoreCase("deposit")) {
                TellerUtils.createDepositTeller(player);
                sendMessage(sender, ChatUtils.PREFIX + "&7Spawning a &adeposit &7teller.");
                return true;
            }

            if (args[1].equalsIgnoreCase("withdraw")) {
                TellerUtils.createWithdrawTeller(player);
                sendMessage(sender, ChatUtils.PREFIX + "&7Spawning a &awithdraw &7teller.");
                return true;
            }

            sendMessage(sender, ChatUtils.PREFIX + "&cUsage: /smp teller (deposit/withdraw)");
            return true;
        } else if (args[0].equalsIgnoreCase("lockholder")) {
            if (!(sender instanceof Player player)) {
                sendMessage(sender, "&cOnly players can use this command!");
                return true;
            }
            if (!sender.hasPermission("smp.lockinspect")) {
                sendMessage(sender, ChatUtils.PREFIX + ChatUtils.DENIED_COMMAND);
                return true;
            }

            Block targetBlock = player.getTargetBlockExact(5);
            if (!(targetBlock.getState() instanceof Sign sign)) {
                sendMessage(sender, ChatUtils.PREFIX + "&cMust be a targeting a sign.");
                return true;
            }
            
            if (sign.getPersistentDataContainer().get(SMPUtils.OWNER_UUID_KEY, PersistentDataType.STRING) == null) {
                sendMessage(sender, ChatUtils.PREFIX + "&cThis sign doesn't have a lock.");
                return true;
            }
            UUID signOwnerUUID = UUID.fromString(sign.getPersistentDataContainer().get(SMPUtils.OWNER_UUID_KEY, PersistentDataType.STRING)); 
            
            OfflinePlayer signOwner = Bukkit.getOfflinePlayer(signOwnerUUID);
            
            if (signOwner == null) {
                sendMessage(sender, ChatUtils.PREFIX + "&cThe sign is locked, but the owner doesn't exist.");
                return true;
            }
            
            sendMessage(sender, ChatUtils.PREFIX + "&a" + signOwner.getName() + " &7is the owner of this lock.");

        } else if (args[0].equalsIgnoreCase("help")) {
            sendMessage(sender, "&8&m------------------------&8&l[&a&lSMP&8&l]&8&m------------------------");
            sendMessage(sender, "&a/smp help &7- Displays this menu");
            sendMessage(sender, "&a/smp features &7- Display the unique features of the server");
            sendMessage(sender, "&a/smp trust &7- Add user to your trust list");
            sendMessage(sender, "&a/smp untrust &7- Remove user from your trust list");
            sendMessage(sender, "&a/smp trustlist &7- Display your trust list");
        } else if (args[0].equalsIgnoreCase("features")) {
            sendMessage(sender, "&8&m------------------------&8&l[&a&lSMP&8&l]&8&m------------------------");
            sendMessage(sender, "&aCheck out the GitHub for a list of features:");
            sendMessage(sender, "&7 - &ahttps://github.com/NRProjects/SMP &7-");
            sendMessage(sender, "&8&m------------------------&8&l[&a&lSMP&8&l]&8&m------------------------");
        } else if (args[0].equalsIgnoreCase("trust") || args[0].equalsIgnoreCase("untrust")) {
            if (!(sender instanceof Player player)) {
                sendMessage(sender, "&cOnly players can use this command!");
                return true;
            }

            String action = args[0].toLowerCase();

            if (args.length != 2) {
                sendMessage(sender, ChatUtils.PREFIX + "&cUsage: /smp " + action + " <player>");
                return true;
            }

            if (args[1].equalsIgnoreCase(player.getName())) {
                sendMessage(sender, ChatUtils.PREFIX + "&cYou cannot " + action + " yourself!");
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (!target.hasPlayedBefore()) {
                sendMessage(sender, ChatUtils.PREFIX + "&cPlayer not found");
                return true;
            }

            boolean updated;
            if (action.equals("trust")) {
                updated = TrustManager.trustPlayer(player, target);
            } else {
                updated = TrustManager.untrustPlayer(player, target);
            }

            if (updated) {
                sendMessage(sender, ChatUtils.PREFIX + "&aYou have " + action + "ed " + target.getName());
            } else {
                sendMessage(sender, ChatUtils.PREFIX + "&cYou've already " + action + "ed that player");
            }
        } else if (args[0].equalsIgnoreCase("trustlist")) {
            if (!(sender instanceof Player player)) {
                return true;
            }

            Set<UUID> trustedPlayers = TrustManager.getTrustedPlayers(player.getUniqueId());
            if (trustedPlayers.isEmpty()) {
                sendMessage(player, ChatUtils.PREFIX + "&cYou have not trusted any players");
                return true;
            }

            String trustedPlayerNames = trustedPlayers.stream()
                    .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                    .filter(Objects::nonNull)
                    .sorted()
                    .collect(Collectors.joining(", "));

            sendMessage(player, ChatUtils.PREFIX + "&aTrusted Players: " + trustedPlayerNames);
        } else {
            sendMessage(sender, ChatUtils.PREFIX + "&cUnknown command");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            return VALID_SUBCOMMANDS.stream()
                    .filter(subcommand -> subcommand.startsWith(args[0].toLowerCase()))
                    .toList();
        } else if (args.length == 2) {
            if ("trust".equalsIgnoreCase(args[0])) {
                return getAllOnlinePlayerNames().stream()
                        .map(String::toLowerCase)
                        .filter(playerName -> playerName.startsWith(args[1].toLowerCase()))
                        .toList();
            } else if ("untrust".equalsIgnoreCase(args[0])) {
                if (!(sender instanceof Player player)) {
                    return Collections.emptyList();
                }

                return TrustManager.getTrustedPlayerNames(player.getUniqueId()).stream()
                        .map(String::toLowerCase)
                        .filter(trustedPlayerName -> trustedPlayerName.startsWith(args[1].toLowerCase()))
                        .sorted()
                        .toList();
            }
        }

        return null;
    }

    private List<String> getAllOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .toList();
    }
}
