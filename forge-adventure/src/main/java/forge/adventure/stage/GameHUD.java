package forge.adventure.stage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.scene.Scene;
import forge.adventure.scene.SceneType;
import forge.adventure.util.Current;
import forge.adventure.util.Config;
import forge.adventure.util.UIActor;
import forge.adventure.world.AdventurePlayer;
import forge.adventure.world.WorldSave;

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

    private GameHUD(GameStage gameStage) {
        super(new FitViewport(Scene.GetIntendedWidth(), Scene.GetIntendedHeight()), gameStage.getBatch());
        instance = this;
        this.gameStage = gameStage;

        UIActor ui = new UIActor(Config.instance().getFile("ui/hud.json"));
        miniMap = ui.findActor("map");

        Pixmap player = new Pixmap(3, 3, Pixmap.Format.RGB888);
        player.setColor(1.0f, 0.0f, 0.0f, 1.0f);
        player.fill();
        miniMapPlayer = new Image(new Texture(player));


        avatar = ui.findActor("avatar");
        ui.onButtonPress("menu", () -> menu());
        ui.onButtonPress("deck", () -> openDeck());
        lifePoints = ui.findActor("lifePoints");
        lifePoints.setText("20/20");
        AdventurePlayer.current().onLifeChange(()->  lifePoints.setText(AdventurePlayer.current().getLife() +"/"+ AdventurePlayer.current().getMaxLife()));
        money = ui.findActor("money");
        WorldSave.getCurrentSave().getPlayer().onGoldChange(()->  money.setText(String.valueOf(AdventurePlayer.current().getGold()))) ;
        miniMap = ui.findActor("map");

        addActor(ui);
        addActor(miniMapPlayer);
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
        if(x>=0&&x<=1.0&&y>=0&&y<=1.0)
        {
            WorldStage.getInstance().GetPlayer().setPosition(x*WorldSave.getCurrentSave().getWorld().getWidthInPixels(),y*WorldSave.getCurrentSave().getWorld().getHeightInPixels());
            return true;
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
        miniMapPlayer.setPosition(miniMap.getX() + xPosMini - 1, miniMap.getY() + yPosMini - 1);
    }

    Texture miniMapTexture;
    public void enter() {

        if(miniMapTexture==null)
        {
            miniMapTexture=new Texture(WorldSave.getCurrentSave().getWorld().getBiomeImage());
        }

        miniMap.setDrawable(new TextureRegionDrawable(miniMapTexture));
        avatar.setDrawable(new TextureRegionDrawable(Current.player().avatar()));


    }

    private Object openDeck() {

        AdventureApplicationAdapter.instance.switchScene(SceneType.DeckEditScene.instance);
        return null;
    }

    private Object menu() {
        gameStage.openMenu();
        return null;
    }
}