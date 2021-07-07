package forge.adventure.character;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class CharacterSprite extends Actor {

    private Sprite Standing;
    public CharacterSprite(FileHandle atlas)
    {
        Standing=new TextureAtlas(atlas).createSprite("Standing");

        setWidth(Standing.getWidth());
        setHeight(Standing.getHeight());
    }
    @Override
    public void draw(Batch batch, float parentAlpha)
    {
        Standing.setPosition(getX(),getY());
        Standing.draw(batch);
    }
    public Rectangle BoundingRect()
    {
        return new Rectangle(getX(),getY(),getWidth(),getHeight());
    }

    public boolean collideWith(CharacterSprite other) {
        if(BoundingRect().overlaps(other.BoundingRect()))
            return true;
        return false;
    }
}
