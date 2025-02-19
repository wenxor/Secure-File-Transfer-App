package org.example.Model;

public class Permission
{


    public String getId_permission()
    {
        return id_permission;
    }

    public void setId_permission(String id_permission)
    {
        this.id_permission = id_permission;
    }

    private String id_permission;
    private String id_file;
    private String id_directory;
    private String id_user;
    private boolean read;
    private boolean write;

    public String getId_file()
    {
        return id_file;
    }

    public void setId_file(String id_file)
    {
        this.id_file = id_file;
    }

    public String getId_directory()
    {
        return id_directory;
    }

    public void setId_directory(String id_directory)
    {
        this.id_directory = id_directory;
    }

    public String getId_user()
    {
        return id_user;
    }

    public void setId_user(String id_user)
    {
        this.id_user = id_user;
    }

    public boolean isRead()
    {
        return read;
    }

    public void setRead(boolean read)
    {
        this.read = read;
    }

    public boolean isWrite()
    {
        return write;
    }

    public void setWrite(boolean write)
    {
        this.write = write;
    }


    public Permission(String id_permission, String id_file, String id_directory, String id_user, boolean read, boolean write)
    {
        this.id_permission = id_permission;
        this.id_file = id_file;
        this.id_directory = id_directory;
        this.id_user = id_user;
        this.read = read;
        this.write = write;
    }


}
