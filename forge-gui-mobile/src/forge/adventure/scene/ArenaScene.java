package forge.adventure.scene;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Array;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import forge.Forge;
import forge.adventure.character.EnemySprite;
import forge.adventure.data.ArenaData;
import forge.adventure.data.WorldData;
import forge.adventure.stage.GameHUD;
import forge.adventure.stage.IAfterMatch;
import forge.adventure.stage.WorldStage;
import forge.adventure.util.*;
import forge.gui.FThreads;
import forge.screens.TransitionScreen;

import java.util.Random;

/**
 * Displays the rewards of a fight or a treasure
 */
public class ArenaScene extends UIScene implements IAfterMatch {
    private static ArenaScene object;
    private final float gridSize;
    private ArenaData arenaData;
    private final TextraButton startButton;

    public static ArenaScene instance() {
        if(object==null)
            object=new ArenaScene();
        return object;
    }

    private final TextraButton doneButton;
    private final TextraLabel goldLabel;

    private final Group arenaPlane;
    private final Random rand=new Random();

    final Sprite fighterSpot;
    final Sprite lostOverlay;
    final Sprite up;
    final Sprite upWin;
    final Sprite side;
    final Sprite sideWin;
    final Sprite edge;
    final Sprite edgeM;
    final Sprite edgeWin;
    final Sprite edgeWinM;
    boolean arenaStarted=false;
    private ArenaScene() {
        super(Forge.isLandscapeMode() ? "ui/arena.json" : "ui/arena_portrait.json");



        TextureAtlas atlas=Config.instance().getAtlas(Paths.ARENA_ATLAS);
        fighterSpot=atlas.createSprite("Spot");
        lostOverlay=atlas.createSprite("Lost");
        up=atlas.createSprite("Up");
        upWin=atlas.createSprite("UpWin");
        side=atlas.createSprite("Side");
        sideWin=atlas.createSprite("SideWin");
        edge=atlas.createSprite("Edge");
        edgeM=atlas.createSprite("Edge");
        edgeM.setFlip(true,false);
        edgeWin=atlas.createSprite("EdgeWin");
        edgeWinM=atlas.createSprite("EdgeWin");
        edgeWinM.setFlip(true,false);
        gridSize=fighterSpot.getRegionWidth();

        goldLabel=ui.findActor("gold");
        ui.onButtonPress("done", () -> {
            if(!arenaStarted)
                ArenaScene.this.done();
            else
                showAreYouSure();
        });
        ui.onButtonPress("start", () -> startButton());
        doneButton = ui.findActor("done");
        ScrollPane pane= ui.findActor("arena");
        arenaPlane=new Table();
        pane.setActor(arenaPlane);

        startButton=ui.findActor("start");



    }

    private void showAreYouSure() {

        Dialog areYouSureDialog= prepareDialog(Forge.getLocalizer().getMessage("lblConcedeTitle"),ButtonYes|ButtonNo,()->loose());
        areYouSureDialog.text(Forge.getLocalizer().getMessage("lblConcedeCurrentGame"));
        showDialog(areYouSureDialog);
    }

    private void loose() {
        doneButton.setText(Forge.getLocalizer().getMessage("lblLeave"));
        startButton.setDisabled(true);
        arenaStarted=false;
    }

    private void startButton() {
        if(roundsWon ==0)
        {
            Dialog startDialog = prepareDialog(Forge.getLocalizer().getMessage("lblStart"), ButtonYes|ButtonNo,()->startArena());
            startDialog.text("Do you want to go into the Arena?");
            showDialog(startDialog);
        }
        else
        {
            startRound();
        }
    }

    int roundsWon =0;
    private void startArena() {
        goldLabel.setVisible(false);
        arenaStarted=true;
        startButton.setText(Forge.getLocalizer().getMessage("lblContinue"));
        doneButton.setText(Forge.getLocalizer().getMessage("lblConcede"));
        Forge.setCursor(null, Forge.magnifyToggle ? "1" : "2");
        Current.player().takeGold(arenaData.entryFee);
        startRound();
    }
    @Override
    public void setWinner(boolean winner) {
        Array<Actor> winners=new Array<>();
        Array<EnemySprite> winnersEnemies=new Array<>();
         for(int i=0;i<fighters.size-2;i+=2)
         {
             boolean leftWon=rand.nextBoolean();
             if(leftWon)
             {
                 winners.add(fighters.get(i));
                 winnersEnemies.add(enemies.get(i));
                 moveFighter(fighters.get(i),true);
                 markLostFighter(fighters.get(i+1));
             }
             else
             {
                 markLostFighter(fighters.get(i));
                 moveFighter(fighters.get(i+1),false);
                 winners.add(fighters.get(i+1));
                 winnersEnemies.add(enemies.get(i+1));
             }
         }
        if(winner)
        {
            markLostFighter(fighters.get(fighters.size-2));
            moveFighter(fighters.get(fighters.size-1),false);
            winners.add(fighters.get(fighters.size-1));
            roundsWon++;
        }
        else
        {
            markLostFighter(fighters.get(fighters.size-1));
            moveFighter(fighters.get(fighters.size-2),true);
            winners.add(fighters.get(fighters.size-2));
            loose();
        }

            fighters=winners;
            enemies=winnersEnemies;
        if(roundsWon >=arenaData.rounds )
        {
            arenaStarted=false;
            startButton.setDisabled(true);
            doneButton.setText(Forge.getLocalizer().getMessage("lblDone"));
        }
    }

    private void moveFighter(Actor actor, boolean leftPlayer) {

        Image spotImg=new Image(upWin);
        double stepsToTheSide=Math.pow(2, roundsWon);
        float widthDiff=actor.getWidth()-spotImg.getWidth();
        spotImg.setPosition(actor.getX()+widthDiff/2,actor.getY()+gridSize+widthDiff/2);
        arenaPlane.addActor(spotImg);
        for(int i=0;i<stepsToTheSide;i++)
        {
            Image leftImg;
            if(i==0)
                leftImg=new Image(leftPlayer?edgeWin:edgeWinM);
            else
                leftImg=new Image(sideWin);
            leftImg.setPosition(actor.getX()+(i*(leftPlayer?1:-1))*gridSize+widthDiff/2,actor.getY()+gridSize*2+widthDiff/2);
            arenaPlane.addActor(leftImg);
        }


        actor.moveBy((float) (gridSize*stepsToTheSide*(leftPlayer?1:-1)),gridSize*2f);
    }

    private void markLostFighter(Actor fighter) {

        Image lost=new Image(lostOverlay);
        float widthDiff=fighter.getWidth()-lost.getWidth();
        lost.setPosition(fighter.getX()+widthDiff/2,fighter.getY()+widthDiff/2);
        arenaPlane.addActor(lost);
    }

    private void startRound() {
        DuelScene duelScene =  DuelScene.instance();
        FThreads.invokeInEdtNowOrLater(() -> {
            Forge.setTransitionScreen(new TransitionScreen(() -> {
                duelScene.initDuels(WorldStage.getInstance().getPlayerSprite(), enemies.get(enemies.size-1));
                Forge.switchScene(DuelScene.instance());
            }, Forge.takeScreenshot(), true, false));
        });
    }

    public boolean start() {


        return true;
    }


    public boolean done() {
        GameHUD.getInstance().getTouchpad().setVisible(false);
        Forge.switchToLast();
        if(roundsWon !=0)
        {
            Array<Reward> data=new Array<>();
            for(int i = 0; i< roundsWon; i++)
            {
                for(int j=0;j<arenaData.rewards[i].length;j++)
                {
                    data.addAll(arenaData.rewards[i][j].generate(false,  null ));
                }
            }
            RewardScene.instance().loadRewards(data, RewardScene.Type.Loot, null);
            Forge.switchScene(RewardScene.instance());
        }
        return true;
    }
    @Override
    public void act(float delta) {
        stage.act(delta);
    }



    Array<EnemySprite> enemies = new Array<>();
    Array<Actor> fighters = new Array<>();
    Actor player;

    public void loadArenaData(ArenaData data,long seed) {
        startButton.setText(Forge.getLocalizer().getMessage("lblStart"));
        doneButton.setText(Forge.getLocalizer().getMessage("lblDone"));
        arenaData=data;
        //rand.setSeed(seed); allow to reshuffle arena enemies for now

        enemies.clear();
        fighters.clear();
        arenaPlane.clear();
        roundsWon =0;
        int numberOfEnemies= (int) (Math.pow(2f, data.rounds)-1);


        for(int i=0;i<numberOfEnemies;i++)
        {
            EnemySprite enemy=new EnemySprite(WorldData.getEnemy(data.enemyPool[rand.nextInt(data.enemyPool.length)]));
            enemies.add(enemy);
            fighters.add(new Image(enemy.getAvatar()));
        }
        fighters.add(new Image(Current.player().avatar()));
        player=fighters.get(fighters.size-1);


        goldLabel.setText(data.entryFee +" [+Gold]");
        goldLabel.setVisible(true);

        startButton.setDisabled(data.entryFee>Current.player().getGold());
        int currentSpots=numberOfEnemies+1;
        int gridWidth=currentSpots*2;
        int gridHeight=data.rounds+1;
        arenaPlane.setSize(gridWidth*gridSize,gridHeight*gridSize*2);
        int fighterIndex=0;
        for(int x=0;x<gridWidth;x++)
        {
            for(int y=0;y<gridHeight;y++)
            {
                if(x % Math.pow(2,y+1) == Math.pow(2,y))
                {
                    if(y==0)
                    {
                        if(fighterIndex<fighters.size)
                        {
                            float widthDiff=gridSize-fighters.get(fighterIndex).getWidth();
                            fighters.get(fighterIndex).setPosition(x*gridSize+widthDiff/2,y*gridSize*2+widthDiff/2);
                            arenaPlane.addActor(fighters.get(fighterIndex));
                            fighterIndex++;
                        }
                    }
                    Image spotImg=new Image(fighterSpot);
                    spotImg.setPosition(x*gridSize,y*gridSize*2);
                    arenaPlane.addActor(spotImg);

                    if(y!=gridHeight-1)
                    {
                        Image upImg=new Image(up);
                        upImg.setPosition(x*gridSize,y*gridSize*2+gridSize);
                        arenaPlane.addActor(upImg);
                    }
                    if(y!=0)
                    {
                        for(int i=0;i<Math.pow(2,(y-1));i++)
                        {
                            Image leftImg;
                            Image rightImg;
                            if(i==Math.pow(2,(y-1))-1)
                            {
                                leftImg=new Image(edge);
                                rightImg=new Image(edgeM);
                            }
                            else
                            {
                                leftImg=new Image(side);
                                rightImg=new Image(side);
                            }
                            leftImg.setPosition((x-(i+1))*gridSize,y*gridSize*2);
                            rightImg.setPosition((x+(i+1))*gridSize,y*gridSize*2);
                            arenaPlane.addActor(leftImg);
                            arenaPlane.addActor(rightImg);
                        }
                    }
                }
            }
        }
    }

}
