package forge.adventure.util;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import forge.adventure.data.ConfigData;
import forge.adventure.data.SettingData;
import forge.deck.Deck;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgeProfileProperties;
import forge.model.FModel;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 * Main resource class to access files from the selected adventure
 */
public class Config {
    private static Config currentConfig;
    private final String prefix;
    private final HashMap<String, FileHandle> Cache = new HashMap<String, FileHandle>();
    private final HashMap<String, TextureAtlas> atlasCache = new HashMap<>();
    private final ConfigData configData;
    private final String[] adventures;
    private SettingData settingsData;
    private String Lang = "en-us";
    private final String plane;

    static public Config instance()
    {
        if(currentConfig==null)
            currentConfig=new Config();
        return currentConfig;
    }
    private Config() {

        String path= Files.exists(Paths.get("./res"))?"./":"../forge-gui/";
         adventures = new File(path + "/res/adventure").list();
        try
        {
            settingsData = new Json().fromJson(SettingData.class, new FileHandle(ForgeConstants.USER_DIR +  "/adventure/settings.json"));

        }
        catch (Exception e)
        {
            settingsData=new SettingData();
        }
        if(settingsData.plane==null||settingsData.plane.isEmpty())
        {
            if(adventures!=null&&adventures.length>=1)
                settingsData.plane=adventures[0];
        }

        if(settingsData.width==0||settingsData.height==0)
        {
            settingsData.width=1280;
            settingsData.height=720;
        }

        this.plane = settingsData.plane;
        currentConfig = this;

        prefix = path + "/res/adventure/" + plane + "/";
        if (FModel.getPreferences() != null)
            Lang = FModel.getPreferences().getPref(ForgePreferences.FPref.UI_LANGUAGE);
        configData = new Json().fromJson(ConfigData.class, new FileHandle(prefix + "config.json"));

    }

    public ConfigData getConfigData() {
        return configData;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getFilePath(String path) {
        return prefix + path;
    }

    public FileHandle getFile(String path) {
        String fullPath = prefix + path;
        if (!Cache.containsKey(fullPath)) {

            String fileName = fullPath.replaceFirst("[.][^.]+$", "");
            String ext = fullPath.substring(fullPath.lastIndexOf('.'));
            String langFile = fileName + "-" + Lang + ext;
            if (Files.exists(Paths.get(langFile))) {
                Cache.put(fullPath, new FileHandle(langFile));
            } else {
                Cache.put(fullPath, new FileHandle(fullPath));
            }
        }
        return Cache.get(fullPath);
    }


    public String getPlane() {
        return plane;
    }

    public Deck[] starterDecks() {

        Deck[] deck = new Deck[configData.starterDecks.length];
        for (int i = 0; i < configData.starterDecks.length; i++) {
            deck[i] = CardUtil.getDeck(configData.starterDecks[i]);
        }
        return deck;
    }

    public TextureAtlas getAtlas(String spriteAtlas) {
        if (!atlasCache.containsKey(spriteAtlas)) {
            atlasCache.put(spriteAtlas, new TextureAtlas(getFile(spriteAtlas)));
        }
        return atlasCache.get(spriteAtlas);
    }
    public SettingData getSettingData()
    {
        return settingsData;
    }
    public String[] getAllAdventures()
    {
        return adventures;
    }

    public void saveSettings() {

        Json json = new Json(JsonWriter.OutputType.json);
        FileHandle handle = new FileHandle(ForgeProfileProperties.getUserDir() +  "/adventure/settings.json");
        handle.writeString(json.prettyPrint(json.toJson(settingsData, SettingData.class)),false);

    }
}
