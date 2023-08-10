package forge.adventure.world;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import forge.adventure.data.BiomeSpriteData;
import forge.adventure.util.Config;

/**
 * class to load and buffer map sprites
 */
public class BiomeSprites {
    private final ObjectMap<String, Array<Sprite>> spriteBuffer = new ObjectMap<>();
    public String textureAtlas;
    public BiomeSpriteData[] sprites;

    public Sprite getSprite(String name, int seed) {
        if (!spriteBuffer.containsKey(name)) {
            spriteBuffer.put(name, new Array<>());
        }
        Array<Sprite> sprites = spriteBuffer.get(name);
        if (sprites.isEmpty()) {
            sprites.addAll(Config.instance().getAtlas(textureAtlas).createSprites(name));
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
