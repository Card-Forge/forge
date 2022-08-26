package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import forge.Forge;
import forge.adventure.character.EnemySprite;
import forge.adventure.character.ShopActor;
import forge.adventure.data.ArenaData;
import forge.adventure.data.EnemyData;
import forge.adventure.data.WorldData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.stage.GameHUD;
import forge.adventure.util.*;
import forge.adventure.world.WorldSave;
import forge.assets.ImageCache;
import forge.sound.SoundEffectType;
import forge.sound.SoundSystem;

import java.util.Random;

import static org.seamless.xhtml.XHTML.ATTR.type;

/**
 * Displays the rewards of a fight or a treasure
 */
public class ArenaScene extends UIScene {
    private static ArenaScene object;

    public static ArenaScene instance() {
        if(object==null)
            object=new ArenaScene();
        return object;
    }


    class ArenaTree
    {
        ArenaTree(int size)
        {
            this(size,size,0);
        }
        ArenaTree(int size,int position,int xOffset)
        {
            round=size-position;
            roundsToGo=size-round;
            x=xOffset;
            y=round*2;
            if(position>0)
            {
                left=new ArenaTree(size,position-1,xOffset-roundsToGo);
                right=new ArenaTree(size,position-1,xOffset+roundsToGo);
            }
        }
        int round;
        int roundsToGo;
        int x;
        int y;
        ArenaTree left;
        ArenaTree right;
    }
    private TextButton doneButton;
    private Label goldLabel;
    private ArenaTree arenaTree;

    private WidgetGroup arenaPlane;
    private Random rand=new Random();

    private ArenaScene() {
        super(Forge.isLandscapeMode() ? "ui/arena.json" : "ui/arena_portrait.json");

        goldLabel=ui.findActor("gold");
        ui.onButtonPress("done", () -> ArenaScene.this.done());
        doneButton = ui.findActor("done");
        ScrollPane pane= ui.findActor("arena");
        arenaPlane=new WidgetGroup();
        arenaPlane.setFillParent(true);
        pane.setActor(arenaPlane);
    }



    public boolean done() {
        GameHUD.getInstance().getTouchpad().setVisible(false);
        return true;
    }
    @Override
    public void act(float delta) {
        stage.act(delta);
    }


    @Override
    public boolean keyPressed(int keycode) {
        if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
            done();
        }
        return true;
    }

    Array<EnemyData> enemies = new Array<>();
    Array<Actor> fighters = new Array<>();
    int currentRound=0;

    public void loadArenaData(ArenaData data,long seed) {
        rand.setSeed(seed);
        enemies.clear();
        fighters.clear();
        currentRound=0;
        int numberOfEnemies= (int) (Math.pow(2f, data.rounds)-1);


        for(int i=0;i<numberOfEnemies;i++)
        {
            EnemyData enemy=WorldData.getEnemy(data.enemyPool[rand.nextInt(data.enemyPool.length)]);
            enemies.add(enemy);
            fighters.add(new Image(new EnemySprite(enemy).getAvatar()));
        }
        fighters.add(new Image(Current.player().avatar()));
        int fighterSize=Current.player().avatar().getRegionWidth();

        TextureAtlas atlas=Config.instance().getAtlas(Paths.ARENA_ATLAS);
        Sprite fighterSpot=atlas.createSprite("Spot");
        Sprite lostOverlay=atlas.createSprite("Lost");
        Sprite up=atlas.createSprite("Up");
        Sprite upWin=atlas.createSprite("UpWin");
        Sprite side=atlas.createSprite("Side");
        Sprite sideWin=atlas.createSprite("SideWin");
        Sprite edge=atlas.createSprite("Edge");
        Sprite edgeM=atlas.createSprite("Edge");
        edgeM.setFlip(true,false);
        Sprite edgeWin=atlas.createSprite("EdgeWin");
        Sprite edgeWinM=atlas.createSprite("EdgeWin");
        edgeWinM.setFlip(true,false);

        int gridSize=fighterSpot.getRegionWidth();
        int currentSpots=numberOfEnemies+1;
        int gridWidth=currentSpots*2;
        arenaTree=new ArenaTree(data.rounds);
        Array<ArenaTree> treeList=new Array<>();
        Array<Image> allSpots=new Array<>();
        treeList.add(arenaTree);
        float leftEdge=0;
        float upperEdge=0;
        while (!treeList.isEmpty())
        {
            ArenaTree currentTreeElement=treeList.get(0);
            treeList.removeIndex(0);
            if(currentTreeElement.right!=null)
            {
                treeList.add(currentTreeElement.left);
                treeList.add(currentTreeElement.right);
            }

            Image spot=new Image(fighterSpot);
            spot.setPosition(currentTreeElement.x*gridSize,currentTreeElement.y*gridSize);
            allSpots.add(spot);
            arenaPlane.addActor(spot);
            if(currentTreeElement.round!=0)
            {

                Image upImg=new Image(up);
                upImg.setPosition(currentTreeElement.x*gridSize,(currentTreeElement.y-1)*gridSize);
                allSpots.add(upImg);
                arenaPlane.addActor(upImg);
            }
            for(int i=0;i<currentTreeElement.roundsToGo;i++)
            {

                Image left;
                Image right;
                if(i==currentTreeElement.roundsToGo-1)
                {

                    left=new Image(edge);
                    right=new Image(edgeM);
                }
                else
                {
                    left=new Image(side);
                    right=new Image(side);
                }
                left.setPosition((currentTreeElement.x-(i+1))*gridSize,currentTreeElement.y*gridSize);
                right.setPosition((currentTreeElement.x+(i+1))*gridSize,currentTreeElement.y*gridSize);
                allSpots.add(left);
                arenaPlane.addActor(left);
                allSpots.add(right);
                arenaPlane.addActor(right);
            }
            if(spot.getX()<leftEdge)
                leftEdge=spot.getX();
            if(spot.getY()>upperEdge)
                upperEdge=spot.getY();
        }
        leftEdge*=-1;
        for(Image img: allSpots)
        {
            img.setX(leftEdge+img.getX());
            img.setY(upperEdge-img.getY());
        }

    }

}
