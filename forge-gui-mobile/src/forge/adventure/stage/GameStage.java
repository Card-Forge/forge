package forge.adventure.stage;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import forge.Forge;
import forge.adventure.character.MapActor;
import forge.adventure.character.PlayerSprite;
import forge.adventure.data.PointOfInterestData;
import forge.adventure.pointofintrest.PointOfInterest;
import forge.adventure.scene.Scene;
import forge.adventure.scene.StartScene;
import forge.adventure.scene.TileMapScene;
import forge.adventure.util.KeyBinding;
import forge.adventure.util.Paths;
import forge.adventure.world.WorldSave;
import forge.gui.GuiBase;
import forge.util.MyRandom;

import java.util.HashMap;
import java.util.Map;

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

    public boolean axisMoved(Controller controller, int axisIndex, float value) {

        if (MapStage.getInstance().isDialogOnlyInput()||isPaused()) {
            return true;
        }
        player.getMovementDirection().x = controller.getAxis(0);
        player.getMovementDirection().y = -controller.getAxis(1);
        if(player.getMovementDirection().len()<0.2)
        {
            player.stop();
        }
        return true;
    }

    enum PlayerModification
    {
        Sprint,
        Hide,
        Fly

    }


    HashMap<PlayerModification,Float> currentModifications=new HashMap<>();
    public void modifyPlayer(PlayerModification mod,float value) {
        float currentValue=0;
        if(currentModifications.containsKey(mod))
        {
            currentValue=currentModifications.get(mod);
        }
        currentModifications.put(mod,currentValue+value);
    }

    public void flyFor(float value) {
        modifyPlayer(PlayerModification.Fly,value);
        player.playEffect(Paths.EFFECT_FLY);
    }
    public void hideFor(float value) {
        modifyPlayer(PlayerModification.Hide,value);
        player.setColor(player.getColor().r,player.getColor().g,player.getColor().b,0.5f);
        player.playEffect(Paths.EFFECT_HIDE);
    }
    public void sprintFor(float value) {
        modifyPlayer(PlayerModification.Sprint,value);
        player.playEffect(Paths.EFFECT_SPRINT);
    }
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
                GameStage.this.getPlayerSprite();
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

    public PlayerSprite getPlayerSprite() {
        if (player == null) {
            player = new PlayerSprite(this);
            foregroundSprites.addActor(player);
        }
        return player;
    }


    public SpriteGroup getSpriteGroup() {
        return foregroundSprites;
    }

    public Group getBackgroundSprites() {
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
        Array<PlayerModification> modsToRemove=new Array<>();
        for(Map.Entry<PlayerModification, Float> mod:currentModifications.entrySet())
        {
            mod.setValue(mod.getValue()-delta);
            if(mod.getValue()<0)
                modsToRemove.add(mod.getKey());
        }
        for(PlayerModification mod:modsToRemove)
        {
            currentModifications.remove(mod);
            onRemoveEffect(mod);
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
        camera.position.x = Math.min(Math.max(Scene.getIntendedWidth() / 2f, player.pos().x), getViewport().getWorldWidth() - Scene.getIntendedWidth() / 2f);
        camera.position.y = Math.min(Math.max(Scene.getIntendedHeight() / 2f, player.pos().y), getViewport().getWorldHeight() - Scene.getIntendedHeight() / 2f);


        onActing(delta);
    }

    private void onRemoveEffect(PlayerModification mod) {
        switch (mod)
        {
            case Hide:
                player.setColor(player.getColor().r,player.getColor().g,player.getColor().b,1f);
                break;
            case Fly:
                player.removeEffect(Paths.EFFECT_FLY);
                break;
            case Sprint:
                player.removeEffect(Paths.EFFECT_SPRINT);
                break;
        }
    }

    abstract protected void onActing(float delta);


    @Override
    public boolean keyDown(int keycode) {
        super.keyDown(keycode);
        if (isPaused())
            return true;
        if (KeyBinding.Left.isPressed(keycode))
        {
            player.getMovementDirection().x = -1;
        }
        if (KeyBinding.Right.isPressed(keycode) )
        {
            player.getMovementDirection().x = +1;
        }
        if (KeyBinding.Up.isPressed(keycode))
        {
            player.getMovementDirection().y = +1;
        }
        if (KeyBinding.Down.isPressed(keycode))
        {
            player.getMovementDirection().y = -1;
        }
        if (keycode == Input.Keys.F5)//todo config
        {
            if (!TileMapScene.instance().currentMap().isInMap()) {
                getPlayerSprite().storePos();
                WorldSave.getCurrentSave().header.createPreview();
                WorldSave.getCurrentSave().quickSave();
            }

        }
        if (keycode == Input.Keys.F8)//todo config
        {
            if (!TileMapScene.instance().currentMap().isInMap()) {
                WorldSave.getCurrentSave().quickLoad();
                enter();
            }
        }
        if (keycode == Input.Keys.F11) {
            debugCollision(false);

        }
        if (keycode == Input.Keys.F12) {
            debugCollision(true);

        }
        if (keycode == Input.Keys.F2) {
            TileMapScene S = TileMapScene.instance();
            PointOfInterestData P = PointOfInterestData.getPointOfInterest("DEBUGZONE");
            if( P != null)
            {
                PointOfInterest PoI = new PointOfInterest(P,new Vector2(0,0), MyRandom.getRandom());
                S.load(PoI);
                Forge.switchScene(S);
            }
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

    public void debugCollision(boolean b) {
        for (Actor actor : foregroundSprites.getChildren()) {
            if (actor instanceof MapActor) {
                ((MapActor) actor).setBoundDebug(b);
            }
        }
        setDebugAll(b);
        player.setBoundDebug(b);
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
        WorldStage.getInstance().getPlayerSprite().setMovementDirection(Vector2.Zero);
        MapStage.getInstance().getPlayerSprite().setMovementDirection(Vector2.Zero);
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
        if (KeyBinding.Left.isPressed(keycode)||KeyBinding.Right.isPressed(keycode))
        {
            player.getMovementDirection().x = 0;
            if (!player.isMoving())
                stop();
        }
        if (KeyBinding.Down.isPressed(keycode)||KeyBinding.Up.isPressed(keycode))
        {
            player.getMovementDirection().y = 0;
            if (!player.isMoving())
                stop();
        }
        if (KeyBinding.Menu.isPressed(keycode)) {
            openMenu();
        }
        return false;
    }

    public void openMenu() {
        if (Forge.restrictAdvMenus)
            return;
        WorldSave.getCurrentSave().header.createPreview();
        Forge.switchScene(StartScene.instance());
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
        if(isColliding(boundingRect))//if player is already colliding (after flying or teleport) allow to move off collision
            return direction;
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
                adjDirX.x = Math.min(0, adjDirX.x + 0.2f);
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
                adjDirY.y = (Math.min(0, adjDirY.y + 0.2f));
        }
        if (foundY && foundX)
            return adjDirX.len() > adjDirY.len() ? adjDirX : adjDirY;
        else if (foundY)
            return adjDirY;
        else if (foundX)
            return adjDirX;
        return Vector2.Zero.cpy();
    }

    protected void teleported(Vector2 position)
    {

    }
    public void setPosition(Vector2 position) {
        getPlayerSprite().setPosition(position);
        teleported(position);
    }

}
