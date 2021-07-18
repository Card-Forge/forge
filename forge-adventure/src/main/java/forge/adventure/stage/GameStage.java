package forge.adventure.stage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.character.MobSprite;
import forge.adventure.character.PlayerSprite;
import forge.adventure.scene.Scene;
import forge.adventure.scene.SceneType;
import forge.adventure.world.WorldSave;
import forge.adventure.world.WorldSaveHeader;


public class GameStage extends Stage {

    private int playerMovementX;
    private int playerMovementY;
    private int playerSpeed=60;
    public WorldBackground background;
    MobSprite mob;
    SpriteGroup spriteGroup;
    PlayerSprite player;
    OrthographicCamera camera;
    public GameStage()
    {
        super(new StretchViewport(Scene.IntendedWidth,Scene.IntendedHeight,new OrthographicCamera()));
        camera = (OrthographicCamera) getCamera();

        spriteGroup=new SpriteGroup();
        background=new WorldBackground(this);
        addActor(background);
        player=new PlayerSprite(this);
        player.setPosition(200,200);
        spriteGroup.addActor(player);
        mob=new MobSprite(this);
        spriteGroup.addActor(mob);
        mob.setPosition(16000,16500);
        addActor(spriteGroup);


    }
    public SpriteGroup GetSpriteGroup()
    {
        return spriteGroup;
    }
    @Override
    public void act(float delta)
    {
        super.act(delta);

        player.moveBy(playerMovementX,playerMovementY);
        getCamera().position.set(player.pos(),getCamera().position.z);

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
    public void draw()
    {
        getBatch().begin();
        background.setPlayerPos(player.getX(),player.getY());
        getBatch().end();
        act(Gdx.graphics.getDeltaTime());
        //spriteGroup.setCullingArea(new Rectangle(player.getX()-getViewport().getWorldHeight()/2,player.getY()-getViewport().getWorldHeight()/2,getViewport().getWorldHeight(),getViewport().getWorldHeight()));
        super.draw();
    }
    @Override
    public boolean scrolled (float amountX, float amountY)
    {
        camera.zoom+=(amountY*0.2);
        return super.scrolled(amountX,amountY);
    }
    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer)
    {

        Vector2 target=this.screenToStageCoordinates(new Vector2((float)screenX, (float)screenY));
        Vector2 diff=target.sub(player.pos());

        diff.setLength(playerSpeed);
        playerMovementX=Math.round(diff.x);
        playerMovementY=Math.round(diff.y);

        return false;
    }
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button)
    {
        Vector2 target=this.screenToStageCoordinates(new Vector2((float)screenX, (float)screenY));
        Vector2 diff=target.sub(player.pos());

        diff.setLength(playerSpeed);
        playerMovementX=Math.round(diff.x);
        playerMovementY=Math.round(diff.y);

        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button)
    {
        playerMovementX=0;
        playerMovementY=0;
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
        if(keycode == Input.Keys.ESCAPE)
        {
            Pixmap pixmap =   Pixmap.createFromFrameBuffer(0,0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            Pixmap scaled = new Pixmap(WorldSaveHeader.previewImageWidth,(int)(WorldSaveHeader.previewImageWidth/(Scene.IntendedWidth/(float)Scene.IntendedHeight)), Pixmap.Format.RGB888);
            scaled.drawPixmap(pixmap,
                    0, 0, pixmap.getWidth(), pixmap.getHeight(),
                    0, 0,scaled.getWidth(),scaled.getHeight());
            pixmap.dispose();
            if(WorldSave.getCurrentSave().header.preview!=null)
                WorldSave.getCurrentSave().header.preview.dispose();
            WorldSave.getCurrentSave().header.preview=scaled;
            AdventureApplicationAdapter.CurrentAdapter.SwitchScene(SceneType.StartScene.instance);
        }
        return false;
    }

    public void Enter() {
        player.LoadPos();
    }
}
