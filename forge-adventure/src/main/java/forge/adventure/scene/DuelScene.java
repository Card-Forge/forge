package forge.adventure.scene;

import forge.LobbyPlayer;
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.character.MobSprite;
import forge.adventure.character.PlayerSprite;
import forge.adventure.libgdxgui.assets.FSkin;
import forge.adventure.libgdxgui.screens.FScreen;
import forge.adventure.libgdxgui.screens.match.MatchController;
import forge.adventure.util.Current;
import forge.adventure.world.AdventurePlayer;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.match.HostedMatch;
import forge.gui.interfaces.IGuiGame;
import forge.player.GamePlayerUtil;
import forge.player.PlayerControllerHuman;
import forge.trackable.TrackableCollection;

import java.util.*;

public class DuelScene extends ForgeScene {

    //GameLobby lobby;
    HostedMatch hostedMatch;
    MobSprite enemy;
    PlayerSprite player;
    RegisteredPlayer humanPlayer;
    public DuelScene() {

    }

    @Override
    public void dispose() {
    }


    public void GameEnd() {
       Scene last= AdventureApplicationAdapter.instance.switchToLast();

       if(last instanceof HudScene)
       {
           ((HudScene)last).stage.setWinner(humanPlayer == hostedMatch.getGame().getMatch().getWinner());
       }

    }

    @Override
    public void enter() {
        Set<GameType> appliedVariants = new HashSet<>();
        appliedVariants.add(GameType.Constructed);

        List<RegisteredPlayer> players = new ArrayList<>();
        humanPlayer = RegisteredPlayer.forVariants(2, appliedVariants, AdventurePlayer.current().getDeck(), null, false, null, null);
        LobbyPlayer playerObject = GamePlayerUtil.getGuiPlayer();
        FSkin.getAvatars().put(90001, Current.player().avatar());
        playerObject.setAvatarIndex(90001);
        humanPlayer.setPlayer(playerObject);
        humanPlayer.setStartingLife(Current.player().getLife());

        RegisteredPlayer aiPlayer = RegisteredPlayer.forVariants(2, appliedVariants, enemy.getData().getDeck(), null, false, null, null);
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
        super.enter();
    }

    @Override
    public FScreen getScreen() {
        return MatchController.getView();
    }



    public void setEnemy(MobSprite data) {
        this.enemy = data;
    }

    public void setPlayer(PlayerSprite sprite) {
        this.player = sprite;
    }
}

