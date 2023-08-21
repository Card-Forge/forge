package forge.adventure.scene;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TypingLabel;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import forge.Forge;
import forge.adventure.data.AdventureQuestData;
import forge.adventure.pointofintrest.PointOfInterest;
import forge.adventure.stage.GameHUD;
import forge.adventure.stage.WorldStage;
import forge.adventure.util.Controls;
import forge.adventure.util.Current;
import forge.adventure.world.WorldSave;

import java.util.List;
import java.util.Set;

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
    private final List<TypingLabel> labels;
    private int index = -1;
    private float avatarX = 0, avatarY = 0;
    private Set<Vector2> positions;
    private Set<PointOfInterest> bookmark;

    public static MapViewScene instance() {
        if (object == null)
            object = new MapViewScene();
        return object;
    }

    private MapViewScene() {
        super(Forge.isLandscapeMode() ? "ui/map.json" : "ui/map_portrait.json");
        ui.onButtonPress("done", this::done);
        ui.onButtonPress("quest", this::scroll);
        scroll = ui.findActor("map");
        labels = Lists.newArrayList();
        positions = Sets.newHashSet();
        bookmark = Sets.newHashSet();
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
        labels.clear();
        positions.clear();
        index = -1;
        Forge.switchToLast();
        return true;
    }

    public void addBookmark(PointOfInterest point) {
        if (point == null)
            return;
        bookmark.add(point);
    }

    public void removeBookmark(PointOfInterest point) {
        if (point == null)
            return;
        bookmark.remove(point);
    }

    public boolean scroll() {
        if (!labels.isEmpty()) {
            index++;
            if (index >= labels.size()) {
                index = -1;
                scroll.scrollTo(avatarX, avatarY, miniMapPlayer.getWidth(), miniMapPlayer.getHeight(), true, true);
                return true;
            }
            TypingLabel label = labels.get(index);
            scroll.scrollTo(label.getX(), label.getY(), miniMapPlayer.getWidth(), miniMapPlayer.getHeight(), true, true);
        }
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
        avatarX = getMapX(WorldStage.getInstance().getPlayerSprite().getX()) - miniMapPlayer.getWidth() / 2;
        avatarY = getMapY(WorldStage.getInstance().getPlayerSprite().getY()) - miniMapPlayer.getHeight() / 2;
        miniMapPlayer.setPosition(avatarX, avatarY);
        miniMapPlayer.layout();
        scroll.scrollTo(avatarX, avatarY, miniMapPlayer.getWidth(), miniMapPlayer.getHeight(), true, true);
        for (AdventureQuestData adq : Current.player().getQuests()) {
            PointOfInterest poi = adq.getTargetPOI();
            if (poi != null) {
                if (positions.contains(poi.getPosition()))
                    continue; //don't map duplicate position to prevent stacking
                TypingLabel label = Controls.newTypingLabel("[+GPS][%?BLACKEN] " + adq.name);
                labels.add(label);
                table.addActor(label);
                label.setPosition(getMapX(poi.getPosition().x) - label.getWidth() / 2, getMapY(poi.getPosition().y) - label.getHeight() / 2);
                label.skipToTheEnd();
                positions.add(poi.getPosition());
            }
        }
        for (PointOfInterest poi : bookmark) {
            TypingLabel label = Controls.newTypingLabel("[%75][+Star] ");
            table.addActor(label);
            label.setPosition(getMapX(poi.getPosition().x) - label.getWidth() / 2, getMapY(poi.getPosition().y) - label.getHeight() / 2);
            label.skipToTheEnd();
        }
        TextraButton questButton = ui.findActor("quest");
        if (questButton != null) {
            questButton.setDisabled(labels.isEmpty());
            questButton.setVisible(!labels.isEmpty());
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
