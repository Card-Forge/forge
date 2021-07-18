package forge.adventure.world;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import forge.adventure.data.BiomSpriteData;
import forge.adventure.util.Res;

public class MapObject {
    Sprite texture;
    String key;
    public MapObject(BiomSpriteData sprite) {

        TextureAtlas atlas = new TextureAtlas(Res.CurrentRes.GetFile(sprite.textureAltas));
        texture=atlas.createSprite(sprite.textureName);
        key=sprite.key();
    }

    public TextureRegion GetTexture() {
        return texture;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj.getClass() != this.getClass()) {
            return false;
        }

        final MapObject other = (MapObject) obj;
        if ((this.key == null) ? (other.key != null) : !this.key.equals(other.key)) {
            return false;
        }
        return true;
    }

    public float getHeight() {
        if(texture==null)return 0;
        return texture.getHeight();
    }
    public float getWidth() {
        if(texture==null)return 0;
        return texture.getWidth();
    }
}
