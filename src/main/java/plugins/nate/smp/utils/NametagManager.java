package plugins.nate.smp.utils;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import plugins.nate.smp.SMP;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NametagManager {
    private static Team getTeam(Scoreboard scoreboard, String name) {
        return scoreboard.getTeam(name) == null ? scoreboard.registerNewTeam(name) : scoreboard.getTeam(name);
    }

    public static void updateNametag(Player player) {
        Team team = getTeam(player.getScoreboard(), player.getName());
        team.addEntry(player.getName());

        List<String> playerPrefixData = SMP.getPlugin().getPrefixes().getStringList("player.prefixes");
        List<String> groupPrefixData = SMP.getPlugin().getPrefixes().getStringList("group.prefixes");
        if (playerPrefixData.isEmpty() && groupPrefixData.isEmpty()) { return; }

        Map<String, String> playerPrefixes = new HashMap<>();
        for (String s : playerPrefixData) {
            String[] splitData = s.split(":");
            playerPrefixes.put(splitData[0], splitData[1]);
        }

        for (Map.Entry<String, String> entry : playerPrefixes.entrySet()) {
            if (player.getUniqueId().toString().equalsIgnoreCase(entry.getKey())) {
                team.setPrefix(colorize(entry.getValue()));
                return;
            }
        }

        Map<String, String> groupPrefixes = new HashMap<>();
        for (String s : groupPrefixData) {
            String[] splitData = s.split(":");
            groupPrefixes.put(splitData[0], splitData[1]);
        }

        for (Map.Entry<String, String> entry : groupPrefixes.entrySet()) {
            if (player.hasPermission("group." + entry.getKey())) {
                team.setPrefix(colorize(entry.getValue()));
                return;
            }
        }

        if (!team.getPrefix().equals("")) {
            team.setPrefix("");
        }
    }

    private static final Pattern HEX_PATTERN = Pattern.compile("&(?<hex>#\\w{6})");
    public static String colorize(String message) {
        Matcher matcher = HEX_PATTERN.matcher(ChatColor.translateAlternateColorCodes('&', message));
        StringBuilder builder = new StringBuilder();

        while (matcher.find()) {
            matcher.appendReplacement(builder, ChatColor.of(matcher.group("hex")).toString());
        }

        return matcher.appendTail(builder).toString();
    }
}
