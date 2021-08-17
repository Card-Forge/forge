package forge.adventure.stage;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.character.CharacterSprite;
import forge.adventure.character.MobSprite;
import forge.adventure.data.BiomData;
import forge.adventure.data.EnemyData;
import forge.adventure.scene.Scene;
import forge.adventure.scene.SceneType;
import forge.adventure.scene.TileMapScene;
import forge.adventure.world.World;
import forge.adventure.world.WorldSave;

import java.util.List;
import java.util.Random;

public class WorldStage extends GameStage {

    private static WorldStage instance;
    protected Random rand = new Random();
    WorldBackground background;
    private float animationTimeout = 0;
    private float spawnDelay = 0;
    private PointOfIntrestMapSprite collidingPoint;

    public WorldStage() {
        super();
        background = new WorldBackground(this);
        addActor(background);
        background.setZIndex(0);
    }

    public static WorldStage getInstance() {
        return instance == null ? instance = new WorldStage() : instance;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (player.isMoving()) {
            HandleMonsterSpawn();
            HandlePointsOfIntrestCollision();
            for (MobSprite mob : enemies) {
                mob.moveTo(player);
                if (player.collideWith(mob)) {
                    player.setAnimation(CharacterSprite.AnimationTypes.Attack);
                    mob.setAnimation(CharacterSprite.AnimationTypes.Attack);
                    animationTimeout = 1;
                    action = CurrentAction.Attack;
                    currentMob = mob;
                }
            }
        } else {
            for (MobSprite mob : enemies) {
                mob.setAnimation(CharacterSprite.AnimationTypes.Idle);
            }
        }
    }

    private void HandlePointsOfIntrestCollision() {

        for (Actor actor : foregroundSprites.getChildren()) {
            if (actor.getClass() == PointOfIntrestMapSprite.class) {
                PointOfIntrestMapSprite point = (PointOfIntrestMapSprite) actor;
                if (player.collideWith(point.getBoundingRect())) {
                    if (point == collidingPoint) {
                        continue;
                    }
                    ((TileMapScene) SceneType.TileMapScene.instance).load(point.getPointOfIntrest());
                    AdventureApplicationAdapter.CurrentAdapter.SwitchScene(SceneType.TileMapScene.instance);
                } else {
                    if (point == collidingPoint) {
                        collidingPoint = null;
                    }
                }
            }

        }
    }

    private void HandleMonsterSpawn() {


        World world = WorldSave.getCurrentSave().world;
        int currentBiom = World.highestBiom(world.GetBiom((int) player.getX() / world.GetTileSize(), (int) player.getY() / world.GetTileSize()));
        List<BiomData> biomdata = WorldSave.getCurrentSave().world.GetData().GetBioms();
        if (biomdata.size() <= currentBiom)
            return;
        BiomData data = biomdata.get(currentBiom);

        if (data == null)
            return;
        Array<EnemyData> list = data.GetEnemyList();
        if (list == null)
            return;
        EnemyData enemyData = data.getEnemy(spawnDelay, 1);
        if (enemyData == null) {
            spawnDelay += 0.0001;
            return;
        }
        spawnDelay = 0;
        MobSprite sprite = new MobSprite(enemyData);
        float unit = Scene.GetIntendedHeight() / 6f;
        Vector2 spawnPos = new Vector2(1, 1);
        spawnPos.setLength(unit + (unit * 3) * rand.nextFloat());
        spawnPos.setAngleDeg(360 * rand.nextFloat());
        sprite.setX(player.getX() + spawnPos.x);
        sprite.setY(player.getY() + spawnPos.y);
        enemies.add(sprite);
        foregroundSprites.addActor(sprite);
    }

    @Override
    public void draw() {
        getBatch().begin();
        background.setPlayerPos(player.getX(), player.getY());
        getBatch().end();
        //spriteGroup.setCullingArea(new Rectangle(player.getX()-getViewport().getWorldHeight()/2,player.getY()-getViewport().getWorldHeight()/2,getViewport().getWorldHeight(),getViewport().getWorldHeight()));
        super.draw();
    }

    @Override
    public void Enter() {

        GetPlayer().LoadPos();
        GetPlayer().setMovementDirection(Vector2.Zero);
        for (Actor actor : foregroundSprites.getChildren()) {
            if (actor.getClass() == PointOfIntrestMapSprite.class) {
                PointOfIntrestMapSprite point = (PointOfIntrestMapSprite) actor;
                if (player.collideWith(point.getBoundingRect())) {
                    collidingPoint = point;
                }
            }

        }

        setBounds(WorldSave.getCurrentSave().world.GetWidthInPixels(), WorldSave.getCurrentSave().world.GetHeightInPixels());
    }

    @Override
    public void Leave() {
        GetPlayer().storePos();
    }
}
