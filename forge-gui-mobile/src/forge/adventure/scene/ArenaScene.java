package forge.adventure.scene;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Array;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import forge.Forge;
import forge.adventure.character.EnemySprite;
import forge.adventure.data.ArenaData;
import forge.adventure.data.EnemyData;
import forge.adventure.data.WorldData;
import forge.adventure.stage.GameHUD;
import forge.adventure.stage.IAfterMatch;
import forge.adventure.stage.MapStage;
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
        if (object == null)
            object = new ArenaScene();
        return object;
    }

    private final TextraButton doneButton;
    private final TextraLabel goldLabel;

    private final Group arenaPlane;
    private final Table arenaTable;
    private final Random rand = new Random();

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
    boolean enable = true;
    boolean arenaStarted = false;
    Dialog startDialog, concedeDialog;

    private ArenaScene() {
        super(Forge.isLandscapeMode() ? "ui/arena.json" : "ui/arena_portrait.json");
        fighterSpot = Config.instance().getAtlasSprite(Paths.ARENA_ATLAS, "Spot");
        lostOverlay = Config.instance().getAtlasSprite(Paths.ARENA_ATLAS, "Lost");
        up = Config.instance().getAtlasSprite(Paths.ARENA_ATLAS, "Up");
        upWin = Config.instance().getAtlasSprite(Paths.ARENA_ATLAS, "UpWin");
        side = Config.instance().getAtlasSprite(Paths.ARENA_ATLAS, "Side");
        sideWin = Config.instance().getAtlasSprite(Paths.ARENA_ATLAS, "SideWin");
        edge = Config.instance().getAtlasSprite(Paths.ARENA_ATLAS, "Edge");
        edgeM = Config.instance().getAtlasSprite(Paths.ARENA_ATLAS, "EdgeFlip");
        edgeM.setFlip(true, false);
        edgeWin = Config.instance().getAtlasSprite(Paths.ARENA_ATLAS, "EdgeWin");
        edgeWinM = Config.instance().getAtlasSprite(Paths.ARENA_ATLAS, "EdgeWinFlip");
        edgeWinM.setFlip(true, false);
        gridSize = fighterSpot.getRegionWidth();

        goldLabel = ui.findActor("gold");
        ui.onButtonPress("done", () -> {
            if (!enable)
                return;
            if (!arenaStarted)
                ArenaScene.this.done();
            else
                showAreYouSure();
        });
        ui.onButtonPress("start", this::startButton);
        doneButton = ui.findActor("done");
        ScrollPane pane = ui.findActor("arena");
        arenaPlane = new Table();
        arenaTable = new Table();
        pane.setActor(arenaPlane);

        startButton = ui.findActor("start");


    }

    private void showAreYouSure() {
        if (concedeDialog == null) {
            concedeDialog = createGenericDialog(Forge.getLocalizer().getMessage("lblConcedeTitle"),
                    "\n" + Forge.getLocalizer().getMessage("lblConcedeCurrentGame"),
                    Forge.getLocalizer().getMessage("lblYes"),
                    Forge.getLocalizer().getMessage("lblNo"), () -> {
                        this.lose();
                        removeDialog();
                    }, this::removeDialog);
        }
        showDialog(concedeDialog);
    }

    private void lose() {
        doneButton.setText("[%80][+Exit]");
        doneButton.layout();
        startButton.setDisabled(true);
        arenaStarted = false;
        AdventureQuestController.instance().updateArenaComplete(false);
        AdventureQuestController.instance().showQuestDialogs(MapStage.getInstance());
    }

    private void startDialog() {
        if (startDialog == null) {
            startDialog = createGenericDialog(Forge.getLocalizer().getMessage("lblStart"),
                    Forge.getLocalizer().getMessage("lblStartArena"), Forge.getLocalizer().getMessage("lblYes"),
                    Forge.getLocalizer().getMessage("lblNo"), () -> {
                        this.startArena();
                        removeDialog();
                    }, this::removeDialog);
        }
        showDialog(startDialog);
    }

    private void startButton() {
        if (!enable)
            return;
        if (roundsWon == 0) {
            startDialog();
        } else {
            startRound();
        }
    }

    int roundsWon = 0;

    private void startArena() {
        enable = false;
        goldLabel.setVisible(false);
        arenaStarted = true;
        startButton.setText("[%80][+OK]");
        startButton.layout();
        doneButton.setText("[%80][+Exit]");
        doneButton.layout();
        Forge.setCursor(null, Forge.magnifyToggle ? "1" : "2");
        Current.player().takeGold(arenaData.entryFee);
        startRound();
    }

    @Override
    public void setWinner(boolean winner, boolean isArena) {
        enable = false;
        Array<ArenaRecord> winners = new Array<>();
        Array<EnemySprite> winnersEnemies = new Array<>();
        for (int i = 0; i < fighters.size - 2; i += 2) {
            int matchHP = enemies.get(i).getData().life + enemies.get(i+1).getData().life;
            boolean leftWon = rand.nextInt(matchHP) < enemies.get(i).getData().life;
            if (leftWon) {
                winners.add(fighters.get(i));
                winnersEnemies.add(enemies.get(i));
                moveFighter(fighters.get(i).actor, true);
                markLostFighter(fighters.get(i + 1).actor);
            } else {
                markLostFighter(fighters.get(i).actor);
                moveFighter(fighters.get(i + 1).actor, false);
                winners.add(fighters.get(i + 1));
                winnersEnemies.add(enemies.get(i + 1));
            }
        }
        if (winner) {
            markLostFighter(fighters.get(fighters.size - 2).actor);
            moveFighter(fighters.get(fighters.size - 1).actor, false);
            winners.add(fighters.get(fighters.size - 1));
            roundsWon++;
        } else {
            markLostFighter(fighters.get(fighters.size - 1).actor);
            moveFighter(fighters.get(fighters.size - 2).actor, true);
            winners.add(fighters.get(fighters.size - 2));
            lose();
        }

        fighters = winners;
        enemies = winnersEnemies;
        if (roundsWon >= arenaData.rounds) {
            arenaStarted = false;
            startButton.setDisabled(true);
            doneButton.setText("[%80][+Exit]");
            doneButton.layout();
            AdventureQuestController.instance().updateArenaComplete(true);
            AdventureQuestController.instance().showQuestDialogs(MapStage.getInstance());
        }
        if (!Forge.isLandscapeMode())
            drawArena();//update
    }

    private void moveFighter(Actor actor, boolean leftPlayer) {
        Image spotImg = new Image(upWin);
        double stepsToTheSide = Math.pow(2, roundsWon);
        float widthDiff = actor.getWidth() - spotImg.getWidth();
        spotImg.setPosition(actor.getX() + widthDiff / 2, actor.getY() + gridSize + widthDiff / 2);
        arenaPlane.addActor(spotImg);
        for (int i = 0; i < stepsToTheSide; i++) {
            Image leftImg;
            if (i == 0)
                leftImg = new Image(leftPlayer ? edgeWin : edgeWinM);
            else
                leftImg = new Image(sideWin);
            leftImg.setPosition(actor.getX() + (i * (leftPlayer ? 1 : -1)) * gridSize + widthDiff / 2, actor.getY() + gridSize * 2 + widthDiff / 2);
            arenaPlane.addActor(leftImg);
        }
        if (Forge.isLandscapeMode()) {
            actor.toFront();
            actor.addAction(Actions.sequence(Actions.moveBy(0f, gridSize * 2f, 1), Actions.moveBy((float) (gridSize * stepsToTheSide * (leftPlayer ? 1 : -1)), 0f, 1), new Action() {
                @Override
                public boolean act(float v) {
                    enable = true;
                    return true;
                }
            }));
        } else {
            enable = true;
        }
    }

    private void markLostFighter(Actor fighter) {
        Image lost = new Image(lostOverlay);
        float widthDiff = fighter.getWidth() - lost.getWidth();
        lost.setPosition(fighter.getX() + widthDiff / 2, fighter.getY() + widthDiff / 2);
        arenaPlane.addActor(lost);
    }

    boolean started = false;

    private void startRound() {
        if (started)
            return;
        started = true;
        DuelScene duelScene = DuelScene.instance();
        EnemySprite enemy = enemies.get(enemies.size - 1);
        FThreads.invokeInEdtNowOrLater(() -> Forge.setTransitionScreen(new TransitionScreen(() -> {
            started = false;
            duelScene.initDuels(WorldStage.getInstance().getPlayerSprite(), enemy, true, null);
            Forge.switchScene(duelScene);
        }, Forge.takeScreenshot(), true, false, false, false, "", Current.player().avatar(), enemy.getAtlasPath(), Current.player().getName(), enemy.getName())));
    }

    public boolean start() {
        return true;
    }


    public boolean done() {
        GameHUD.getInstance().getTouchpad().setVisible(false);
        Forge.switchToLast();
        if (roundsWon != 0) {
            Array<Reward> data = new Array<>();
            for (int i = 0; i < roundsWon; i++) {
                for (int j = 0; j < arenaData.rewards[i].length; j++) {
                    data.addAll(arenaData.rewards[i][j].generate(false, null, true));
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
    Array<ArenaRecord> fighters = new Array<>();
    Actor player;

    public void loadArenaData(ArenaData data, long seed) {
        startButton.setText("[%80][+OK]");
        startButton.layout();
        doneButton.setText("[%80][+Exit]");
        doneButton.layout();
        arenaData = data;
        //rand.setSeed(seed); allow to reshuffle arena enemies for now

        enemies.clear();
        fighters.clear();
        arenaPlane.clear();
        roundsWon = 0;
        int numberOfEnemies = (int) (Math.pow(2f, data.rounds) - 1);


        for (int i = 0; i < numberOfEnemies; i++) {
            EnemyData enemyData = null;
            while (enemyData == null)
                enemyData = WorldData.getEnemy(data.enemyPool[rand.nextInt(data.enemyPool.length)]);
            EnemySprite enemy = new EnemySprite(enemyData);
            enemies.add(enemy);
            fighters.add(new ArenaRecord(new Image(enemy.getAvatar()), enemyData.getName()));
        }
        fighters.add(new ArenaRecord(new Image(Current.player().avatar()), Current.player().getName()));
        player = fighters.get(fighters.size - 1).actor;

        goldLabel.setText("[+GoldCoin] " + data.entryFee);
        goldLabel.layout();
        goldLabel.setVisible(true);

        startButton.setDisabled(data.entryFee > Current.player().getGold());
        int currentSpots = numberOfEnemies + 1;
        int gridWidth = currentSpots * 2;
        int gridHeight = data.rounds + 1;
        arenaPlane.setSize(gridWidth * gridSize, gridHeight * gridSize * 2);
        int fighterIndex = 0;
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                if (x % Math.pow(2, y + 1) == Math.pow(2, y)) {
                    if (y == 0) {
                        if (fighterIndex < fighters.size) {
                            float widthDiff = gridSize - fighters.get(fighterIndex).actor.getWidth();
                            fighters.get(fighterIndex).actor.setPosition(x * gridSize + widthDiff / 2, y * gridSize * 2 + widthDiff / 2);
                            arenaPlane.addActor(fighters.get(fighterIndex).actor);
                            fighterIndex++;
                        }
                    }
                    Image spotImg = new Image(fighterSpot);
                    spotImg.setPosition(x * gridSize, y * gridSize * 2);
                    arenaPlane.addActor(spotImg);

                    if (y != gridHeight - 1) {
                        Image upImg = new Image(up);
                        upImg.setPosition(x * gridSize, y * gridSize * 2 + gridSize);
                        arenaPlane.addActor(upImg);
                    }
                    if (y != 0) {
                        for (int i = 0; i < Math.pow(2, (y - 1)); i++) {
                            Image leftImg;
                            Image rightImg;
                            if (i == Math.pow(2, (y - 1)) - 1) {
                                leftImg = new Image(edge);
                                rightImg = new Image(edgeM);
                            } else {
                                leftImg = new Image(side);
                                rightImg = new Image(side);
                            }
                            leftImg.setPosition((x - (i + 1)) * gridSize, y * gridSize * 2);
                            rightImg.setPosition((x + (i + 1)) * gridSize, y * gridSize * 2);
                            arenaPlane.addActor(leftImg);
                            arenaPlane.addActor(rightImg);
                        }
                    }
                }
            }
        }
        drawArena();
    }

    void drawArena() {
        //center the arenaPlane
        ScrollPane pane = ui.findActor("arena");
        if (pane != null) {
            pane.clear();
            arenaTable.clear();
            if (Forge.isLandscapeMode()) {
                arenaTable.add(Controls.newTextraLabel("[;][%150]" + GameScene.instance().getAdventurePlayerLocation(true, true) + " Arena")).top();
                arenaTable.row();
                arenaTable.add(arenaPlane).width(arenaPlane.getWidth()).height(arenaPlane.getHeight());
                pane.setActor(arenaTable);
            } else {
                arenaTable.add(Controls.newTextraLabel("[;][%150]" + GameScene.instance().getAdventurePlayerLocation(true, true) + " Arena")).colspan(3).top();
                arenaTable.row();
                int size = fighters.size;
                int pv = 0;
                for (int x = 0; x < size; x++) {
                    ArenaRecord record = fighters.get(x);
                    int divider = size == 1 ? 2 : size == 2 ? 3 : size;
                    arenaTable.add(record.actor).pad(20, 5, 20, 5).size(pane.getWidth() / divider);
                    pv++;
                    if (pv == 1) {
                        if (size > 1)
                            arenaTable.add(Controls.newTextraLabel("[%135]VS")).padLeft(5).padRight(5);
                        else {
                            arenaTable.row();
                            arenaTable.add(Controls.newTextraLabel("[%135]Winner!")).padLeft(5).padRight(5);
                        }
                    }
                    if (pv == 2) {
                        arenaTable.row();
                        pv = 0;
                    }

                }
                pane.setActor(arenaTable);
            }
        }
    }

    class ArenaRecord {
        Actor actor;
        String name;

        ArenaRecord(Actor a, String n) {
            actor = a;
            name = n;
        }
    }
}
