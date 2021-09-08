package forge.adventure.data;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import forge.adventure.util.Config;

import java.util.HashMap;

public class BiomSprites {
    private final HashMap<String, Array<Sprite>> spriteBuffer = new HashMap<>();
    public String textureAltas;
    public BiomSpriteData[] sprites;
    private TextureAtlas textureAtlasBuffer;

    public Sprite GetSprite(String name, int seed) {
        if (textureAtlasBuffer == null) {
            textureAtlasBuffer = Config.instance().getAtlas(textureAltas);
            for (Texture texture : textureAtlasBuffer.getTextures()) {
                texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            }
        }
        if (!spriteBuffer.containsKey(name)) {
            spriteBuffer.put(name, new Array<Sprite>());
        }
        Array<Sprite> sprites = spriteBuffer.get(name);
        if (sprites.isEmpty()) {
            sprites.addAll(textureAtlasBuffer.createSprites(name));
        }
        return sprites.get(seed % sprites.size);
    }

    public BiomSpriteData GetSpriteData(String name) {
        for (BiomSpriteData data : sprites) {
            if (data.name.equals(name))
                return data;
        }
        return null;
    }
}
