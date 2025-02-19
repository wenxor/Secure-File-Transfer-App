package org.example.Client;

import com.google.gson.Gson;
import org.example.Model.User;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Scanner;
import java.util.UUID;

public class Client1
{
    static BufferedReader in;
    static PrintWriter out;
    static Scanner sc;
    static Socket controlSocket;
    static User user_login;
    static String commander;
    static final int CONTROL_PORT = 2100;
    static final int DATA_PORT = 2000;
    static final String DOWNLOAD_DIRECTORY = "download";
    static final String SERVER_NAME = "localhost";
    static boolean isPaused = false;
    static final Object pauseLock = new Object();
    private static final String AES_ALGO = "AES";
    private static final String secret_key = "tnqa_osint_ninja";
    protected static final SecretKeySpec secretKeySpec = new SecretKeySpec(secret_key.getBytes(), AES_ALGO);
    protected static Cipher cipher;

    public static void main(String[] args)
    {
        try
        {
            controlSocket = new Socket("localhost", CONTROL_PORT);
            in = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
            out = new PrintWriter(controlSocket.getOutputStream(), true);
            sc = new Scanner(System.in);
            cipher = Cipher.getInstance(AES_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            System.out.println("---- WELCOME TO FILTRA SERVER ----");
            startingProgram();
        } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException |
                 IllegalBlockSizeException | BadPaddingException e)
        {
            System.out.println(e.getMessage());
        }
    }

    private static void startingProgram() throws IOException, IllegalBlockSizeException, BadPaddingException
    {
        while (true)
        {
            System.out.print("> ");
            String raw_cmd = sc.nextLine().trim();
            sendCommandToServer(raw_cmd);
            if (commander.equals("QUIT") || controlSocket.isClosed())
            {
                return;
            }
        }

    }

    private static void sendCommandToServer(String raw_command) throws IOException, IllegalBlockSizeException, BadPaddingException
    {
        commander = raw_command.substring(0, (raw_command + " ").indexOf(" ")).toUpperCase();
        switch (commander)
        {
            case "PD":
                pauseDownload();
                break;
            case "RD":
                resumeDownload();
                break;
            case "PU":
                pauseUpload();
                break;
            case "RU":
                resumeUpload();
                break;
            case "LOG":
                login();
                break;
            case "REG":
                register();
                break;
            case "OUT":
                logout();
                break;
            case "OTP":
                activeEmail();
                break;
            case "INFO":
                showProfile();
                break;
            case "HELP":
                showHelp();
                break;
            case "QUIT":
                quitServer();
                break;
            case "LSHR":
                listFileAndDirectoryReceived();
                break;
            case "NOTI":
                listNotifications();
                break;
            case "SHR":
                shareFile(raw_command);
                break;
            case "CD":
                moveToDirectory(raw_command);
                break;
            case "LS":
                listFileAndDirectory(raw_command);
                break;
            case "MKDIR":
                makeDirectory(raw_command);
                break;
            case "RM":
                removeFileOrDirectory(raw_command);
                break;
            case "ANONMODE":
                switchingAnonymousMode(raw_command);
                break;
            case "UP":
                new Thread(() ->
                {
                    try
                    {
                        uploadFile(raw_command);
                    } catch (IOException e)
                    {
                        throw new RuntimeException(e);
                    }
                }).start();
                break;
            case "GET":
                new Thread(() ->
                {
                    try
                    {
                        downloadFile(raw_command);
                    } catch (IOException e)
                    {
                        throw new RuntimeException(e);
                    }
                }).start();
                break;
            case "UPTO":
                new Thread(() ->
                {
                    try
                    {
                        uploadToDirectoryShared(raw_command);
                    } catch (IOException e)
                    {
                        throw new RuntimeException(e);
                    }
                }).start();
                break;
            case "GETFS":
                new Thread(() ->
                {
                    try
                    {
                        downloadFileShared(raw_command);
                    } catch (IOException e)
                    {
                        throw new RuntimeException(e);
                    }
                }).start();
                break;
            default:
                System.out.println("Type 'help'");
                break;
        }
    }

    private static void showHelp()
    {
        //write some help command here
        System.out.println("reg - register new account");
        System.out.println("log - login to your account");
        System.out.println("info - show your information");
        System.out.println("otp - verify your email");
        System.out.println("ls - show file on server");
        System.out.println("get - download file from server");
        System.out.println("pd - pause download");
        System.out.println("rd - resume download");
        System.out.println("up - upload file to server");
        System.out.println("pu - pause upload");
        System.out.println("ru - resume upload");
        System.out.println("anonmode - switching anonymous mode");
        System.out.println("shr - sharing your file or directory to another user");
        System.out.println("getfs - download the file you received from another user");
        System.out.println("upto - upload file to the shared directory");
        System.out.println("noti - show notifications");
        System.out.println("lshr - show file and directory received");
        System.out.println("cd - move to a directory");
        System.out.println("mkdir - create a directory");
        System.out.println("rm - remove file or directory");
        System.out.println("out - logout");
        System.out.println("quit - quit from the server");
        System.out.println("help - see this help");
    }

    private static void listNotifications() throws IOException
    {
        out.println(commander);
        String status = in.readLine();
        System.out.println(status);
        if (status.contains("Login"))
        {
            return;
        }
        Gson gson = new Gson();
        String[] notify_list = gson.fromJson(in.readLine(), String[].class);
        for (String i : notify_list)
        {
            System.out.println(i);
        }
    }

    private static void switchingAnonymousMode(String raw_cmd) throws IOException
    {
        out.println(raw_cmd);
        String status = in.readLine();
        System.out.println(status);
    }

    private static void listFileAndDirectoryReceived() throws IOException
    {
        out.println(commander);
        String status = in.readLine();
        System.out.println(status);
        if (status.contains("Login"))
        {
            return;
        }
        Gson gson = new Gson();
        String[] list_file = gson.fromJson(in.readLine(), String[].class);
        String[] list_dir = gson.fromJson(in.readLine(), String[].class);
        for (String dir : list_dir)
        {
            System.out.println(dir);
        }
        for (String file : list_file)
        {
            System.out.println(file);
        }
    }

    private static void shareFile(String raw_cmd) throws IOException
    {
        out.println(raw_cmd);
        String sharing_status = in.readLine();
        System.out.println(sharing_status);
    }

    private static void pauseDownload()
    {
        System.out.println("Paused download ... (Type 'rd' to resume download)");
        out.println(commander);
    }

    private static void resumeDownload()
    {
        System.out.println("Resume download");
        out.println(commander);
    }

    private static void pauseUpload()
    {
        System.out.println("Paused upload ... (type 'ru' to resume upload)");
        synchronized (pauseLock)
        {
            isPaused = true;
        }
    }

    private static void resumeUpload()
    {
        System.out.println("Resume upload!");
        synchronized (pauseLock)
        {
            isPaused = false;
            pauseLock.notifyAll();
        }
    }

    private static void moveToDirectory(String raw_cmd) throws IOException
    {
        out.println(raw_cmd);
        String cd_status = in.readLine();
        System.out.println(cd_status);
    }

    private static void showProfile()
    {
        if (user_login == null)
        {
            System.out.println("Login first!");
            return;
        }
        System.out.println(user_login);
    }

    private static void activeEmail() throws IOException
    {
        out.println(commander);
        String activate_status = in.readLine();
        if (activate_status.contains("nope"))
        {
            System.out.println("Please check your Email to get the OTP");
            System.out.print("Type your OTP: ");
            String otp = sc.nextLine();
            out.println(otp);
            activate_status = in.readLine();
            System.out.println(activate_status);
            return;
        }
        System.out.println(activate_status);
    }

    private static void removeFileOrDirectory(String raw_cmd) throws IOException
    {
        if (raw_cmd.length() == 2)
        {
            System.out.println("Usage: rm <file-or-directory-name>");
            return;
        }
        out.println(raw_cmd);
        String remove_status = in.readLine();
        System.out.println(remove_status);
    }

    private static void listFileAndDirectory(String raw_cmd) throws IOException
    {
        out.println(raw_cmd);
        String response_LS;
        while ((response_LS = in.readLine()) != null)
        {
            if (response_LS.contains("Login"))
            {
                System.out.println(response_LS);
                break;
            }
            if (response_LS.equals("END"))
            {
                break;
            }
            System.out.println(response_LS);

        }
        out.flush();
    }

    private static void quitServer() throws IOException
    {
        System.out.println("BYE");
        controlSocket.close();
    }

    private static void downloadFileShared(String raw_cmd) throws IOException
    {
        if (raw_cmd.length() == 5)
        {
            System.out.print("\rUsage: getfs <email> <file-name>\n> ");
            return;
        }
        File download_directory = new File(DOWNLOAD_DIRECTORY);
        if (!download_directory.exists())
        {
            download_directory.mkdirs();
        }
        out.println(raw_cmd);
        String[] parameters = raw_cmd.split(" ");
        String file_name = parameters[2].trim();
        String new_file_name = getUniqueFileName(file_name);
        String download_status = in.readLine();
        System.out.print("\rType 'pd' to pause download\nType 'rd' to resume download\n");
        System.out.print("\r" + download_status + "\n\r> ");
        if (download_status.contains("Login") || download_status.contains("not found") || download_status.contains("failed"))
        {
            return;
        }
        try (Socket dataSocket = new Socket(SERVER_NAME, DATA_PORT); BufferedInputStream in = new BufferedInputStream(dataSocket.getInputStream()); BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new_file_name)))
        {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1)
            {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
            dataSocket.close();
            System.out.print("\rDownloaded successful!\nLocation: '" + new_file_name + "'\n> ");
        } catch (IOException e)
        {
            System.out.println(e.getMessage());
        }
    }

    private static void uploadToDirectoryShared(String raw_cmd) throws IOException
    {
        if (raw_cmd.length() == 4)
        {
            System.out.print("\rUsage: upto <email> -d <directory-name> -f <file-name>\n> ");
            return;
        }
        if (!raw_cmd.contains("-d") || !raw_cmd.contains("-f"))
        {
            System.out.print("\rUsage: upto <email> -d <directory-name> -f <file-name>\n> ");
            return;
        }
        String filename = raw_cmd.substring(raw_cmd.indexOf("-f") + 2).trim();
        if (filename.isEmpty())
        {
            System.out.print("\rUsage: upto <email> -d <directory-name> -f <file-name>\n> ");
            return;
        }
        File uploadFile = new File(DOWNLOAD_DIRECTORY + File.separator + filename);
        if (!uploadFile.exists())
        {
            System.out.println(uploadFile.getName() + " not found!");
            return;
        }
        out.println(raw_cmd);
        out.println(uploadFile.length());
        String upload_status = in.readLine();
        System.out.print("\rType 'pu' to pause upload\nType 'ru' to resume upload\n");
        System.out.print("\r" + upload_status + "\n\r> ");
        if (upload_status.contains("Login") || upload_status.contains("large") || upload_status.contains("permission"))
        {
            return;
        }
        File download_dir = new File(DOWNLOAD_DIRECTORY);
        if (!download_dir.exists())
        {
            download_dir.mkdirs();
        }
        long current_byte = 0;
        try (Socket dataSocket = new Socket(SERVER_NAME, DATA_PORT); BufferedInputStream in = new BufferedInputStream(new FileInputStream(uploadFile)); BufferedOutputStream out = new BufferedOutputStream(dataSocket.getOutputStream()))
        {
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1)
            {
                synchronized (pauseLock)
                {
                    current_byte += bytesRead;
                    while (isPaused)
                    {
                        System.out.print("\r < " + Math.floor(((float) current_byte / Math.pow(1024, 2)) / uploadFile.length()) + "/100%" + "\n> ");
                        pauseLock.wait();
                    }
                    out.write(buffer, 0, bytesRead);
                }
            }
            out.flush();
            dataSocket.close();
            System.out.print("\rUploaded successful!\n> ");
        } catch (IOException | InterruptedException e)
        {
            System.out.println(e.getMessage());
        }

    }

    private static void downloadFile(String raw_cmd) throws IOException
    {
        if (raw_cmd.length() == 3)
        {
            System.out.println("Usage: get <file-name>");
            return;
        }
        File download_directory = new File(DOWNLOAD_DIRECTORY);
        if (!download_directory.exists())
        {
            download_directory.mkdirs();
        }
        out.println(raw_cmd);
        String filename_download = raw_cmd.substring(raw_cmd.indexOf(" ") + 1);
        String new_file_name = getUniqueFileName(filename_download);
        String download_status = in.readLine();
        System.out.print("\rType 'pd' to pause download\nType 'rd' to resume download\n");
        System.out.print("\r" + download_status + "\n\r> ");

        if (download_status.contains("Login") || download_status.contains("not found"))
        {
            return;
        }
        try (Socket dataSocket = new Socket(SERVER_NAME, DATA_PORT); BufferedInputStream in = new BufferedInputStream(dataSocket.getInputStream()); BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new_file_name)))
        {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1)
            {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
            dataSocket.close();
            System.out.print("\rDownloaded successful!\nLocation: '" + new_file_name + "'\n> ");
        } catch (IOException e)
        {
            System.out.println(e.getMessage());
        }
    }

    private static String getUniqueFileName(String filename)
    {
        File file = new File(DOWNLOAD_DIRECTORY + File.separator + filename);
        if (!file.exists())
        {
            return file.getAbsolutePath();
        }

        String name = filename;
        String extension = "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1)
        {
            name = filename.substring(0, dotIndex);
            extension = filename.substring(dotIndex);
        }

        int count = 1;
        while (file.exists())
        {
            String new_file_name = name + "(" + count + ")" + extension;
            file = new File(DOWNLOAD_DIRECTORY + File.separator + new_file_name);
            count++;
        }
        return file.getAbsolutePath();
    }

    private static void uploadFile(String raw_cmd) throws IOException
    {

        if (raw_cmd.length() == 2)
        {
            System.out.println("Usage: up <file-name>");
            return;
        }
        String filename = raw_cmd.substring(raw_cmd.indexOf(" ") + 1);
        File uploadFile = new File(DOWNLOAD_DIRECTORY + File.separator + filename);
        if (!uploadFile.exists())
        {
            System.out.println(uploadFile.getName() + " not found!");
            return;
        }

        out.println(raw_cmd);
        out.println(uploadFile.length());
        String upload_status = in.readLine();
        System.out.print("\rType 'pu' to pause upload\nType 'ru' to resume upload\n");
        System.out.print("\r" + upload_status + "\n\r> ");
        if (upload_status.contains("Login") || upload_status.contains("large"))
        {
            return;
        }

        File download_dir = new File(DOWNLOAD_DIRECTORY);
        if (!download_dir.exists())
        {
            download_dir.mkdirs();
        }
        long current_byte = 0;
        try (Socket dataSocket = new Socket(SERVER_NAME, DATA_PORT);
             BufferedInputStream in = new BufferedInputStream(new FileInputStream(uploadFile));
             BufferedOutputStream out = new BufferedOutputStream(dataSocket.getOutputStream()))
        {
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1)
            {
                current_byte += bytesRead;
                synchronized (pauseLock)
                {
                    while (isPaused)
                    {
                        System.out.print("\r < " + Math.floor(((float) current_byte / Math.pow(1024, 2)) / uploadFile.length()) + "/100%" + "\n> ");
                        pauseLock.wait();
                    }
                    out.write(buffer, 0, bytesRead);
                }
            }
            out.flush();
            dataSocket.close();
            System.out.print("\rUploaded successful!\n> ");
        } catch (IOException | InterruptedException e)
        {
            System.out.println(e.getMessage());
        }

    }

    private static void makeDirectory(String raw_cmd) throws IOException
    {
        out.println(raw_cmd);
        String mkdir_status = in.readLine();
        System.out.println(mkdir_status);
    }

    private static void logout() throws IOException
    {
        out.println(commander);
        String logout_status = in.readLine();
        System.out.println(logout_status);
    }

    private static void login() throws IOException, IllegalBlockSizeException, BadPaddingException
    {
        out.println(commander);
        String login_status = in.readLine();
        if (login_status.contains("logout"))
        {
            System.out.println(login_status);
            return;
        }
        System.out.print("Username: ");
        String username = sc.nextLine();
        System.out.print("Password: ");
        String passwd = sc.nextLine();
        byte[] enc_username = cipher.doFinal(username.getBytes());
        byte[] enc_passwd = cipher.doFinal(passwd.getBytes());
        out.println(Base64.getEncoder().encodeToString(enc_username));
        out.println(Base64.getEncoder().encodeToString(enc_passwd));
        login_status = in.readLine();
        if (login_status.contains("failed") || login_status.contains("blocked"))
        {
            System.out.println(login_status);
            return;
        }
        Gson gson = new Gson();
        user_login = gson.fromJson(login_status, User.class);
        System.out.println("Login success!");
    }

    private static void register() throws IOException, IllegalBlockSizeException, BadPaddingException
    {
        out.println(commander);
        String fullname, username, email, passwd, rePasswd;
        String register_status = in.readLine();
        if (register_status.contains("logout"))
        {
            System.out.println(register_status);
            return;
        }
        do
        {
            System.out.print("Fullname: ");
            fullname = sc.nextLine();
            System.out.print("Username: ");
            username = sc.nextLine();
            System.out.print("Email: ");
            email = sc.nextLine();
            System.out.print("Password: ");
            passwd = sc.nextLine();
            System.out.print("Retype password: ");
            rePasswd = sc.nextLine();
        } while (!rePasswd.equals(passwd));
        Gson gson = new Gson();
        byte[] username_byte = cipher.doFinal(username.getBytes());
        byte[] passwd_byte = cipher.doFinal(passwd.getBytes());
        String encrypted_username = Base64.getEncoder().encodeToString(username_byte);
        String encrypted_passwd = Base64.getEncoder().encodeToString(passwd_byte);
        User register_user = new User(UUID.randomUUID().toString(), fullname, encrypted_username, email, encrypted_passwd, LocalDateTime.now().toString(), true, false, 0);
        out.println(gson.toJson(register_user));
        register_status = in.readLine();
        System.out.println(register_status);
    }
}


