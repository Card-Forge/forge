package forge.adventure.stage;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.character.MobSprite;
import forge.adventure.character.PlayerSprite;
import forge.adventure.scene.Scene;
import forge.adventure.scene.SceneType;


public class GameStage extends Stage {

    private int playerMovementX;
    private int playerMovementY;
    private int playerSpeed=6;
    private Vector2 target;
    MobSprite mob;
    PlayerSprite player;
    public GameStage()
    {
        super(new StretchViewport(Scene.IntendedWidth,Scene.IntendedHeight));
        player=new PlayerSprite();
        player.setPosition(200,200);
        addActor(player);
        mob=new MobSprite();
        mob.setPosition(1000,400);
        addActor(mob);
    }
    @Override
    public void act(float delta)
    {
        super.act(delta);
        if(target!=null)
        {
            if(target.x<player.getX())
                playerMovementX=-playerSpeed;
            else if(target.x>player.getX())
                playerMovementX=+playerSpeed;
            else
                playerMovementX=0;
            if(target.y<player.getY())
                playerMovementY=-playerSpeed;
            else if(target.y>player.getY())
                playerMovementY=+playerSpeed;
            else
                playerMovementY=0;
        }
        player.moveBy(playerMovementX,playerMovementY);

        if(player.collideWith(mob))
        {

            AdventureApplicationAdapter.CurrentAdapter.SwitchScene(SceneType.DuelScene.instance);
        }
    }

    @Override
    public boolean keyDown(int keycode) {
        super.keyDown(keycode);
        if(keycode == Input.Keys.LEFT||keycode==Input.Keys.A)//todo config
        {
            playerMovementX=-playerSpeed;
        }
        if(keycode == Input.Keys.RIGHT||keycode==Input.Keys.D)//todo config
        {
            playerMovementX=+playerSpeed;
        }
        if(keycode == Input.Keys.UP||keycode==Input.Keys.W)//todo config
        {
            playerMovementY=+playerSpeed;
        }
        if(keycode == Input.Keys.DOWN||keycode==Input.Keys.S)//todo config
        {
            playerMovementY=-playerSpeed;
        }
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer)
    {

        target=this.screenToStageCoordinates(new Vector2((float)screenX, (float)screenY));

        return false;
    }
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button)
    {
        target=this.screenToStageCoordinates(new Vector2((float)screenX, (float)screenY));

        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button)
    {
        target=null;
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        if(keycode == Input.Keys.LEFT||keycode==Input.Keys.A||keycode == Input.Keys.RIGHT||keycode==Input.Keys.D)//todo config
        {
            playerMovementX=0;
        }
        if(keycode == Input.Keys.UP||keycode==Input.Keys.W||keycode == Input.Keys.DOWN||keycode==Input.Keys.S)//todo config
        {
            playerMovementY=0;
        }
        return false;
    }

}
