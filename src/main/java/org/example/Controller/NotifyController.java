package org.example.Controller;

import org.example.Model.Notify;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import static org.example.DB.DatabaseConnector.connectToDatabase;

public class NotifyController
{
    public enum Action
    {
        SHARE, UPLOAD, CREATE_DIRECTORY, REMOVE
    }

    static Connection connection;

    static
    {
        try
        {
            connection = connectToDatabase();
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void createNotify(Notify notify) throws SQLException
    {
        String register_query = "INSERT INTO notifications (id_notify, id_user, id_user_interacted, id_directory, id_file, action, notify_time) VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps;
        try
        {
            ps = connection.prepareStatement(register_query);
            ps.setString(1, notify.getId_notify());
            ps.setString(2, notify.getId_user());
            ps.setString(3, notify.getId_user_interacted());
            ps.setString(4, notify.getId_directory());
            ps.setString(5, notify.getId_file());
            ps.setString(6, notify.getAction().toString());
            ps.setString(7, notify.getNotify_time());
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        ps.executeUpdate();
    }

    public static ArrayList<Notify> getUserNotifications(String id_user)
    {
        ArrayList<Notify> list_notify = new ArrayList<>();
        String query = "SELECT * FROM notifications WHERE id_user = ?";
        PreparedStatement ps;
        try
        {
            ps = connection.prepareStatement(query);
            ps.setString(1, id_user);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
            {
                Notify notify = new Notify(
                        rs.getString("id_notify"),
                        rs.getString("id_user"),
                        rs.getString("id_user_interacted"),
                        rs.getString("id_directory"),
                        rs.getString("id_file"),
                        Action.valueOf(rs.getString("action")),
                        rs.getString("notify_time")
                );
                list_notify.add(notify);
            }

        } catch (SQLException e)
        {
            System.out.println(e.getMessage());
        }
        return list_notify;
    }
}
