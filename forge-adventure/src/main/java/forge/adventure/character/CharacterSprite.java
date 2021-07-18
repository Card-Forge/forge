package forge.adventure.character;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import forge.adventure.stage.GameStage;

public class CharacterSprite extends Actor {

    private Sprite Standing;
    GameStage stage;
    public CharacterSprite(FileHandle atlas, GameStage stage)
    {
        this.stage=stage;
        Standing=new TextureAtlas(atlas).createSprite("Standing");

        setWidth(Standing.getWidth());
        setHeight(Standing.getHeight());
    }
    @Override
    protected void positionChanged () {
        stage.GetSpriteGroup().UpdateActorZ(this);
    }



    public Vector2 pos()
    {
        return new Vector2(getX(),getY());
    }
    @Override
    public void draw(Batch batch, float parentAlpha)
    {
        Standing.setPosition(getX()-getWidth()/2,getY());
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
