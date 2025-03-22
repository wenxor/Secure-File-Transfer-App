package org.example.Controller;

import org.example.DB.DatabaseConnector;
import org.example.Model.User;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;

import static org.example.DB.DatabaseConnector.connectToDatabase;

public class AccountController
{
    private static final long DEFAULT_MAX_SIZE = (long) (5 * Math.pow(1024, 3));
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


    public static boolean createUser(User new_user) throws SQLException
    {
        String register_query = "INSERT INTO users (id, username, password, email, fullname, date_created, anonymous, activated, max_size) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps;
        try
        {
            ps = connection.prepareStatement(register_query);
            ps.setString(1, new_user.getId());
            ps.setString(2, new_user.getUsername());
            ps.setString(3, new_user.getPassword());
            ps.setString(4, new_user.getEmail());
            ps.setString(5, new_user.getFullname());
            ps.setString(6, new_user.getDate_created());
            ps.setBoolean(7, new_user.isAnonymous());
            ps.setBoolean(8, new_user.isActivated());
            ps.setLong(9, DEFAULT_MAX_SIZE);

        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        int new_row_user = ps.executeUpdate();
        return new_row_user > 0;
    }

    public static boolean isUserExist(String username, String email) throws SQLException
    {
        String query = "SELECT * FROM users WHERE username = ? OR email = ?";
        try (Connection con = DatabaseConnector.connectToDatabase();
             PreparedStatement ps = con.prepareStatement(query))
        {
            ps.setString(1, username);
            ps.setString(2, email);
            ResultSet rs = ps.executeQuery();

            return rs.next();
        } catch (SQLException e)
        {
            System.out.println(e.getMessage());
            throw e;
        }
    }

    public static User loginUser(String username, String passwd) throws SQLException
    {
        User user_login;
        String login_query = "SELECT * FROM users WHERE username=? AND password=?";
        PreparedStatement ps;
        try
        {
            ps = connection.prepareStatement(login_query);
            ps.setString(1, username);
            ps.setString(2, passwd);
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        ResultSet rs = ps.executeQuery();
        if (rs.next())
        {
            user_login = new User(
                    rs.getString("id"),
                    rs.getString("fullname"),
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("date_created"),
                    rs.getBoolean("anonymous"),
                    rs.getBoolean("activated"),
                    rs.getLong("max_size")
            );
            return user_login;
        }
        return null;
    }

    public static void sentEmail(String email_user, String otp)
    {
        String HOST_NAME = "smtp.gmail.com";
        String SSL_PORT = "587"; //  "587" for TSL
        String APP_EMAIL = "tuanduy1411@gmail.com";
        String APP_PASSWORD = "yxzvrylniyxlojpe";

        //(send email)
        //cài đặt properties để connect
        // Thiết lập properties cho SMTP server
        Properties properties = System.getProperties();
        properties.put("mail.smtp.host", HOST_NAME);
        properties.put("mail.smtp.port", SSL_PORT);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.setProperty("mail.smtp.ssl.protocols", "TLSv1.2");

        properties.put("mail.smtp.ssl.trust", HOST_NAME);

        // Xác thực tài khoản email và password
        Session session = Session.getInstance(properties,
                new javax.mail.Authenticator()
                {
                    protected PasswordAuthentication getPasswordAuthentication()
                    {
                        return new PasswordAuthentication(APP_EMAIL, APP_PASSWORD);
                    }
                });

        //viết tin nhắn
        try
        {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(APP_EMAIL));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(email_user));
            message.setSubject("Your OTP Code");
            message.setText("Your OTP code is: " + otp);
            //Gửi email
            Transport.send(message);
//            System.out.println("Filtra-ftp had sent OTP to your email. Please check your mail");

        } catch (MessagingException mes)
        {
//            System.out.println("Failed to send OTP email. Error: " + mes.getMessage());
        }

    }

    public static boolean isActivateAccount(String otp_from_client, String otp_generated, String email) throws SQLException
    {
        if (otp_from_client.equals(otp_generated))
        {
            try (Connection conn = DatabaseConnector.connectToDatabase())
            {

                String query = "UPDATE users SET activated = ? WHERE email = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query))
                {
                    stmt.setBoolean(1, true);
                    stmt.setString(2, email);
                    stmt.executeUpdate();
                }
            }
            return true;
        }
        return false;
    }

    public static boolean isEmailActivated(String email) throws SQLException
    {

        try (Connection conn = DatabaseConnector.connectToDatabase())
        {
            boolean activated = false;
            String query = "SELECT activated FROM users WHERE email = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query))
            {
                stmt.setString(1, email);
                try (ResultSet rs = stmt.executeQuery())
                {
                    if (rs.next())
                    {
                        activated = rs.getBoolean("activated");
//                        if (activated)
//                        {
//                            System.out.println("User is Activated");
//                            return true;
//                        } else
//                        {
//                            System.out.println("User is not Activated");
//                            return false;
//                        }
                        return activated;
                    } else
                    {
                        return false;
                    }
                }

            }

        }
    }

    public static User getUserByEmail(String email) throws SQLException
    {
        User user;
        String login_query = "SELECT * FROM users WHERE email = ?";
        PreparedStatement ps;
        try
        {
            ps = connection.prepareStatement(login_query);
            ps.setString(1, email);
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        ResultSet rs = ps.executeQuery();
        if (rs.next())
        {
//            System.out.println("Founded user: " + rs.getString("username"));
            user = new User(
                    rs.getString("id"),
                    rs.getString("fullname"),
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("date_created"),
                    rs.getBoolean("anonymous"),
                    rs.getBoolean("activated"),
                    rs.getLong("max_size")
            );
            return user;
        }
        System.out.println("Can not find " + email);
        return null;
    }

    public static ArrayList<User> getAllUser()
    {
        ArrayList<User> list_user = new ArrayList<>();
        String query = "SELECT * FROM users";
        PreparedStatement ps;
        try
        {
            ps = connection.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
            {
                User user = new User(
                        rs.getString("id"),
                        rs.getString("fullname"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("date_created"),
                        rs.getBoolean("anonymous"),
                        rs.getBoolean("activated"),
                        rs.getLong("max_size")
                );
                list_user.add(user);
            }
            return list_user;
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void blockUserById(String id_user) throws SQLException
    {
        String query = "INSERT INTO block_users (id) VALUES (?)";
        PreparedStatement ps;
        try
        {
            ps = connection.prepareStatement(query);
            ps.setString(1, id_user);
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        int new_row_user = ps.executeUpdate();
        if (new_row_user > 0)
        {
            System.out.println("Block user success!");
        } else
            System.out.println("Block user failed!");
    }

    public static void unblockUserById(String id_user)
    {
        String query = "DELETE FROM block_users WHERE (`id` = ?)";
        PreparedStatement ps;
        try
        {
            ps = connection.prepareStatement(query);
            ps.setString(1, id_user);
            ps.executeUpdate();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void blockAnonymousFeatureById(String id, String mode)
    {
        String query = "UPDATE users SET anonymous = ? WHERE id = ?";
        PreparedStatement ps;
        try
        {
            ps = connection.prepareStatement(query);
            if (mode.equalsIgnoreCase("AL"))
            {
                ps.setBoolean(1, true);
            } else if (mode.equalsIgnoreCase("BL"))
            {
                ps.setBoolean(1, false);
            }
            ps.setString(2, id);
            ps.executeUpdate();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }

    }

    public static ArrayList<String> getBlockedUsers()
    {
        ArrayList<String> id_users = new ArrayList<>();
        String query = "SELECT * FROM block_users";
        PreparedStatement ps;
        try
        {
            ps = connection.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
            {
                id_users.add(rs.getString("id"));
            }
            return id_users;
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean isUserBlocked(String id_user)
    {
        String query = "SELECT * FROM block_users WHERE id = ?";
        PreparedStatement ps;
        try
        {
            ps = connection.prepareStatement(query);
            ps.setString(1, id_user);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
            {
                return true;
            }

        } catch (SQLException e)
        {
            System.out.println(e.getMessage());

        }
        return false;
    }

    public static User getUserById(String id) throws SQLException
    {
        User user;
        String query = "SELECT * FROM users WHERE id = ?";
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
            user = new User(
                    rs.getString("id"),
                    rs.getString("fullname"),
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("date_created"),
                    rs.getBoolean("anonymous"),
                    rs.getBoolean("activated"),
                    rs.getLong("max_size")
            );
            return user;
        }
        return null;
    }
}

