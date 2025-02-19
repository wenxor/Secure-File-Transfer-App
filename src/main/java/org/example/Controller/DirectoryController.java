package org.example.Controller;

import org.example.Model.Directory;
import org.example.Model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.example.DB.DatabaseConnector.connectToDatabase;

public class DirectoryController
{
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

    public static void createDirectory(Directory upload_dir) throws SQLException
    {

        String query = "INSERT INTO directories (id_directory, id_user, path_directory, name_directory, created_date) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps;
        try
        {
            ps = connection.prepareStatement(query);
            ps.setString(1, upload_dir.getId_directory());
            ps.setString(2, upload_dir.getId_user());
            ps.setString(3, upload_dir.getPath_directory());
            ps.setString(4, upload_dir.getName_directory());
            ps.setString(5, upload_dir.getCreated_date());
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        int new_row_file = ps.executeUpdate();
        if (new_row_file > 0)
        {
//            System.out.println("'" + upload_dir.getName_directory() + "' created!");
        } else
        {
//            System.out.println("failed create directory!");
        }
    }

    public static Directory getDirectoryByPath(String path_directory) throws SQLException
    {
        Directory file;
        String login_query = "SELECT * FROM directories WHERE path_directory = ?";
        PreparedStatement ps;
        try
        {
            ps = connection.prepareStatement(login_query);
            ps.setString(1, path_directory);
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        ResultSet rs = ps.executeQuery();
        if (rs.next())
        {
            file = new Directory(
                    rs.getString("id_directory"),
                    rs.getString("id_user"),
                    rs.getString("path_directory"),
                    rs.getString("name_directory"),
                    rs.getString("created_date")
            );
            return file;
        }
//        System.out.println("Can not find " + path_directory);
        return null;
    }

    public static User getUserUploadByDirectoryId(String id_dir)
    {
        User user_upload = null;
        String query = "SELECT u.* FROM users u, directories d WHERE u.id = d.id_user AND d.id_directory = ?";
        PreparedStatement ps;
        try
        {
            ps = connection.prepareStatement(query);
            ps.setString(1, id_dir);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
            {
                user_upload = new User(
                        rs.getString("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("email"),
                        rs.getString("fullname"),
                        rs.getString("date_created"),
                        rs.getBoolean("anonymous"),
                        rs.getBoolean("activated"),
                        rs.getLong("max_size")
                );
            }
            return user_upload;
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static boolean setUserDataById(String id_user, long data_size)
    {
        String query = "UPDATE users SET max_size = ? WHERE id = ?";
        PreparedStatement ps;
        try
        {
            ps = connection.prepareStatement(query);
            ps.setLong(1, data_size);
            ps.setString(2, id_user);
            ps.executeUpdate();
            return true;
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
        return false;
    }

    public static Directory getDirectoryById(String id) throws SQLException
    {
        String query = "SELECT * FROM directories WHERE id_directory = ?";
        PreparedStatement ps;
        try
        {
            ps = connection.prepareStatement(query);
            ps.setString(1, id);
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        ResultSet rs = ps.executeQuery();
        if (rs.next())
        {
            return new Directory(
                    rs.getString("id_directory"),
                    rs.getString("id_user"),
                    rs.getString("path_directory"),
                    rs.getString("name_directory"),
                    rs.getString("created_date")
            );
        }
        return null;
    }

    public static long calculateDirectorySize(java.io.File directory)
    {
        long size = 0;
        java.io.File[] files = directory.listFiles();
        if (files != null)
        {
            for (java.io.File file : files)
            {
                if (file.isFile())
                {
                    size += file.length();
                } else if (file.isDirectory())
                {
                    size += calculateDirectorySize(file);
                }
            }
        }
        return size;
    }

    public static void removeDirectory(java.io.File dir_remove)
    {
        String query = "DELETE FROM `directories` WHERE path_directory = ?";
        PreparedStatement ps;
        try
        {
            ps = connection.prepareStatement(query);
            ps.setString(1, dir_remove.getAbsolutePath());
            ps.executeUpdate();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static Directory getDirectoryShared(String email, String dir_name)
    {
        String query = "SELECT * FROM directories d, users u WHERE d.id_user = u.id && u.email = ? && d.name_directory = ?";
        PreparedStatement ps;
        try
        {
            ps = connection.prepareStatement(query);
            ps.setString(1, email);
            ps.setString(2, dir_name);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
            {
                return new Directory(
                        rs.getString("id_directory"),
                        rs.getString("id_user"),
                        rs.getString("path_directory"),
                        rs.getString("name_directory"),
                        rs.getString("created_date")
                );
            }

        } catch (SQLException e)
        {
            System.out.println(e.getMessage());
        }
        return null;
    }
}
