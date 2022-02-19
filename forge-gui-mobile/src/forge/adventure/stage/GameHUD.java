package forge.adventure.stage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import forge.Forge;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.scene.Scene;
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
    UIActor ui;

    private GameHUD(GameStage gameStage) {
        super(new FitViewport(Scene.GetIntendedWidth(), Scene.GetIntendedHeight()), gameStage.getBatch());
        instance = this;
        this.gameStage = gameStage;

        ui = new UIActor(Config.instance().getFile(GuiBase.isAndroid() ? "ui/hud_mobile.json" : "ui/hud.json"));
        miniMap = ui.findActor("map");


        miniMapPlayer = new Image(new Texture(Config.instance().getFile("ui/minimap_player.png")));


        avatar = ui.findActor("avatar");
        ui.onButtonPress("menu", new Runnable() {
            @Override
            public void run() {
                GameHUD.this.menu();
            }
        });
        ui.onButtonPress("statistic", new Runnable() {
            @Override
            public void run() {
                Forge.switchScene(SceneType.PlayerStatisticScene.instance);
            }
        });
        ui.onButtonPress("deck", new Runnable() {
            @Override
            public void run() {
                GameHUD.this.openDeck();
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

    @Override
    public boolean touchDown (int screenX, int screenY, int pointer, int button)
    {
        Vector2 c=new Vector2();
        screenToStageCoordinates(c.set(screenX, screenY));

        float x=(c.x-miniMap.getX())/miniMap.getWidth();
        float y=(c.y-miniMap.getY())/miniMap.getHeight();

        float deckX = ui.findActor("deck").getX();
        float deckY = ui.findActor("deck").getY();
        float deckR = ui.findActor("deck").getRight();
        float deckT = ui.findActor("deck").getTop();
        //deck button bounds
        if (c.x>=deckX&&c.x<=deckR&&c.y>=deckY&&c.y<=deckT) {
            instance.openDeck();
            return true;
        }

        float menuX = ui.findActor("menu").getX();
        float menuY = ui.findActor("menu").getY();
        float menuR = ui.findActor("menu").getRight();
        float menuT = ui.findActor("menu").getTop();
        //menu button bounds
        if (c.x>=menuX&&c.x<=menuR&&c.y>=menuY&&c.y<=menuT) {
            instance.menu();
            return true;
        }

        float statsX = ui.findActor("statistic").getX();
        float statsY = ui.findActor("statistic").getY();
        float statsR = ui.findActor("statistic").getRight();
        float statsT = ui.findActor("statistic").getTop();
        //stats button bounds
        if (c.x>=statsX&&c.x<=statsR&&c.y>=statsY&&c.y<=statsT) {
            instance.statistic();
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

        //move player except touching the gamehud bounds and buttons
        if(x>=0&&x<=1.0&&y>=0&&y<=1.0)
        {
            WorldStage.getInstance().GetPlayer().setPosition(x*WorldSave.getCurrentSave().getWorld().getWidthInPixels(),y*WorldSave.getCurrentSave().getWorld().getHeightInPixels());
        }
        return super.touchDown(screenX,screenY,  pointer,button);
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

    private Object openDeck() {

        Forge.switchScene(SceneType.DeckSelectScene.instance);
        return null;
    }

    private Object menu() {
        gameStage.openMenu();
        return null;
    }
}