package forge.adventure.stage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad.TouchpadStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import forge.Forge;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.scene.SceneType;
import forge.adventure.util.Config;
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
    private Image miniMap;
    private TextButton deckActor, menuActor, statsActor;
    private boolean deckPressed = false;
    private boolean menuPressed = false;
    private boolean statsPressed = false;
    private UIActor ui;
    private Touchpad touchpad;
    private TouchpadStyle touchpadStyle;
    private Skin touchpadSkin;
    private Drawable touchBackground;
    private Drawable touchKnob;
    float TOUCHPAD_SCALE = 70f;
    float TOUCHPAD_KNOB_MIN_WIDTH = 40f;

    private GameHUD(GameStage gameStage) {
        super(new ScalingViewport(Scaling.fillX, 480f, 270f), gameStage.getBatch());
        instance = this;
        this.gameStage = gameStage;

        ui = new UIActor(Config.instance().getFile(GuiBase.isAndroid() ? "ui/hud_mobile.json" : "ui/hud.json"));
        miniMap = ui.findActor("map");
        deckActor = ui.findActor("deck");
        menuActor = ui.findActor("menu");
        statsActor = ui.findActor("statistic");

        miniMapPlayer = new Image(new Texture(Config.instance().getFile("ui/minimap_player.png")));
        //create touchpad skin
        touchpadSkin = new Skin();
        touchpadSkin.add("touchBackground", new Texture(Config.instance().getFile("ui/touchBackground.png")));
        touchpadSkin.add("touchKnob", new Texture(Config.instance().getFile("ui/touchKnob.png")));
        //set touchpad skin background and knob
        touchBackground = touchpadSkin.getDrawable("touchBackground");
        touchKnob = touchpadSkin.getDrawable("touchKnob");
        //set touchpad style
        touchpadStyle = new TouchpadStyle(touchBackground, touchKnob);
        //set touchpad knob size
        touchpadStyle.knob.setMinWidth(TOUCHPAD_KNOB_MIN_WIDTH);
        touchpadStyle.knob.setMinHeight(TOUCHPAD_KNOB_MIN_WIDTH);
        //create touchpad
        touchpad = new Touchpad(10, touchpadStyle);
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
        ui.onButtonPress("menu", new Runnable() {
            @Override
            public void run() {
                menu();
            }
        });
        ui.onButtonPress("statistic", new Runnable() {
            @Override
            public void run() {
                statistic();
            }
        });
        ui.onButtonPress("deck", new Runnable() {
            @Override
            public void run() {
                openDeck();
            }
        });
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
        miniMap = ui.findActor("map");

        addActor(ui);
        addActor(miniMapPlayer);
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
        //simulate button
        checkButtonState(deckActor, pointer);
        checkButtonState(menuActor, pointer);
        checkButtonState(statsActor, pointer);
        deckPressed = false;
        menuPressed = false;
        statsPressed = false;
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

        float deckX = deckActor.getX();
        float deckY = deckActor.getY();
        float deckR = deckActor.getRight();
        float deckT = deckActor.getTop();
        float deckOriginX = deckActor.getOriginX();
        //deck button bounds
        if (c.x>=deckX&&c.x<=deckR&&c.y>=deckY&&c.y<=deckT) {
            if (pointer < 1)
                deckPressed = true;
            return true;
        }

        float menuX = menuActor.getX();
        float menuY = menuActor.getY();
        float menuR = menuActor.getRight();
        float menuT = menuActor.getTop();
        float menuOriginX = menuActor.getOriginX();
        //menu button bounds
        if (c.x>=menuX&&c.x<=menuR&&c.y>=menuY&&c.y<=menuT) {
            if (pointer < 1)
                menuPressed = true;
            return true;
        }

        float statsX = statsActor.getX();
        float statsY = statsActor.getY();
        float statsR = statsActor.getRight();
        float statsT = statsActor.getTop();
        float statsOriginX = statsActor.getOriginX();
        //stats button bounds
        if (c.x>=statsX&&c.x<=statsR&&c.y>=statsY&&c.y<=statsT) {
            if (pointer < 1)
                statsPressed = true;
            return true;
        }

        float uiX = ui.findActor("gamehud").getX();
        float uiY = ui.findActor("gamehud").getY();
        float uiTop = ui.findActor("gamehud").getTop();
        float uiRight = ui.findActor("gamehud").getRight();
        //gamehud bounds
        if (c.x>=uiX&&c.x<=uiRight&&c.y>=uiY&&c.y<=uiTop) {
            return true;
        }

        float mMapX = ui.findActor("map").getX();
        float mMapY = ui.findActor("map").getY();
        float mMapT = ui.findActor("map").getTop();
        float mMapR = ui.findActor("map").getRight();
        //map bounds
        if (c.x>=mMapX&&c.x<=mMapR&&c.y>=mMapY&&c.y<=mMapT) {
            if (MapStage.getInstance().isInMap())
                return true;
            WorldStage.getInstance().GetPlayer().setPosition(x*WorldSave.getCurrentSave().getWorld().getWidthInPixels(),y*WorldSave.getCurrentSave().getWorld().getHeightInPixels());
            return true;
        }
        //display bounds
        float displayX = ui.getX();
        float displayY = ui.getY();
        float displayT = ui.getTop();
        float displayR = ui.getRight();
        //auto follow touchpad
        if (GuiBase.isAndroid()) {
            if (!(touch.x>=mMapX&&touch.x<=mMapR&&touch.y>=mMapY&&touch.y<=mMapT) // not inside map bounds
                && !(touch.x>=uiX&&touch.x<=uiRight&&touch.y>=uiY&&touch.y<=uiTop) //not inside gamehud bounds
                && (touch.x>=displayX&&touch.x<=displayR&&touch.y>=displayY&&touch.y<=displayT) //inside display bounds
                && pointer < 1) { //not more than 1 pointer
                    touchpad.setBounds(touch.x-TOUCHPAD_SCALE/2, touch.y-TOUCHPAD_SCALE/2, TOUCHPAD_SCALE, TOUCHPAD_SCALE);
                    touchpad.setVisible(true);
                    touchpad.setResetOnTouchUp(true);
                return super.touchDown(screenX, screenY, pointer, button);
            }
        }
        return super.touchDown(screenX, screenY, pointer, button);
    }
    void checkButtonState(Actor actor, int pointer) {
        Array<EventListener> listeners = actor.getListeners();
        for(int i=0;i<listeners.size;i++)
        {
            if(listeners.get(i) instanceof ClickListener){
                if (((ClickListener)listeners.get(i)).isVisualPressed()) {
                    ((ClickListener)listeners.get(i)).setVisualPressed(false);
                    ((ClickListener)listeners.get(i)).clicked(null, 0, 0);
                }
            }
        }
    }
    void updateVisualState(Actor actor, boolean state) {
        Array<EventListener> listeners = actor.getListeners();
        for(int i=0;i<listeners.size;i++)
        {
            if(listeners.get(i) instanceof ClickListener){
                ((ClickListener)listeners.get(i)).setVisualPressed(state);
            }
        }
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
        updateVisualState(statsActor, statsPressed);
        updateVisualState(menuActor, menuPressed);
        updateVisualState(deckActor, deckPressed);
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

    private void menu() {
        gameStage.openMenu();
    }
}
