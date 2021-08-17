package forge.adventure.util;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Json;
import forge.adventure.data.ResData;
import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

public class Res {
    public static Res CurrentRes;
    private final String Prefix;
    private final HashMap<String, FileHandle> Cache = new HashMap<String, FileHandle>();
    private final HashMap<String, TextureAtlas> atlasCache = new HashMap<>();
    private final ResData configData;
    private String Lang = "en-us";
    private String plane = "";

    public Res(String plane) {
        this.plane = plane;
        CurrentRes = this;
        Prefix = GuiBase.getInterface().getAssetsDir() + "/res/adventure/" + plane + "/";
        if (FModel.getPreferences() != null)
            Lang = FModel.getPreferences().getPref(ForgePreferences.FPref.UI_LANGUAGE);
        configData = new Json().fromJson(ResData.class, new FileHandle(Prefix + "config.json"));

    }

    public ResData GetConfigData() {
        return configData;
    }

    public String GetPrefix() {
        return Prefix;
    }

    public String GetFilePath(String path) {
        return Prefix + path;
    }

    public FileHandle GetFile(String path) {
        String fullPath = Prefix + path;
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


    public String GetPlane() {
        return plane;
    }

    public Deck[] starterDecks() {

        Deck[] deck = new Deck[configData.starterDecks.size];
        for (int i = 0; i < configData.starterDecks.size; i++) {
            deck[i] = DeckSerializer.fromFile(new File(Res.CurrentRes.GetFilePath(configData.starterDecks.get(i))));
        }
        return deck;
    }

    public TextureAtlas getAtlas(String spriteAtlas) {
        if (!atlasCache.containsKey(spriteAtlas)) {
            atlasCache.put(spriteAtlas, new TextureAtlas(GetFile(spriteAtlas)));
        }
        return atlasCache.get(spriteAtlas);
    }
}
