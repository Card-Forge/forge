package forge.adventure.scene;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import forge.Forge;
import forge.adventure.stage.GameHUD;
import forge.adventure.stage.WorldStage;
import forge.adventure.util.Current;
import forge.adventure.world.WorldSave;

/**
 * Displays the rewards of a fight or a treasure
 */
public class MapViewScene extends UIScene   {
    private static MapViewScene object;
    private final ScrollPane scroll;
    private final Image img;
    private Texture miniMapTexture;
    private final Image miniMapPlayer;

    public static MapViewScene instance() {
        if(object==null)
            object=new MapViewScene();
        return object;
    }

    private MapViewScene() {
        super(Forge.isLandscapeMode() ? "ui/map.json" : "ui/map_portrait.json");


        ui.onButtonPress("done", this::done);

        scroll = ui.findActor("map");
        Group table=new Group();
        scroll.setActor(table);
        img=new Image();
        miniMapPlayer=new Image();
        img.setPosition(0,0);
        table.addActor(img);
        table.addActor(miniMapPlayer);
        //img.setFillParent(true);

    }

    public boolean done() {
        GameHUD.getInstance().getTouchpad().setVisible(false);
        Forge.switchToLast();
        return true;
    }
    @Override
    public void enter() {
        if(miniMapTexture!=null)
            miniMapTexture.dispose();
        miniMapTexture=new Texture(WorldSave.getCurrentSave().getWorld().getBiomeImage());
        //img.setSize(miniMapTexture.getWidth(),miniMapTexture.getHeight());
        img.setSize(WorldSave.getCurrentSave().getWorld().getBiomeImage().getWidth(),WorldSave.getCurrentSave().getWorld().getBiomeImage().getHeight());
        img.getParent().setSize(WorldSave.getCurrentSave().getWorld().getBiomeImage().getWidth(),WorldSave.getCurrentSave().getWorld().getBiomeImage().getHeight());
        img.setDrawable(new TextureRegionDrawable(miniMapTexture));
        miniMapPlayer.setDrawable(new TextureRegionDrawable(Current.player().avatar()));

        int yPos = (int) WorldStage.getInstance().getPlayerSprite().getY();
        int xPos = (int) WorldStage.getInstance().getPlayerSprite().getX();
        int xPosMini = (int) (((float) xPos / (float) WorldSave.getCurrentSave().getWorld().getTileSize() / (float) WorldSave.getCurrentSave().getWorld().getWidthInTiles()) * img.getWidth());
        int yPosMini = (int) (((float) yPos / (float) WorldSave.getCurrentSave().getWorld().getTileSize() / (float) WorldSave.getCurrentSave().getWorld().getHeightInTiles()) * img.getHeight());

        miniMapPlayer.setSize(Current.player().avatar().getRegionWidth(),Current.player().avatar().getRegionHeight());
        miniMapPlayer.setPosition( xPosMini - miniMapPlayer.getWidth()/2,   yPosMini -  miniMapPlayer.getHeight()/2);
        miniMapPlayer.layout();
        scroll.scrollTo(xPosMini - miniMapPlayer.getWidth()/2,   yPosMini -  miniMapPlayer.getHeight()/2,miniMapPlayer.getWidth(),
                miniMapPlayer.getHeight(),true,true);
        //img.setAlign(Align.center);


        super.enter();
    }

}
