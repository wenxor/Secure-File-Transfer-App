package org.example.Model;

public class Directory
{
    private String id_directory;
    private String id_user;
    private String path_directory;
    private String name_directory;
    private String created_date;

    public Directory(String id_directory, String id_user, String path_directory, String name_directory, String created_date)
    {
        this.id_directory = id_directory;
        this.id_user = id_user;
        this.path_directory = path_directory;
        this.name_directory = name_directory;
        this.created_date = created_date;
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

    public String getPath_directory()
    {
        return path_directory;
    }

    public void setPath_directory(String path_directory)
    {
        this.path_directory = path_directory;
    }

    public String getName_directory()
    {
        return name_directory;
    }

    public void setName_directory(String name_directory)
    {
        this.name_directory = name_directory;
    }

    public String getCreated_date()
    {
        return created_date;
    }

    public void setCreated_date(String created_date)
    {
        this.created_date = created_date;
    }
}
