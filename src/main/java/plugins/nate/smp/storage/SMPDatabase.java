package plugins.nate.smp.storage;

import org.jetbrains.annotations.NotNull;
import plugins.nate.smp.SMP;

import java.io.File;
import java.sql.*;

public class SMPDatabase {
    private Connection connection;

    public void initialize() {
        try {
            File dataFolder = SMP.getPlugin().getDataFolder();
            if (!dataFolder.exists() && !dataFolder.mkdir()) {
                throw new IllegalStateException("Failed to create plugin data folder: " + dataFolder);
            }

            String url = "jdbc:sqlite:" + new File(SMP.getPlugin().getDataFolder(), "smp.db").getAbsolutePath();
            connection = DriverManager.getConnection(url);

            try (Statement statement = connection.createStatement()) {
                String sql = "CREATE TABLE IF NOT EXISTS claims (" +
                        "ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "OwnerUUID TEXT," +
                        "World TEXT," +
                        "Pos1_X INTEGER," +
                        "Pos1_Y INTEGER," +
                        "Pos1_Z INTEGER," +
                        "Pos2_Y INTEGER," +
                        "Pos2_X INTEGER," +
                        "Pos2_Z INTEGER," +
                        "Members TEXT);";

                statement.execute(sql);
            }

            try (Statement statement = connection.createStatement()) {
                String sql = "CREATE TABLE IF NOT EXISTS players (" +
                        "PlayerUUID INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "MaxBlocks INTEGER);";

                statement.execute(sql);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                initialize();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    @SafeVarargs
    @NotNull
    public static <T> ResultSet queryDB(final String query, final T... args) {
        try {
            PreparedStatement statement = SMP.getPlugin().getDatabase().getConnection().prepareStatement(query);
            for (int i = 0; i < args.length; i++) {
                T arg = args[i];
                if (arg instanceof Integer) {
                    statement.setInt(i + 1, (Integer) arg);
                } else if (arg instanceof Double) {
                    statement.setDouble(i + 1, (Double) arg);
                } else if (arg instanceof String) {
                    statement.setString(i + 1, (String) arg);
                } else if (arg instanceof Long) {
                    statement.setLong(i + 1, (Long) arg);
                }
            }
            if (statement.execute()) {
                return statement.getResultSet();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}
