package forge.adventure.util;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
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
    private Skin SelectedSkin=null;
    private HashMap<String,FileHandle> Cache=new HashMap<String,FileHandle>();
    public Res(String plane) {
        CurrentRes=this;
        Prefix= GuiBase.getInterface().getAssetsDir()+"/res/adventure/"+plane+"/";
        if(FModel.getPreferences()!=null)
            Lang= FModel.getPreferences().getPref(ForgePreferences.FPref.UI_LANGUAGE);

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

    public Skin GetSkin() {

        if(SelectedSkin==null)
            SelectedSkin=new Skin(GetFile("skin/uiskin.json"));
        return SelectedSkin;
    }
}
