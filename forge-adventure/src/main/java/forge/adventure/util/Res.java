package forge.adventure.util;

import com.badlogic.gdx.files.FileHandle;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
public class Res {
    public static Res CurrentRes;
    private String Prefix;
    private String Lang="en-us";
    private String plane="";
    private HashMap<String,FileHandle> Cache=new HashMap<String,FileHandle>();

    public String GetPrefix(){return Prefix;}
    public Res(String plane) {
        this.plane=plane;
        CurrentRes=this;
        Prefix= GuiBase.getInterface().getAssetsDir()+"/res/adventure/"+plane+"/";
        if(FModel.getPreferences()!=null)
            Lang= FModel.getPreferences().getPref(ForgePreferences.FPref.UI_LANGUAGE);

    }
    public String GetFilePath(String path)
    {
        return Prefix+path;
    }
    public FileHandle GetFile(String path)
    {
        String fullPath=Prefix+path;
        if(!Cache.containsKey(fullPath))
        {

            String fileName = fullPath.replaceFirst("[.][^.]+$", "");
            String ext= fullPath.substring(fullPath.lastIndexOf('.'));
            String langFile=fileName+"-"+Lang+ext;
            if(Files.exists(Paths.get(langFile) ))
            {
                Cache.put(fullPath,new FileHandle(langFile));
            }
            else
            {
                Cache.put(fullPath,new FileHandle(fullPath));
            }
        }
        return Cache.get(fullPath);
    }


    public String GetPlane() {
        return plane;
    }
}
