package org.example;

import org.example.Controller.AccountController;
import org.example.Controller.DirectoryController;
import org.example.Model.User;

import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;

public class ServerMain
{
    static final int CONTROL_PORT = 2100;
    static ServerSocket ss;

    public static void main(String[] args)
    {

        Thread server_management = new Thread(() ->
        {
            try
            {
                serverManagementProgram();
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });
        server_management.start();
        try
        {
            ss = new ServerSocket(CONTROL_PORT);
//            System.out.println("Server is running now ...");
            while (true)
            {
                Socket clientSocket = ss.accept();
                Thread client = new ControlThread(clientSocket);
                client.start();
            }

        } catch (IOException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e)
        {
            throw new RuntimeException(e);
        }

    }

    public static void serverManagementProgram() throws Exception
    {
        Scanner sc = new Scanner(System.in);
        while (true)
        {
            System.out.print("FTP> ");
            String cmd = sc.nextLine();
            executeProgram(cmd);
        }
    }

    public static void executeProgram(String cmd) throws Exception
    {
        String commander = cmd.substring(0, (cmd + " ").indexOf(" ")).trim().toUpperCase();
        switch (commander)
        {
            case "USERS":
                showAllUser();
                break;
            case "BLOCK":
                blockUser(cmd);
                break;
            case "UNBLOCK":
                unBlockUser(cmd);
                break;
            case "LSBLK":
                listBlockedUsers();
                break;
            case "SETCP":
                setUserDataSize(cmd);
                break;
            case "ANON":
                configAnonymousMode(cmd);
                break;
            case "MAXUP":
                setMaxSizeUpload(cmd);
                break;
            case "CLEAR":
                clearConsole();
                break;
            case "HELP":
                showHelp();
                break;
            default:
                System.out.println("\n");
                break;
        }
    }

    private static void showHelp()
    {
        System.out.println("USERS - show all user");
        System.out.println("BLOCK - block a user");
        System.out.println("UNBLOCK - unblock a user");
        System.out.println("LSBLK - list blocked users");
        System.out.println("ANON - config anonymous mode for user");
        System.out.println("SETCP - set capacity size for a user");
        System.out.println("MAXUP - set max size upload file");
        System.out.println("CLEAR - clear the console screen");
        System.out.println("HELP - show this help");
    }

    public static void clearConsole()
    {
        System.out.print("\033\143");
    }

    private static void listBlockedUsers()
    {
        for (String id : Objects.requireNonNull(AccountController.getBlockedUsers()))
        {
            System.out.println(id);
        }
    }

    private static void setMaxSizeUpload(String cmd)
    {
        if (cmd.length() == 5)
        {
            System.out.println("Usage: maxup <number-of-MegaByte>");
            return;
        }
        ControlThread.UPLOAD_MAX_SIZE = (long) (Long.parseLong(cmd.substring(cmd.indexOf(" ") + 1).trim()) * Math.pow(1024, 2));
        System.out.println("Max size upload change to " + ControlThread.UPLOAD_MAX_SIZE / Math.pow(1024, 2) + "MB");
    }

    private static void showAllUser()
    {
        ArrayList<User> users = AccountController.getAllUser();
        int maxIdLength = "ID".length();
        int maxFullnameLength = "Full name".length();
        int maxUsernameLength = "Username".length();
        int maxEmailLength = "Email".length();
        int maxMaxSizeLength = "Capacity".length();
        int maxDateCreatedLength = "Date created".length();
        int maxAnonymousLength = "Anonymous allowed".length();
        int maxActivatedLength = "Email activated".length();

        for (User user : users)
        {
            maxIdLength = Math.max(maxIdLength, user.getId().length());
            maxFullnameLength = Math.max(maxFullnameLength, user.getFullname().length());
            maxUsernameLength = Math.max(maxUsernameLength, user.getUsername().length());
            maxEmailLength = Math.max(maxEmailLength, user.getEmail().length());
            maxMaxSizeLength = Math.max(maxMaxSizeLength, String.valueOf(user.getMax_size()).length());
            maxDateCreatedLength = Math.max(maxDateCreatedLength, user.getDate_created().length());
        }

        String format = "| %-" + maxIdLength + "s | %-" + maxFullnameLength + "s | %-" + maxUsernameLength + "s | %-" + maxEmailLength + "s | %-" + maxMaxSizeLength + "s | %-" + maxDateCreatedLength + "s | %-" + maxAnonymousLength + "s | %-" + maxActivatedLength + "s |%n";

        System.out.printf(format, "ID", "Full name", "Username", "Email", "Capacity", "Date created", "Anonymous allowed", "Email activated");

        for (int i = 0; i < maxIdLength + maxFullnameLength + maxUsernameLength + maxEmailLength + maxMaxSizeLength + maxDateCreatedLength + maxAnonymousLength + maxActivatedLength + 26; i++)
        {
            System.out.print("-");
        }
        System.out.println();

        for (User user : users)
        {
            System.out.printf(format, user.getId(), user.getFullname(), user.getUsername(), user.getEmail(), Math.round(user.getMax_size() / Math.pow(1024, 2)) + "MB", user.getDate_created(), user.isAnonymous() ? "ALLOWED" : "BLOCKED", user.isActivated() ? "ACTIVATED" : "NON-ACTIVE");
        }
    }

    private static void configAnonymousMode(String cmd)
    {
        if (cmd.length() == 4)
        {
            System.out.println("Usage: anon <id-user> AL|BL");
            return;
        }
        String[] parameters = cmd.split(" ");
        if (parameters.length != 3)
        {
            System.out.println("Usage: anon <id-user> AL|BL");
            return;
        }
        String id_user = parameters[1].trim();
        String mode = parameters[2].trim();
        if (id_user.isEmpty() || mode.isEmpty())
        {
            System.out.println("Usage: anon <id-user> AL|BL");
            return;
        }
        AccountController.blockAnonymousFeatureById(id_user, mode);
        if (mode.equalsIgnoreCase("AL"))
        {
            System.out.println(id_user + " Anonymous allowed: ALLOW");
        } else if (mode.equalsIgnoreCase("BL"))
        {
            System.out.println(id_user + " Anonymous allowed: BLOCK");
        }
    }

    private static void blockUser(String cmd) throws SQLException
    {
        if (cmd.length() == 5)
        {
            System.out.println("Usage: block <id-user>");
            return;
        }
        String id_user = cmd.substring(cmd.indexOf(" ") + 1).trim();
        if (id_user.isEmpty())
        {
            System.out.println("Usage: block <id-user>");
            return;
        }
        if (AccountController.isUserBlocked(id_user))
        {
            System.out.println(id_user + " had been blocked!");
        } else
        {
            AccountController.blockUserById(id_user);
        }
    }

    private static void unBlockUser(String cmd)
    {
        if (cmd.length() == 7)
        {
            System.out.println("Usage: unblock <id-user>");
            return;
        }
        String id_user = cmd.substring(cmd.indexOf(" ") + 1).trim();
        if (id_user.isEmpty())
        {
            System.out.println("Usage: unblock <id-user>");
            return;
        }
        if (AccountController.isUserBlocked(id_user))
        {
            AccountController.unblockUserById(id_user);
            System.out.println("Unblocked user " + id_user);
        } else
        {
            System.out.println("User was not blocked!");
        }
    }

    private static void setUserDataSize(String cmd) throws SQLException
    {
        String str_parameters = cmd.substring((cmd + " ").indexOf(" ")).trim();
        String[] parameters = str_parameters.split(" ");
        if (parameters[0].isEmpty() || parameters[1].isEmpty())
        {
            System.out.println("Usage: SETCP <id-user> <number-of-MegaByte>");
            System.out.println("ex: SETCP a1234-x1y2z3-abcxyz789 500");
            return;
        }
        User user = AccountController.getUserById(parameters[0]);

        if (user == null)
        {
            System.out.println("Can not find " + parameters[0]);
            return;
        }
        File user_dir = new File(ControlThread.UPLOAD_DIRECTORY + File.separator + user.getUsername());
        long max_data_size = (long) (Long.parseLong(parameters[1]) * Math.pow(1024, 2));
        long user_current_data = DirectoryController.calculateDirectorySize(user_dir);
        if (max_data_size < user_current_data)
        {
            System.out.println("The current capacity size is " + user_current_data + "MB!");
            return;
        }
        if (DirectoryController.setUserDataById(parameters[0], max_data_size))
        {
            System.out.println("Set capacity size for " + parameters[0] + " success!");
        } else
        {
            System.out.println("Failed to set capacity size for " + parameters[0] + "!");
        }
    }

}
