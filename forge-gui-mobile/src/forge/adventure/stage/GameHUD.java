package forge.adventure.stage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import com.github.tommyettinger.textra.TypingAdapter;
import com.github.tommyettinger.textra.TypingLabel;
import forge.Forge;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.scene.*;
import forge.adventure.util.*;
import forge.adventure.world.WorldSave;
import forge.deck.Deck;
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
    private final TextraLabel lifePoints;
    private final TextraLabel money;
    private final TextraLabel mana;
    private final Image miniMap, gamehud, mapborder, avatarborder, blank;
    private final InputEvent eventTouchDown;
    private final InputEvent eventTouchUp;
    private final TextraButton deckActor;
    private final TextraButton openMapActor;
    private final TextraButton menuActor;
    private final TextraButton statsActor;
    private final TextraButton inventoryActor;
    private final TextraButton exitToWorldMapActor;
    public final UIActor ui;
    private final Touchpad touchpad;
    private final Console console;
    float TOUCHPAD_SCALE = 70f, referenceX;
    boolean isHiding = false, isShowing = false;
    float opacity = 1f;
    private boolean debugMap;

    private final Dialog dialog;
    private boolean dialogOnlyInput;
    private final Array<TextraButton> dialogButtonMap = new Array<>();
    TextraButton selectedKey;

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
        openMapActor = ui.findActor("openmap");
        ui.onButtonPress("openmap", () -> GameHUD.this.openMap());
        menuActor = ui.findActor("menu");
        referenceX = menuActor.getX();
        statsActor = ui.findActor("statistic");
        inventoryActor = ui.findActor("inventory");
        gamehud = ui.findActor("gamehud");
        exitToWorldMapActor = ui.findActor("exittoworldmap");
        dialog = Controls.newDialog("");

        miniMapPlayer = new Image(new Texture(Config.instance().getFile("ui/minimap_player.png")));
        //create touchpad
        touchpad = new Touchpad(10, Controls.getSkin());
        touchpad.setBounds(15, 15, TOUCHPAD_SCALE, TOUCHPAD_SCALE);
        touchpad.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                if (MapStage.getInstance().isInMap()) {
                    MapStage.getInstance().getPlayerSprite().getMovementDirection().x += ((Touchpad) actor).getKnobPercentX();
                    MapStage.getInstance().getPlayerSprite().getMovementDirection().y += ((Touchpad) actor).getKnobPercentY();
                } else {
                    WorldStage.getInstance().getPlayerSprite().getMovementDirection().x += ((Touchpad) actor).getKnobPercentX();
                    WorldStage.getInstance().getPlayerSprite().getMovementDirection().y += ((Touchpad) actor).getKnobPercentY();
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
        ui.onButtonPress("exittoworldmap", () -> exitToWorldMap());
        lifePoints = ui.findActor("lifePoints");
        mana = ui.findActor("mana");
        money = ui.findActor("money");
        mana.setText("{Scale=80%}0/0");
        lifePoints.setText("{Scale=80%}20/20");
        AdventurePlayer.current().onLifeChange(() -> lifePoints.setText("{Scale=80%}"+AdventurePlayer.current().getLife() + "/" + AdventurePlayer.current().getMaxLife()));
        AdventurePlayer.current().onManaChange(() -> mana.setText("{Scale=80%}"+AdventurePlayer.current().getMana() + "/" + AdventurePlayer.current().getMaxMana()));

        WorldSave.getCurrentSave().getPlayer().onGoldChange(() -> money.setText("{Scale=80%}"+String.valueOf(AdventurePlayer.current().getGold())));
        addActor(ui);
        addActor(miniMapPlayer);
        console = new Console();
        console.setBounds(0, GuiBase.isAndroid() ? getHeight() : 0, getWidth(), getHeight() / 2);
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
    }

    private void openMap() {
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
        Vector2 c = new Vector2();
        screenToStageCoordinates(c.set(screenX, screenY));

        float x = (c.x - miniMap.getX()) / miniMap.getWidth();
        float y = (c.y - miniMap.getY()) / miniMap.getHeight();
        //map bounds
        if (Controls.actorContainsVector(miniMap, c)) {
            touchpad.setVisible(false);

            if (debugMap)
                WorldStage.getInstance().getPlayerSprite().setPosition(x * WorldSave.getCurrentSave().getWorld().getWidthInPixels(), y * WorldSave.getCurrentSave().getWorld().getHeightInPixels());

            return true;
        }
        return super.touchDragged(screenX, screenY, pointer);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        Vector2 c = new Vector2();
        Vector2 touch = new Vector2();
        screenToStageCoordinates(touch.set(screenX, screenY));
        screenToStageCoordinates(c.set(screenX, screenY));

        float x = (c.x - miniMap.getX()) / miniMap.getWidth();
        float y = (c.y - miniMap.getY()) / miniMap.getHeight();
        if (Controls.actorContainsVector(gamehud, c)) {
            super.touchDown(screenX, screenY, pointer, button);
            return true;
        }
        if (Controls.actorContainsVector(miniMap, c)) {
            if (debugMap)
                WorldStage.getInstance().getPlayerSprite().setPosition(x * WorldSave.getCurrentSave().getWorld().getWidthInPixels(), y * WorldSave.getCurrentSave().getWorld().getHeightInPixels());
            return true;
        }
        //auto follow touchpad
        if (GuiBase.isAndroid() && !MapStage.getInstance().getDialogOnlyInput() && !console.isVisible()) {
            if (!(Controls.actorContainsVector(avatar, touch)) // not inside avatar bounds
                    && !(Controls.actorContainsVector(miniMap, touch)) // not inside map bounds
                    && !(Controls.actorContainsVector(gamehud, touch)) //not inside gamehud bounds
                    && !(Controls.actorContainsVector(menuActor, touch)) //not inside menu button
                    && !(Controls.actorContainsVector(deckActor, touch)) //not inside deck button
                    && !(Controls.actorContainsVector(openMapActor, touch)) //not inside openmap button
                    && !(Controls.actorContainsVector(statsActor, touch)) //not inside stats button
                    && !(Controls.actorContainsVector(inventoryActor, touch)) //not inside inventory button
                    && !(Controls.actorContainsVector(exitToWorldMapActor, touch)) //not inside deck button
                    && (Controls.actorContainsVector(ui, touch)) //inside display bounds
                    && pointer < 1) { //not more than 1 pointer
                touchpad.setBounds(touch.x - TOUCHPAD_SCALE / 2, touch.y - TOUCHPAD_SCALE / 2, TOUCHPAD_SCALE, TOUCHPAD_SCALE);
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
        miniMapPlayer.setPosition(miniMap.getX() + xPosMini - miniMapPlayer.getWidth() / 2, miniMap.getY() + yPosMini - miniMapPlayer.getHeight() / 2);
        if (GuiBase.isAndroid()) // prevent drawing on top of console
            miniMapPlayer.setVisible(!console.isVisible() && miniMap.isVisible());
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
        if (miniMapTexture != null)
            miniMapTexture.dispose();
        miniMapTexture = new Texture(WorldSave.getCurrentSave().getWorld().getBiomeImage());
        if (miniMapToolTipTexture != null)
            miniMapToolTipTexture.dispose();
        if (miniMapToolTipPixmap != null)
            miniMapToolTipPixmap.dispose();
        miniMapToolTipPixmap = new Pixmap((int) (miniMap.getWidth() * 3), (int) (miniMap.getHeight() * 3), Pixmap.Format.RGBA8888);
        miniMapToolTipPixmap.drawPixmap(WorldSave.getCurrentSave().getWorld().getBiomeImage(), 0, 0, WorldSave.getCurrentSave().getWorld().getBiomeImage().getWidth(), WorldSave.getCurrentSave().getWorld().getBiomeImage().getHeight(), 0, 0, miniMapToolTipPixmap.getWidth(), miniMapToolTipPixmap.getHeight());
        miniMapToolTipTexture = new Texture(miniMapToolTipPixmap);
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

    private void exitToWorldMap(){
        dialog.getButtonTable().clear();
        dialog.getContentTable().clear();
        TextraButton YES = Controls.newTextButton(Forge.getLocalizer().getMessage("lblYes"), this::exitDungeonCallback);
        YES.setVisible(false);
        TextraButton NO = Controls.newTextButton(Forge.getLocalizer().getMessage("lblNo"), this::hideDialog);
        NO.setVisible(false);
        TypingLabel L = Controls.newTypingLabel(Forge.getLocalizer().getMessageorUseDefault("lblExitToWoldMap", "Exit to the World Map?"));
        L.setWrap(true);
        L.setTypingListener(new TypingAdapter() {
            @Override
            public void end() {
                YES.setVisible(true);
                NO.setVisible(true);
            }
        });

        dialog.getButtonTable().add(YES).width(60f);
        dialog.getButtonTable().add(NO).width(60f);
        dialog.getContentTable().add(L).width(120f);
        dialog.setKeepWithinStage(true);
        showDialog();

    }
    private void exitDungeonCallback(){
        hideDialog();
        Forge.switchScene(GameScene.instance());
        WorldStage.getInstance().getPlayerSprite().setMovementDirection(Vector2.Zero);
        MapStage.getInstance().getPlayerSprite().setMovementDirection(Vector2.Zero);

        gameStage.getPlayerSprite().stop();
        exitToWorldMapActor.setVisible(false);
    }

    private void menu() {
        gameStage.openMenu();
    }

    public void setVisibility(Actor actor, boolean visible) {
        if (actor != null)
            actor.setVisible(visible);
    }

    public void setAlpha(Actor actor, boolean visible) {
        if (actor != null) {
            if (visible)
                actor.getColor().a = 1f;
            else
                actor.getColor().a = 0.4f;
        }
    }

    public void showHideMap(boolean visible) {
        setVisibility(miniMap, visible);
        setVisibility(mapborder, visible);
        setVisibility(openMapActor, visible);
        setVisibility(miniMapPlayer, visible);
        setVisibility(gamehud, visible);
        setVisibility(lifePoints, visible);
        setVisibility(mana, visible);
        setVisibility(money, visible);
        setVisibility(blank, visible);
        setVisibility(exitToWorldMapActor, GameScene.instance().isInDungeonOrCave());
        setAlpha(avatarborder, visible);
        setAlpha(avatar, visible);
        setAlpha(deckActor, visible);
        setAlpha(menuActor, visible);
        setAlpha(statsActor, visible);
        setAlpha(inventoryActor, visible);
        setAlpha(exitToWorldMapActor, visible);
        opacity = visible ? 1f : 0.4f;
    }

    @Override
    public boolean keyUp(int keycode) {
        ui.pressUp(keycode);
        return super.keyUp(keycode);
    }

    @Override
    public boolean keyDown(int keycode) {
        if (dialogOnlyInput) {
            return dialogInput(keycode);
        }
        ui.pressDown(keycode);
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
        if (console.isVisible())
            return true;
        Button pressedButton = ui.buttonPressed(keycode);
        if (pressedButton != null) {
            performTouch(pressedButton);
        }
        return super.keyDown(keycode);
    }

    public boolean dialogInput(int keycode) {
        if (dialogOnlyInput) {
            if (KeyBinding.Up.isPressed(keycode)) {
                selectPreviousDialogButton();
            }
            if (KeyBinding.Down.isPressed(keycode)) {
                selectNextDialogButton();
            }
            if (KeyBinding.Use.isPressed(keycode)) {
                performTouch(this.getKeyboardFocus());
            }
        }
        return true;
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
        deckActor.addAction(Actions.sequence(Actions.fadeOut(0.10f), Actions.hide(), Actions.moveTo(deckActor.getX() + deckActor.getWidth(), deckActor.getY())));
        inventoryActor.addAction(Actions.sequence(Actions.fadeOut(0.15f), Actions.hide(), Actions.moveTo(inventoryActor.getX() + inventoryActor.getWidth(), inventoryActor.getY())));
        statsActor.addAction(Actions.sequence(Actions.fadeOut(0.20f), Actions.hide(), Actions.moveTo(statsActor.getX() + statsActor.getWidth(), statsActor.getY())));
        menuActor.addAction(Actions.sequence(Actions.fadeOut(0.25f), Actions.hide(), Actions.moveTo(menuActor.getX() + menuActor.getWidth(), menuActor.getY())));
        if (GameScene.instance().isInDungeonOrCave())
            exitToWorldMapActor.addAction(Actions.sequence(Actions.fadeOut(0.2f), Actions.hide(), Actions.moveTo(exitToWorldMapActor.getX() + exitToWorldMapActor.getWidth(), exitToWorldMapActor.getY())));
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
        menuActor.addAction(Actions.sequence(Actions.delay(0.1f), Actions.parallel(Actions.show(), Actions.alpha(opacity, 0.1f), Actions.moveTo(referenceX, menuActor.getY(), 0.25f))));
        statsActor.addAction(Actions.sequence(Actions.delay(0.15f), Actions.parallel(Actions.show(), Actions.alpha(opacity, 0.1f), Actions.moveTo(referenceX, statsActor.getY(), 0.25f))));
        inventoryActor.addAction(Actions.sequence(Actions.delay(0.2f), Actions.parallel(Actions.show(), Actions.alpha(opacity, 0.1f), Actions.moveTo(referenceX, inventoryActor.getY(), 0.25f))));
        deckActor.addAction(Actions.sequence(Actions.delay(0.25f), Actions.parallel(Actions.show(), Actions.alpha(opacity, 0.1f), Actions.moveTo(referenceX, deckActor.getY(), 0.25f))));
        if (GameScene.instance().isInDungeonOrCave())
            exitToWorldMapActor.addAction(Actions.sequence(Actions.delay(0.25f), Actions.parallel(Actions.show(), Actions.alpha(opacity, 0.1f), Actions.moveTo(referenceX, exitToWorldMapActor.getY(), 0.25f))));
        FThreads.delayInEDT(300, () -> isShowing = false);
    }

    public void setDebug(boolean b) {
        debugMap = b;
    }

    public void showDialog() {

        dialogButtonMap.clear();
        for (int i = 0; i < dialog.getButtonTable().getCells().size; i++) {
            dialogButtonMap.add((TextraButton) dialog.getButtonTable().getCells().get(i).getActor());
        }
        dialog.show(this, Actions.show());
        dialog.setPosition((this.getWidth() - dialog.getWidth()) / 2, (this.getHeight() - dialog.getHeight()) / 2);
        dialogOnlyInput = true;
        if (Forge.hasGamepad() && !dialogButtonMap.isEmpty())
            this.setKeyboardFocus(dialogButtonMap.first());
    }

    public void hideDialog() {
        dialog.hide(Actions.sequence(Actions.sizeTo(dialog.getOriginX(), dialog.getOriginY(), 0.3f), Actions.hide()));
        dialogOnlyInput = false;
        selectedKey = null;
    }

    private void selectNextDialogButton() {
        if (dialogButtonMap.size < 2)
            return;
        if (!(this.getKeyboardFocus() instanceof Button)) {
            this.setKeyboardFocus(dialogButtonMap.first());
            return;
        }
        for (int i = 0; i < dialogButtonMap.size; i++) {
            if (this.getKeyboardFocus() == dialogButtonMap.get(i)) {
                i += 1;
                i %= dialogButtonMap.size;
                this.setKeyboardFocus(dialogButtonMap.get(i));
                return;
            }
        }
    }

    private void selectPreviousDialogButton() {
        if (dialogButtonMap.size < 2)
            return;
        if (!(this.getKeyboardFocus() instanceof Button)) {
            this.setKeyboardFocus(dialogButtonMap.first());
            return;
        }
        for (int i = 0; i < dialogButtonMap.size; i++) {
            if (this.getKeyboardFocus() == dialogButtonMap.get(i)) {
                i -= 1;
                if (i < 0)
                    i = dialogButtonMap.size - 1;
                this.setKeyboardFocus(dialogButtonMap.get(i));
                return;
            }
        }
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
