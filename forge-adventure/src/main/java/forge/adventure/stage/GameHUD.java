package forge.adventure.stage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.scene.Scene;
import forge.adventure.scene.SceneType;
import forge.adventure.util.UIActor;
import forge.adventure.world.WorldSave;

public class GameHUD extends Stage {

    static public GameHUD instance;
    private final GameStage gameStage;
    private final FitViewport stageViewport;
    private final Image avatar;
    private final Image miniMapPlayer;
    private final UIActor ui;
    private final Label lifePoints;
    private final Label money;
    private Image miniMap;

    private GameHUD(GameStage gstage) {
        super(new FitViewport(Scene.GetIntendedWidth(), Scene.GetIntendedHeight()), gstage.getBatch());
        instance = this;
        gameStage = gstage;
        stageViewport = new FitViewport(Scene.GetIntendedWidth(), Scene.GetIntendedHeight());

        ui = new UIActor(AdventureApplicationAdapter.CurrentAdapter.GetRes().GetFile("ui/hud.json"));
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
        money = ui.findActor("money");
        money.setText("3000");
        miniMap = ui.findActor("map");
        addActor(ui);
        addActor(miniMapPlayer);
    }

    public static GameHUD getInstance() {
        return instance == null ? instance = new GameHUD(WorldStage.getInstance()) : instance;
    }


    @Override
    public void draw() {

        int yPos = (int) gameStage.player.getY();
        int xPos = (int) gameStage.player.getX();
        act(Gdx.graphics.getDeltaTime()); //act the Hud
        super.draw(); //draw the Hud
        int xposMini = (int) (((float) xPos / (float) WorldSave.getCurrentSave().world.GetTileSize() / (float) WorldSave.getCurrentSave().world.GetWidthInTiles()) * miniMap.getWidth());
        int yposMini = (int) (((float) yPos / (float) WorldSave.getCurrentSave().world.GetTileSize() / (float) WorldSave.getCurrentSave().world.GetHeightInTiles()) * miniMap.getHeight());
        miniMapPlayer.setPosition(miniMap.getX() + xposMini - 1, miniMap.getY() + yposMini - 1);
    }

    public void Enter() {//TODO leak


        miniMap.setDrawable(new TextureRegionDrawable(new Texture(WorldSave.getCurrentSave().world.getBiomImage())));
        avatar.setDrawable(new TextureRegionDrawable(WorldSave.getCurrentSave().player.avatar()));


    }

    private Object openDeck() {

        AdventureApplicationAdapter.CurrentAdapter.SwitchScene(SceneType.DeckEditScene.instance);
        return null;
    }

    private Object menu() {
        gameStage.openMenu();
        return null;
    }
}