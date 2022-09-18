package forge.adventure.stage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import forge.Forge;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.scene.*;
import forge.adventure.util.Config;
import forge.adventure.util.Controls;
import forge.adventure.util.Current;
import forge.adventure.util.UIActor;
import forge.adventure.world.WorldSave;
import forge.deck.Deck;
import forge.gui.FThreads;
import forge.gui.GuiBase;

/**
 * Stage to handle everything rendered in the HUD
 */
public class GameHUD extends Stage implements ControllerListener {

    static public GameHUD instance;
    private final GameStage gameStage;
    private final Image avatar;
    private final Image miniMapPlayer;
    private final TextraLabel lifePoints;
    private final TextraLabel money;
    private final TextraLabel mana;
    private final Image miniMap, gamehud, mapborder, avatarborder, blank;
    private final InputEvent eventTouchDown;
    private final InputEvent eventTouchUp;
    private final TextraButton deckActor;
    private final TextraButton menuActor;
    private final TextraButton statsActor;
    private final TextraButton inventoryActor;
    private final UIActor ui;
    private final Touchpad touchpad;
    private final Console console;
    float TOUCHPAD_SCALE = 70f, referenceX;
    boolean isHiding = false, isShowing = false;
    float opacity = 1f;

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
        menuActor = ui.findActor("menu");
        referenceX = menuActor.getX();
        statsActor = ui.findActor("statistic");
        inventoryActor = ui.findActor("inventory");
        gamehud = ui.findActor("gamehud");

        miniMapPlayer = new Image(new Texture(Config.instance().getFile("ui/minimap_player.png")));
        //create touchpad
        touchpad = new Touchpad(10, Controls.getSkin());
        touchpad.setBounds(15, 15, TOUCHPAD_SCALE, TOUCHPAD_SCALE);
        touchpad.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                if (MapStage.getInstance().isInMap()) {
                    MapStage.getInstance().getPlayerSprite().getMovementDirection().x+=((Touchpad) actor).getKnobPercentX();
                    MapStage.getInstance().getPlayerSprite().getMovementDirection().y+=((Touchpad) actor).getKnobPercentY();
                } else {
                    WorldStage.getInstance().getPlayerSprite().getMovementDirection().x+=((Touchpad) actor).getKnobPercentX();
                    WorldStage.getInstance().getPlayerSprite().getMovementDirection().y+=((Touchpad) actor).getKnobPercentY();
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
        mana = ui.findActor("mana");
        money = ui.findActor("money");
        mana.setText("0/0");
        lifePoints.setText("20/20");
        AdventurePlayer.current().onLifeChange(() -> lifePoints.setText(AdventurePlayer.current().getLife() + "/" + AdventurePlayer.current().getMaxLife()));
        AdventurePlayer.current().onManaChange(() -> mana.setText(AdventurePlayer.current().getMana() + "/" + AdventurePlayer.current().getMaxMana()));

        WorldSave.getCurrentSave().getPlayer().onGoldChange(() -> money.setText(String.valueOf(AdventurePlayer.current().getGold()))) ;
        addActor(ui);
        addActor(miniMapPlayer);
        console=new Console();
        console.setBounds(0, GuiBase.isAndroid() ? getHeight() : 0, getWidth(),getHeight()/2);
        console.setVisible(false);
        ui.addActor(console);
        if (GuiBase.isAndroid()) {
            avatar.addListener(new ConsoleToggleListener());
            avatarborder.addListener(new ConsoleToggleListener());
            gamehud.addListener(new ConsoleToggleListener());
        }
        WorldSave.getCurrentSave().onLoad(() -> GameHUD.this.enter());
        eventTouchDown = new InputEvent();
        eventTouchDown.setPointer(-1);
        eventTouchDown.setType(InputEvent.Type.touchDown);
        eventTouchUp = new InputEvent();
        eventTouchUp.setPointer(-1);
        eventTouchUp.setType(InputEvent.Type.touchUp);
        Controllers.addListener(this);
    }

    private void openMap()  {
        Forge.switchScene(MapViewScene.instance());
    }

    private void statistic() {
        Forge.switchScene(PlayerStatisticScene.instance());
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
        MapStage.getInstance().getPlayerSprite().setMovementDirection(Vector2.Zero);
        WorldStage.getInstance().getPlayerSprite().setMovementDirection(Vector2.Zero);
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

            if(Current.isInDebug())
                WorldStage.getInstance().getPlayerSprite().setPosition(x*WorldSave.getCurrentSave().getWorld().getWidthInPixels(),y*WorldSave.getCurrentSave().getWorld().getHeightInPixels());

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
            if(Current.isInDebug())
                WorldStage.getInstance().getPlayerSprite().setPosition(x*WorldSave.getCurrentSave().getWorld().getWidthInPixels(),y*WorldSave.getCurrentSave().getWorld().getHeightInPixels());
            else
                openMap();
        return true;
        }
        //auto follow touchpad
        if (GuiBase.isAndroid() && !MapStage.getInstance().getDialogOnlyInput() && !console.isVisible()) {
            if (!(Controls.actorContainsVector(avatar,touch)) // not inside avatar bounds
                    && !(Controls.actorContainsVector(miniMap,touch)) // not inside map bounds
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
        if (GuiBase.isAndroid()) // prevent drawing on top of console
            miniMapPlayer.setVisible(!console.isVisible()&&miniMap.isVisible());
        //colored lifepoints
        if (Current.player().getLife() >= Current.player().getMaxLife()) {
            //color green if max life
            lifePoints.setColor(Color.GREEN);
        } else if (Current.player().getLife() <= 5) {
            //color red if critical
            lifePoints.setColor(Color.RED);
        } else {
            lifePoints.setColor(Color.WHITE);
        }
    }

    Texture miniMapTexture;
    Texture miniMapToolTipTexture;
    Pixmap miniMapToolTipPixmap;
    public void enter() {
        if(miniMapTexture!=null)
            miniMapTexture.dispose();
        miniMapTexture=new Texture(WorldSave.getCurrentSave().getWorld().getBiomeImage());
        if(miniMapToolTipTexture!=null)
            miniMapToolTipTexture.dispose();
        if(miniMapToolTipPixmap!=null)
            miniMapToolTipPixmap.dispose();
        miniMapToolTipPixmap=new Pixmap((int) (miniMap.getWidth()*3), (int) (miniMap.getHeight()*3), Pixmap.Format.RGBA8888);
        miniMapToolTipPixmap.drawPixmap(WorldSave.getCurrentSave().getWorld().getBiomeImage(),0,0,WorldSave.getCurrentSave().getWorld().getBiomeImage().getWidth(),WorldSave.getCurrentSave().getWorld().getBiomeImage().getHeight(),0,0,miniMapToolTipPixmap.getWidth(),miniMapToolTipPixmap.getHeight());
        miniMapToolTipTexture=new Texture(miniMapToolTipPixmap);
        miniMap.setDrawable(new TextureRegionDrawable(miniMapTexture));
        avatar.setDrawable(new TextureRegionDrawable(Current.player().avatar()));
        Deck deck = AdventurePlayer.current().getSelectedDeck();
        if (deck == null || deck.isEmpty() || deck.getMain().toFlatList().size() < 30) {
            deckActor.setColor(Color.RED);
        } else {
            deckActor.setColor(menuActor.getColor());
        }
    }

    private void openDeck() {
        Forge.switchScene(DeckSelectScene.instance());
    }

    private void openInventory() {
        WorldSave.getCurrentSave().header.createPreview();
        Forge.switchScene(InventoryScene.instance());
    }
    private void menu() {
        gameStage.openMenu();
    }
    public void showHideMap(boolean visible) {
        miniMap.setVisible(visible);
        mapborder.setVisible(visible);
        miniMapPlayer.setVisible(visible);
        gamehud.setVisible(visible);
        lifePoints.setVisible(visible);
        mana.setVisible(visible);
        money.setVisible(visible);
        blank.setVisible(visible);
        if (visible) {
            avatarborder.getColor().a = 1f;
            avatar.getColor().a = 1f;
            deckActor.getColor().a = 1f;
            menuActor.getColor().a = 1f;
            statsActor.getColor().a = 1f;
            inventoryActor.getColor().a = 1f;
            opacity = 1f;
        } else {
            avatarborder.getColor().a = 0.4f;
            avatar.getColor().a = 0.4f;
            deckActor.getColor().a = 0.4f;
            menuActor.getColor().a = 0.4f;
            statsActor.getColor().a = 0.4f;
            inventoryActor.getColor().a = 0.4f;
            opacity = 0.4f;
        }
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.F9 || keycode == Input.Keys.F10) {
            console.toggle();
            return true;
        }
        if (keycode == Input.Keys.BACK) {
            if (console.isVisible()) {
                console.toggle();
            } else {
                if (menuActor.isVisible())
                    hideButtons();
                else
                    showButtons();
            }
        }
        if (keycode == Input.Keys.BUTTON_B) {
            performTouch(statsActor);
        }
        if (keycode == Input.Keys.BUTTON_Y) {
            performTouch(inventoryActor);
        }
        if (keycode == Input.Keys.BUTTON_X) {
            performTouch(deckActor);
        }
        if (keycode == Input.Keys.BUTTON_A) {
            performTouch(menuActor);
        }
        return super.keyDown(keycode);
    }
    public void performTouch(Actor actor) {
        if (actor == null)
            return;
        actor.fire(eventTouchDown);
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                actor.fire(eventTouchUp);
            }
        }, 0.10f);
    }
    public void hideButtons() {
        if (isShowing)
            return;
        if (isHiding)
            return;
        isHiding = true;
        deckActor.addAction(Actions.sequence(Actions.fadeOut(0.10f), Actions.hide(), Actions.moveTo(deckActor.getX()+deckActor.getWidth(), deckActor.getY())));
        inventoryActor.addAction(Actions.sequence(Actions.fadeOut(0.15f), Actions.hide(), Actions.moveTo(inventoryActor.getX()+inventoryActor.getWidth(), inventoryActor.getY())));
        statsActor.addAction(Actions.sequence(Actions.fadeOut(0.20f), Actions.hide(), Actions.moveTo(statsActor.getX()+statsActor.getWidth(), statsActor.getY())));
        menuActor.addAction(Actions.sequence(Actions.fadeOut(0.25f), Actions.hide(), Actions.moveTo(menuActor.getX() + menuActor.getWidth(), menuActor.getY())));
        FThreads.delayInEDT(300, () -> isHiding = false);
    }
    public void showButtons() {
        if (console.isVisible())
            return;
        if (isHiding)
            return;
        if (isShowing)
            return;
        isShowing = true;
        menuActor.addAction(Actions.sequence(Actions.delay(0.1f), Actions.parallel(Actions.show(), Actions.alpha(opacity,0.1f), Actions.moveTo(referenceX, menuActor.getY(), 0.25f))));
        statsActor.addAction(Actions.sequence(Actions.delay(0.15f), Actions.parallel(Actions.show(), Actions.alpha(opacity,0.1f), Actions.moveTo(referenceX, statsActor.getY(), 0.25f))));
        inventoryActor.addAction(Actions.sequence(Actions.delay(0.2f), Actions.parallel(Actions.show(), Actions.alpha(opacity,0.1f), Actions.moveTo(referenceX, inventoryActor.getY(), 0.25f))));
        deckActor.addAction(Actions.sequence(Actions.delay(0.25f), Actions.parallel(Actions.show(), Actions.alpha(opacity,0.1f), Actions.moveTo(referenceX, deckActor.getY(), 0.25f))));
        FThreads.delayInEDT(300, () -> isShowing = false);
    }

    @Override
    public void connected(Controller controller) {

    }

    @Override
    public void disconnected(Controller controller) {

    }

    @Override
    public boolean buttonDown(Controller controller, int buttonIndex) {
        if (Forge.getCurrentScene() instanceof HudScene) {
            if (controller.getMapping().buttonA == buttonIndex)
                return ((HudScene) Forge.getCurrentScene()).keyDown(Input.Keys.BUTTON_A);
            if (controller.getMapping().buttonB == buttonIndex)
                return ((HudScene) Forge.getCurrentScene()).keyDown(Input.Keys.BUTTON_B);
            if (controller.getMapping().buttonX == buttonIndex)
                return ((HudScene) Forge.getCurrentScene()).keyDown(Input.Keys.BUTTON_X);
            if (controller.getMapping().buttonY == buttonIndex)
                return ((HudScene) Forge.getCurrentScene()).keyDown(Input.Keys.BUTTON_Y);
            if (controller.getMapping().buttonDpadUp == buttonIndex)
                return ((HudScene) Forge.getCurrentScene()).keyDown(Input.Keys.DPAD_UP);
            if (controller.getMapping().buttonDpadRight == buttonIndex)
                return ((HudScene) Forge.getCurrentScene()).keyDown(Input.Keys.DPAD_RIGHT);
            if (controller.getMapping().buttonDpadDown == buttonIndex)
                return ((HudScene) Forge.getCurrentScene()).keyDown(Input.Keys.DPAD_DOWN);
            if (controller.getMapping().buttonDpadLeft == buttonIndex)
                return ((HudScene) Forge.getCurrentScene()).keyDown(Input.Keys.DPAD_LEFT);
        } else if (Forge.getCurrentScene() instanceof UIScene) {
            if (controller.getMapping().buttonDpadUp == buttonIndex)
                return ((UIScene) Forge.getCurrentScene()).keyPressed(Input.Keys.DPAD_UP);
            if (controller.getMapping().buttonDpadRight == buttonIndex)
                return ((UIScene) Forge.getCurrentScene()).keyPressed(Input.Keys.DPAD_RIGHT);
            if (controller.getMapping().buttonDpadDown == buttonIndex)
                return ((UIScene) Forge.getCurrentScene()).keyPressed(Input.Keys.DPAD_DOWN);
            if (controller.getMapping().buttonDpadLeft == buttonIndex)
                return ((UIScene) Forge.getCurrentScene()).keyPressed(Input.Keys.DPAD_LEFT);
            if (controller.getMapping().buttonA == buttonIndex)
                return ((UIScene) Forge.getCurrentScene()).keyPressed(Input.Keys.BUTTON_A);
            if (controller.getMapping().buttonB == buttonIndex)
                return ((UIScene) Forge.getCurrentScene()).keyPressed(Input.Keys.BUTTON_B);
            if (controller.getMapping().buttonX == buttonIndex)
                return ((UIScene) Forge.getCurrentScene()).keyPressed(Input.Keys.BUTTON_X);
            if (controller.getMapping().buttonY == buttonIndex)
                return ((UIScene) Forge.getCurrentScene()).keyPressed(Input.Keys.BUTTON_Y);
            if (controller.getMapping().buttonR1 == buttonIndex)
                return ((UIScene) Forge.getCurrentScene()).keyPressed(Input.Keys.BUTTON_R1);
            if (controller.getMapping().buttonL1 == buttonIndex)
                return ((UIScene) Forge.getCurrentScene()).keyPressed(Input.Keys.BUTTON_L1);
            if (controller.getMapping().buttonR2 == buttonIndex)
                return ((UIScene) Forge.getCurrentScene()).keyPressed(Input.Keys.BUTTON_R2);
            if (controller.getMapping().buttonL2 == buttonIndex)
                return ((UIScene) Forge.getCurrentScene()).keyPressed(Input.Keys.BUTTON_L2);
            if (controller.getMapping().buttonBack == buttonIndex)
                return ((UIScene) Forge.getCurrentScene()).keyPressed(Input.Keys.BUTTON_SELECT);
            if (controller.getMapping().buttonStart == buttonIndex)
                return ((UIScene) Forge.getCurrentScene()).keyPressed(Input.Keys.BUTTON_START);
        }
        return false;
    }

    @Override
    public boolean buttonUp(Controller controller, int buttonIndex) {
        if (Forge.getCurrentScene() instanceof HudScene) {
            if (controller.getMapping().buttonA == buttonIndex)
                return ((HudScene) Forge.getCurrentScene()).keyUp(Input.Keys.BUTTON_A);
            if (controller.getMapping().buttonB == buttonIndex)
                return ((HudScene) Forge.getCurrentScene()).keyUp(Input.Keys.BUTTON_B);
            if (controller.getMapping().buttonX == buttonIndex)
                return ((HudScene) Forge.getCurrentScene()).keyUp(Input.Keys.BUTTON_X);
            if (controller.getMapping().buttonY == buttonIndex)
                return ((HudScene) Forge.getCurrentScene()).keyUp(Input.Keys.BUTTON_Y);
            if (controller.getMapping().buttonDpadUp == buttonIndex)
                return ((HudScene) Forge.getCurrentScene()).keyUp(Input.Keys.DPAD_UP);
            if (controller.getMapping().buttonDpadRight == buttonIndex)
                return ((HudScene) Forge.getCurrentScene()).keyUp(Input.Keys.DPAD_RIGHT);
            if (controller.getMapping().buttonDpadDown == buttonIndex)
                return ((HudScene) Forge.getCurrentScene()).keyUp(Input.Keys.DPAD_DOWN);
            if (controller.getMapping().buttonDpadLeft == buttonIndex)
                return ((HudScene) Forge.getCurrentScene()).keyUp(Input.Keys.DPAD_LEFT);
        }
        return false;
    }

    @Override
    public boolean axisMoved(Controller controller, int axisIndex, float value) {
        if (Forge.hasGamepad()) {
            if (Forge.getCurrentScene() instanceof HudScene) {
                if (controller.getAxis(controller.getMapping().axisLeftX) > 0.5f) {
                    ((HudScene) Forge.getCurrentScene()).keyDown(Input.Keys.DPAD_RIGHT);
                } else if (controller.getAxis(controller.getMapping().axisLeftX) < -0.5f) {
                    ((HudScene) Forge.getCurrentScene()).keyDown(Input.Keys.DPAD_LEFT);
                } else {
                    ((HudScene) Forge.getCurrentScene()).keyUp(Input.Keys.DPAD_LEFT);
                    ((HudScene) Forge.getCurrentScene()).keyUp(Input.Keys.DPAD_RIGHT);
                }
                if (controller.getAxis(controller.getMapping().axisLeftY) > 0.5f) {
                    ((HudScene) Forge.getCurrentScene()).keyDown(Input.Keys.DPAD_DOWN);
                } else if (controller.getAxis(controller.getMapping().axisLeftY) < -0.5f) {
                    ((HudScene) Forge.getCurrentScene()).keyDown(Input.Keys.DPAD_UP);
                } else {
                    ((HudScene) Forge.getCurrentScene()).keyUp(Input.Keys.DPAD_UP);
                    ((HudScene) Forge.getCurrentScene()).keyUp(Input.Keys.DPAD_DOWN);
                }
            } else if (Forge.getCurrentScene() instanceof UIScene) {
                if (controller.getAxis(4) == 1f) //L2
                    ((UIScene) Forge.getCurrentScene()).keyPressed(Input.Keys.BUTTON_L2);
                if (controller.getAxis(5) == 1f) //R2
                    ((UIScene) Forge.getCurrentScene()).keyPressed(Input.Keys.BUTTON_R2);
            }
        }

        return true;
    }

    class ConsoleToggleListener extends ActorGestureListener {
        public ConsoleToggleListener() {
            getGestureDetector().setLongPressSeconds(0.6f);
        }
        @Override
        public boolean longPress(Actor actor, float x, float y) {
            hideButtons();
            console.toggle();
            return super.longPress(actor, x, y);
        }
        @Override
        public void tap(InputEvent event, float x, float y, int count, int button) {
            super.tap(event, x, y, count, button);
            //show menu buttons if double tapping the avatar, for android devices without visible navigation buttons
            if (count > 1)
                showButtons();
        }
    }
}
