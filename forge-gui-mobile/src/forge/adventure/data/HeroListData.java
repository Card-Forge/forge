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
    private HeroData[] heroes;
    private String avatar;
    private TextureAtlas avatarAtlas;
    private final ObjectMap<String, Array<Sprite>> avatarSprites = new ObjectMap<>();

    static public HeroListData instance() {
        if (instance == null) {
            Json json = new Json();
            FileHandle handle = Config.instance().getFile(Paths.HEROES);
            if (handle.exists()) {
                instance = json.fromJson(HeroListData.class, handle);
                instance.avatarAtlas = Config.instance().getAtlas(instance.avatar);
                // leaving here as reference since PixelArt images use Nearest without MipMaps. By default it shouldn't have any filters
                /*instance.avatarSprites.getTextures().first().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);*/
            } else {
                throw new RuntimeException("Path not found: " + handle.path());
            }
        }
        return instance;
    }

    public String getHero(int raceIndex, boolean female) {
        HeroData data =  instance().heroes[raceIndex];

        if (female)
            return data.female;
        return data.male;

    }

    public TextureRegion getAvatar(int heroRace, boolean isFemale, int avatarIndex) {
        HeroData data = instance().heroes[heroRace];
        Array<Sprite> sprites = instance().avatarSprites.get(isFemale ? data.femaleAvatar : data.maleAvatar);
        if (sprites == null) {
            sprites = instance().avatarAtlas.createSprites(isFemale ? data.femaleAvatar : data.maleAvatar);
            instance().avatarSprites.put(isFemale ? data.femaleAvatar : data.maleAvatar, sprites);
        }
        avatarIndex %= sprites.size;
        if (avatarIndex < 0) {
            avatarIndex += sprites.size;
        }
        return sprites.get(avatarIndex);
    }

    public Array<String> getRaces() {
        Array<String> ret = new Array<>();
        for (HeroData hero : instance().heroes) {
            ret.add(Forge.getLocalizer().getMessageorUseDefault("lbl"+hero.name, hero.name));
        }
        return ret;
    }
}
