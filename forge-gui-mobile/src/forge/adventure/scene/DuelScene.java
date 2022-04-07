package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import forge.Forge;
import forge.LobbyPlayer;
import forge.adventure.character.EnemySprite;
import forge.adventure.character.PlayerSprite;
import forge.adventure.data.ItemData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.util.Config;
import forge.adventure.util.Current;
import forge.assets.FSkin;
import forge.deck.Deck;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.match.HostedMatch;
import forge.gui.interfaces.IGuiGame;
import forge.item.IPaperCard;
import forge.player.GamePlayerUtil;
import forge.player.PlayerControllerHuman;
import forge.screens.FScreen;
import forge.screens.match.MatchController;
import forge.sound.MusicPlaylist;
import forge.sound.SoundSystem;
import forge.trackable.TrackableCollection;

import java.util.*;

/**
 * DuelScene
 * Forge screen scene that contains the duel screen
 */
public class DuelScene extends ForgeScene {

    //GameLobby lobby;
    HostedMatch hostedMatch;
    EnemySprite enemy;
    PlayerSprite player;
    RegisteredPlayer humanPlayer;

    public DuelScene() {

    }

    @Override
    public void dispose() {
    }


    public void GameEnd() {
        boolean winner=humanPlayer == hostedMatch.getGame().getMatch().getWinner();
        String enemyName=enemy.getData().name;
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                SoundSystem.instance.setBackgroundMusic(MusicPlaylist.MENUS); //start background music
                Scene last = Forge.switchToLast();

                if (last instanceof HudScene) {
                    Current.player().getStatistic().setResult(enemyName, winner);
                    ((HudScene) last).stage.setWinner(winner);
                }
            }
        });


    }
    void addItemEffects(RegisteredPlayer player,Array<ItemData> items)
    {
        if(items==null)
            return;

        int lifeMod=0;
        int changeStartCards=0;
        Array<IPaperCard> startCards=new Array<>();

        for(ItemData data:items)
        {
            lifeMod+=data.lifeModifier;
            changeStartCards+= data.changeStartCards;
            startCards.addAll(data.startBattleWithCards());
        }
        player.setCardsOnBattlefield(startCards);
        player.setStartingLife(Math.max(1,lifeMod+player.getStartingLife()));
        player.setStartingHand(player.getStartingHand()+changeStartCards);
    }


    @Override
    public void enter() {
        Set<GameType> appliedVariants = new HashSet<>();
        appliedVariants.add(GameType.Constructed);

        List<RegisteredPlayer> players = new ArrayList<>();
        Deck playerDeck=(Deck)AdventurePlayer.current().getSelectedDeck().copyTo("PlayerDeckCopy");
        int missingCards= Config.instance().getConfigData().minDeckSize-playerDeck.getMain().countAll();
        if(missingCards>0)
            playerDeck.getMain().add("Wastes",missingCards);
        humanPlayer = RegisteredPlayer.forVariants(2, appliedVariants,playerDeck, null, false, null, null);
        LobbyPlayer playerObject = GamePlayerUtil.getGuiPlayer();
        FSkin.getAvatars().put(90001, Current.player().avatar());
        playerObject.setAvatarIndex(90001);
        humanPlayer.setPlayer(playerObject);
        humanPlayer.setStartingLife(Current.player().getLife());
        Current.setLatestDeck(enemy.getData().generateDeck());
        RegisteredPlayer aiPlayer = RegisteredPlayer.forVariants(2, appliedVariants, Current.latestDeck(), null, false, null, null);
        LobbyPlayer enemyPlayer = GamePlayerUtil.createAiPlayer(this.enemy.getData().name, selectAI(this.enemy.getData().ai));

        FSkin.getAvatars().put(90000, this.enemy.getAvatar());
        enemyPlayer.setAvatarIndex(90000);

        aiPlayer.setPlayer(enemyPlayer);
        aiPlayer.setStartingLife(Math.round((float)enemy.getData().life*Current.player().getDifficulty().enemyLifeFactor));



        Array<ItemData> playerItems=new Array<>();
        Array<ItemData> oppItems=new Array<>();

        for(String playerItem:Current.player().getEquippedItems())
        {
            ItemData item=ItemData.getItem(playerItem);
            playerItems.add(item);
            if(item.opponent !=null)
                oppItems.add(item.opponent);
        }
        if(enemy.getData().equipment!=null)
        {
            for(String oppItem:enemy.getData().equipment)
            {
                ItemData item=ItemData.getItem(oppItem);
                oppItems.add(item);
                if(item.opponent !=null)
                    playerItems.add(item.opponent);
            }
        }

        addItemEffects(humanPlayer,playerItems);
        addItemEffects(aiPlayer,oppItems);

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

        hostedMatch.setEndGameHook(new Runnable() {
            @Override
            public void run() {
                DuelScene.this.GameEnd();
            }
        });
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

    public void setEnemy(EnemySprite data) {
        this.enemy = data;
    }

    public void setPlayer(PlayerSprite sprite) {
        this.player = sprite;
    }

    private String selectAI(String ai) { //Decide opponent AI.
        String AI = ""; //Use user settings if it's null.
        if (ai != null){
            switch (this.enemy.getData().ai.toLowerCase()) { //We use this way to ensure capitalization is exact.
                //We don't want misspellings here.
                case "default":
                    AI = "Default";  break;
                case "reckless":
                    AI = "Reckless"; break;
                case "cautious":
                    AI = "Cautious"; break;
                case "experimental":
                    AI = "Experimental"; break;
                default:
                    AI = ""; //User settings.
            }
        }
        return AI;
    }
}

