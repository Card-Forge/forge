package forge.adventure.stage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
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
import forge.gui.FThreads;
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
    float TOUCHPAD_SCALE = 70f, referenceX;
    boolean moveStarted = false, buttonsVisible = true;

    private GameHUD(GameStage gameStage) {
        super(new ScalingViewport(Scaling.stretch, Scene.getIntendedWidth(), Scene.getIntendedHeight()), gameStage.getBatch());
        instance = this;
        this.gameStage = gameStage;

        ui = new UIActor(Config.instance().getFile(GuiBase.isAndroid()
                ? Forge.isLandscapeMode() ? "ui/hud_landscape.json" : "ui/hud_portrait.json"
                : Forge.isLandscapeMode() ? "ui/hud.json" : "ui/hud_portrait.json"));

        blank = ui.findActor("blank");
        miniMap = ui.findActor("map");
        mapborder = ui.findActor("mapborder");
        avatarborder = ui.findActor("avatarborder");
        deckActor = ui.findActor("deck");
        deckActor.getLabel().setText(Forge.getLocalizer().getMessage("lblDeck"));
        menuActor = ui.findActor("menu");
        referenceX = menuActor.getX();
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
        //map bounds
        if (Controls.actorContainsVector(miniMap,c)) {
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
        Vector2 c=new Vector2();
        Vector2 touch =new Vector2();
        screenToStageCoordinates(touch.set(screenX, screenY));
        screenToStageCoordinates(c.set(screenX, screenY));

        float x=(c.x-miniMap.getX())/miniMap.getWidth();
        float y=(c.y-miniMap.getY())/miniMap.getHeight();
        if (Controls.actorContainsVector(gamehud,c)) {
            super.touchDown(screenX, screenY, pointer, button);
            return true;
        }
        if (Controls.actorContainsVector(miniMap,c)) {
            if (MapStage.getInstance().isInMap())
                return true;
            if(Current.isInDebug())
                WorldStage.getInstance().GetPlayer().setPosition(x*WorldSave.getCurrentSave().getWorld().getWidthInPixels(),y*WorldSave.getCurrentSave().getWorld().getHeightInPixels());
            return true;
        }
        //auto follow touchpad
        if (GuiBase.isAndroid() && !MapStage.getInstance().getDialogOnlyInput()) {
            if (!(Controls.actorContainsVector(miniMap,touch)) // not inside map bounds
                    && !(Controls.actorContainsVector(gamehud,touch)) //not inside gamehud bounds
                    && !(Controls.actorContainsVector(menuActor,touch)) //not inside menu button
                    && !(Controls.actorContainsVector(deckActor,touch)) //not inside deck button
                    && !(Controls.actorContainsVector(statsActor,touch)) //not inside stats button
                    && !(Controls.actorContainsVector(inventoryActor,touch)) //not inside inventory button
                    && (Controls.actorContainsVector(ui,touch)) //inside display bounds
                    && pointer < 1) { //not more than 1 pointer
                touchpad.setBounds(touch.x-TOUCHPAD_SCALE/2, touch.y-TOUCHPAD_SCALE/2, TOUCHPAD_SCALE, TOUCHPAD_SCALE);
                touchpad.setVisible(true);
                touchpad.setResetOnTouchUp(true);
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
            moveButtons();
        }
        return super.keyDown(keycode);
    }
    public void hideButtons() {
        if (buttonsVisible)
            moveButtons();
    }
    public void moveButtons() {
        if (moveStarted)
            return;
        moveStarted = true;
        FThreads.invokeInEdtNowOrLater(() -> {
            int delay = 500;
            if (menuActor.isVisible()) {
                delay = 250;
                menuActor.addAction(Actions.sequence(Actions.fadeOut(0.25f), Actions.hide(), Actions.moveTo(menuActor.getX()+menuActor.getWidth(), menuActor.getY())));
            } else {
                menuActor.addAction(Actions.sequence(Actions.delay(0.1f), Actions.parallel(Actions.show(), Actions.fadeIn(0.1f), Actions.moveTo(referenceX, menuActor.getY(), 0.25f))));
            }
            if (statsActor.isVisible()) {
                statsActor.addAction(Actions.sequence(Actions.fadeOut(0.20f), Actions.hide(), Actions.moveTo(statsActor.getX()+statsActor.getWidth(), statsActor.getY())));
            } else {
                statsActor.addAction(Actions.sequence(Actions.delay(0.15f), Actions.parallel(Actions.show(), Actions.fadeIn(0.1f), Actions.moveTo(referenceX, statsActor.getY(), 0.25f))));
            }
            if (inventoryActor.isVisible()) {
                inventoryActor.addAction(Actions.sequence(Actions.fadeOut(0.15f), Actions.hide(), Actions.moveTo(inventoryActor.getX()+inventoryActor.getWidth(), inventoryActor.getY())));
            } else {
                inventoryActor.addAction(Actions.sequence(Actions.delay(0.2f), Actions.parallel(Actions.fadeIn(0.1f), Actions.show(), Actions.moveTo(referenceX, inventoryActor.getY(), 0.25f))));
            }
            if (deckActor.isVisible()) {
                deckActor.addAction(Actions.sequence(Actions.fadeOut(0.10f), Actions.hide(), Actions.moveTo(deckActor.getX()+deckActor.getWidth(), deckActor.getY())));
            } else {
                deckActor.addAction(Actions.sequence(Actions.delay(0.25f), Actions.parallel(Actions.fadeIn(0.1f), Actions.show(), Actions.moveTo(referenceX, deckActor.getY(), 0.25f))));
            }
            FThreads.delayInEDT(delay, () -> {
                buttonsVisible = menuActor.getX() == referenceX;
                moveStarted = false;
            });
        });
    }
}
