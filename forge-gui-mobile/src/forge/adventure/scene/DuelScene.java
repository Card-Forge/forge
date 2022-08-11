package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.google.common.collect.Lists;
import forge.Forge;
import forge.Graphics;
import forge.LobbyPlayer;
import forge.adventure.character.EnemySprite;
import forge.adventure.character.PlayerSprite;
import forge.adventure.data.EffectData;
import forge.adventure.data.EnemyData;
import forge.adventure.data.ItemData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.util.Config;
import forge.adventure.util.Current;
import forge.assets.FBufferedImage;
import forge.assets.FSkin;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.quest.QuestUtil;
import forge.gui.FThreads;
import forge.gui.interfaces.IGuiGame;
import forge.item.IPaperCard;
import forge.player.GamePlayerUtil;
import forge.player.PlayerControllerHuman;
import forge.screens.FScreen;
import forge.screens.match.MatchController;
import forge.sound.MusicPlaylist;
import forge.sound.SoundSystem;
import forge.toolbox.FOptionPane;
import forge.trackable.TrackableCollection;
import forge.util.Aggregates;
import forge.util.Callback;
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
    Deck playerDeck;
    boolean chaosBattle = false;
    boolean callbackExit = false;
    List<IPaperCard> playerExtras = new ArrayList<>();
    List<IPaperCard> AIExtras = new ArrayList<>();


    public DuelScene() {}

    @Override
    public void dispose() {}

    public boolean hasCallbackExit() {
        return callbackExit;
    }

    public void GameEnd() {
        boolean winner=humanPlayer == hostedMatch.getGame().getMatch().getWinner();
        String enemyName=(enemy.nameOverride.isEmpty() ? enemy.getData().name : enemy.nameOverride);
        Current.player().clearBlessing();
        if (chaosBattle && !winner) {
            callbackExit = true;
            List<String> insult = Lists.newArrayList("I'm sorry...","... ...." ,"Learn from your defeat.",
                    "I haven't begun to use my full power.","No matter how much you try, you still won't beat me.",
                    "Your technique need work.","Rookie.","That's your best?","Hah ha ha ha ha ha ha!","?!......... (Seriously?!)",
                    "Forget about a rematch. Practice more instead." ,"That was a 100% effort on my part! Well, actually, no... That was more like 50%.",
                    "If you expected me to lose out of generosity, I'm truly sorry!" ,"You'll appreciate that I held back during the match!",
                    "That's the best you can do?","Don't waste my time with your skills!","Ha-ha-ha! What's the matter?",
                    "I hope I didn't hurt your ego too badly... Oops!","This match... I think I've learned something from this.",
                    "Hey! Don't worry about it!","You are not worthy!","Hm! You should go back to playing puzzle games!",
                    "Thought you could beat me?  Whew, talk about conceited.","*Yawn* ... Huh? It's over already? But I just woke up!",
                    "Next time bring an army. It might give you a chance." ,"The reason you lost is quite simple...",
                    "Is that all you can do?","You need to learn more to stand a chance.","You weren't that bad.","You made an effort at least.",
                    "From today, you can call me teacher.", "Hmph, predictable!", "I haven't used a fraction of my REAL power!" );
            String message = Aggregates.random(insult);
            FThreads.invokeInEdtNowOrLater(() -> FOptionPane.showMessageDialog(message, enemyName, new FBufferedImage(120, 120) {
                @Override
                protected void draw(Graphics g, float w, float h) {
                    if (FSkin.getAvatars().get(90000) != null)
                        g.drawImage(FSkin.getAvatars().get(90000), 0, 0, w, h);
                }
            }, new Callback<Integer>() {
                @Override
                public void run(Integer result) {
                    if (result == 0) {
                        afterGameEnd(enemyName, winner);
                    }
                }
            }));
        } else {
            afterGameEnd(enemyName, winner);
        }
    }
    void afterGameEnd(String enemyName, boolean winner) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                SoundSystem.instance.setBackgroundMusic(MusicPlaylist.MENUS); //start background music
                dungeonEffect = null;
                callbackExit = false;
                Forge.clearTransitionScreen();
                Forge.clearCurrentScreen();
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
        //Apply various combat effects.
        int lifeMod=0;
        int changeStartCards=0;
        Array<IPaperCard> startCards=new Array<>();

        for(EffectData data:effects) {
            lifeMod+=data.lifeModifier;
            changeStartCards+= data.changeStartCards;
            startCards.addAll(data.startBattleWithCards());
        }
        player.addExtraCardsOnBattlefield(startCards);
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
        int playerCount=1;
        EnemyData currentEnemy=enemy.getData();
        for(int i=0;i<8&&currentEnemy!=null;i++)
        {
            playerCount++;
            currentEnemy=currentEnemy.nextEnemy;
        }

        humanPlayer = RegisteredPlayer.forVariants(playerCount, appliedVariants,playerDeck, null, false, null, null);
        LobbyPlayer playerObject = GamePlayerUtil.getGuiPlayer();
        FSkin.getAvatars().put(90000, advPlayer.avatar());
        playerObject.setAvatarIndex(90000);
        humanPlayer.setPlayer(playerObject);
        humanPlayer.setTeamNumber(0);
        humanPlayer.setStartingLife(advPlayer.getLife());

        Array<EffectData> playerEffects = new Array<>();
        Array<EffectData> oppEffects    = new Array<>();


        Map<DeckProxy, Pair<List<String>, List<String>>> deckProxyMapMap = null;
        DeckProxy deckProxy =null;
        if(chaosBattle)
        {
             deckProxyMapMap = DeckProxy.getAllQuestChallenges();
            List<DeckProxy> decks = new ArrayList<>(deckProxyMapMap.keySet());
            deckProxy = Aggregates.random(decks);
            //playerextras
            List<IPaperCard> playerCards = new ArrayList<>();
            for (String s : deckProxyMapMap.get(deckProxy).getLeft()) {
                playerCards.add(QuestUtil.readExtraCard(s));
            }
            humanPlayer.addExtraCardsOnBattlefield(playerCards);
        }

        //Collect and add items effects first.
        for(String playerItem:advPlayer.getEquippedItems()) {
            ItemData item=ItemData.getItem(playerItem);
            if(item != null) {
                playerEffects.add(item.effect);
                if (item.effect.opponent != null) oppEffects.add(item.effect.opponent);
            } else {
                System.err.printf("Item %s not found.", playerItem);
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

        currentEnemy=enemy.getData();
        for(int i=0;i<8&&currentEnemy!=null;i++)
        {
            Deck deck=null;

            if (this.chaosBattle) { //random challenge for chaos mode
                //aiextras
                List<IPaperCard> aiCards = new ArrayList<>();
                for (String s : deckProxyMapMap.get(deckProxy).getRight()) {
                    aiCards.add(QuestUtil.readExtraCard(s));
                }
                this.AIExtras = aiCards;
                deck = deckProxy.getDeck();
            } else {
                deck=currentEnemy.copyPlayerDeck ? this.playerDeck : currentEnemy.generateDeck(Current.player().isFantasyMode(), Current.player().getDifficulty().name.equalsIgnoreCase("Hard"));
            }
            RegisteredPlayer aiPlayer = RegisteredPlayer.forVariants(playerCount, appliedVariants, deck, null, false, null, null);

            LobbyPlayer enemyPlayer = GamePlayerUtil.createAiPlayer(currentEnemy.name, selectAI(currentEnemy.ai));
            if(!enemy.nameOverride.isEmpty()) enemyPlayer.setName(enemy.nameOverride); //Override name if defined in the map.(only supported for 1 enemy atm)
            FSkin.getAvatars().put(90001+i, enemy.getAvatar(i));
            enemyPlayer.setAvatarIndex(90001+i);
            aiPlayer.setPlayer(enemyPlayer);
            aiPlayer.setTeamNumber(currentEnemy.teamNumber);
            aiPlayer.setStartingLife(Math.round((float)currentEnemy.life*advPlayer.getDifficulty().enemyLifeFactor));

            Array<EffectData> equipmentEffects    = new Array<>();
            if(currentEnemy.equipment!=null) {
                for(String oppItem:currentEnemy.equipment) {
                    ItemData item=ItemData.getItem(oppItem);
                    equipmentEffects.add(item.effect);
                    if(item.effect.opponent !=null) playerEffects.add(item.effect.opponent);
                }
            }
            addEffects(aiPlayer,oppEffects);
            addEffects(aiPlayer,equipmentEffects);


            //add extra cards for challenger mode
            if (chaosBattle) {
                aiPlayer.addExtraCardsOnBattlefield(AIExtras);
            }

            players.add(aiPlayer);



            Current.setLatestDeck(deck);

            currentEnemy=currentEnemy.nextEnemy;
        }



        players.add(humanPlayer);

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
        if (chaosBattle) {
            List<String> list = Lists.newArrayList("It all depends on your skill!","It's showtime!","Let's party!",
                    "You've proved yourself!","Are you ready? Go!","Prepare to strike, now!","Let's go!","What's next?",
                    "Yeah, I've been waitin' for this!","The stage of battle is set!","And the battle begins!","Let's get started!",
                    "Are you ready?","It's the battle of the century!","Let's keep it up!","How long will this go on?","I hope you're ready!",
                    "This could be the end.","Pull out all the stops!","It all comes down to this.","Who will emerge victorious?",
                    "Nowhere to run, nowhere to hide!","This battle is over!","There was no way out of that one!","Let's do this!","Let the madness begin!",
                    "It's all or nothing!","It's all on the line!","You can't back down now!","Do you have what it takes?","What will happen next?",
                    "Don't blink!","You can't lose here!","There's no turning back!","It's all or nothing now!");
            String message = Aggregates.random(list);
            FThreads.delayInEDT(600, () -> FThreads.invokeInEdtNowOrLater(() -> FOptionPane.showMessageDialog(message, enemy.getName(), new FBufferedImage(120, 120) {
                @Override
                protected void draw(Graphics g, float w, float h) {
                    if (FSkin.getAvatars().get(90000) != null)
                        g.drawImage(FSkin.getAvatars().get(90000), 0, 0, w, h);
                }
            })));
        }



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


        this.AIExtras.clear();
        this.playerExtras.clear();

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

