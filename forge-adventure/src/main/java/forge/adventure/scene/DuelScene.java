package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import forge.adventure.libgdxgui.Graphics;
import forge.LobbyPlayer;
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.character.MobSprite;
import forge.adventure.character.PlayerSprite;
import forge.adventure.util.Res;
import forge.adventure.world.WorldSave;
import forge.adventure.libgdxgui.animation.ForgeAnimation;
import forge.adventure.libgdxgui.assets.FSkin;
import forge.adventure.libgdxgui.assets.ImageCache;
import forge.deck.io.DeckSerializer;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.match.LobbySlotType;
import forge.gui.interfaces.IGuiGame;
import forge.interfaces.IUpdateable;
import forge.player.GamePlayerUtil;
import forge.player.PlayerControllerHuman;
import forge.adventure.libgdxgui.screens.FScreen;
import forge.adventure.libgdxgui.screens.match.MatchController;
import forge.adventure.libgdxgui.toolbox.FDisplayObject;
import forge.adventure.libgdxgui.toolbox.FOverlay;
import forge.trackable.TrackableCollection;

import java.io.File;
import java.util.*;

public class DuelScene extends Scene implements IUpdateable {

    //GameLobby lobby;
    FScreen screen;
    Graphics localGraphics;
    HostedMatch hostedMatch;
    MobSprite enemy;
    PlayerSprite player;
    RegisteredPlayer humanPlayer;
    private DuelInput duelInput;

    public DuelScene() {

    }

    @Override
    public void dispose() {
    }

    @Override
    public void render() {

        //Batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        /*
        Gdx.gl.glClearColor(0,1,1,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Stage.getBatch().begin();
        Stage.getBatch().end();
        Stage.act(Gdx.graphics.getDeltaTime());
        Stage.draw();
        */
        ImageCache.allowSingleLoad();
        ForgeAnimation.advanceAll();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT); // Clear the screen.
        if (hostedMatch == null || hostedMatch.getGameView() == null)
            return;
        if (screen == null) {
            return;
        }


        localGraphics.begin(AdventureApplicationAdapter.CurrentAdapter.getCurrentWidth(), AdventureApplicationAdapter.CurrentAdapter.getCurrentHeight());
        screen.screenPos.setSize(AdventureApplicationAdapter.CurrentAdapter.getCurrentWidth(), AdventureApplicationAdapter.CurrentAdapter.getCurrentHeight());
        if (screen.getRotate180()) {
            localGraphics.startRotateTransform(AdventureApplicationAdapter.CurrentAdapter.getCurrentWidth() / 2, AdventureApplicationAdapter.CurrentAdapter.getCurrentHeight() / 2, 180);
        }
        screen.draw(localGraphics);
        if (screen.getRotate180()) {
            localGraphics.endTransform();
        }
        for (FOverlay overlay : FOverlay.getOverlays()) {
            if (overlay.isVisibleOnScreen(screen)) {
                overlay.screenPos.setSize(AdventureApplicationAdapter.CurrentAdapter.getCurrentWidth(), AdventureApplicationAdapter.CurrentAdapter.getCurrentHeight());
                overlay.setSize(AdventureApplicationAdapter.CurrentAdapter.getCurrentWidth(), AdventureApplicationAdapter.CurrentAdapter.getCurrentHeight()); //update overlay sizes as they're rendered
                if (overlay.getRotate180()) {
                    localGraphics.startRotateTransform(AdventureApplicationAdapter.CurrentAdapter.getCurrentHeight() / 2, AdventureApplicationAdapter.CurrentAdapter.getCurrentHeight() / 2, 180);
                }
                overlay.draw(localGraphics);
                if (overlay.getRotate180()) {
                    localGraphics.endTransform();
                }
            }
        }
        localGraphics.end();

        //Batch.end();
    }

    public void GameEnd() {
        AdventureApplicationAdapter.CurrentAdapter.SwitchScene(AdventureApplicationAdapter.CurrentAdapter.GetLastScene());

        ((GameScene) SceneType.GameScene.instance).stage.setWinner(humanPlayer == hostedMatch.getGame().getMatch().getWinner());

    }

    @Override
    public void Enter() {
        Set<GameType> appliedVariants = new HashSet<>();
        appliedVariants.add(GameType.Constructed);

        List<RegisteredPlayer> players = new ArrayList<>();
        humanPlayer = RegisteredPlayer.forVariants(2, appliedVariants, WorldSave.getCurrentSave().player.getDeck(), null, false, null, null);
        LobbyPlayer playerObject = GamePlayerUtil.getGuiPlayer();
        FSkin.getAvatars().put(90001, player.getAvatar());
        playerObject.setAvatarIndex(90001);
        humanPlayer.setPlayer(playerObject);


        RegisteredPlayer aiPlayer = RegisteredPlayer.forVariants(2, appliedVariants, DeckSerializer.fromFile(new File(Res.CurrentRes.GetFilePath(enemy.getData().deck))), null, false, null, null);
        LobbyPlayer enemyPlayer = GamePlayerUtil.createAiPlayer();

        FSkin.getAvatars().put(90000, this.enemy.getAvatar());
        enemyPlayer.setAvatarIndex(90000);

        enemyPlayer.setName(this.enemy.getData().name);
        aiPlayer.setPlayer(enemyPlayer);
        aiPlayer.setStartingLife(enemy.getData().life);

        players.add(humanPlayer);
        players.add(aiPlayer);

        final Map<RegisteredPlayer, IGuiGame> guiMap = new HashMap<>();
        guiMap.put(humanPlayer, MatchController.instance);

        hostedMatch = MatchController.hostMatch();


        GameRules rules = new GameRules(GameType.Constructed);
        rules.setPlayForAnte(false);
        rules.setMatchAnteRarity(true);
        rules.setGamesPerMatch(1);
        rules.setManaBurn(false);

        hostedMatch.setEndGameHook(() -> GameEnd());
        hostedMatch.startMatch(rules, appliedVariants, players, guiMap);

        MatchController.instance.setGameView(hostedMatch.getGameView());


        for (final Player p : hostedMatch.getGame().getPlayers()) {
            if (p.getController() instanceof PlayerControllerHuman) {
                final PlayerControllerHuman humanController = (PlayerControllerHuman) p.getController();
                humanController.setGui(MatchController.instance);
                MatchController.instance.setOriginalGameController(p.getView(), humanController);
                MatchController.instance.openView(new TrackableCollection<>(p.getView()));
            }
        }

        screen = MatchController.getView();

        screen.setSize(AdventureApplicationAdapter.CurrentAdapter.getCurrentWidth(), AdventureApplicationAdapter.CurrentAdapter.getCurrentHeight());


        Gdx.input.setInputProcessor(duelInput);

    }

    public void buildTouchListeners(int x, int y, List<FDisplayObject> potentialListeners) {
        screen.buildTouchListeners(x, y, potentialListeners);
    }

    @Override
    public void ResLoaded() {
        duelInput = new DuelInput();
        localGraphics = AdventureApplicationAdapter.CurrentAdapter.getGraphics();
    }


    @Override
    public void update(boolean fullUpdate) {

    }

    @Override
    public void update(int slot, LobbySlotType type) {

    }

    @Override
    public FScreen forgeScreen() {
        return screen;
    }

    public void setEnemy(MobSprite data) {
        this.enemy = data;
    }

    public void setPlayer(PlayerSprite sprite) {
        this.player = sprite;
    }
}

