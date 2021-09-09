package forge.adventure.stage;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.character.CharacterSprite;
import forge.adventure.character.MobSprite;
import forge.adventure.data.BiomData;
import forge.adventure.data.EnemyData;
import forge.adventure.scene.*;
import forge.adventure.util.Current;
import forge.adventure.world.World;
import forge.adventure.world.WorldSave;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class WorldStage extends GameStage {

    private static WorldStage instance=null;
    protected MobSprite currentMob;
    protected Random rand = new Random();
    WorldBackground background;
    private float spawnDelay = 0;
    private final float spawnInterval = 4;
    private PointOfIntrestMapSprite collidingPoint;
    protected ArrayList<Pair<Float,MobSprite>> enemies = new ArrayList<>();
    private final Float dieTimer=20f;
    private Float globalTimer=0f;

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
    protected void onActing(float delta) {
        if (player.isMoving()) {
            HandleMonsterSpawn(delta);
            HandlePointsOfIntrestCollision();
            globalTimer+=delta;
            Iterator<Pair<Float,MobSprite>> it = enemies.iterator();
            while (it.hasNext()) {
                Pair<Float,MobSprite> pair= it.next();
                if(globalTimer>=pair.getKey()+dieTimer)
                {

                    foregroundSprites.removeActor(pair.getValue());
                    it.remove();
                    continue;
                }
                MobSprite mob=pair.getValue();
                mob.moveTo(player,delta);
                if (player.collideWith(mob)) {
                    player.setAnimation(CharacterSprite.AnimationTypes.Attack);
                    mob.setAnimation(CharacterSprite.AnimationTypes.Attack);
                    startPause(1,()->{

                        ((DuelScene) SceneType.DuelScene.instance).setEnemy(currentMob);
                        ((DuelScene) SceneType.DuelScene.instance).setPlayer(player);
                        AdventureApplicationAdapter.instance.switchScene(SceneType.DuelScene.instance);
                    });
                    currentMob = mob;
                    break;
                }
            }


        } else {
            for (Pair<Float,MobSprite> pair : enemies) {
                pair.getValue().setAnimation(CharacterSprite.AnimationTypes.Idle);
            }
        }
    }
    private void removeEnemy(MobSprite currentMob) {

        foregroundSprites.removeActor(currentMob);
        Iterator<Pair<Float,MobSprite>> it = enemies.iterator();
        while (it.hasNext()) {
            Pair<Float, MobSprite> pair = it.next();
            if (pair.getValue()==currentMob) {
                it.remove();
                return;
            }
        }
    }
    @Override
    public void setWinner(boolean playerIsWinner) {

        if (playerIsWinner) {
            player.setAnimation(CharacterSprite.AnimationTypes.Attack);
            currentMob.setAnimation(CharacterSprite.AnimationTypes.Death);
            startPause(1,()->
            {
                ((RewardScene)SceneType.RewardScene.instance).loadRewards(currentMob.getRewards(), RewardScene.Type.Loot, null);
                removeEnemy(currentMob);
                currentMob = null;
                AdventureApplicationAdapter.instance.switchScene(SceneType.RewardScene.instance);
            } );
        } else {
            player.setAnimation(CharacterSprite.AnimationTypes.Hit);
            currentMob.setAnimation(CharacterSprite.AnimationTypes.Attack);
            startPause(1,()->
            {
                Current.player().defeated();
                removeEnemy(currentMob);
                currentMob = null;
            } );

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
                    AdventureApplicationAdapter.instance.switchScene(SceneType.TileMapScene.instance);
                } else {
                    if (point == collidingPoint) {
                        collidingPoint = null;
                    }
                }
            }

        }
    }
    @Override
    public boolean isColliding(Rectangle boundingRect)
    {

        World world = WorldSave.getCurrentSave().getWorld();
        int currentBiom = World.highestBiom(world.getBiom((int) boundingRect.getX() / world.getTileSize(), (int) boundingRect.getY() / world.getTileSize()));
        if(currentBiom==0)
            return true;
         currentBiom = World.highestBiom(world.getBiom((int) (boundingRect.getX()+boundingRect.getWidth()) / world.getTileSize(), (int) boundingRect.getY() / world.getTileSize()));
        if(currentBiom==0)
            return true;
         currentBiom = World.highestBiom(world.getBiom((int) (boundingRect.getX()+boundingRect.getWidth())/ world.getTileSize(), (int) (boundingRect.getY()+boundingRect.getHeight()) / world.getTileSize()));
        if(currentBiom==0)
            return true;
         currentBiom = World.highestBiom(world.getBiom((int) boundingRect.getX() / world.getTileSize(), (int) (boundingRect.getY()+boundingRect.getHeight()) / world.getTileSize()));

        return (currentBiom==0);
    }

    private void HandleMonsterSpawn(float delta) {


        World world = WorldSave.getCurrentSave().getWorld();
        int currentBiom = World.highestBiom(world.getBiom((int) player.getX() / world.getTileSize(), (int) player.getY() / world.getTileSize()));
        List<BiomData> biomdata = WorldSave.getCurrentSave().getWorld().getData().GetBioms();
        if (biomdata.size() <= currentBiom)
        {
            player.setMoveModifier(1.5f);
            return;
        }
        player.setMoveModifier(1.0f);
        BiomData data = biomdata.get(currentBiom);

        if (data == null)
            return;
        ArrayList<EnemyData> list = data.getEnemyList();
        if (list == null)
            return;
        spawnDelay -= delta;
        if(spawnDelay>=0)
            return;
        spawnDelay=spawnInterval+(rand.nextFloat()*4);
        EnemyData enemyData = data.getEnemy( 1);
        if (enemyData == null) {
            return;
        }
        MobSprite sprite = new MobSprite(enemyData);
        float unit = Scene.GetIntendedHeight() / 6f;
        Vector2 spawnPos = new Vector2(1, 1);
        spawnPos.setLength(unit + (unit * 3) * rand.nextFloat());
        spawnPos.setAngleDeg(360 * rand.nextFloat());
        sprite.setX(player.getX() + spawnPos.x);
        sprite.setY(player.getY() + spawnPos.y);
        enemies.add(Pair.of(globalTimer,sprite));
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
    public void enter() {

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

        setBounds(WorldSave.getCurrentSave().getWorld().getWidthInPixels(), WorldSave.getCurrentSave().getWorld().getHeightInPixels());
    }

    @Override
    public void leave() {
        GetPlayer().storePos();
    }
}
