package forge.adventure.stage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.viewport.FitViewport;
import forge.adventure.scene.Scene;
import forge.adventure.util.Controls;
import forge.adventure.world.WorldSave;

public class GameHUD {

    private Stage stage;
    private GameStage gameStage;
    private FitViewport stageViewport;
    private Image miniMap;
    private Image noiseMap;
    private Image miniMapPlayer;
    private Image noiseMapPlayer;
    private int miniMapSize=250;
    Label label;

    public GameHUD(GameStage gstage) {
        gameStage=gstage;
        stageViewport = new FitViewport(Scene.IntendedWidth,Scene.IntendedHeight);
        stage = new Stage(stageViewport, gameStage.getBatch()); //create stage with the stageViewport and the SpriteBatch given in Constructor


        label = Controls.newLabel("10");
        stage.addActor(label);
    }

    public Stage getStage() { return stage; }

    public void dispose(){
        stage.dispose();
    }

    public void draw() {

        int yPos=(int)gameStage.player.getY();
        int xPos=(int)gameStage.player.getX();
        label.setText(xPos+"/"+yPos);
        stage.act(Gdx.graphics.getDeltaTime()); //act the Hud
        stage.draw(); //draw the Hud
        int xposMini=(int)(((float)xPos/(float)WorldSave.getCurrentSave().world.GetTileSize()/(float)WorldSave.getCurrentSave().world.GetWidthInTiles())*miniMapSize);
        int yposMini=(int)(((float)yPos/(float)WorldSave.getCurrentSave().world.GetTileSize()/(float)WorldSave.getCurrentSave().world.GetHeightInTiles())*miniMapSize);
        miniMapPlayer.setPosition(miniMap.getX()+xposMini-2,miniMap.getY()+yposMini-2);
        noiseMapPlayer.setPosition(noiseMap.getX()+xposMini-2,noiseMap.getY()+yposMini-2);
    }

    public void Enter() {

        miniMap=new Image(new Texture(WorldSave.getCurrentSave().world.getBiomImage()));
        miniMap.setBounds(0,Scene.IntendedHeight-250,250,250);
        stage.addActor(miniMap);
        noiseMap=new Image(new Texture(WorldSave.getCurrentSave().world.getNoiseImage()));
        noiseMap.setBounds(Scene.IntendedWidth-250,Scene.IntendedHeight-250,250,250);

        Pixmap player=new Pixmap(5,5, Pixmap.Format.RGB888);
        player.setColor(1.0f,0.0f,0.0f,1.0f);
        player.fill();
        miniMapPlayer=new Image(new Texture(player));
        noiseMapPlayer=new Image(new Texture(player));

        stage.addActor(noiseMap);
        stage.addActor(miniMapPlayer);
        stage.addActor(noiseMapPlayer);

    }
}