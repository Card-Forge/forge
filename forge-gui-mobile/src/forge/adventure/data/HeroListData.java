package forge.adventure.data;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;
import forge.Forge;
import forge.adventure.util.Config;
import forge.adventure.util.Paths;

/**
 * Data class that will be used to read Json configuration files
 * BiomeData
 * contains the a list of all heroes
 */
public class HeroListData {
    static private HeroListData instance;
    public HeroData[] heroes;
    public String avatar;
    private TextureAtlas avatarAtlas;
    private final ObjectMap<String, Array<Sprite>> avatarSprites = new ObjectMap<>();

    static private HeroListData read() {
        Json json = new Json();
        FileHandle handle = Config.instance().getFile(Paths.HEROES);
        if (handle.exists()) {
            instance = json.fromJson(HeroListData.class, handle);
            instance.avatarAtlas = Config.instance().getAtlas(instance.avatar);

         /*
            instance.avatarSprites.getTextures().first().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
         */
        }
        return instance;
    }

    static public String getHero(int raceIndex, boolean female) {
        if (instance == null)
            instance = read();
        HeroData data = instance.heroes[raceIndex];

        if (female)
            return data.female;
        return data.male;

    }

    public static TextureRegion getAvatar(int heroRace, boolean isFemale, int avatarIndex) {
        if (instance == null)
            instance = read();
        HeroData data = instance.heroes[heroRace];
        Array<Sprite> sprites = instance.avatarSprites.get(isFemale ? data.femaleAvatar : data.maleAvatar);
        if (sprites == null) {
            sprites = instance.avatarAtlas.createSprites(isFemale ? data.femaleAvatar : data.maleAvatar);
            instance.avatarSprites.put(isFemale ? data.femaleAvatar : data.maleAvatar, sprites);
        }
        avatarIndex %= sprites.size;
        if (avatarIndex < 0) {
            avatarIndex += sprites.size;
        }
        return sprites.get(avatarIndex);
    }

    public static Array<String> getRaces() {
        if (instance == null)
            instance = read();
        Array<String> ret = new Array<>();
        for (HeroData hero : instance.heroes) {
            ret.add(Forge.getLocalizer().getMessageorUseDefault("lbl"+hero.name, hero.name));
        }
        return ret;
    }
}
