package forge.adventure.character;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.Array;
import forge.adventure.archipelago.ArchipelagoData;
import forge.adventure.archipelago.ArchipelagoMode;
import forge.adventure.archipelago.ArchipelagoRandomizer;
import forge.adventure.data.DialogData;
import forge.adventure.scene.TileMapScene;
import forge.adventure.stage.MapStage;
import forge.adventure.util.Config;
import forge.adventure.util.MapDialog;
import forge.adventure.util.Paths;

import java.util.HashMap;

/**
 * PortalActor
 * Extension of EntryActor, visible on map, multiple states that change behavior
 */
public class PortalActor extends EntryActor {
    private final HashMap<PortalAnimationTypes, Animation<TextureRegion>> animations = new HashMap<>();
    private Animation<TextureRegion> currentAnimation = null;
    private PortalAnimationTypes currentAnimationType = PortalAnimationTypes.Closed;

    float timer;

    float transitionTimer;

    public PortalActor(MapStage stage, int id, String targetMap, float x, float y, float w, float h, String direction, String currentMap, int portalTargetObject, String path) {
        super(stage, id, targetMap, x, y, w, h, direction, currentMap, portalTargetObject);
        load(path);
    }

    public MapStage getMapStage() {
        return stage;
    }

    @Override
    public void onPlayerCollide() {
        if (currentAnimationType == PortalAnimationTypes.Inactive) {
            //Activate portal? Launch Dialog?
        }
        if (currentAnimationType == PortalAnimationTypes.Active) {
            if (ArchipelagoData.getInstance().getArchipelagoMode() == ArchipelagoMode.networked_archipelago && ArchipelagoRandomizer.getInstance().getSlotData() == null) {
                // Check if the player has filled slotData, if not, tell them to connect to Archipelago before continuing.
                DialogData dialog = new DialogData();
                dialog.text =
                        """
                        You must first connect to the Archipelago server before continuing your adventure.
                        \s
                        Please open the game menu and look for the Archipelago button to set up your connection.
                        """;

                DialogData button = new DialogData();
                button.name = "Take me there";
                DialogData.ActionData action = new DialogData.ActionData();
                action.openArchipelagoSettings = true;
                button.action = new DialogData.ActionData[] { action };
                dialog.options = new DialogData[] { button };
                MapDialog mapDialog = new MapDialog(dialog, stage, -1, null);
                stage.showDialog();
                mapDialog.activate();
                return;
            }

            if (targetMap == null || targetMap.isEmpty()) {
                stage.exitDungeon(false, false);
            } else {
                if (targetMap.equals(currentMap)) {
                    stage.spawn(entryTargetObject);
                    stage.getPlayerSprite().playEffect(Paths.EFFECT_TELEPORT, 0.5f);
                    stage.startPause(1.5f);
                } else {
                    currentMap = targetMap;
                    TileMapScene.instance().loadNext(targetMap, entryTargetObject);
                    stage.getPlayerSprite().playEffect(Paths.EFFECT_TELEPORT, 0.5f);
                }
            }
        }
    }

    public void spawn() {
        switch (direction) {
            case "up":
                stage.getPlayerSprite().setPosition(x + w / 2 - stage.getPlayerSprite().getWidth() / 2, y + h);
                break;
            case "down":
                stage.getPlayerSprite().setPosition(x + w / 2 - stage.getPlayerSprite().getWidth() / 2, y - stage.getPlayerSprite().getHeight());
                break;
            case "right":
                stage.getPlayerSprite().setPosition(x - stage.getPlayerSprite().getWidth(), y + h / 2 - stage.getPlayerSprite().getHeight() / 2);
                break;
            case "left":
                stage.getPlayerSprite().setPosition(x + w, y + h / 2 - stage.getPlayerSprite().getHeight() / 2);
                break;
        }
    }

    protected void load(String path) {
        if (path == null || path.isEmpty()) return;
        animations.clear();
        for (PortalAnimationTypes stand : PortalAnimationTypes.values()) {
            Array<Sprite> anim = Config.instance().getAnimatedSprites(path, stand.toString());
            if (anim.size != 0) {
                animations.put(stand, new Animation<>(0.2f, anim));
                if (getWidth() == 0.0)//init size onload
                {
                    setWidth(anim.first().getWidth());
                    setHeight(anim.first().getHeight());
                }
            }
        }

        setAnimation(PortalAnimationTypes.Closed);
        updateAnimation();
    }

    public void setAnimation(PortalAnimationTypes type) {
        if (currentAnimationType != type) {
            currentAnimationType = type;
            updateAnimation();
        }
    }

    public void setAnimation(String typeName) {
        switch (typeName.toLowerCase()) {
            case "active":
                setAnimation(PortalAnimationTypes.Active);
                break;
            case "inactive":
                setAnimation(PortalAnimationTypes.Inactive);
                break;
            case "closed":
                setAnimation(PortalAnimationTypes.Closed);
                break;
            case "opening":
                setAnimation(PortalAnimationTypes.Opening);
                break;
            case "closing":
                setAnimation(PortalAnimationTypes.Closing);
                break;
        }
    }

    public String getAnimation() {
        return currentAnimationType.toString().toLowerCase();
    }

    private void updateAnimation() {
        PortalAnimationTypes aniType = currentAnimationType;
        if (!animations.containsKey(aniType)) {
            aniType = PortalAnimationTypes.Inactive;
        }
        if (!animations.containsKey(aniType)) {
            return;
        }
        currentAnimation = animations.get(aniType);
    }

    public enum PortalAnimationTypes {
        Closed,
        Active,
        Inactive,
        Opening,
        Closing
    }

    public void act(float delta) {
        timer += delta;
        super.act(delta);
    }

    public void draw(Batch batch, float parentAlpha) {
        if (currentAnimation == null) {
            return;
        }
        super.draw(batch, parentAlpha);
        beforeDraw(batch, parentAlpha);

        TextureRegion currentFrame;
        if (currentAnimationType.equals(PortalAnimationTypes.Opening) || currentAnimationType.equals(PortalAnimationTypes.Closing)) {
            currentFrame = currentAnimation.getKeyFrame(transitionTimer, false);
        } else {
            currentFrame = currentAnimation.getKeyFrame(timer, true);
        }

        setHeight(currentFrame.getRegionHeight());
        setWidth(currentFrame.getRegionWidth());
        Color oldColor = batch.getColor().cpy();
        batch.setColor(getColor());
        batch.draw(currentFrame, getX(), getY(), getWidth(), getHeight());
        batch.setColor(oldColor);
        super.draw(batch, parentAlpha);
        //batch.draw(getDebugTexture(),getX(),getY());
    }
}

