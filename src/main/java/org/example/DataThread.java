package org.example;

import org.example.Controller.FileController;
import org.example.Controller.NotifyController;
import org.example.Model.Directory;
import org.example.Model.Notify;
import org.example.Model.User;
import org.example.Model.File;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

public class DataThread extends Thread
{
    Socket dataSocket;
    java.io.File file;
    String command;
    User user_login;
    User user_sharing;
    Directory dir_shared;

    public DataThread(Socket dataSocket, java.io.File file, String command, User user_login, User user_sharing, Directory dir_shared)
    {
        this.dataSocket = dataSocket;
        this.file = file;
        this.command = command.toUpperCase();
        this.user_login = user_login;
        this.user_sharing = user_sharing;
        this.dir_shared = dir_shared;
    }

    @Override
    public void run()
    {
        if (command.equals("GET"))
        {
            downloadFile();
        } else if (command.equals("UP"))
        {
            uploadFile();
        }
    }

    private void downloadFile()
    {
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
             BufferedOutputStream out = new BufferedOutputStream(dataSocket.getOutputStream()))
        {

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1)
            {
                synchronized (ControlThread.pauseLock)
                {
                    while (ControlThread.isPaused)
                    {
                        ControlThread.pauseLock.wait();
                    }
                    out.write(buffer, 0, bytesRead);
                }
            }
            out.flush();
        } catch (IOException | InterruptedException e)
        {
            System.out.println(e.getMessage());
        } finally
        {
            try
            {
                dataSocket.close();
            } catch (IOException e)
            {
                System.out.println(e.getMessage());
            }
        }
    }

    private void uploadFile()
    {
        try (BufferedInputStream in = new BufferedInputStream(dataSocket.getInputStream());
             BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file)))
        {
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1)
            {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
            File file_up = new File(
                    UUID.randomUUID().toString(),
                    user_login.getId(),
                    file.getName(),
                    file.getAbsolutePath(),
                    FileController.detectFileType(file),
                    LocalDateTime.now().toString(),
                    file.length()
            );
            FileController.uploadFile(user_login, file_up);
            if (!ControlThread.anonymous_mode)
            {
                if (user_sharing != null && dir_shared != null)
                {
                    Notify notify = new Notify(
                            UUID.randomUUID().toString(),
                            user_sharing.getId(),
                            user_login.getId(),
                            dir_shared.getId_directory(),
                            file_up.getId_file(),
                            NotifyController.Action.UPLOAD,
                            LocalDateTime.now().toString()
                    );
                    NotifyController.createNotify(notify);
                }
            }

        } catch (IOException | SQLException e)
        {
            System.out.println(e.getMessage());
        } finally
        {
            try
            {
                dataSocket.close();

            } catch (IOException e)
            {
                System.out.println(e.getMessage());
            }
        }
    }


}
