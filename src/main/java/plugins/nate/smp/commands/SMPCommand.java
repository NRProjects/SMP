package plugins.nate.smp.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import plugins.nate.smp.listeners.ChestLockListener;
import plugins.nate.smp.managers.TrustManager;
import plugins.nate.smp.utils.ChatUtils;
import plugins.nate.smp.utils.SMPUtils;

import java.util.*;
import java.util.stream.Collectors;

import static plugins.nate.smp.utils.ChatUtils.sendMessage;

public class SMPCommand implements CommandExecutor, TabCompleter {
    private static final Set<String> VALID_SUBCOMMANDS = Set.of("help", "features", "reload", "trust", "untrust", "trustlist", "lock", "unlock");

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
        } else if (args[0].equalsIgnoreCase("lock")) {
            if (!(sender instanceof Player)) {
                sendMessage(sender, "&cOnly players can use this command!");
                return true;
            }

            Player player = (Player) sender;
            Block targetBlock = player.getTargetBlock(null, 5);
            if (!(targetBlock.getState() instanceof Container)) {
                sendMessage(sender, "&cNot a valid container.");
                return true;
            }
            Container container = (Container) targetBlock;

            // ChestLockListener chestLockListener = new ChestLockListener();
            ChestLockListener.lockContainer(player.getUniqueId(), container);

        } else if (args[0].equalsIgnoreCase("unlock")) {
            if (!(sender instanceof Player)) {
                sendMessage(sender, "&cOnly players can use this command!");
                return true;
            }
            
            Player player = (Player) sender;
            Block targetBlock = player.getTargetBlock(null, 5);

            if (!(targetBlock.getState() instanceof Container)) {
                sendMessage(sender, "&cNot a valid container.");
                return true;
            }

            Container container = (Container) targetBlock;
            
            if (sender.hasPermission("smp.bypasslocks") == false) {
                sendMessage(sender, "&cYou don't have permission to do this!");
                return true;
            }

            // ChestLockListener chestLockListener = new ChestLockListener();
            UUID poppedUUID = ChestLockListener.unlockContainer(container);
            if (poppedUUID == null) {
                sendMessage(sender, "&cThere is no lock on this container.");
                return true;
            }

            sendMessage(sender, "&aSuccesfully removed " + Bukkit.getServer().getOfflinePlayer(poppedUUID).getName() + " from the lock.");
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
