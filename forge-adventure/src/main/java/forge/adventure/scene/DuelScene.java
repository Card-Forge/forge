package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import forge.Graphics;
import forge.adventure.AdventureApplicationAdapter;
import forge.animation.ForgeAnimation;
import forge.assets.ImageCache;
import forge.deck.io.DeckSerializer;
import forge.game.GameType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.match.GameLobby;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.match.LobbySlotType;
import forge.gui.interfaces.IGuiGame;
import forge.interfaces.IUpdateable;
import forge.player.GamePlayerUtil;
import forge.player.PlayerControllerHuman;
import forge.screens.FScreen;
import forge.screens.match.MatchController;
import forge.toolbox.FOverlay;
import forge.trackable.TrackableCollection;

import java.io.File;
import java.util.*;

public class DuelScene extends Scene implements IUpdateable {

    //GameLobby lobby;
    FScreen screen;
    Graphics localGraphics;
    HostedMatch hostedMatch;
    public DuelScene() {

    }

    @Override
    public void dispose() {
        if(Stage!=null)
            Stage.dispose();
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
        if(hostedMatch== null || hostedMatch .getGameView()==null)
            return;
        if (screen==null)
        {

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

    public void GameEnd()
    {

    }

    private DuelInput duelInput;
    @Override
    public void Enter()
    {
        Set<GameType> appliedVariants = new HashSet<>();
        appliedVariants.add(GameType.Constructed);

        List<RegisteredPlayer> players = new ArrayList<>();
        RegisteredPlayer humanPlayer = RegisteredPlayer.forVariants(2, appliedVariants, DeckSerializer.fromFile(new File("../forge-gui/res/quest/duels/Agent K 1.dck")), null, false, null, null);
        humanPlayer.setPlayer(GamePlayerUtil.getGuiPlayer());
        RegisteredPlayer aiPlayer = RegisteredPlayer.forVariants(2, appliedVariants, DeckSerializer.fromFile(new File("../forge-gui/res/quest/duels/Agent K 1.dck")), null, false, null, null);
        aiPlayer.setPlayer(GamePlayerUtil.createAiPlayer());
        players.add(humanPlayer);
        players.add(aiPlayer);

        final Map<RegisteredPlayer, IGuiGame> guiMap = new HashMap<>();
        guiMap.put(humanPlayer, MatchController.instance);

        hostedMatch = MatchController.instance.hostMatch();

        hostedMatch.setEndGameHook(()->GameEnd());
        hostedMatch.startMatch(GameType.Constructed, appliedVariants, players, guiMap);

        MatchController.instance.setGameView(hostedMatch.getGameView());


        for (final Player p :         hostedMatch.getGame().getPlayers()) {
            if (p.getController() instanceof PlayerControllerHuman) {
                final PlayerControllerHuman humanController = (PlayerControllerHuman) p.getController();
                humanController.setGui(MatchController.instance);
                MatchController.instance.setOriginalGameController(p.getView(), humanController);
                MatchController.instance.openView(new TrackableCollection<>(p.getView()));
            }
        }

        screen =  MatchController.getView();
        screen.setHeaderCaption("DUUUUUUUELLL");
        screen.setSize(AdventureApplicationAdapter.CurrentAdapter.getCurrentWidth(), AdventureApplicationAdapter.CurrentAdapter.getCurrentHeight());


        Gdx.input.setInputProcessor(duelInput);

    }

    @Override
    public void create() {
        duelInput=new DuelInput();
        localGraphics= AdventureApplicationAdapter.CurrentAdapter.getGraphics();
        //lobby = new LocalLobby();
        //initLobby(lobby);






    }
    protected void initLobby(GameLobby lobby) {
        lobby.setListener(this);

        boolean hasControl = lobby.hasControl();
        while (lobby.getNumberOfSlots() < 2){
            lobby.addSlot();
        }
        for(int i=0;i<lobby.getNumberOfSlots();i++)
        {
            lobby.getSlot(i).setDeck(DeckSerializer.fromFile(new File("../forge-gui/res/quest/duels/Agent K 1.dck")));
        }
    }

    @Override
    public void update(boolean fullUpdate) {

    }

    @Override
    public void update(int slot, LobbySlotType type) {

    }
}

