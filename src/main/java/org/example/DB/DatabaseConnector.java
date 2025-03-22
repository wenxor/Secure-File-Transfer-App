package org.example.DB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnector
{
    private static final String URL = "jdbc:mysql://localhost:3306/myftpdb";
    private static final String USERDB = "root";
    private static final String PASSDB = "";
    private static Connection connection;

    public static Connection connectToDatabase() throws SQLException
    {
        connection = DriverManager.getConnection(URL, USERDB, PASSDB);
        return connection;
    }

    public static void closeConnectionToDatabase() throws SQLException
    {
        if (connection != null && !connection.isClosed())
        {
            connection.close();
        }
    }
}
