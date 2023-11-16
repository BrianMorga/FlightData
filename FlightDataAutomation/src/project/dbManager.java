package project;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class dbManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/flightdb";

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL, "root", "password");
    }

    public static void close(Connection connection) throws SQLException {
        if(connection == null) {
            connection.close();
        }
    }
}
