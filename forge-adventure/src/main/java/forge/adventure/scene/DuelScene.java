package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import forge.Graphics;
import forge.deck.io.DeckSerializer;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.match.GameLobby;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.match.LobbySlotType;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.interfaces.IUpdateable;
import forge.player.GamePlayerUtil;
import forge.screens.FScreen;
import forge.screens.match.MatchController;
import forge.toolbox.FOverlay;

import java.io.File;
import java.util.*;

public class DuelScene extends Scene implements IUpdateable {

    //GameLobby lobby;
    FScreen screen;
    Graphics graphics;
    HostedMatch hostedMatch;
    public DuelScene() {

    }

    @Override
    public void dispose() {
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
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT); // Clear the screen.
        if(hostedMatch== null || hostedMatch .getGameView()==null)
            return;
        if (screen==null)
        {

            screen =  MatchController.getView();
            screen.setSize(IntendedWidth, IntendedHeight);
        }

        graphics.begin(IntendedWidth, IntendedHeight);
        screen.screenPos.setSize(IntendedWidth, IntendedHeight);
        if (screen.getRotate180()) {
            graphics.startRotateTransform(IntendedWidth / 2, IntendedHeight / 2, 180);
        }
        screen.draw(graphics);
        if (screen.getRotate180()) {
            graphics.endTransform();
        }
        for (FOverlay overlay : FOverlay.getOverlays()) {
            if (overlay.isVisibleOnScreen(screen)) {
                overlay.screenPos.setSize(IntendedWidth, IntendedHeight);
                overlay.setSize(IntendedWidth, IntendedHeight); //update overlay sizes as they're rendered
                if (overlay.getRotate180()) {
                    graphics.startRotateTransform(IntendedWidth / 2, IntendedHeight / 2, 180);
                }
                overlay.draw(graphics);
                if (overlay.getRotate180()) {
                    graphics.endTransform();
                }
            }
        }
        graphics.end();


        //Batch.end();
    }


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

        hostedMatch = GuiBase.getInterface().hostMatch();
        hostedMatch.startMatch(GameType.Constructed, appliedVariants, players, guiMap);

        Gdx.input.setInputProcessor(new DuelInput(hostedMatch));

    }
    public boolean Resume()
    {
        return true;
    }
    public boolean Exit()
    {
        Gdx.app.exit();
        return true;
    }
    @Override
    public void create() {
        Stage = new Stage(new StretchViewport(IntendedWidth,IntendedHeight));
        //lobby = new LocalLobby();
        graphics=new Graphics();
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

