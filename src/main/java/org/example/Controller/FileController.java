package org.example.Controller;

import org.apache.tika.Tika;
import org.example.Model.File;
import org.example.Model.User;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.example.DB.DatabaseConnector.connectToDatabase;

public class FileController
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

    public static String detectFileType(java.io.File file) throws IOException
    {
        Tika tika = new Tika();
        return tika.detect(file);
    }

    public static void updateFileType(java.io.File file, String id_file)
    {
        String query = "UPDATE files SET filetype = ? WHERE id_file = ?";
        PreparedStatement ps;
        try
        {
            ps = connection.prepareStatement(query);
            ps.setString(1, detectFileType(file));
            ps.setString(2, id_file);
            ps.executeUpdate();
        } catch (SQLException | IOException e)
        {
            System.out.println(e.getMessage());
        }
    }

    public static void uploadFile(User user_upload, org.example.Model.File upload_file) throws SQLException
    {
        String query = "INSERT INTO files (id_file, id_user_upload, filename, filepath, filetype, upload_date, filesize) VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps;
        try
        {
            ps = connection.prepareStatement(query);
            ps.setString(1, upload_file.getId_file());
            ps.setString(2, user_upload.getId());
            ps.setString(3, upload_file.getFilename());
            ps.setString(4, upload_file.getFilepath());
            ps.setString(5, upload_file.getFiletype());
            ps.setString(6, upload_file.getUpload_date());
            ps.setLong(7, upload_file.getFilesize());
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        int new_row_file = ps.executeUpdate();
        if (new_row_file > 0)
        {
//            System.out.println(user_upload.getUsername() + " UPLOAD '" + upload_file.getFilename() + "' SUCCESS!");
        } else
        {
//            System.out.println("UPLOAD FAILED!");
        }
    }

    public static File getFileByPath(String path_file) throws SQLException
    {
        File file;
        String login_query = "SELECT * FROM files WHERE filepath = ?";
        PreparedStatement ps;
        try
        {
            ps = connection.prepareStatement(login_query);
            ps.setString(1, path_file);
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        ResultSet rs = ps.executeQuery();
        if (rs.next())
        {
            file = new File(
                    rs.getString("id_file"),
                    rs.getString("id_user_upload"),
                    rs.getString("filename"),
                    rs.getString("filepath"),
                    rs.getString("filetype"),
                    rs.getString("upload_date"),
                    rs.getLong("filesize")
            );
            return file;
        }
//        System.out.println("Can not find " + path_file);
        return null;
    }

    public static File getFileById(String id) throws SQLException
    {
        String query = "SELECT * FROM files WHERE id_file = ?";
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
            return new File(
                    rs.getString("id_file"),
                    rs.getString("id_user_upload"),
                    rs.getString("filename"),
                    rs.getString("filepath"),
                    rs.getString("filetype"),
                    rs.getString("upload_date"),
                    rs.getLong("filesize")
            );
        }
        return null;
    }

    public static User getUserUploadByFileId(String id_file)
    {
        User user_upload = null;
        String query = "SELECT u.* FROM users u, files f WHERE u.id = f.id_user_upload AND f.id_file = ?";
        PreparedStatement ps;
        try
        {
            ps = connection.prepareStatement(query);
            ps.setString(1, id_file);
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

    public static void removeFile(java.io.File file_remove)
    {
        String query = "DELETE FROM `files` WHERE filepath = ?";
        PreparedStatement ps;
        try
        {
            ps = connection.prepareStatement(query);
            ps.setString(1, file_remove.getAbsolutePath());
            ps.executeUpdate();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static File getFileShared(String email, String file_name)
    {
        String query = "SELECT f.* FROM files f, users u WHERE f.id_user_upload = u.id && u.email = ? && f.filename = ?";
        PreparedStatement ps;
        try
        {
            ps = connection.prepareStatement(query);
            ps.setString(1, email);
            ps.setString(2, file_name);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
            {
                return new File(
                        rs.getString("id_file"),
                        rs.getString("id_user_upload"),
                        rs.getString("filename"),
                        rs.getString("filepath"),
                        rs.getString("filetype"),
                        rs.getString("upload_date"),
                        rs.getLong("filesize")
                );
            }
            return null;
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }
}
