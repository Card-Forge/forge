package forge.adventure.scene;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.SnapshotArray;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TypingLabel;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import forge.Forge;
import forge.adventure.data.AdventureEventData;
import forge.adventure.data.AdventureQuestData;
import forge.adventure.player.AdventurePlayer;
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
    private final List<TypingLabel> details;
    private final float maxZoom = 1.2f;
    private final float minZoom = 0.25f;
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
        //TODO:Add Translations for buttons
        ui.onButtonPress("details", this::details);
        ui.onButtonPress("events", this::events);
        ui.onButtonPress("reputation", this::reputation);
        ui.onButtonPress("names", this::names);
        ui.onButtonPress("zoomIn", this::zoomIn);
        ui.onButtonPress("zoomOut", this::zoomOut);
        scroll = new ScrollPane(null,Controls.getSkin()) {
            @Override
            public void addScrollListener() {
                return;
            }
        };
        scroll.setName("map");
        scroll.setActor(Controls.newTextraLabel(""));
        scroll.setWidth(ui.findActor("map").getWidth());
        scroll.setHeight(ui.findActor("map").getHeight());
        ui.addActor(scroll);
        scroll.setZIndex(1);
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
        miniMapPlayer.setZIndex(2);
        details = Lists.newArrayList();
        ui.addListener(new InputListener() {
            public boolean scrolled(InputEvent event, float x, float y, float scrollAmountX, float scrollAmountY) {
                event.cancel();
                scroll.setScrollbarsVisible(true);
                if (scrollAmountY > 0) {
                    zoomOut();
                    return true;
                } else if (scrollAmountY < 0) {
                    zoomIn();
                    return true;
                }
                return false;
            }
        });
        stage.setScrollFocus(ui);

    }

    public void test() {
        img.setPosition((scroll.getScrollPercentX()*2334 +233)*0.1f + 0.9f*img.getX(),(2544-scroll.getScrollPercentY()*2544 +128)*0.1f + 0.9f*img.getY());
        img.setScale(img.getScaleX()*0.9f);
        miniMapPlayer.setPosition((scroll.getScrollPercentX()*2334 +233)*0.1f + 0.9f*miniMapPlayer.getX(),(2544-scroll.getScrollPercentY()*2544 +128)*0.1f + 0.9f*miniMapPlayer.getY());
        miniMapPlayer.setScale(miniMapPlayer.getScaleX()*0.9f);
        for(Actor actor : table.getChildren()) {
            if (actor instanceof TypingLabel) {
                actor.setPosition((scroll.getScrollPercentX() * 2334 + 233) * 0.1f + 0.9f * actor.getX(), (2544 - scroll.getScrollPercentY() * 2544 + 128) * 0.1f + 0.9f * actor.getY());
            }
        }
    }

    public boolean done() {
        GameHUD.getInstance().getTouchpad().setVisible(false);
        SnapshotArray<Actor> allActors = table.getChildren();
        for (int i = 0; i < allActors.size; i++) {
            if (allActors.get(i) instanceof TypingLabel) {
                allActors.get(i).remove();
                i--;
            }
        }
        labels.clear();
        positions.clear();
        details.clear();
        miniMapPlayer.setScale(1);
        img.setScale(1);
        img.setPosition(0,0);
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


    public void details() {
        TextraButton detailsButton = ui.findActor("details");
        if (detailsButton != null) {
            detailsButton.setVisible(false);
            detailsButton.setDisabled(true);
        }
        TextraButton eventButton = ui.findActor("events");
        if (eventButton != null) {
            eventButton.setVisible(true);
            eventButton.setDisabled(false);
        }
        List<PointOfInterest> allPois = Current.world().getAllPointOfInterest();
        for (PointOfInterest poi : allPois) {
            for (AdventureEventData data : AdventurePlayer.current().getEvents()) {
                if (data.sourceID.equals(poi.getID())) {
                    StringBuilder sb = new StringBuilder();

                    sb.append("[%?BLACKEN]");
                    if (data.isDraftComplete) {
                        sb.append("[red]!!![]");
                    }
                    sb.append(" ").append(data.getCardBlock());

                    TypingLabel label = Controls.newTypingLabel(sb.toString());
                    table.addActor(label);
                    details.add(label);
                    label.setPosition(img.getScaleX()*(getMapX(poi.getPosition().x) - label.getWidth() / 2) + img.getX(), img.getScaleY()*(getMapY(poi.getPosition().y) - label.getHeight() / 2) + img.getY());
                    label.skipToTheEnd();
                }
            }
        }
    }

    public void events() {
        TextraButton eventsButton = ui.findActor("events");
        if (eventsButton != null) {
            eventsButton.setVisible(false);
            eventsButton.setDisabled(true);
        }
        TextraButton repButton = ui.findActor("reputation");
        if (repButton != null) {
            repButton.setVisible(true);
            repButton.setDisabled(false);
        }
        for (TypingLabel detail : details) {
            table.removeActor(detail);
        }
        List<PointOfInterest> allPois = Current.world().getAllPointOfInterest();
        details.clear();
        for (PointOfInterest poi : allPois) {
            int rep = WorldSave.getCurrentSave().getPointOfInterestChanges(poi.getID()).getMapReputation();
            if (rep != 0) {
                TypingLabel label = Controls.newTypingLabel("[%?BLACKEN] " + rep);
                table.addActor(label);
                details.add(label);
                label.setPosition(img.getScaleX()*(getMapX(poi.getPosition().x) - label.getWidth() / 2) + img.getX(), img.getScaleY()*(getMapY(poi.getPosition().y) - label.getHeight() / 2) + img.getY());
                label.skipToTheEnd();
            }
        }
    }

    public void reputation() {
        TextraButton repButton = ui.findActor("reputation");
        if (repButton != null) {
            repButton.setVisible(false);
            repButton.setDisabled(true);
        }
        TextraButton namesButton = ui.findActor("names");
        if (namesButton != null) {
            namesButton.setVisible(true);
            namesButton.setDisabled(false);
        }
        for (TypingLabel detail : details) {
            table.removeActor(detail);
        }
        details.clear();
        List<PointOfInterest> allPois = Current.world().getAllPointOfInterest();
        for (PointOfInterest poi : allPois) {
            if (WorldSave.getCurrentSave().getPointOfInterestChanges(poi.getID()).isVisited()) {
                if ("cave".equalsIgnoreCase(poi.getData().type) || "dungeon".equalsIgnoreCase(poi.getData().type) || "castle".equalsIgnoreCase(poi.getData().type)) {
                    TypingLabel label = Controls.newTypingLabel("[%?BLACKEN] " + poi.getDisplayName());
                    table.addActor(label);
                    details.add(label);
                    label.setPosition(img.getScaleX()*(getMapX(poi.getPosition().x) - label.getWidth() / 2) + img.getX(), img.getScaleY()*(getMapY(poi.getPosition().y) - label.getHeight() / 2) + img.getY());
                    label.skipToTheEnd();
                }
            }
        }
    }

    public void names() {
        TextraButton namesButton = ui.findActor("names");
        if (namesButton != null) {
            namesButton.setVisible(false);
            namesButton.setDisabled(true);
        }
        TextraButton detailsButton = ui.findActor("details");
        if (detailsButton != null) {
            detailsButton.setVisible(true);
            detailsButton.setDisabled(false);
        }
        for (TypingLabel detail : details) {
            table.removeActor(detail);
        }
        details.clear();

    }

    public void zoomOut() {
        if (img.getScaleX()*0.9f > minZoom) {
            img.setPosition((scroll.getScrollX() + scroll.getWidth()/2) * 0.1f + 0.9f * img.getX(), (scroll.getMaxY() - scroll.getScrollY() + scroll.getHeight()/2) * 0.1f + 0.9f * img.getY());
            img.setScale(img.getScaleX() * 0.9f);
            miniMapPlayer.setPosition((scroll.getScrollX() + scroll.getWidth()/2) * 0.1f + 0.9f * miniMapPlayer.getX(), (scroll.getMaxY() - scroll.getScrollY() + scroll.getHeight()/2) * 0.1f + 0.9f * miniMapPlayer.getY());
            miniMapPlayer.setScale(miniMapPlayer.getScaleX() * 0.9f);
            for (Actor actor : table.getChildren()) {
                if (actor instanceof TypingLabel) {
                    actor.setPosition((scroll.getScrollX() + scroll.getWidth()/2) * 0.1f + 0.9f * actor.getX(), (scroll.getMaxY() - scroll.getScrollY() + scroll.getHeight()/2) * 0.1f + 0.9f * actor.getY());
                }
            }
        }
    }
    public void zoomIn() {
        if (img.getScaleX()*1.1f < maxZoom) {
            img.setPosition(-(scroll.getScrollX() + scroll.getWidth()/2) * 0.1f + 1.1f * img.getX(), -(scroll.getMaxY() - scroll.getScrollY() + scroll.getHeight()/2) * 0.1f + 1.1f * img.getY());
            img.setScale(img.getScaleX() * 1.1f);
            miniMapPlayer.setPosition(-(scroll.getScrollX() + scroll.getWidth()/2) * 0.1f + 1.1f * miniMapPlayer.getX(), -(scroll.getMaxY() - scroll.getScrollY() + scroll.getHeight()/2) * 0.1f + 1.1f * miniMapPlayer.getY());
            miniMapPlayer.setScale(miniMapPlayer.getScaleX() * 1.1f);
            for (Actor actor : table.getChildren()) {
                if (actor instanceof TypingLabel) {
                    actor.setPosition(-(scroll.getScrollX() + scroll.getWidth()/2) * 0.1f + 1.1f * actor.getX(), -(scroll.getMaxY() - scroll.getScrollY() + scroll.getHeight()/2) * 0.1f + 1.1f * actor.getY());
                }
            }
        }
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

        TextraButton detailsButton = ui.findActor("details");
        if (detailsButton != null) {
            detailsButton.setVisible(true);
            detailsButton.setDisabled(false);
        }
        TextraButton eventButton = ui.findActor("events");
        if (eventButton != null) {
            eventButton.setVisible(false);
            eventButton.setDisabled(true);
        }
        TextraButton repButton = ui.findActor("reputation");
        if (repButton != null) {
            repButton.setVisible(false);
            repButton.setDisabled(true);
        }
        TextraButton namesButton = ui.findActor("names");
        if (namesButton != null) {
            namesButton.setVisible(false);
            namesButton.setDisabled(true);
        }
        TextraButton zoomInButton = ui.findActor("zoomIn");
        if (zoomInButton != null) {
            zoomInButton.setVisible(true);
            zoomInButton.setDisabled(false);
        }
        TextraButton zoomOutButton = ui.findActor("zoomOut");
        if (zoomOutButton != null) {
            zoomOutButton.setVisible(true);
            zoomOutButton.setDisabled(false);
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

    public void clearBookMarks() {
        if (bookmark != null)
            bookmark.clear();
    }
}
