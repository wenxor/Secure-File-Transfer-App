package org.example;

import com.google.gson.Gson;
import org.example.Controller.*;
import org.example.Model.Directory;
import org.example.Model.Notify;
import org.example.Model.Permission;
import org.example.Model.User;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ControlThread extends Thread
{
    BufferedReader in;
    PrintWriter out;
    Socket clientSocket;
    String raw_cmd;
    int indexCommander;
    User user_login;
    static String UPLOAD_DIRECTORY = "upload";
    public String WORKING_DIRECTORY = UPLOAD_DIRECTORY;
    public String USER_UPLOAD_DIRECTORY = UPLOAD_DIRECTORY;
    final int DATA_PORT = 2000;
    public static boolean isPaused = false;
    public static boolean anonymous_mode = false;
    public static long UPLOAD_MAX_SIZE = (long) (10 * Math.pow(1024, 2));
    public static final Object pauseLock = new Object();
    private static final String AES_ALGO = "AES";
    private final String secret_key = "tnqa_osint_ninja";
    protected final SecretKeySpec secretKeySpec = new SecretKeySpec(secret_key.getBytes(), AES_ALGO);
    protected final Cipher encryptionCipher = Cipher.getInstance(AES_ALGO);
    protected final Cipher decryptionCipher = Cipher.getInstance(AES_ALGO);

    public ControlThread(Socket clientSocket) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException
    {
        this.clientSocket = clientSocket;
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        encryptionCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        decryptionCipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
    }

    @Override
    public void run()
    {
        try
        {
            while (true)
            {
                raw_cmd = in.readLine();
                if (raw_cmd == null)
                {
                    clientSocket.close();
                    break;
                }
                indexCommander = (raw_cmd + " ").indexOf(" ");
                String commander = raw_cmd.substring(0, indexCommander).toUpperCase();
                startingProgramByCommander(commander);
            }
        } catch (IOException | SQLException | IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException |
                 NoSuchAlgorithmException | InvalidKeyException e)
        {
            System.out.println(e.getMessage());
        } finally
        {
            try
            {
                clientSocket.close();
            } catch (IOException e)
            {
                System.out.println(e.getMessage());
            }
        }
    }

    private void startingProgramByCommander(String commander) throws SQLException, IOException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException
    {
        switch (commander)
        {
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
                activateEmail();
                break;
            case "LS":
                listFileAndDirectory();
                break;
            case "MKDIR":
                createDirectory();
                break;
            case "UP":
                uploadFile();
                break;
            case "GET":
                downloadFile();
                break;
            case "UPTO":
                uploadToDirectoryShared();
                break;
            case "GETFS":
                downloadFileShared();
                break;
            case "SHR":
                shareFile();
                break;
            case "LSHR":
                listFileAndDirectoryReceived();
                break;
            case "RM":
                removeFileOrDirectory();
                break;
            case "CD":
                moveToDirectory();
                break;
            case "PD":
                pauseDownload();
                break;
            case "RD":
                resumeDownload();
                break;
            case "ANONMODE":
                switchingAnonymousMode();
                break;
            case "NOTI":
                listNotifications();
                break;
            default:
//                System.out.println("Wrong command!");
                break;
        }
    }

    private void listFileAndDirectoryReceived() throws SQLException
    {
        if (user_login == null)
        {
            out.println("Login first!");
            return;
        }
        out.println("-- Received --");
        Gson gson = new Gson();
        ArrayList<org.example.Model.File> files_received = PermissionController.getAllFileReceived(user_login.getId());
        ArrayList<Directory> directories_received = PermissionController.getAllDirectoryReceived(user_login.getId());
        String[] list_file_share = new String[files_received.size()];
        String[] list_dir_share = new String[directories_received.size()];
        for (int i = 0; i < list_file_share.length; i++)
        {
            User user_shared = FileController.getUserUploadByFileId(files_received.get(0).getId_file());
            list_file_share[i] = "F:\t" + files_received.get(i).getFilename() + "\tFROM: " + user_shared.getEmail();
        }
        for (int i = 0; i < list_dir_share.length; i++)
        {
            User user_shared = DirectoryController.getUserUploadByDirectoryId(directories_received.get(0).getId_directory());
            list_dir_share[i] = "D:\t" + directories_received.get(i).getName_directory() + "\tFROM: " + user_shared.getEmail();
        }
        out.println(gson.toJson(list_file_share));
        out.println(gson.toJson(list_dir_share));
    }

    private void switchingAnonymousMode()
    {
        if (user_login == null)
        {
            out.println("Login first!");
            return;
        }
        if (raw_cmd.length() == 8)
        {
            out.println("Usage: anonmode ON|OFF");
            return;
        }
        if (!user_login.isAnonymous())
        {
            out.println("Can not use Anonymous mode!");
            return;
        }
        String mode = raw_cmd.substring(indexCommander + 1).trim();
        if (mode.equalsIgnoreCase("ON"))
        {
            anonymous_mode = true;
            out.println("Anonymous mode: ON");
        } else if (mode.equalsIgnoreCase("OFF"))
        {
            anonymous_mode = false;
            out.println("Anonymous mode: OFF");
        }
    }

    private void listNotifications() throws SQLException
    {
        if (user_login == null)
        {
            out.println("Login first!");
            return;
        }
        out.println("-- Notification --");
        ArrayList<Notify> list_notifications = NotifyController.getUserNotifications(user_login.getId());
        String[] notify_message = new String[list_notifications.size()];
        for (int i = 0; i < list_notifications.size(); i++)
        {
            String email_user_interacted = Objects.requireNonNull(AccountController.getUserById(list_notifications.get(i).getId_user_interacted())).getEmail();
            String file_name = "";
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            LocalDateTime dateTime = LocalDateTime.parse(list_notifications.get(i).getNotify_time(), dateTimeFormatter);
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("HH:mm:ss - dd/MM/yyyy");
            String notify_time = dateTime.format(outputFormatter);
            if (list_notifications.get(i).getId_file().isEmpty())
            {
                file_name = "'" + Objects.requireNonNull(DirectoryController.getDirectoryById(list_notifications.get(i).getId_directory())).getName_directory();
            } else if (list_notifications.get(i).getId_directory().isEmpty())
            {
                file_name = "'" + Objects.requireNonNull(FileController.getFileById(list_notifications.get(i).getId_file())).getFilename();
            } else if (!(list_notifications.get(i).getId_directory().isEmpty() && list_notifications.get(i).getId_file().isEmpty()))
            {
                file_name = "'" + Objects.requireNonNull(FileController.getFileById(list_notifications.get(i).getId_file())).getFilename()
                        + "' to '" + Objects.requireNonNull(DirectoryController.getDirectoryById(list_notifications.get(i).getId_directory())).getName_directory() + "' at ";
            }
            NotifyController.Action action = list_notifications.get(i).getAction();
            if (action == NotifyController.Action.SHARE)
            {
                notify_message[i] = email_user_interacted + " shared " + file_name + "' to you at " + notify_time;
            } else if (action == NotifyController.Action.UPLOAD)
            {
                notify_message[i] = email_user_interacted + " uploaded " + file_name + notify_time;
            } else if (action == NotifyController.Action.CREATE_DIRECTORY)
            {
                notify_message[i] = email_user_interacted + " created a directory";
            } else if (action == NotifyController.Action.REMOVE)
            {
                notify_message[i] = email_user_interacted + " removed" + file_name;
            }
        }
        Gson gson = new Gson();
        out.println(gson.toJson(notify_message));
    }

    private void shareFile() throws SQLException
    {
        if (user_login == null)
        {
            out.println("Login first!");
            return;
        }
        if (!AccountController.isEmailActivated(user_login.getEmail()))
        {
            out.println("You need to activated your email first!");
            return;
        }
        if (!(raw_cmd.contains("-e") && raw_cmd.contains("-p") && (raw_cmd.contains("-f") || raw_cmd.contains("-d"))))
        {
            out.println("Usage: shr -e <email-receive> -p <WRITE|READ|FULL> -f|-d <file|directory-name>");
            return;
        }
        String raw_parameters = raw_cmd.substring(indexCommander + 1);
        String[] share_parts = raw_parameters.split("-");
        String email_user = share_parts[1].substring(share_parts[1].indexOf("e") + 2).trim();
        String permission = share_parts[2].substring(share_parts[2].indexOf("p") + 2).trim();
        String file_sharing = "";
        if (share_parts[3].charAt(0) == 'f')
        {
            file_sharing = share_parts[3].substring(share_parts[3].indexOf("f") + 2).trim();
        } else if (share_parts[3].charAt(0) == 'd')
        {
            file_sharing = share_parts[3].substring(share_parts[3].indexOf("d") + 2).trim();
        }
        File file_shr = new File(WORKING_DIRECTORY + File.separator + file_sharing);
        if (!file_shr.exists())
        {
            out.println("Sharing failed!");
            return;
        }
        if (email_user.isEmpty() || permission.isEmpty() || file_sharing.isEmpty())
        {
            out.println("Usage: shr -e <email-receive> -p <WRITE|READ|FULL> -f|-d <file|directory-name>");
            return;
        }
        Notify notify = null;
        User user_be_interacted = AccountController.getUserByEmail(email_user);
        if (user_be_interacted == null)
        {
            out.println("Sharing failed!");
            return;
        }
        if (file_shr.isFile())
        {
            if (PermissionController.sharingWithPermission(email_user, permission, file_shr.getAbsolutePath(), "f"))
            {
                notify = new Notify(UUID.randomUUID().toString(), user_be_interacted.getId(), user_login.getId(), "", Objects.requireNonNull(FileController.getFileByPath(file_shr.getAbsolutePath())).getId_file(), NotifyController.Action.SHARE, LocalDateTime.now().toString());
                out.println("Sharing '" + file_sharing + "'" + " to " + email_user);
            } else
            {
                out.println("Sharing failed!");
            }
        } else if (file_shr.isDirectory())
        {
            if (PermissionController.sharingWithPermission(email_user, permission, file_shr.getAbsolutePath(), "d"))
            {
                notify = new Notify(UUID.randomUUID().toString(), user_be_interacted.getId(), user_login.getId(), Objects.requireNonNull(DirectoryController.getDirectoryByPath(file_shr.getAbsolutePath())).getId_directory(), "", NotifyController.Action.SHARE, LocalDateTime.now().toString());
                out.println("Sharing '" + file_sharing + "'" + " to " + email_user);
            } else
            {
                out.println("Sharing failed!");
            }
        }
        NotifyController.createNotify(Objects.requireNonNull(notify));
    }

    private void pauseDownload()
    {
        synchronized (pauseLock)
        {
            isPaused = true;
        }
    }

    private void resumeDownload()
    {
        synchronized (pauseLock)
        {
            isPaused = false;
            pauseLock.notifyAll();
        }
    }

    private void moveToDirectory() throws IOException
    {
        if (user_login == null)
        {
            out.println("Login first!");
            return;
        }
        if (raw_cmd.length() == 2)
        {
            WORKING_DIRECTORY = UPLOAD_DIRECTORY + File.separator + user_login.getUsername();
            out.println("You're in home");
            return;
        }
        String destination = raw_cmd.substring(indexCommander + 1);
        File newDir = new File(WORKING_DIRECTORY, destination).getCanonicalFile();
        File userRootDir = new File(UPLOAD_DIRECTORY + File.separator + user_login.getUsername()).getCanonicalFile();
        if (!newDir.getPath().startsWith(userRootDir.getPath()))
        {
            WORKING_DIRECTORY = UPLOAD_DIRECTORY + File.separator + user_login.getUsername();
            out.println("You're in home");
            return;
        }
        if (newDir.exists() && newDir.isDirectory())
        {
            WORKING_DIRECTORY = newDir.getPath();
            out.println("You're in " + destination);
        } else
        {
            out.println("Not a valid directory!");
        }
    }

    private void activateEmail() throws SQLException, IOException
    {
        if (user_login == null)
        {
            out.println("Login first!");
            return;
        } else if (AccountController.isEmailActivated(user_login.getEmail()))
        {
            out.println("Email already activated!");
            return;
        }
        out.println("nope");
        Random numberOTP = new Random();
        int otp_random = numberOTP.nextInt(99999);
        String sendOTP = String.format("%05d", otp_random);
        AccountController.sentEmail(user_login.getEmail(), sendOTP);
        String otp_from_client = in.readLine();
        if (AccountController.isActivateAccount(otp_from_client, sendOTP, user_login.getEmail()))
        {
            out.println("Verified OTP successfully!");
            return;
        }
        out.println("Your OTP is not valid!");
    }

    private void downloadFileShared() throws IOException
    {
        if (user_login == null)
        {
            out.println("Login first!");
            return;
        }
        String[] parameters = raw_cmd.split(" ");
        String email = parameters[1].trim();
        String file_name = parameters[2].trim();
        org.example.Model.File file_shared = FileController.getFileShared(email, file_name);
        if (file_shared == null)
        {
            out.println("Download failed!");
            return;
        }
        Permission permission_of_file_shared = PermissionController.getFileSharedPermission(file_shared.getId_file(), user_login.getId());
        if (permission_of_file_shared == null)
        {
            out.println("Download failed!");
            return;
        }
        File file_download = new File(file_shared.getFilepath());
        if (!file_download.exists())
        {
            out.println("'" + file_download.getName() + "'" + " not found!");
            return;
        }
        ServerSocket serverDataSocket = new ServerSocket(DATA_PORT);
        out.println("Downloading ...");
        Socket clientDataSocket = serverDataSocket.accept();
        Thread dataThread = new DataThread(clientDataSocket, file_download, "GET", user_login, null, null);
        dataThread.start();
        serverDataSocket.close();

    }

    private void uploadToDirectoryShared() throws SQLException, IOException
    {
        if (user_login == null)
        {
            out.println("Login first!");
            return;
        }
        String raw_parameters = raw_cmd.substring(indexCommander + 1);
        if (!raw_parameters.contains("-d") || !raw_parameters.contains("-f"))
        {
            out.println("Usage: upto <email> -d <directory-name> -f <file-name>");
            return;
        }
        String[] parameters = raw_parameters.split("-");
        String email = parameters[0].trim();
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))
        {
            out.println("Email not valid!");
            return;
        }
        String directory = parameters[1].substring(parameters[1].indexOf("d") + 1).trim();
        String file = parameters[2].substring(parameters[2].indexOf("f") + 1).trim();
        Directory dir_shared = DirectoryController.getDirectoryShared(email, directory);

        if (dir_shared == null)
        {
            out.println("Upload failed!");
            return;
        }
        Permission permission_dir = PermissionController.getDirectorySharedPermission(dir_shared.getId_directory(), user_login.getId());
        if (permission_dir == null || !permission_dir.isWrite())
        {
            out.println("You do not have permission!");
            return;
        }
        File dir = new File(dir_shared.getPath_directory());
        String file_sz = in.readLine();
        if (file_sz == null)
        {
            return;
        }
        long file_size = Long.parseLong(file_sz);
        User user_sharing = AccountController.getUserById(dir_shared.getId_user());
        if (user_sharing == null)
        {
            out.println("Upload failed!");
            return;
        }

        long user_sharing_current_size = DirectoryController.calculateDirectorySize(new File(UPLOAD_DIRECTORY + File.separator + user_sharing.getUsername()));
        if (file_size > UPLOAD_MAX_SIZE || (file_size > user_sharing.getMax_size() - user_sharing_current_size))
        {
            out.println("Your file is too large to upload!");
            return;
        }

        String new_file_name = getUniqueFileName(file, dir.getAbsolutePath());


        File file_upload = new File(new_file_name);
        ServerSocket serverDataSocket = new ServerSocket(DATA_PORT);
        out.println("Uploading ...");
        Socket clientDataSocket = serverDataSocket.accept();
        Thread dataThread = new DataThread(clientDataSocket, file_upload, "UP", user_login, user_sharing, dir_shared);
        dataThread.start();
        serverDataSocket.close();

    }

    private void removeFileOrDirectory()
    {
        if (user_login == null)
        {
            out.println("Login first!");
            return;
        }
        String remove_path = raw_cmd.substring(indexCommander + 1);
        File remove_file = new File(WORKING_DIRECTORY + File.separator + remove_path);

        if (!remove_file.exists())
        {
            out.println("'" + remove_file.getName() + "'" + " not found!");
            return;
        }
        if (remove_file.delete())
        {
            if (remove_file.isFile())
            {
                FileController.removeFile(remove_file);
            } else
            {
                DirectoryController.removeDirectory(remove_file);
            }
            out.println("Removed " + "'" + remove_file.getName() + "'");
        } else
        {
            out.println("Failed to remove " + "'" + remove_file.getName() + "'");
        }
    }

    private void listFileAndDirectory()
    {
        if (user_login == null)
        {
            out.println("Login first!");
            return;
        }
        if (raw_cmd.length() == 2)
        {
            File currentDir = new File(WORKING_DIRECTORY);
            walk(currentDir, currentDir.getAbsolutePath().length());
        } else
        {
            String directory_path = raw_cmd.substring(indexCommander + 1);
            if (directory_path.isEmpty())
            {
                out.println("The specified path is not valid!");
                return;
            }

            directory_path = WORKING_DIRECTORY + File.separator + directory_path;
            File folder = new File(directory_path);
            if (folder.exists() && folder.isDirectory())
            {
                walk(folder, folder.getAbsolutePath().length());
            } else
            {
                out.println("The specified path is not valid!");
            }
        }
        out.println("END");
    }

    public void walk(File folder, int baseLength)
    {
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null)
        {
            for (File file : listOfFiles)
            {
                String relativePath = file.getAbsolutePath().substring(baseLength + 1);
                if (file.isDirectory())
                {
                    out.println("D:\t" + relativePath);
                } else
                {
                    out.println("F:\t" + relativePath);
                }
            }
        } else
        {
            out.println("The folder is empty or cannot be read.");
        }
    }

    private void downloadFile() throws IOException
    {
        if (user_login == null)
        {
            out.println("Login first!");
            return;
        }
        String file_download_name = raw_cmd.substring(indexCommander + 1);
        File file_download = new File(WORKING_DIRECTORY + File.separator + file_download_name);
        if (!file_download.exists())
        {
            out.println("'" + file_download.getName() + "'" + " not found!");
            return;
        }
        ServerSocket serverDataSocket = new ServerSocket(DATA_PORT);
        out.println("Downloading ...");
        Socket clientDataSocket = serverDataSocket.accept();
        Thread dataThread = new DataThread(clientDataSocket, file_download, "GET", user_login, null, null);
        dataThread.start();
        serverDataSocket.close();
    }

    private void uploadFile() throws IOException
    {
        if (user_login == null)
        {
            out.println("Login first!");
            return;
        }
        File upload_dir = new File(WORKING_DIRECTORY);
        if (!upload_dir.exists())
        {
            upload_dir.mkdirs();
        }
        String file_upload_name = raw_cmd.substring(indexCommander + 1);
        String file_sz = in.readLine();
        if (file_sz == null)
        {
            return;
        }
        long file_size = Long.parseLong(file_sz);
        if ((file_size > UPLOAD_MAX_SIZE) || (file_size > user_login.getMax_size() - DirectoryController.calculateDirectorySize(new File(USER_UPLOAD_DIRECTORY))))
        {
            out.println("Your file is too large to upload!");
            return;
        }
        String new_file_name = getUniqueFileName(file_upload_name, WORKING_DIRECTORY);
        File file_upload = new File(new_file_name);
        ServerSocket serverDataSocket = new ServerSocket(DATA_PORT);
        out.println("Uploading ...");
        Socket clientDataSocket = serverDataSocket.accept();
        Thread dataThread = new DataThread(clientDataSocket, file_upload, "UP", user_login, null, null);
        dataThread.start();
        serverDataSocket.close();
    }

    private void createDirectory() throws SQLException
    {
        if (user_login == null)
        {
            out.println("Login first!");
            return;
        }

        int indexCommander = raw_cmd.indexOf(' ');
        if (indexCommander == -1)
        {
            out.println("usage: 'mkdir <directory-name>'");
            return;
        }

        String directoryName = raw_cmd.substring(indexCommander + 1).trim();
        if (directoryName.isEmpty())
        {
            out.println("usage: 'mkdir <directory-name>'");
            return;
        }
        String uniqueDirectoryName = getUniqueFileName(directoryName, WORKING_DIRECTORY);
        File new_dir = new File(uniqueDirectoryName);
        if (!new_dir.exists())
        {
            if (new_dir.mkdirs())
            {
                Directory dir = new Directory(UUID.randomUUID().toString(), user_login.getId(), new_dir.getAbsolutePath(), new_dir.getName(), LocalDateTime.now().toString());
                DirectoryController.createDirectory(dir);
                out.println("Create directory success!");
            } else
            {
                out.println("Failed to create directory!");
            }
        }
    }

    private void logout()
    {
        if (user_login == null)
        {
            out.println("Login first!");
            return;
        }
        out.println("See you again " + user_login.getUsername() + "!");
        user_login = null;
        WORKING_DIRECTORY = UPLOAD_DIRECTORY;
    }

    private void createPersonalDirectory(User new_user) throws SQLException
    {
        File personalDir = new File(UPLOAD_DIRECTORY + File.separator + new_user.getUsername());
        if (personalDir.mkdirs())
        {
            Directory userDir = new Directory(UUID.randomUUID().toString(), new_user.getId(), personalDir.getAbsolutePath(), personalDir.getName(), LocalDateTime.now().toString());
            DirectoryController.createDirectory(userDir);
        }
    }

    private String getUniqueFileName(String filename, String directory_path)
    {
        File file = new File(directory_path + File.separator + filename);
        if (!file.exists() && !file.isDirectory())
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
        while (file.exists() || file.isDirectory())
        {
            String newFileName = name + "(" + count + ")" + extension;
            file = new File(directory_path + File.separator + newFileName);
            count++;
        }
        return file.getAbsolutePath();
    }

    private void register() throws IOException, SQLException, IllegalBlockSizeException, BadPaddingException
    {
        if (user_login != null)
        {
            out.println("You need to logout first!");
            return;
        }
        out.println("-- REGISTER --");
        Gson gson = new Gson();
        User registerUser = gson.fromJson(in.readLine(), User.class);
        byte[] username_byte = decryptionCipher.doFinal(Base64.getDecoder().decode(registerUser.getUsername()));
        byte[] passwd_byte = decryptionCipher.doFinal(Base64.getDecoder().decode(registerUser.getPassword()));
        String username = new String(username_byte);
        String passwd = new String(passwd_byte);
        if (AccountController.isUserExist(username, passwd))
        {
            out.println("Username or Email existed!");
        } else
        {
            registerUser.setUsername(username);
            registerUser.setPassword(passwd);
            if (AccountController.createUser(registerUser))
            {
                out.println("Register success!");
                createPersonalDirectory(registerUser);
            } else
            {
                out.println("Register failed!");
            }
        }
    }

    private void login() throws IOException, SQLException, IllegalBlockSizeException, BadPaddingException
    {
        if (user_login != null)
        {
            out.println("You need to logout first!");
            return;
        }
        out.println("-- LOGIN --");
        String username = in.readLine();
        String passwd = in.readLine();
        byte[] decrypted_username = decryptionCipher.doFinal(Base64.getDecoder().decode(username));
        byte[] decrypted_passwd = decryptionCipher.doFinal(Base64.getDecoder().decode(passwd));
        username = new String(decrypted_username);
        passwd = new String(decrypted_passwd);
        user_login = AccountController.loginUser(username, passwd);
        if (user_login != null)
        {
            if (AccountController.isUserBlocked(user_login.getId()))
            {
                out.println("This account has been blocked!");
                return;
            }
            Gson gson = new Gson();
            out.println(gson.toJson(user_login));
            WORKING_DIRECTORY = UPLOAD_DIRECTORY + File.separator + user_login.getUsername();
            File personal_dir = new File(WORKING_DIRECTORY);
            if (!personal_dir.exists())
            {
                createPersonalDirectory(user_login);
            }
            USER_UPLOAD_DIRECTORY = WORKING_DIRECTORY;
        } else
        {
            out.println("Login failed!");
        }
    }
}
