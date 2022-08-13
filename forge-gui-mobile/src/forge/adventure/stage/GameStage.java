package forge.adventure.stage;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import forge.Forge;
import forge.adventure.character.MapActor;
import forge.adventure.character.PlayerSprite;
import forge.adventure.data.PointOfInterestData;
import forge.adventure.pointofintrest.PointOfInterest;
import forge.adventure.scene.Scene;
import forge.adventure.scene.SceneType;
import forge.adventure.scene.TileMapScene;
import forge.adventure.world.WorldSave;
import forge.gui.GuiBase;
import forge.util.MyRandom;

/**
 * Base class to render a player sprite on a map
 * used for the over world and dungeons
 */
public abstract class GameStage extends Stage {


    private final OrthographicCamera camera;
    Group backgroundSprites;
    SpriteGroup foregroundSprites;
    PlayerSprite player;
    private float touchX = -1;
    private float touchY = -1;
    private final float timer = 0;
    private float animationTimeout = 0;
    public static float maximumScrollDistance=1.5f;
    public static float minimumScrollDistance=0.3f;

    public void startPause(float i) {
        startPause(i, null);
    }

    public void startPause(float i, Runnable runnable) {
        onEndAction = runnable;
        animationTimeout = i;
        player.setMovementDirection(Vector2.Zero);
    }

    public boolean isPaused() {
        return animationTimeout > 0;
    }

    public GameStage() {
        super(new ScalingViewport(Scaling.stretch, Scene.getIntendedWidth(), Scene.getIntendedHeight(), new OrthographicCamera()));
        WorldSave.getCurrentSave().onLoad(new Runnable() {
            @Override
            public void run() {
                if (player == null)
                    return;
                foregroundSprites.removeActor(player);
                player = null;
                GameStage.this.GetPlayer();
            }
        });
        camera = (OrthographicCamera) getCamera();

        backgroundSprites = new Group();
        foregroundSprites = new SpriteGroup();


        addActor(backgroundSprites);
        addActor(foregroundSprites);


    }

    public void setWinner(boolean b) {
    }

    public void setBounds(float width, float height) {
        getViewport().setWorldSize(width, height);
    }

    public PlayerSprite GetPlayer() {
        if (player == null) {
            player = new PlayerSprite(this);
            foregroundSprites.addActor(player);
        }
        return player;
    }


    public SpriteGroup GetSpriteGroup() {
        return foregroundSprites;
    }

    public Group GetBackgroundSprites() {
        return backgroundSprites;
    }


    Runnable onEndAction;

    @Override
    public final void act(float delta) {
        super.act(delta);

        if (animationTimeout >= 0) {
            animationTimeout -= delta;
            return;
        }
        if (isPaused()) {
            return;
        }


        if (onEndAction != null) {

            onEndAction.run();
            onEndAction = null;
        }

        if (touchX >= 0) {
            Vector2 target = this.screenToStageCoordinates(new Vector2(touchX, touchY));
            target.x -= player.getWidth() / 2f;
            Vector2 diff = target.sub(player.pos());

            if (diff.len() < 2) {
                diff.setZero();
                player.stop();
            }
            player.setMovementDirection(diff);
        }
        //debug speed up
        /*
        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT))
            player.setMoveModifier(20);
        else
            player.setMoveModifier(1);*/

        camera.position.x = Math.min(Math.max(Scene.getIntendedWidth() / 2f, player.pos().x), getViewport().getWorldWidth() - Scene.getIntendedWidth() / 2f);
        camera.position.y = Math.min(Math.max(Scene.getIntendedHeight() / 2f, player.pos().y), getViewport().getWorldHeight() - Scene.getIntendedHeight() / 2f);


        onActing(delta);
    }

    abstract protected void onActing(float delta);


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
        if (keycode == Input.Keys.F5)//todo config
        {
            if (!((TileMapScene) SceneType.TileMapScene.instance).currentMap().isInMap()) {
                GetPlayer().storePos();
                WorldSave.getCurrentSave().header.createPreview();
                WorldSave.getCurrentSave().quickSave();
            }

        }
        if (keycode == Input.Keys.F8)//todo config
        {
            if (!((TileMapScene) SceneType.TileMapScene.instance).currentMap().isInMap()) {
                WorldSave.getCurrentSave().quickLoad();
                enter();
            }
        }
        if (keycode == Input.Keys.F12) {
            debugCollision(true);
            for (Actor actor : foregroundSprites.getChildren()) {
                if (actor instanceof MapActor) {
                    ((MapActor) actor).setBoundDebug(true);
                }
            }
            setDebugAll(true);
            player.setBoundDebug(true);
        }
        if (keycode == Input.Keys.F2) {
            TileMapScene S = ((TileMapScene)SceneType.TileMapScene.instance);
            PointOfInterestData P = PointOfInterestData.getPointOfInterest("DEBUGZONE");
            PointOfInterest PoI = new PointOfInterest(P,new Vector2(0,0), MyRandom.getRandom());
            S.load(PoI);
            Forge.switchScene(S);
        }
        if (keycode == Input.Keys.F11) {
            debugCollision(false);
            for (Actor actor : foregroundSprites.getChildren()) {
                if (actor instanceof MapActor) {
                    ((MapActor) actor).setBoundDebug(false);
                }
            }
            player.setBoundDebug(false);
            setDebugAll(false);
        }
        return true;
    }

    protected void debugCollision(boolean b) {
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        if (isPaused())
            return true;
        camera.zoom += (amountY * 0.03);
        if (camera.zoom < minimumScrollDistance)
            camera.zoom = minimumScrollDistance;
        if (camera.zoom > maximumScrollDistance)
            camera.zoom = maximumScrollDistance;
        return super.scrolled(amountX, amountY);
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (isPaused())
            return true;
        if (!GuiBase.isAndroid()) {
            touchX = screenX;
            touchY = screenY;
        }

        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (isPaused())
            return true;
        if (!GuiBase.isAndroid()) {
            touchX = screenX;
            touchY = screenY;
        }


        return true;
    }

    public void stop() {
        WorldStage.getInstance().GetPlayer().setMovementDirection(Vector2.Zero);
        MapStage.getInstance().GetPlayer().setMovementDirection(Vector2.Zero);
        touchX = -1;
        touchY = -1;
        player.stop();
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        stop();
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (isPaused())
            return true;
        if (keycode == Input.Keys.LEFT || keycode == Input.Keys.A || keycode == Input.Keys.RIGHT || keycode == Input.Keys.D)//todo config
        {
            player.getMovementDirection().x = 0;
            if (!player.isMoving())
                stop();
        }
        if (keycode == Input.Keys.UP || keycode == Input.Keys.W || keycode == Input.Keys.DOWN || keycode == Input.Keys.S)//todo config
        {
            player.getMovementDirection().y = 0;
            if (!player.isMoving())
                stop();
        }
        if (keycode == Input.Keys.ESCAPE) {
            openMenu();
        }
        return false;
    }

    public void openMenu() {

        WorldSave.getCurrentSave().header.createPreview();
        Forge.switchScene(SceneType.StartScene.instance);
    }

    public void enter() {
        stop();
    }

    public void leave() {
        stop();
    }

    public boolean isColliding(Rectangle boundingRect) {
        return false;
    }

    public void prepareCollision(Vector2 pos, Vector2 direction, Rectangle boundingRect) {
    }

    public Vector2 adjustMovement(Vector2 direction, Rectangle boundingRect) {
        Vector2 adjDirX = direction.cpy();
        Vector2 adjDirY = direction.cpy();
        boolean foundX = false;
        boolean foundY = false;
        while (true) {

            if (!isColliding(new Rectangle(boundingRect.x + adjDirX.x, boundingRect.y + adjDirX.y, boundingRect.width, boundingRect.height))) {
                foundX = true;
                break;
            }
            if (adjDirX.x == 0)
                break;

            if (adjDirX.x >= 0)
                adjDirX.x = Math.max(0, adjDirX.x - 0.2f);
            else
                adjDirX.x = Math.max(0, adjDirX.x + 0.2f);
        }
        while (true) {
            if (!isColliding(new Rectangle(boundingRect.x + adjDirY.x, boundingRect.y + adjDirY.y, boundingRect.width, boundingRect.height))) {
                foundY = true;
                break;
            }
            if (adjDirY.y == 0)
                break;

            if (adjDirY.y >= 0)
                adjDirY.y = (Math.max(0, adjDirY.y - 0.2f));
            else
                adjDirY.y = (Math.max(0, adjDirY.y + 0.2f));
        }
        if (foundY && foundX)
            return adjDirX.len() > adjDirY.len() ? adjDirX : adjDirY;
        else if (foundY)
            return adjDirY;
        else if (foundX)
            return adjDirX;
        return Vector2.Zero.cpy();
    }

}
