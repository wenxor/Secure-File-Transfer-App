package org.example.Controller;

import org.example.Model.Directory;
import org.example.Model.File;
import org.example.Model.Permission;
import org.example.Model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

import static org.example.DB.DatabaseConnector.connectToDatabase;

public class PermissionController
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

    public static ArrayList<File> getAllFileReceived(String id_user) throws SQLException
    {
        ArrayList<File> file_receives = new ArrayList<>();
        String query = "SELECT f.* FROM files f, permissions p WHERE f.id_file = p.id_file AND p.id_user = ?";
        PreparedStatement ps;
        try
        {
            ps = connection.prepareStatement(query);
            ps.setString(1, id_user);
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        ResultSet rs = ps.executeQuery();

        while (rs.next())
        {
            File permit_file = new File(
                    rs.getString("id_file"),
                    rs.getString("id_user_upload"),
                    rs.getString("filename"),
                    rs.getString("filepath"),
                    rs.getString("filetype"),
                    rs.getString("upload_date"),
                    rs.getLong("filesize")
            );
            file_receives.add(permit_file);
        }
        return file_receives;
    }

    public static ArrayList<Directory> getAllDirectoryReceived(String id_user) throws SQLException
    {
        ArrayList<Directory> dir_receives = new ArrayList<>();
        String query = "SELECT d.* FROM directories d, permissions p WHERE d.id_directory = p.id_directory AND p.id_user = ?";
        PreparedStatement ps;
        try
        {
            ps = connection.prepareStatement(query);
            ps.setString(1, id_user);
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        ResultSet rs = ps.executeQuery();

        while (rs.next())
        {
            Directory permit_dir = new Directory(
                    rs.getString("id_directory"),
                    rs.getString("id_user"),
                    rs.getString("path_directory"),
                    rs.getString("name_directory"),
                    rs.getString("created_date")
            );
            dir_receives.add(permit_dir);
        }
        return dir_receives;
    }

    public static boolean sharingWithPermission(String email, String permission, String file_path, String type) throws SQLException
    {
        boolean write_permit = false;
        boolean read_permit = false;
        File file_share = null;
        Directory directory_share = null;
        if (permission.equalsIgnoreCase("READ"))
        {
            read_permit = true;
        } else if (permission.equalsIgnoreCase("WRITE"))
        {
            write_permit = true;
        } else if (permission.equalsIgnoreCase("FULL"))
        {
            read_permit = write_permit = true;
        }
        User user_receive = AccountController.getUserByEmail(email);
        if (type.equals("f"))
        {
            file_share = FileController.getFileByPath(file_path);
        } else if (type.equals("d"))
        {
            directory_share = DirectoryController.getDirectoryByPath(file_path);
        }
        if (user_receive != null)
        {
            Permission user_permission = new Permission("", "", "", "", false, false);
            if (file_share != null)
            {
                user_permission = new Permission(
                        UUID.randomUUID().toString(),
                        file_share.getId_file(),
                        null,
                        user_receive.getId(),
                        read_permit,
                        write_permit);
            } else if (directory_share != null)
            {
                user_permission = new Permission(
                        UUID.randomUUID().toString(),
                        null,
                        directory_share.getId_directory(),
                        user_receive.getId(),
                        read_permit,
                        write_permit);
            }
            String query = "INSERT INTO permissions (id_permission, id_file, id_directory, id_user, isRead, isWrite) VALUES (?, ?, ?, ?, ?, ?);";
            PreparedStatement ps;
            try
            {
                ps = connection.prepareStatement(query);
                ps.setString(1, user_permission.getId_permission());
                ps.setString(2, user_permission.getId_file());
                ps.setString(3, user_permission.getId_directory());
                ps.setString(4, user_permission.getId_user());
                ps.setBoolean(5, user_permission.isRead());
                ps.setBoolean(6, user_permission.isWrite());
            } catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
            int new_permission = ps.executeUpdate();
            return new_permission > 0;
        }
        return false;
    }

    public static Permission getFileSharedPermission(String id_file, String id_user_receive)
    {
        String query = "SELECT * FROM permissions WHERE id_file = ? && id_user = ?";
        PreparedStatement ps;
        try
        {
            ps = connection.prepareStatement(query);
            ps.setString(1, id_file);
            ps.setString(2, id_user_receive);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
            {
                return new Permission(
                        rs.getString("id_permission"),
                        rs.getString("id_file"),
                        rs.getString("id_directory"),
                        rs.getString("id_user"),
                        rs.getBoolean("isRead"),
                        rs.getBoolean("isWrite")

                );
            }

        } catch (SQLException e)
        {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public static Permission getDirectorySharedPermission(String id_directory, String id_user_receive)
    {
        String query = "SELECT * FROM permissions WHERE id_directory = ? && id_user = ?";
        PreparedStatement ps;
        try
        {
            ps = connection.prepareStatement(query);
            ps.setString(1, id_directory);
            ps.setString(2, id_user_receive);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
            {
                return new Permission(
                        rs.getString("id_permission"),
                        rs.getString("id_file"),
                        rs.getString("id_directory"),
                        rs.getString("id_user"),
                        rs.getBoolean("isRead"),
                        rs.getBoolean("isWrite")

                );
            }

        } catch (SQLException e)
        {
            System.out.println(e.getMessage());
        }
        return null;
    }

}
