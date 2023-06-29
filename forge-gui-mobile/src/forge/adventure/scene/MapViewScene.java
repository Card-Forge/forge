package forge.adventure.scene;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.github.tommyettinger.textra.TypingLabel;
import forge.Forge;
import forge.adventure.data.AdventureQuestData;
import forge.adventure.pointofintrest.PointOfInterest;
import forge.adventure.stage.GameHUD;
import forge.adventure.stage.WorldStage;
import forge.adventure.util.Controls;
import forge.adventure.util.Current;
import forge.adventure.world.WorldSave;

/**
 * Displays the rewards of a fight or a treasure
 */
public class MapViewScene extends UIScene {
    private static MapViewScene object;
    private final ScrollPane scroll;
    private final Image img;
    private Texture miniMapTexture;
    private final Image miniMapPlayer;
    private final Group table;

    public static MapViewScene instance() {
        if (object == null)
            object = new MapViewScene();
        return object;
    }

    private MapViewScene() {
        super(Forge.isLandscapeMode() ? "ui/map.json" : "ui/map_portrait.json");
        ui.onButtonPress("done", this::done);
        scroll = ui.findActor("map");
        table = new Group();
        scroll.setActor(table);
        img = new Image();
        miniMapPlayer = new Image();
        img.setPosition(0, 0);
        table.addActor(img);
        table.addActor(miniMapPlayer);
    }

    public boolean done() {
        GameHUD.getInstance().getTouchpad().setVisible(false);
        for (Actor a : table.getChildren()) {
            if (a instanceof TypingLabel)
                a.remove();
        }
        Forge.switchToLast();
        return true;
    }

    @Override
    public void enter() {
        if (miniMapTexture != null)
            miniMapTexture.dispose();
        miniMapTexture = new Texture(WorldSave.getCurrentSave().getWorld().getBiomeImage());
        img.setSize(WorldSave.getCurrentSave().getWorld().getBiomeImage().getWidth(), WorldSave.getCurrentSave().getWorld().getBiomeImage().getHeight());
        img.getParent().setSize(WorldSave.getCurrentSave().getWorld().getBiomeImage().getWidth(), WorldSave.getCurrentSave().getWorld().getBiomeImage().getHeight());
        img.setDrawable(new TextureRegionDrawable(miniMapTexture));
        miniMapPlayer.setDrawable(new TextureRegionDrawable(Current.player().avatar()));
        miniMapPlayer.setSize(Current.player().avatar().getRegionWidth(), Current.player().avatar().getRegionHeight());
        float avatarX = getMapX(WorldStage.getInstance().getPlayerSprite().getX()) - miniMapPlayer.getWidth() / 2;
        float avatarY = getMapY(WorldStage.getInstance().getPlayerSprite().getY()) - miniMapPlayer.getHeight() / 2;
        miniMapPlayer.setPosition(avatarX, avatarY);
        miniMapPlayer.layout();
        scroll.scrollTo(avatarX, avatarY, miniMapPlayer.getWidth(), miniMapPlayer.getHeight(), true, true);
        for (AdventureQuestData adq : Current.player().getQuests()) {
            if (adq.isTracked) {
                PointOfInterest poi = adq.getTargetPOI();
                if (poi != null) {
                    TypingLabel label = Controls.newTypingLabel("[%80][+GPS]{GRADIENT=RED;WHITE;1;1}-" + poi.getData().name + "{ENDGRADIENT}");
                    table.addActor(label);
                    label.setPosition(getMapX(poi.getPosition().x) - label.getWidth() / 2, getMapY(poi.getPosition().y) - label.getHeight() / 2);
                    label.skipToTheEnd();
                }
            }
        }
        super.enter();
    }
    float getMapX(float posX) {
        return (posX / (float) WorldSave.getCurrentSave().getWorld().getTileSize() / (float) WorldSave.getCurrentSave().getWorld().getWidthInTiles()) * img.getWidth();
    }
    float getMapY(float posY) {
        return (posY / (float) WorldSave.getCurrentSave().getWorld().getTileSize() / (float) WorldSave.getCurrentSave().getWorld().getHeightInTiles()) * img.getHeight();
    }

}
