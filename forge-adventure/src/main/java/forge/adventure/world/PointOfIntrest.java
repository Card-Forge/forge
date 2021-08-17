package forge.adventure.world;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import forge.adventure.data.PointOfIntrestData;
import forge.adventure.util.Res;

import java.util.Random;

public class PointOfIntrest {
    PointOfIntrestData data;
    Vector2 position;
    Sprite sprite;
    int spriteIndex;
    Rectangle rectangle;

    public PointOfIntrest(PointOfIntrestData d, Vector2 pos, Random rand) {
        Array<Sprite> textureAtlas = Res.CurrentRes.getAtlas(d.spriteAtlas).createSprites(d.sprite);
        if (textureAtlas.isEmpty()) {
            System.out.printf("sprite " + d.sprite + " not found");
        }
        spriteIndex = rand.nextInt(Integer.SIZE - 1) % textureAtlas.size;
        sprite = textureAtlas.get(spriteIndex);
        data = d;
        position = pos;

        rectangle = new Rectangle(position.y, position.y, sprite.getWidth(), sprite.getHeight());
    }

    public Sprite getSprite() {
        return sprite;
    }

    public Vector2 getPosition() {
        return position;
    }

    public Vector2 getTilePosition(int tileSize) {
        return new Vector2((int) (position.x + (sprite.getWidth() / 2)) / tileSize, (int) position.y / tileSize);
    }

    public Rectangle getBoundingRectangle() {
        return rectangle;
    }

    public PointOfIntrestData getData() {
        return data;
    }
}
