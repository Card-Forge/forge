package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import forge.Forge;
import forge.LobbyPlayer;
import forge.adventure.character.EnemySprite;
import forge.adventure.character.PlayerSprite;
import forge.adventure.data.EffectData;
import forge.adventure.data.ItemData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.util.Config;
import forge.adventure.util.Current;
import forge.assets.FSkin;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.quest.QuestUtil;
import forge.gui.interfaces.IGuiGame;
import forge.item.IPaperCard;
import forge.player.GamePlayerUtil;
import forge.player.PlayerControllerHuman;
import forge.screens.FScreen;
import forge.screens.match.MatchController;
import forge.sound.MusicPlaylist;
import forge.sound.SoundSystem;
import forge.trackable.TrackableCollection;
import forge.util.Aggregates;
import org.apache.commons.lang3.tuple.Pair;

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
    private EffectData dungeonEffect;
    Deck playerDeck, enemyDeck;
    boolean chaosBattle = false;
    List<IPaperCard> playerExtras = new ArrayList<>();
    List<IPaperCard> AIExtras = new ArrayList<>();


    public DuelScene() {

    }

    @Override
    public void dispose() {
    }


    public void GameEnd() {
        boolean winner=humanPlayer == hostedMatch.getGame().getMatch().getWinner();
        String enemyName=(enemy.nameOverride.isEmpty() ? enemy.getData().name : enemy.nameOverride);
        Current.player().clearBlessing();
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                SoundSystem.instance.setBackgroundMusic(MusicPlaylist.MENUS); //start background music
                dungeonEffect = null;
                Scene last = Forge.switchToLast();

                if (last instanceof HudScene) {
                    Current.player().getStatistic().setResult(enemyName, winner);
                    ((HudScene) last).stage.setWinner(winner);
                }
            }
        });


    }
    void addEffects(RegisteredPlayer player,Array<EffectData> effects) {
        if( effects == null ) return;
        //Apply various effects.
        int lifeMod=0;
        int changeStartCards=0;
        Array<IPaperCard> startCards=new Array<>();

        for(EffectData data:effects) {
            lifeMod+=data.lifeModifier;
            changeStartCards+= data.changeStartCards;
            startCards.addAll(data.startBattleWithCards());
        }
        player.setCardsOnBattlefield(startCards);
        player.setStartingLife(Math.max(1,lifeMod+player.getStartingLife()));
        player.setStartingHand(player.getStartingHand()+changeStartCards);
    }

    public void setDungeonEffect(EffectData E) {
        dungeonEffect = E;
    }


    @Override
    public void enter() {
        Set<GameType> appliedVariants = new HashSet<>();
        appliedVariants.add(GameType.Constructed);
        AdventurePlayer advPlayer = Current.player();

        List<RegisteredPlayer> players = new ArrayList<>();
        int missingCards= Config.instance().getConfigData().minDeckSize-playerDeck.getMain().countAll();
        if( missingCards > 0 ) //Replace unknown cards for a Wastes.
            playerDeck.getMain().add("Wastes",missingCards);
        humanPlayer = RegisteredPlayer.forVariants(2, appliedVariants,playerDeck, null, false, null, null);
        LobbyPlayer playerObject = GamePlayerUtil.getGuiPlayer();
        FSkin.getAvatars().put(90001, advPlayer.avatar());
        playerObject.setAvatarIndex(90001);
        humanPlayer.setPlayer(playerObject);
        humanPlayer.setStartingLife(advPlayer.getLife());
        Current.setLatestDeck(enemyDeck);
        RegisteredPlayer aiPlayer = RegisteredPlayer.forVariants(2, appliedVariants, Current.latestDeck(), null, false, null, null);
        LobbyPlayer enemyPlayer = GamePlayerUtil.createAiPlayer(this.enemy.getData().name, selectAI(this.enemy.getData().ai));
        if(!enemy.nameOverride.isEmpty()) enemyPlayer.setName(enemy.nameOverride); //Override name if defined in the map.
        FSkin.getAvatars().put(90000, this.enemy.getAvatar());
        enemyPlayer.setAvatarIndex(90000);

        aiPlayer.setPlayer(enemyPlayer);
        aiPlayer.setStartingLife(Math.round((float)enemy.getData().life*advPlayer.getDifficulty().enemyLifeFactor));

        Array<EffectData> playerEffects = new Array<>();
        Array<EffectData> oppEffects    = new Array<>();

        //Collect and add items effects first.
        for(String playerItem:advPlayer.getEquippedItems()) {
            ItemData item=ItemData.getItem(playerItem);
            playerEffects.add(item.effect);
            if(item.effect.opponent != null) oppEffects.add(item.effect.opponent);
        }
        if(enemy.getData().equipment!=null) {
            for(String oppItem:enemy.getData().equipment) {
                ItemData item=ItemData.getItem(oppItem);
                oppEffects.add(item.effect);
                if(item.effect.opponent !=null) playerEffects.add(item.effect.opponent);
            }
        }

        //Collect and add player blessings.
        if(advPlayer.getBlessing() != null){
            playerEffects.add(advPlayer.getBlessing());
            if(advPlayer.getBlessing().opponent != null) oppEffects.add(advPlayer.getBlessing().opponent);
        }

        //Collect and add enemy effects (same as blessings but for individual enemies).
        if(enemy.effect != null){
            oppEffects.add(enemy.effect);
            if(enemy.effect.opponent != null)
                playerEffects.add(enemy.effect.opponent);
        }

        //Collect and add dungeon-wide effects.
        if(dungeonEffect != null) {
            oppEffects.add(dungeonEffect);
            if(dungeonEffect.opponent != null)
                playerEffects.add(dungeonEffect.opponent);
        }

        addEffects(humanPlayer,playerEffects);
        addEffects(aiPlayer,oppEffects);

        //add extra cards for challenger mode
        if (chaosBattle) {
            humanPlayer.addExtraCardsOnBattlefield(playerExtras);
            aiPlayer.addExtraCardsOnBattlefield(AIExtras);
        }

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

    public void initDuels(PlayerSprite playerSprite, EnemySprite enemySprite) {
        this.player = playerSprite;
        this.enemy = enemySprite;
        this.playerDeck = (Deck)AdventurePlayer.current().getSelectedDeck().copyTo("PlayerDeckCopy");
        this.chaosBattle = this.enemy.getData().copyPlayerDeck && Current.player().isFantasyMode();
        if (this.chaosBattle) { //random challenge for chaos mode
            Map<DeckProxy, Pair<List<String>, List<String>>> deckProxyMapMap = DeckProxy.getAllQuestChallenges();
            List<DeckProxy> decks = new ArrayList<>(deckProxyMapMap.keySet());
            DeckProxy deck = Aggregates.random(decks);
            //playerextras
            List<IPaperCard> playerCards = new ArrayList<>();
            for (String s : deckProxyMapMap.get(deck).getLeft()) {
                playerCards.add(QuestUtil.readExtraCard(s));
            }
            this.playerExtras = playerCards;
            //aiextras
            List<IPaperCard> aiCards = new ArrayList<>();
            for (String s : deckProxyMapMap.get(deck).getRight()) {
                aiCards.add(QuestUtil.readExtraCard(s));
            }
            this.AIExtras = aiCards;
            this.enemyDeck = deck.getDeck();
        } else {
            this.AIExtras.clear();
            this.playerExtras.clear();
            this.enemyDeck = this.enemy.getData().copyPlayerDeck ? this.playerDeck : this.enemy.getData().generateDeck(Current.player().isFantasyMode());
        }
    }
    public Deck getPlayerDeck() {
        return this.playerDeck;
    }
    public Deck getEnemyDeck() {
        return this.enemyDeck;
    }

    private String selectAI(String ai) { //Decide opponent AI.
        String AI = ""; //Use user settings if it's null.
        if (ai != null){
            switch (ai.toLowerCase()) { //We use this way to ensure capitalization is exact.
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

