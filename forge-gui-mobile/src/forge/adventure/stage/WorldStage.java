package forge.adventure.stage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.Viewport;
import forge.Forge;
import forge.adventure.character.CharacterSprite;
import forge.adventure.character.EnemySprite;
import forge.adventure.data.*;
import forge.adventure.pointofintrest.PointOfInterest;
import forge.adventure.scene.DuelScene;
import forge.adventure.scene.RewardScene;
import forge.adventure.scene.Scene;
import forge.adventure.scene.TileMapScene;
import forge.adventure.util.*;
import forge.adventure.world.World;
import forge.adventure.world.WorldSave;
import forge.gui.FThreads;
import forge.screens.TransitionScreen;
import forge.sound.SoundEffectType;
import forge.sound.SoundSystem;
import forge.util.MyRandom;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;


/**
 * Stage for the over world. Will handle monster spawns
 */
public class WorldStage extends GameStage implements SaveFileContent {
    private static WorldStage instance = null;
    protected EnemySprite currentMob;
    protected Random rand = MyRandom.getRandom();
    WorldBackground background;
    private float spawnDelay = 0;
    private static final float spawnInterval = 4;//todo config
    private PointOfInterestMapSprite collidingPoint;
    protected ArrayList<Pair<Float, EnemySprite>> enemies = new ArrayList<>();
    private final static Float dieTimer = 20f;//todo config
    private Float globalTimer = 0f;
    private transient boolean directlyEnterPOI = false;

    NavArrowActor navArrow;
    public WorldStage() {
        super();
        background = new WorldBackground(this);
        addActor(background);
        background.setZIndex(0);
        navArrow = new NavArrowActor();
        addActor(navArrow);
        navArrow.toFront();
    }

    public static WorldStage getInstance() {
        return instance == null ? instance = new WorldStage() : instance;
    }

    final Rectangle tempBoundingRect = new Rectangle();
    final Vector2 enemyMoveVector = new Vector2();

    @Override
    protected void onActing(float delta) {
        if (isPaused() || MapStage.getInstance().isDialogOnlyInput())
            return;
        drawNavigationArrow();
        if (player.isMoving()) {
            handleMonsterSpawn(delta);
            handlePointsOfInterestCollision();
            globalTimer += delta;
            Iterator<Pair<Float, EnemySprite>> it = enemies.iterator();
            while (it.hasNext()) {
                Pair<Float, EnemySprite> pair = it.next();
                if (globalTimer >= pair.getKey() + pair.getValue().getLifetime()) {
                    AdventureQuestController.instance().updateDespawn(pair.getValue());
                    AdventureQuestController.instance().showQuestDialogs(MapStage.getInstance());
                    foregroundSprites.removeActor(pair.getValue());
                    it.remove();
                    continue;
                }
                EnemySprite mob = pair.getValue();

                if (!currentModifications.containsKey(PlayerModification.Hide)) {
                    enemyMoveVector.set(player.getX(), player.getY()).sub(mob.pos());
                    enemyMoveVector.setLength(mob.speed() * delta);
                    tempBoundingRect.set(mob.getX() + enemyMoveVector.x, mob.getY() + enemyMoveVector.y, mob.getWidth(), mob.getHeight() * mob.getCollisionHeight());

                    if (!mob.getData().flying && WorldSave.getCurrentSave().getWorld().collidingTile(tempBoundingRect))//if direct path is not possible
                    {
                        tempBoundingRect.set(mob.getX() + enemyMoveVector.x, mob.getY(), mob.getWidth(), mob.getHeight());
                        if (WorldSave.getCurrentSave().getWorld().collidingTile(tempBoundingRect))//if only x path is not possible
                        {
                            tempBoundingRect.set(mob.getX(), mob.getY() + enemyMoveVector.y, mob.getWidth(), mob.getHeight());
                            if (!WorldSave.getCurrentSave().getWorld().collidingTile(tempBoundingRect))//if y path is possible
                            {
                                mob.moveBy(0, enemyMoveVector.y);
                            }
                        } else {

                            mob.moveBy(enemyMoveVector.x, 0);
                        }
                    } else {
                        mob.moveBy(enemyMoveVector.x, enemyMoveVector.y);
                    }
                }

                if (player.collideWith(mob)) {
                    player.setAnimation(CharacterSprite.AnimationTypes.Attack);
                    player.playEffect(Paths.EFFECT_SPARKS, 0.5f);
                    mob.setAnimation(CharacterSprite.AnimationTypes.Attack);
                    SoundSystem.instance.play(SoundEffectType.Block, false);
                    Gdx.input.vibrate(50);
                    int duration = mob.getData().boss ? 400 : 200;
                    if (Controllers.getCurrent() != null && Controllers.getCurrent().canVibrate())
                        Controllers.getCurrent().startVibration(duration, 1);
                    Forge.restrictAdvMenus = true;
                    player.clearCollisionHeight();
                    startPause(0.8f, () -> {
                        Forge.setCursor(null, Forge.magnifyToggle ? "1" : "2");
                        SoundSystem.instance.play(SoundEffectType.ManaBurn, false);
                        DuelScene duelScene = DuelScene.instance();
                        FThreads.invokeInEdtNowOrLater(() -> {
                            Forge.setTransitionScreen(new TransitionScreen(() -> {
                                duelScene.initDuels(player, mob);
                                Forge.switchScene(duelScene);
                            }, Forge.takeScreenshot(), true, false, false, false, "", Current.player().avatar(), mob.getAtlasPath(), Current.player().getName(), mob.getName()));
                            currentMob = mob;
                            WorldSave.getCurrentSave().autoSave();
                        });
                    });
                    break;
                }
            }
        } else {
            for (Pair<Float, EnemySprite> pair : enemies) {
                pair.getValue().setAnimation(CharacterSprite.AnimationTypes.Idle);
            }
        }
    }

    private void removeEnemy(EnemySprite currentMob) {
        currentMob.removeAfterEffects();
        Iterator<Pair<Float, EnemySprite>> it = enemies.iterator();
        while (it.hasNext()) {
            Pair<Float, EnemySprite> pair = it.next();
            if (pair.getValue() == currentMob) {
                it.remove();
                return;
            }
        }
    }

    @Override
    public void setWinner(boolean playerIsWinner) {
        if (playerIsWinner) {
            currentMob.clearCollisionHeight();
            Current.player().win();
            player.setAnimation(CharacterSprite.AnimationTypes.Attack);
            currentMob.playEffect(Paths.EFFECT_BLOOD, 0.5f);
            Timer.schedule(new Timer.Task() {
                @Override
                public void run() {
                    currentMob.setAnimation(CharacterSprite.AnimationTypes.Death);
                    currentMob.resetCollisionHeight();
                    startPause(0.3f, () -> {
                        RewardScene.instance().loadRewards(currentMob.getRewards(), RewardScene.Type.Loot, null);
                        WorldStage.this.removeEnemy(currentMob);
                        AdventureQuestController.instance().updateQuestsWin(currentMob);
                        AdventureQuestController.instance().showQuestDialogs(MapStage.getInstance());
                        Forge.switchScene(RewardScene.instance());
                        currentMob = null;
                    });
                }
            }, 1f);
        } else {
            currentMob.clearCollisionHeight();
            player.setAnimation(CharacterSprite.AnimationTypes.Hit);
            currentMob.setAnimation(CharacterSprite.AnimationTypes.Attack);
            startPause(0.5f, () -> {
                currentMob.resetCollisionHeight();
                Current.player().defeated();
                AdventureQuestController.instance().updateQuestsLose(currentMob);
                AdventureQuestController.instance().showQuestDialogs(MapStage.getInstance());
                WorldStage.this.removeEnemy(currentMob);
                currentMob = null;
            });
        }
    }

    public void handlePointsOfInterestCollision() {
        for (Actor actor : foregroundSprites.getChildren()) {
            if (actor.getClass() == PointOfInterestMapSprite.class) {
                PointOfInterestMapSprite point = (PointOfInterestMapSprite) actor;
                if (!point.getPointOfInterest().getActive())
                {
                    continue;
                }
                if (player.collideWith(point.getBoundingRect())) {
                    if (point == collidingPoint) {
                        continue;
                    }
                    try {
                        WorldSave.getCurrentSave().autoSave();
                        TileMapScene.instance().load(point.getPointOfInterest());
                        stop();
                        Forge.switchScene(TileMapScene.instance());
                        point.getMapSprite().checkOut();
                    } catch (Exception e) {
                        System.err.println("Error loading map...");
                        e.printStackTrace();
                    }
                } else {
                    if (point == collidingPoint) {
                        collidingPoint = null;
                    }
                }
            }
        }
    }

    @Override
    public boolean isColliding(Rectangle boundingRect) {
        if (currentModifications.containsKey(PlayerModification.Fly))
            return false;
        return WorldSave.getCurrentSave().getWorld().collidingTile(boundingRect);
    }

    @Override
    public Vector2 adjustMovement(Vector2 direction, Rectangle boundingRect) {
        if (isColliding(boundingRect)) //if player is already colliding (after flying or teleport) allow to move off collision
            return direction;
        return super.adjustMovement(direction, boundingRect);
    }

    public boolean spawn(String enemy) {
        return spawn(WorldData.getEnemy(enemy));
    }

    private void handleMonsterSpawn(float delta) {
        for (EnemySprite questSprite : AdventureQuestController.instance().getQuestSprites()) {
            if (!foregroundSprites.getChildren().contains(questSprite, true)) {
                spawnQuestSprite(questSprite,2.5f);
            }
        }

        World world = WorldSave.getCurrentSave().getWorld();
        int currentBiome = World.highestBiome(world.getBiome((int) player.getX() / world.getTileSize(), (int) player.getY() / world.getTileSize()));
        List<BiomeData> biomeData = WorldSave.getCurrentSave().getWorld().getData().GetBiomes();
        float sprintingMod = currentModifications.containsKey(PlayerModification.Sprint) ? 2 : 1;
        if (biomeData.size() <= currentBiome) {// "if isOnRoad
            player.setMoveModifier(1.5f * sprintingMod);
            return;
        }
        player.setMoveModifier(1.0f * sprintingMod);
        BiomeData data = biomeData.get(currentBiome);
        if (data == null) return;

        spawnDelay -= delta;
        if (spawnDelay >= 0) return;
        spawnDelay = spawnInterval + (rand.nextFloat() * 4.0f);

        ArrayList<EnemyData> list = data.getEnemyList();
        if (list == null)
            return;
        EnemyData enemyData = data.getEnemy(1.0f);
        spawn(enemyData);
    }

    private boolean spawn(EnemySprite sprite){
        if (sprite == null)
            return false;
        float unit = Scene.getIntendedHeight() / 6f;
        Vector2 spawnPos = new Vector2(1, 1);
        for (int j = 0; j < 10; j++) {
            spawnPos.setLength(unit + (unit * 3) * rand.nextFloat());
            spawnPos.setAngleDeg(360 * rand.nextFloat());
            for (int i = 0; i < 10; i++) {
                boolean enemyXIsBigger = sprite.getX() > player.getX();
                boolean enemyYIsBigger = sprite.getY() > player.getY();
                sprite.setX(player.getX() + spawnPos.x + (i * sprite.getWidth() * (enemyXIsBigger ? 1 : -1)));//maybe find a better way to get spawn points
                sprite.setY(player.getY() + spawnPos.y + (i * sprite.getHeight() * (enemyYIsBigger ? 1 : -1)));
                if (sprite.getData().flying || !WorldSave.getCurrentSave().getWorld().collidingTile(sprite.boundingRect())) {
                    enemies.add(Pair.of(globalTimer, sprite));
                    foregroundSprites.addActor(sprite);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean spawn(EnemyData enemyData) {
        if (enemyData == null)
            return false;
        EnemySprite sprite = new EnemySprite(enemyData);
        return spawn(sprite);

    }

    private boolean spawnQuestSprite(EnemySprite sprite, float distanceMultiplier){
        if (sprite == null)
            return false;
        float unit = Scene.getIntendedHeight() / 6f;
        Vector2 spawnPos = new Vector2(1, 1);
        for (int j = 0; j < 10; j++) {
            spawnPos.setLength((unit + (unit * 3) * rand.nextFloat()) * distanceMultiplier);
            spawnPos.setAngleDeg(360 * rand.nextFloat());
            for (int i = 0; i < 10; i++) {
                boolean enemyXIsBigger = sprite.getX() > player.getX();
                boolean enemyYIsBigger = sprite.getY() > player.getY();
                sprite.setX(player.getX() + spawnPos.x + (i * sprite.getWidth() * (enemyXIsBigger ? 1 : -1)));//maybe find a better way to get spawn points
                sprite.setY(player.getY() + spawnPos.y + (i * sprite.getHeight() * (enemyYIsBigger ? 1 : -1)));
                if (sprite.getData().flying || !WorldSave.getCurrentSave().getWorld().collidingTile(sprite.boundingRect())) {
                    enemies.add(Pair.of(globalTimer, sprite));
                    foregroundSprites.addActor(sprite);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void draw() {
        background.setPlayerPos(player.getX(), player.getY());
        //spriteGroup.setCullingArea(new Rectangle(player.getX()-getViewport().getWorldHeight()/2,player.getY()-getViewport().getWorldHeight()/2,getViewport().getWorldHeight(),getViewport().getWorldHeight()));
        super.draw();
        if (WorldSave.getCurrentSave().getPlayer().hasAnnounceFantasy()) {
            MapStage.getInstance().showDeckAwardDialog("{BLINK=WHITE;RED}Chaos Mode!{ENDBLINK}\n" +
                    "Enemy will use Preconstructed or Random Generated Decks. Genetic AI Decks will be available to some enemies on Hard difficulty.",
                    WorldSave.getCurrentSave().getPlayer().getSelectedDeck());
            WorldSave.getCurrentSave().getPlayer().clearAnnounceFantasy();
        } else if (WorldSave.getCurrentSave().getPlayer().hasAnnounceCustom()) {
            MapStage.getInstance().showDeckAwardDialog("{GRADIENT}Custom Deck Mode!{ENDGRADIENT}\n" +
                    "Some enemies will use Genetic AI Decks randomly.", WorldSave.getCurrentSave().getPlayer().getSelectedDeck());
            WorldSave.getCurrentSave().getPlayer().clearAnnounceCustom();
        }
    }

    public void setDirectlyEnterPOI(){
        directlyEnterPOI = true; //On a new game, we want to automatically enter any POI the player overlaps with.
    }

    public PointOfInterestMapSprite getMapSprite(PointOfInterest poi) {
        if (poi == null)
            return null;
        for (Actor actor : foregroundSprites.getChildren()) {
            if (actor.getClass() == PointOfInterestMapSprite.class) {
                PointOfInterestMapSprite point = (PointOfInterestMapSprite) actor;
                if (poi == point.getPointOfInterest() && poi.getPosition() == point.getPointOfInterest().getPosition())
                    return point;
            }
        }
        return null;
    }

    @Override
    public void enter() {
        getPlayerSprite().LoadPos();
        getPlayerSprite().setMovementDirection(Vector2.Zero);
        if (directlyEnterPOI) {
            directlyEnterPOI = false;
        }
        else {
            for (Actor actor : foregroundSprites.getChildren()) {
                if (actor.getClass() == PointOfInterestMapSprite.class) {
                    PointOfInterestMapSprite point = (PointOfInterestMapSprite) actor;
                    if (player.collideWith(point.getBoundingRect())) {
                        collidingPoint = point;
                    }
                }
            }
        }
        setBounds(WorldSave.getCurrentSave().getWorld().getWidthInPixels(), WorldSave.getCurrentSave().getWorld().getHeightInPixels());
        GridPoint2 pos = background.translateFromWorldToChunk(player.getX(), player.getY());
        background.loadChunk(pos.x, pos.y);
    }

    @Override
    public void leave() {
        getPlayerSprite().storePos();
    }

    @Override
    public void load(SaveFileData data) {
        try {
            clearCache();
            List<Float> timeouts = (List<Float>) data.readObject("timeouts");
            List<String> names = (List<String>) data.readObject("names");
            List<Float> x = (List<Float>) data.readObject("x");
            List<Float> y = (List<Float>) data.readObject("y");
            List<String> questStageIDs = (List<String>) data.readObject("questStageIDs");
            for (int i = 0; i < timeouts.size(); i++) {
                EnemySprite sprite = new EnemySprite(WorldData.getEnemy(names.get(i)));
                sprite.setX(x.get(i));
                sprite.setY(y.get(i));
                sprite.questStageID = questStageIDs.get(i);
                if (sprite.questStageID != null)
                    AdventureQuestController.instance().rematchQuestSprite(sprite);
                enemies.add(Pair.of(timeouts.get(i), sprite));
                foregroundSprites.addActor(sprite);
            }
            globalTimer = data.readFloat("globalTimer");
        } catch (Exception e) {

        }
    }

    public void clearCache() {
        for (Pair<Float, EnemySprite> enemy : enemies)
            foregroundSprites.removeActor(enemy.getValue());
        enemies.clear();
        background.clear();
        player = null;
    }

    @Override
    public SaveFileData save() {
        SaveFileData data = new SaveFileData();
        List<Float> timeouts = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<Float> x = new ArrayList<>();
        List<Float> y = new ArrayList<>();
        List<String> questStageIDs = new ArrayList<>();
        for (Pair<Float, EnemySprite> enemy : enemies) {
            timeouts.add(enemy.getKey());
            names.add(enemy.getValue().getData().getName());
            x.add(enemy.getValue().getX());
            y.add(enemy.getValue().getY());
            questStageIDs.add(enemy.getValue().questStageID);
        }
        data.storeObject("timeouts", timeouts);
        data.storeObject("names", names);
        data.storeObject("x", x);
        data.storeObject("y", y);
        data.storeObject("questStageIDs", questStageIDs);
        data.store("globalTimer", globalTimer);
        return data;
    }

    @Override
    public Viewport getViewport() {
        return super.getViewport();
    }


    public void removeNearestEnemy() {
        float shortestDist = Float.MAX_VALUE;
        EnemySprite enemy = null;
        for (Pair<Float, EnemySprite> pair : enemies) {
            float dist = pair.getValue().pos().sub(player.pos()).len();
            if (dist < shortestDist) {
                shortestDist = dist;
                enemy = pair.getValue();
            }
        }
        if (enemy != null) {
            enemy.playEffect(Paths.EFFECT_KILL);
            removeEnemy(enemy);
            player.playEffect(Paths.TRIGGER_KILL);
        }
    }

    private void drawNavigationArrow(){
        Vector2 navDirection = null;
        for (AdventureQuestData adq: Current.player().getQuests())
        {
            if (adq.isTracked) {
                if (adq.getTargetPOI() != null) {
                    navDirection = adq.getTargetPOI().getNavigationVector(player.pos());

                } else if (adq.getTargetEnemySprite() != null) {
                    EnemySprite target = adq.getTargetEnemySprite();
                    for (Pair<Float, EnemySprite> active :enemies)
                    {
                        EnemySprite sprite = active.getValue();
                        if (sprite.equals(target)){
                            navDirection = new Vector2(adq.getTargetEnemySprite().pos()).sub(player.getX(), player.getY());
                            break;
                        }
                    }
                }
                break;
            }
        }
        if (navDirection != null)
        {
            navArrow.navTargetAngle = navDirection.angleDeg();
            navArrow.setVisible(true);
            navArrow.setPosition(getPlayerSprite().getX() + (getPlayerSprite().getWidth()/2), getPlayerSprite().getY() + (getPlayerSprite().getHeight()/2));
        }
        else
        {
            navArrow.setVisible(false);
        }
    }
}
