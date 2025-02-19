package org.example.Model;

import org.example.Controller.NotifyController;

public class Notify
{
    private String id_notify;
    private String id_user;
    private String id_user_interacted;
    private String id_directory;
    private String id_file;
    private NotifyController.Action action;
    private String notify_time;

    public String getId_notify()
    {
        return id_notify;
    }

    public void setId_notify(String id_notify)
    {
        this.id_notify = id_notify;
    }

    public String getId_user()
    {
        return id_user;
    }

    public void setId_user(String id_user)
    {
        this.id_user = id_user;
    }

    public String getId_user_interacted()
    {
        return id_user_interacted;
    }

    public void setId_user_interacted(String id_user_interacted)
    {
        this.id_user_interacted = id_user_interacted;
    }

    public String getId_directory()
    {
        return id_directory;
    }

    public void setId_directory(String id_directory)
    {
        this.id_directory = id_directory;
    }

    public String getId_file()
    {
        return id_file;
    }

    public void setId_file(String id_file)
    {
        this.id_file = id_file;
    }

    public NotifyController.Action getAction()
    {
        return action;
    }

    public void setAction(NotifyController.Action action)
    {
        this.action = action;
    }

    public String getNotify_time()
    {
        return notify_time;
    }

    public void setNotify_time(String notify_time)
    {
        this.notify_time = notify_time;
    }

    public Notify(String id_notify, String id_user, String id_user_interacted, String id_directory, String id_file, NotifyController.Action action, String notify_time)
    {
        this.id_notify = id_notify;
        this.id_user = id_user;
        this.id_user_interacted = id_user_interacted;
        this.id_directory = id_directory;
        this.id_file = id_file;
        this.action = action;
        this.notify_time = notify_time;
    }


}
