package forge.adventure.stage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import forge.Forge;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.scene.Scene;
import forge.adventure.scene.SceneType;
import forge.adventure.util.Config;
import forge.adventure.util.Controls;
import forge.adventure.util.Current;
import forge.adventure.util.UIActor;
import forge.adventure.world.WorldSave;
import forge.gui.GuiBase;

/**
 * Stage to handle everything rendered in the HUD
 */
public class GameHUD extends Stage {

    static public GameHUD instance;
    private final GameStage gameStage;
    private final Image avatar;
    private final Image miniMapPlayer;
    private final Label lifePoints;
    private final Label money;
    private final Image miniMap, gamehud, mapborder, avatarborder, blank;
    private TextButton deckActor, menuActor, statsActor, inventoryActor;
    private UIActor ui;
    private Touchpad touchpad;
    private Console console;
    float TOUCHPAD_SCALE = 70f;

    private GameHUD(GameStage gameStage) {
        super(new ScalingViewport(Scaling.stretch, Scene.getIntendedWidth(), Scene.getIntendedHeight()), gameStage.getBatch());
        instance = this;
        this.gameStage = gameStage;

        ui = new UIActor(Config.instance().getFile(Forge.isLandscapeMode()?"ui/hud.json":"ui/hud_portrait.json"));

        blank = ui.findActor("blank");
        miniMap = ui.findActor("map");
        mapborder = ui.findActor("mapborder");
        avatarborder = ui.findActor("avatarborder");
        deckActor = ui.findActor("deck");
        deckActor.getLabel().setText(Forge.getLocalizer().getMessage("lblDeck"));
        menuActor = ui.findActor("menu");
        menuActor.getLabel().setText(Forge.getLocalizer().getMessage("lblMenu"));
        statsActor = ui.findActor("statistic");
        statsActor.getLabel().setText(Forge.getLocalizer().getMessage("lblStatus"));
        inventoryActor = ui.findActor("inventory");
        inventoryActor.getLabel().setText(Forge.getLocalizer().getMessage("lblItem"));
        gamehud = ui.findActor("gamehud");

        miniMapPlayer = new Image(new Texture(Config.instance().getFile("ui/minimap_player.png")));
        //create touchpad
        touchpad = new Touchpad(10, Controls.GetSkin());
        touchpad.setBounds(15, 15, TOUCHPAD_SCALE, TOUCHPAD_SCALE);
        touchpad.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                if (MapStage.getInstance().isInMap()) {
                    MapStage.getInstance().GetPlayer().getMovementDirection().x+=((Touchpad) actor).getKnobPercentX();
                    MapStage.getInstance().GetPlayer().getMovementDirection().y+=((Touchpad) actor).getKnobPercentY();
                } else {
                    WorldStage.getInstance().GetPlayer().getMovementDirection().x+=((Touchpad) actor).getKnobPercentX();
                    WorldStage.getInstance().GetPlayer().getMovementDirection().y+=((Touchpad) actor).getKnobPercentY();
                }
            }
        });
        if (GuiBase.isAndroid()) //add touchpad for android
            ui.addActor(touchpad);

        avatar = ui.findActor("avatar");
        ui.onButtonPress("menu", () -> menu());
        ui.onButtonPress("inventory", () -> openInventory());
        ui.onButtonPress("statistic", () -> statistic());
        ui.onButtonPress("deck", () -> openDeck());
        lifePoints = ui.findActor("lifePoints");
        lifePoints.setText("20/20");
        AdventurePlayer.current().onLifeChange(new Runnable() {
            @Override
            public void run() {
                lifePoints.setText(AdventurePlayer.current().getLife() + "/" + AdventurePlayer.current().getMaxLife());
            }
        });
        money = ui.findActor("money");
        WorldSave.getCurrentSave().getPlayer().onGoldChange(new Runnable() {
            @Override
            public void run() {
                money.setText(String.valueOf(AdventurePlayer.current().getGold()));
            }
        }) ;
        addActor(ui);
        addActor(miniMapPlayer);
        console=new Console();
        console.setBounds(0,0,getWidth(),getHeight()/2);
        console.setVisible(false);
        ui.addActor(console);
        WorldSave.getCurrentSave().onLoad(new Runnable() {
            @Override
            public void run() {
                GameHUD.this.enter();
            }
        });
    }

    private void statistic() {
        Forge.switchScene(SceneType.PlayerStatisticScene.instance);
    }

    public static GameHUD getInstance() {
        return instance == null ? instance = new GameHUD(WorldStage.getInstance()) : instance;
    }

    public Touchpad getTouchpad() {
        return touchpad;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        touchpad.setVisible(false);
        MapStage.getInstance().GetPlayer().setMovementDirection(Vector2.Zero);
        WorldStage.getInstance().GetPlayer().setMovementDirection(Vector2.Zero);
        return super.touchUp(screenX, screenY, pointer, button);
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        Vector2 c=new Vector2();
        screenToStageCoordinates(c.set(screenX, screenY));

        float x=(c.x-miniMap.getX())/miniMap.getWidth();
        float y=(c.y-miniMap.getY())/miniMap.getHeight();
        float mMapX = ui.findActor("map").getX();
        float mMapY = ui.findActor("map").getY();
        float mMapT = ui.findActor("map").getTop();
        float mMapR = ui.findActor("map").getRight();
        //map bounds
        if (c.x>=mMapX&&c.x<=mMapR&&c.y>=mMapY&&c.y<=mMapT) {
            touchpad.setVisible(false);
            if (MapStage.getInstance().isInMap())
                return true;
            if(Current.isInDebug())
                WorldStage.getInstance().GetPlayer().setPosition(x*WorldSave.getCurrentSave().getWorld().getWidthInPixels(),y*WorldSave.getCurrentSave().getWorld().getHeightInPixels());
            return true;
        }
        return super.touchDragged(screenX, screenY, pointer);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button)
    {
        return setPosition(screenX, screenY, pointer, button);
    }

    boolean setPosition(int screenX, int screenY, int pointer, int button) {
        Vector2 c=new Vector2();
        Vector2 touch =new Vector2();
        screenToStageCoordinates(touch.set(screenX, screenY));
        screenToStageCoordinates(c.set(screenX, screenY));

        float x=(c.x-miniMap.getX())/miniMap.getWidth();
        float y=(c.y-miniMap.getY())/miniMap.getHeight();


        float uiX = gamehud.getX();
        float uiY = gamehud.getY();
        float uiTop = gamehud.getTop();
        float uiRight = gamehud.getRight();
        //gamehud bounds
        if (c.x>=uiX&&c.x<=uiRight&&c.y>=uiY&&c.y<=uiTop) {
            super.touchDown(screenX, screenY, pointer, button);
            return true;
        }

        float mMapX = miniMap.getX();
        float mMapY = miniMap.getY();
        float mMapT = miniMap.getTop();
        float mMapR = miniMap.getRight();
        //map bounds
        if (c.x>=mMapX&&c.x<=mMapR&&c.y>=mMapY&&c.y<=mMapT) {
            if (MapStage.getInstance().isInMap())
                return true;
            if(Current.isInDebug())
                WorldStage.getInstance().GetPlayer().setPosition(x*WorldSave.getCurrentSave().getWorld().getWidthInPixels(),y*WorldSave.getCurrentSave().getWorld().getHeightInPixels());
            return true;
        }
        //display bounds
        float displayX = ui.getX();
        float displayY = ui.getY();
        float displayT = ui.getTop();
        float displayR = ui.getRight();
        //menu Y bounds
        float menuY = menuActor.getY();
        //auto follow touchpad
        if (GuiBase.isAndroid()) {
            if (!(touch.x>=mMapX&&touch.x<=mMapR&&touch.y>=mMapY&&touch.y<=mMapT) // not inside map bounds
                    && !(touch.x>=uiX&&touch.x<=uiRight&&touch.y>=menuY&&touch.y<=uiTop) //not inside gamehud bounds and menu Y bounds
                    && (touch.x>=displayX&&touch.x<=displayR&&touch.y>=displayY&&touch.y<=displayT) //inside display bounds
                    && pointer < 1) { //not more than 1 pointer
                touchpad.setBounds(touch.x-TOUCHPAD_SCALE/2, touch.y-TOUCHPAD_SCALE/2, TOUCHPAD_SCALE, TOUCHPAD_SCALE);
                touchpad.setVisible(true);
                touchpad.setResetOnTouchUp(true);
                if (!Forge.isLandscapeMode())
                    hideButtons();
                return super.touchDown(screenX, screenY, pointer, button);
            }
        }
        return super.touchDown(screenX, screenY, pointer, button);
    }


    @Override
    public void draw() {
        int yPos = (int) gameStage.player.getY();
        int xPos = (int) gameStage.player.getX();
        act(Gdx.graphics.getDeltaTime()); //act the Hud
        super.draw(); //draw the Hud
        int xPosMini = (int) (((float) xPos / (float) WorldSave.getCurrentSave().getWorld().getTileSize() / (float) WorldSave.getCurrentSave().getWorld().getWidthInTiles()) * miniMap.getWidth());
        int yPosMini = (int) (((float) yPos / (float) WorldSave.getCurrentSave().getWorld().getTileSize() / (float) WorldSave.getCurrentSave().getWorld().getHeightInTiles()) * miniMap.getHeight());
        miniMapPlayer.setPosition(miniMap.getX() + xPosMini - miniMapPlayer.getWidth()/2, miniMap.getY() + yPosMini -  miniMapPlayer.getHeight()/2);

    }

    Texture miniMapTexture;
    public void enter() {
        if(miniMapTexture!=null)
            miniMapTexture.dispose();
        miniMapTexture=new Texture(WorldSave.getCurrentSave().getWorld().getBiomeImage());

        miniMap.setDrawable(new TextureRegionDrawable(miniMapTexture));
        avatar.setDrawable(new TextureRegionDrawable(Current.player().avatar()));
    }

    private void openDeck() {
        Forge.switchScene(SceneType.DeckSelectScene.instance);
    }

    private void openInventory() {
        WorldSave.getCurrentSave().header.createPreview();
        Forge.switchScene(SceneType.InventoryScene.instance);
    }
    private void menu() {
        gameStage.openMenu();
    }
    public void showHideMap(boolean visible) {
        miniMap.setVisible(visible);
        mapborder.setVisible(visible);
        miniMapPlayer.setVisible(visible);
        gamehud.setVisible(visible);
        avatarborder.setVisible(visible);
        avatar.setVisible(visible);
        lifePoints.setVisible(visible);
        money.setVisible(visible);
        blank.setVisible(visible);
        if (visible) {
            deckActor.getColor().a = 1f;
            menuActor.getColor().a = 1f;
            statsActor.getColor().a = 1f;
            inventoryActor.getColor().a = 1f;
        } else {
            deckActor.getColor().a = 0.5f;
            menuActor.getColor().a = 0.5f;
            statsActor.getColor().a = 0.5f;
            inventoryActor.getColor().a = 0.5f;
        }
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.F10) {
            console.toggle();

            return true;
        }
        if (keycode == Input.Keys.F9) {
            console.toggle();

            return true;
        }
        if (keycode == Input.Keys.BACK) {
            if (!Forge.isLandscapeMode()) {
                menuActor.setVisible(!menuActor.isVisible());
                statsActor.setVisible(!statsActor.isVisible());
                inventoryActor.setVisible(!inventoryActor.isVisible());
                deckActor.setVisible(!deckActor.isVisible());
            }
        }
        return super.keyDown(keycode);
    }
    public void hideButtons() {
        menuActor.setVisible(false);
        deckActor.setVisible(false);
        inventoryActor.setVisible(false);
        statsActor.setVisible(false);
    }
}
