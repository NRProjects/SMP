package plugins.nate.smp.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseUtils {
    public static Location[] getClaimPointsFromDatabase(ResultSet resultSet) {
        try {
            Location pos1 = new Location(Bukkit.getWorld(resultSet.getString("World")), resultSet.getInt("MaxX"), resultSet.getInt("MaxY"), resultSet.getInt("MaxZ"));
            Location pos2 = new Location(Bukkit.getWorld(resultSet.getString("World")), resultSet.getInt("MinX"), resultSet.getInt("MinY"), resultSet.getInt("MinZ"));

            return new Location[] { pos1, pos2 };
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
}
