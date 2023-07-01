package forge.adventure.world;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import forge.adventure.data.BiomeSpriteData;
import forge.adventure.util.Config;

import java.util.HashMap;

/**
 * class to load and buffer map sprites
 */
public class BiomeSprites {
    private final HashMap<String, Array<Sprite>> spriteBuffer = new HashMap<>();
    public String textureAtlas;
    public BiomeSpriteData[] sprites;
    private TextureAtlas textureAtlasBuffer;

    public Sprite getSprite(String name, int seed) {
        if (textureAtlasBuffer == null) {
            textureAtlasBuffer = Config.instance().getAtlas(textureAtlas);
        }
        if (!spriteBuffer.containsKey(name)) {
            spriteBuffer.put(name, new Array<Sprite>());
        }
        Array<Sprite> sprites = spriteBuffer.get(name);
        if (sprites.isEmpty()) {
            sprites.addAll(textureAtlasBuffer.createSprites(name));
        }
        int index = seed % sprites.size;
        if (index >= sprites.size || index < 0) {
            System.err.println("Invalid index: " + index + " [" + name + "]");
            return null;
        }
        return sprites.get(index);
    }

    public BiomeSpriteData getSpriteData(String name) {
        for (BiomeSpriteData data : sprites) {
            if (data.name.equals(name))
                return data;
        }
        return null;
    }
}
