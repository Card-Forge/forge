package forge.adventure.stage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.character.CharacterSprite;
import forge.adventure.character.MobSprite;
import forge.adventure.character.PlayerSprite;
import forge.adventure.scene.DuelScene;
import forge.adventure.scene.Scene;
import forge.adventure.scene.SceneType;
import forge.adventure.world.WorldSave;
import forge.adventure.world.WorldSaveHeader;

import java.util.ArrayList;


public class GameStage extends Stage {


    private final OrthographicCamera camera;
    protected MobSprite currentMob;
    protected ArrayList<MobSprite> enemies = new ArrayList<>();
    Group backgroundSprites;
    SpriteGroup foregroundSprites;
    PlayerSprite player;
    CurrentAction action = CurrentAction.NoAction;
    private float touchX = -1;
    private float touchY = -1;
    private float timer = 0;
    private float animationTimeout = 0;


    public GameStage() {
        super(new StretchViewport(Scene.GetIntendedWidth(), Scene.GetIntendedHeight(), new OrthographicCamera()));
        camera = (OrthographicCamera) getCamera();

        backgroundSprites = new Group();
        foregroundSprites = new SpriteGroup();


        addActor(backgroundSprites);
        addActor(foregroundSprites);


    }

    public void setBounds(float width, float height) {
        getViewport().setWorldSize(width, height);
    }

    public PlayerSprite GetPlayer() {
        if (player == null) {
            player = new PlayerSprite();
            foregroundSprites.addActor(player);
        }
        return player;
    }

    public float GetTimer() {
        return timer;
    }

    public SpriteGroup GetSpriteGroup() {
        return foregroundSprites;
    }

    public Group GetBackgroundSprites() {
        return backgroundSprites;
    }

    @Override
    public void draw() {
        timer += Gdx.graphics.getDeltaTime();
        act(Gdx.graphics.getDeltaTime());
        //spriteGroup.setCullingArea(new Rectangle(player.getX()-getViewport().getWorldHeight()/2,player.getY()-getViewport().getWorldHeight()/2,getViewport().getWorldHeight(),getViewport().getWorldHeight()));
        super.draw();
    }

    public void setWinner(boolean b) {

        if (b) {
            player.setAnimation(CharacterSprite.AnimationTypes.Attack);
            currentMob.setAnimation(CharacterSprite.AnimationTypes.Death);
        } else {
            player.setAnimation(CharacterSprite.AnimationTypes.Hit);
            currentMob.setAnimation(CharacterSprite.AnimationTypes.Attack);
        }
        animationTimeout = 1;
        action = CurrentAction.FightEnd;
    }

    @Override
    public void act(float delta) {
        super.act(delta);


        if (touchX >= 0) {
            Vector2 target = this.screenToStageCoordinates(new Vector2(touchX, touchY));
            target.x -= player.getWidth() / 2;
            Vector2 diff = target.sub(player.pos());

            if (diff.len() < 2) {
                diff.setZero();
            }
            player.setMovementDirection(diff);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT))
            player.setMoveModifier(20);
        else
            player.setMoveModifier(1);

        if (currentMob != null) {

            if (animationTimeout >= 0) {
                animationTimeout -= delta;
                return;
            }
            switch (action) {
                case FightEnd:
                    removeEnemy(currentMob);
                    currentMob = null;
                    break;
                case Attack:
                    animationTimeout = 0;
                    ((DuelScene) SceneType.DuelScene.instance).setEnemy(currentMob);
                    ((DuelScene) SceneType.DuelScene.instance).setPlayer(player);
                    AdventureApplicationAdapter.CurrentAdapter.SwitchScene(SceneType.DuelScene.instance);
                    break;
            }
        }
        if (player == null)
            return;
        camera.position.x = Math.min(Math.max(Scene.GetIntendedWidth() / 2, player.pos().x), getViewport().getWorldWidth() - Scene.GetIntendedWidth() / 2);
        camera.position.y = Math.min(Math.max(Scene.GetIntendedHeight() / 2, player.pos().y), getViewport().getWorldHeight() - Scene.GetIntendedHeight() / 2);

    }

    private void removeEnemy(MobSprite currentMob) {

        foregroundSprites.removeActor(currentMob);
        enemies.remove(currentMob);
    }

    @Override
    public boolean keyDown(int keycode) {
        super.keyDown(keycode);
        if (keycode == Input.Keys.LEFT || keycode == Input.Keys.A)//todo config
        {
            player.getMovementDirection().x = -1;
        }
        if (keycode == Input.Keys.RIGHT || keycode == Input.Keys.D)//todo config
        {
            player.getMovementDirection().x = +1;
        }
        if (keycode == Input.Keys.UP || keycode == Input.Keys.W)//todo config
        {
            player.getMovementDirection().y = +1;
        }
        if (keycode == Input.Keys.DOWN || keycode == Input.Keys.S)//todo config
        {
            player.getMovementDirection().y = -1;
        }
        return true;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        camera.zoom += (amountY * 0.03);
        if (camera.zoom < 0.2f)
            camera.zoom = 0.2f;
        if (camera.zoom > 1.5f)
            camera.zoom = 1.5f;
        return super.scrolled(amountX, amountY);
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {

        touchX = screenX;
        touchY = screenY;

        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        touchX = screenX;
        touchY = screenY;


        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        touchX = -1;
        touchY = -1;
        player.setMovementDirection(Vector2.Zero);
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (keycode == Input.Keys.LEFT || keycode == Input.Keys.A || keycode == Input.Keys.RIGHT || keycode == Input.Keys.D)//todo config
        {
            player.getMovementDirection().x = 0;
        }
        if (keycode == Input.Keys.UP || keycode == Input.Keys.W || keycode == Input.Keys.DOWN || keycode == Input.Keys.S)//todo config
        {
            player.getMovementDirection().y = 0;
        }
        if (keycode == Input.Keys.ESCAPE) {
            openMenu();
        }
        return false;
    }

    public void openMenu() {
        Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Pixmap scaled = new Pixmap(WorldSaveHeader.previewImageWidth, (int) (WorldSaveHeader.previewImageWidth / (Scene.GetIntendedWidth() / (float) Scene.GetIntendedHeight())), Pixmap.Format.RGB888);
        scaled.drawPixmap(pixmap,
                0, 0, pixmap.getWidth(), pixmap.getHeight(),
                0, 0, scaled.getWidth(), scaled.getHeight());
        pixmap.dispose();
        if (WorldSave.getCurrentSave().header.preview != null)
            WorldSave.getCurrentSave().header.preview.dispose();
        WorldSave.getCurrentSave().header.preview = scaled;
        AdventureApplicationAdapter.CurrentAdapter.SwitchScene(SceneType.StartScene.instance);
    }

    public void Enter() {
    }

    public void Leave() {
    }

    enum CurrentAction {
        NoAction,
        Attack,
        FightEnd
    }

}
