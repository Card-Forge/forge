package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.google.common.collect.ImmutableList;
import forge.Forge;
import forge.Graphics;
import forge.LobbyPlayer;
import forge.adventure.character.EnemySprite;
import forge.adventure.character.PlayerSprite;
import forge.adventure.data.*;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.stage.GameHUD;
import forge.adventure.stage.IAfterMatch;
import forge.adventure.util.AdventureEventController;
import forge.adventure.util.AdventureModes;
import forge.adventure.util.Config;
import forge.adventure.util.Current;
import forge.assets.FBufferedImage;
import forge.assets.FSkin;
import forge.deck.*;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.GameOutcome;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.quest.QuestUtil;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.gui.FThreads;
import forge.gui.interfaces.IGuiGame;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.player.GamePlayerUtil;
import forge.player.PlayerControllerHuman;
import forge.screens.FScreen;
import forge.screens.LoadingOverlay;
import forge.screens.TransitionScreen;
import forge.screens.match.MatchController;
import forge.sound.MusicPlaylist;
import forge.sound.SoundSystem;
import forge.toolbox.FOptionPane;
import forge.trackable.TrackableCollection;
import forge.util.Aggregates;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * DuelScene
 * Forge screen scene that contains the duel screen
 */
public class DuelScene extends ForgeScene {
    private static DuelScene object;

    public static DuelScene instance() {
        if (object == null)
            object = new DuelScene();
        return object;
    }

    //GameLobby lobby;
    HostedMatch hostedMatch;
    EnemySprite enemy;
    PlayerSprite player;
    RegisteredPlayer humanPlayer;
    private EffectData dungeonEffect;
    Deck playerDeck;
    boolean chaosBattle = false;
    boolean callbackExit = false;
    boolean arenaBattleChallenge = false;
    boolean isArena = false;
    AdventureEventData eventData;
    final int enemyAvatarKey = 90001;
    final int playerAvatarKey = 90000;
    FOptionPane bossDialogue;
    List<IPaperCard> playerExtras = new ArrayList<>();
    List<IPaperCard> AIExtras = new ArrayList<>();


    private DuelScene() {
    }


    @Override
    public void dispose() {
    }

    public boolean hasCallbackExit() {
        return callbackExit;
    }

    public void GameEnd() {
        //TODO: Progress towards applicable Adventure quests also needs to be reported here.
        if (eventData != null)
            eventData.nextOpponent = null;
        boolean winner = false;
        try {
            winner = humanPlayer == hostedMatch.getGame().getMatch().getWinner();

            //Persists expended (or potentially gained) shards back to Adventure
            if (eventData == null || eventData.eventRules.allowsShards) {
                List<PlayerControllerHuman> humans = hostedMatch.getHumanControllers();
                {
                    if (humans.size() == 1) {
                        Current.player().setShards(humans.get(0).getPlayer().getNumManaShards());
                    }
                }
            }

            // Mostly for ante handling, but also blacker lotus
            GameOutcome.AnteResult anteResult = hostedMatch.getAnteResult(humanPlayer);
            if (anteResult != null) {
                if (eventData != null) {
                    //In an event. Apply the ante result to the current event deck.
                    eventData.registeredDeck.getOrCreate(DeckSection.Sideboard).add(anteResult.wonCards);
                    if(eventData.draftedDeck != null)
                        eventData.draftedDeck.getOrCreate(DeckSection.Sideboard).add(anteResult.wonCards);
                    for(PaperCard card : anteResult.lostCards) {
                        eventData.registeredDeck.removeAnteCard(card);
                        if(eventData.draftedDeck != null)
                            eventData.draftedDeck.removeAnteCard(card);
                    }
                    //Could also add the cards to the opponent's pool, but their games aren't simulated and they never edit their decks.
                }
                else {
                    for (PaperCard card : anteResult.wonCards) {
                        Current.player().addCard(card);
                    }
                    for (PaperCard card : anteResult.lostCards) {
                        // We could clean this up by trying to combine all the lostCards into a mapping, but good enough for now
                        Current.player().removeLostCardFromPools(card);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String enemyName = enemy.getName();
        boolean showMessages = enemy.getData().boss || (enemy.getData().copyPlayerDeck && Current.player().isUsingCustomDeck());
        Current.player().clearBlessing();
        if ((chaosBattle || showMessages) && !winner) {
            final FBufferedImage fb = getFBEnemyAvatar();
            callbackExit = true;
            boolean finalWinner = winner;
            bossDialogue = createFOption(Forge.getLocalizer().getMessage("AdvBossInsult" + Aggregates.randomInt(1, 44)),
                    enemyName, fb, () -> {
                        afterGameEnd(enemyName, finalWinner);
                        exitDuelScene();
                        fb.dispose();
                    });
            FThreads.invokeInEdtNowOrLater(() -> bossDialogue.show());
        } else {
            afterGameEnd(enemyName, winner);
        }
    }

    Runnable endRunnable = null;

    void afterGameEnd(String enemyName, boolean winner) {
        Forge.advFreezePlayerControls = winner;
        endRunnable = () -> Gdx.app.postRunnable(() -> {
            GameHUD.getInstance().updateBGM();
            dungeonEffect = null;
            callbackExit = false;
            Forge.clearTransitionScreen();
            Forge.clearScreenStack();
            Forge.advFreezePlayerControls = false;
            Scene last = Forge.switchToLast();
            Current.player().getStatistic().setResult(enemyName, winner);

            if (last instanceof IAfterMatch) {
                ((IAfterMatch) last).setWinner(winner, isArena);
            }
        });
    }

    public void exitDuelScene() {
        Forge.setTransitionScreen(new TransitionScreen(endRunnable, Forge.takeScreenshot(), false, false));
    }

    private FOptionPane createFOption(String message, String title, FBufferedImage icon, Runnable runnable) {
        return new FOptionPane(message, null, title, icon, null, ImmutableList.of(Forge.getLocalizer().getMessage("lblOK")), -1, result -> {
            if (runnable != null)
                runnable.run();
        });
    }

    void addEffects(RegisteredPlayer player, Array<EffectData> effects) {
        if (effects == null) return;
        //Apply various combat effects.
        int lifeMod = 0;
        int changeStartCards = 0;
        int extraManaShards = 0;
        Array<IPaperCard> startCards = new Array<>();
        Array<IPaperCard> startCardsInCommandZone = new Array<>();

        for (EffectData data : effects) {
            lifeMod += data.lifeModifier;
            changeStartCards += data.changeStartCards;
            startCards.addAll(data.startBattleWithCards());
            startCardsInCommandZone.addAll(data.startBattleWithCardsInCommandZone());

            extraManaShards += data.extraManaShards;
        }
        player.addExtraCardsOnBattlefield(startCards);
        player.addExtraCardsInCommandZone(startCardsInCommandZone);

        if (lifeMod != 0)
            player.setStartingLife(Math.max(1, lifeMod + player.getStartingLife()));
        player.setStartingHand(player.getStartingHand() + changeStartCards);
        player.setManaShards((player.getManaShards() + extraManaShards));
        player.setEnableETBCountersEffect(true); //enable etbcounters on starting cards like Ring of Three Wishes, etc...
    }

    public void setDungeonEffect(EffectData E) {
        dungeonEffect = E;
    }

    @Override
    public void enter() {
        SoundSystem.instance.stopBackgroundMusic();
        GameType mainGameType;
        boolean isDeckMissing = false;
        String isDeckMissingMsg = "";
        if (eventData != null && eventData.eventRules != null) {
            mainGameType = eventData.eventRules.gameType;
        } else if (AdventurePlayer.current().getAdventureMode() == AdventureModes.Commander){
            mainGameType = GameType.Commander;
        } else {
            mainGameType = GameType.Adventure;
        }
        Set<GameType> appliedVariants = EnumSet.of(mainGameType);

        AdventurePlayer advPlayer = Current.player();

        List<RegisteredPlayer> players = new ArrayList<>();

        applyAdventureDeckRules(mainGameType.getDeckFormat());
        int playerCount = 1;
        EnemyData currentEnemy = enemy.getData();
        for (int i = 0; i < 8 && currentEnemy != null; i++) {
            playerCount++;
            currentEnemy = currentEnemy.nextEnemy;
        }

        humanPlayer = RegisteredPlayer.forVariants(playerCount, appliedVariants, playerDeck, null, false, null, null);
        LobbyPlayer playerObject = GamePlayerUtil.getGuiPlayer();
        FSkin.getAvatars().put(playerAvatarKey, advPlayer.avatar());
        playerObject.setAvatarIndex(playerAvatarKey);
        humanPlayer.setPlayer(playerObject);
        humanPlayer.setTeamNumber(0);
        humanPlayer.setStartingLife(eventData != null ? eventData.eventRules.startingLife : advPlayer.getLife());
        if (eventData == null || eventData.eventRules.allowsShards)
            humanPlayer.setManaShards(advPlayer.getShards());

        Array<EffectData> playerEffects = new Array<>();
        Array<EffectData> oppEffects = new Array<>();

        Map<DeckProxy, Pair<List<String>, List<String>>> deckProxyMapMap = null;
        DeckProxy deckProxy = null;
        if (chaosBattle) {
            deckProxyMapMap = DeckProxy.getAllQuestChallenges();
            deckProxy = Aggregates.random(deckProxyMapMap.keySet());
            //playerextras
            List<IPaperCard> playerCards = new ArrayList<>();
            for (String s : deckProxyMapMap.get(deckProxy).getLeft()) {
                playerCards.add(QuestUtil.readExtraCard(s));
            }
            humanPlayer.addExtraCardsOnBattlefield(playerCards);
        }

        if (eventData == null || eventData.eventRules.allowsItems) {
            //Collect and add items effects first.
            for (Long id : advPlayer.getEquippedItems()) {
                ItemData item = Current.player().getEquippedItem(id);
                if (item != null && item.effect != null) {
                    playerEffects.add(item.effect);
                    if (item.effect.opponent != null) oppEffects.add(item.effect.opponent);
                } else {
                    System.err.printf("Item %s not found.", id);
                }
            }
        }
        if (eventData == null || eventData.eventRules.allowsBlessings) {
            //Collect and add player blessings.
            if (advPlayer.getBlessing() != null) {
                playerEffects.add(advPlayer.getBlessing());
                if (advPlayer.getBlessing().opponent != null) oppEffects.add(advPlayer.getBlessing().opponent);
            }

            //Collect and add enemy effects (same as blessings but for individual enemies).
            if (enemy.effect != null) {
                oppEffects.add(enemy.effect);
                if (enemy.effect.opponent != null)
                    playerEffects.add(enemy.effect.opponent);
            }
        }
        //Collect and add dungeon-wide effects.
        if (dungeonEffect != null) {
            oppEffects.add(dungeonEffect);
            if (dungeonEffect.opponent != null)
                playerEffects.add(dungeonEffect.opponent);
        }

        addEffects(humanPlayer, playerEffects);

        currentEnemy = enemy.getData();
        boolean bossBattle = currentEnemy.boss;
        for (int i = 0; i < playerCount && currentEnemy != null; i++) {
            Deck deck;

            if (this.chaosBattle) { //random challenge for chaos mode
                if (deckProxyMapMap == null)
                    continue;
                //aiextras
                List<IPaperCard> aiCards = new ArrayList<>();
                for (String s : deckProxyMapMap.get(deckProxy).getRight()) {
                    aiCards.add(QuestUtil.readExtraCard(s));
                }
                this.AIExtras = aiCards;
                deck = deckProxy.getDeck();
            } else if (this.arenaBattleChallenge) {
                deck = Aggregates.random(DeckProxy.getAllGeneticAIDecks()).getDeck();
            } else if (this.eventData != null) {
                deck = eventData.nextOpponent.getDeck();
            } else {
                deck = currentEnemy.copyPlayerDeck ? this.playerDeck : currentEnemy.generateDeck(Current.player().isFantasyMode(), Current.player().isUsingCustomDeck() || Current.player().isHardorInsaneDifficulty());
            }
            if (deck == null) {
                isDeckMissing = true;
                isDeckMissingMsg = "Deck for " + currentEnemy.getName() + " is missing! " + (this.eventData == null ? "Genetic AI deck will be used." : "Player deck will be used.");
                System.err.println(isDeckMissingMsg);
                deck = this.eventData == null ? Aggregates.random(DeckProxy.getAllGeneticAIDecks()).getDeck() : this.playerDeck;
            }
            RegisteredPlayer aiPlayer = RegisteredPlayer.forVariants(playerCount, appliedVariants, deck, null, false, null, null);

            LobbyPlayer enemyPlayer = GamePlayerUtil.createAiPlayer(currentEnemy.getName(), selectAI(currentEnemy.ai));
            enemyPlayer.setName(enemy.getName()); //Override name if defined in the map.(only supported for 1 enemy atm)
            TextureRegion enemyAvatar = enemy.getAvatar(i);
            enemyAvatar.flip(true, false); //flip facing left
            FSkin.getAvatars().put(enemyAvatarKey + i, enemyAvatar);
            enemyPlayer.setAvatarIndex(enemyAvatarKey + i);
            aiPlayer.setPlayer(enemyPlayer);
            aiPlayer.setTeamNumber(currentEnemy.teamNumber);
            aiPlayer.setStartingLife(eventData != null ? eventData.eventRules.startingLife : Math.round((float) currentEnemy.life * advPlayer.getDifficulty().enemyLifeFactor));

            Array<EffectData> equipmentEffects = new Array<>();
            if (eventData != null && eventData.eventRules.allowsItems) {
                if (currentEnemy.equipment != null) {
                    for (String oppItem : currentEnemy.equipment) {
                        ItemData item = ItemListData.getItem(oppItem);
                        if (item == null)
                            continue;
                        equipmentEffects.add(item.effect);
                        if (item.effect.opponent != null)
                            playerEffects.add(item.effect.opponent);
                    }
                }
            }
            addEffects(aiPlayer, oppEffects);
            addEffects(aiPlayer, equipmentEffects);

            //add extra cards for challenger mode
            if (chaosBattle) {
                aiPlayer.addExtraCardsOnBattlefield(AIExtras);
            }

            players.add(aiPlayer);

            if (eventData == null) {
                Current.setLatestDeck(deck);
            }

            currentEnemy = currentEnemy.nextEnemy;
        }

        players.add(humanPlayer);

        if(eventData != null && eventData.draft != null) {
            for(RegisteredPlayer p : players)
                p.assignConspiracies();
        }

        final Map<RegisteredPlayer, IGuiGame> guiMap = new HashMap<>();
        guiMap.put(humanPlayer, MatchController.instance);

        hostedMatch = MatchController.hostMatch();

        GameRules rules;

        if (eventData != null) {
            rules = new GameRules(eventData.eventRules.gameType);
            rules.setGamesPerMatch(eventData.eventRules.gamesPerMatch);
            bossBattle = false;
        } else {
            rules = new GameRules(GameType.Adventure);
            rules.setGamesPerMatch(enemy.getData().gamesPerMatch);
        }
        rules.setPlayForAnte(FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_ANTE));
        rules.setMatchAnteRarity(FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_ANTE_MATCH_RARITY));
        rules.setManaBurn(false);
        rules.setWarnAboutAICards(false);

        //hostedMatch.setEndGameHook(() -> DuelScene.this.GameEnd());
        hostedMatch.startMatch(rules, appliedVariants, players, guiMap, bossBattle ? MusicPlaylist.BOSS : MusicPlaylist.MATCH);
        MatchController.instance.setGameView(hostedMatch.getGameView());
        boolean showMessages = enemy.getData().boss || (enemy.getData().copyPlayerDeck && Current.player().isUsingCustomDeck());
        LoadingOverlay matchOverlay;
        if (chaosBattle || showMessages || isDeckMissing) {
            final FBufferedImage fb = getFBEnemyAvatar();
            bossDialogue = createFOption(isDeckMissing ? isDeckMissingMsg : Forge.getLocalizer().getMessage("AdvBossIntro" + Aggregates.randomInt(1, 35)),
                    enemy.getName(), fb, fb::dispose);
            matchOverlay = new LoadingOverlay(() -> FThreads.delayInEDT(300, () -> FThreads.invokeInEdtNowOrLater(() ->
                    bossDialogue.show())), false, true);
        } else {
            matchOverlay = new LoadingOverlay(null);
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
        matchOverlay.show();
    }

    private static final String PLACEHOLDER_MAIN = "Wastes";
    private static final String PLACEHOLDER_ATTRACTION = "Coin-Operated Pony";
    private static final String PLACEHOLDER_CONTRAPTION = "Automatic Fidget Spinner";

    private void applyAdventureDeckRules(DeckFormat format) {
        //Can't just keep the player from entering a battle if their deck is invalid. So instead we'll just edit their deck.
        CardPool mainSection = playerDeck.getMain(), attractions = playerDeck.get(DeckSection.Attractions), contraptions = playerDeck.get(DeckSection.Contraptions);

        removeExcessCopies(mainSection, format);
        removeExcessCopies(attractions, format);
        removeExcessCopies(contraptions, format);

        int mainSize = mainSection.countAll();

        int maxDeckSize = format == DeckFormat.Adventure ? Integer.MAX_VALUE : format.getMainRange().getMaximum();
        int excessCards = mainSize - maxDeckSize;
        if (excessCards > 0) {
            List<PaperCard> removals = Aggregates.random(mainSection.toFlatList(), excessCards);
            mainSection.removeAllFlat(removals);
        }

        int minDeckSize = format == DeckFormat.Adventure ? Config.instance().getConfigData().minDeckSize : format.getMainRange().getMinimum();
        int missingCards = minDeckSize - mainSize;
        if (missingCards > 0) //Replace unknown cards for a Wastes.
            mainSection.add(PLACEHOLDER_MAIN, missingCards);

        if(attractions != null && !attractions.isEmpty()) {
            int missingAttractions = 10 - attractions.countAll(); //TODO: These shouldn't be hard coded but DeckFormat's gonna need some reorganizing to fetch this dynamically
            if(missingAttractions > 0)
                attractions.add(PLACEHOLDER_ATTRACTION, missingAttractions);
        }
        if(contraptions != null && !contraptions.isEmpty()) {
            int missingContraptions = 15 - contraptions.countAll();
            if(missingContraptions > 0)
                contraptions.add(PLACEHOLDER_CONTRAPTION, missingContraptions);
        }
    }

    private static void removeExcessCopies(CardPool section, DeckFormat format) {
        if(section == null)
            return;
        for(Map.Entry<PaperCard, Integer> e : section) {
            PaperCard card = e.getKey();
            int amount = e.getValue();
            int limit = format.getMaxCardCopies(card);
            if(amount > limit)
                section.remove(card, amount - limit);
        }
    }

    @Override
    public FScreen getScreen() {
        return MatchController.getView();
    }

    public void initDuels(PlayerSprite playerSprite, EnemySprite enemySprite) {
        initDuels(playerSprite, enemySprite, false, null);
    }

    public void initDuels(PlayerSprite playerSprite, EnemySprite enemySprite, boolean isArena, AdventureEventData eventData) {
        this.player = playerSprite;
        this.enemy = enemySprite;
        this.isArena = isArena;
        this.eventData = eventData;
        if (eventData != null && eventData.eventRules == null)
            eventData.eventRules = new AdventureEventData.AdventureEventRules(AdventureEventController.EventFormat.Constructed, 1.0f);
        this.arenaBattleChallenge = isArena && Current.player().isHardorInsaneDifficulty();
        if (eventData != null && eventData.registeredDeck != null)
            this.playerDeck = eventData.registeredDeck;
        else
            this.playerDeck = (Deck) Current.player().getSelectedDeck().copyTo("PlayerDeckCopy");
        this.chaosBattle = this.enemy.getData().copyPlayerDeck && Current.player().isFantasyMode();
        this.AIExtras.clear();
        this.playerExtras.clear();
    }

    private String selectAI(String ai) { //Decide opponent AI.
        String AI = ""; //Use user settings if it's null.
        if (ai != null) {
            AI = switch (ai.toLowerCase()) { //We use this way to ensure capitalization is exact.
                //We don't want misspellings here.
                case "default" -> "Default";
                case "reckless" -> "Reckless";
                case "cautious" -> "Cautious";
                case "experimental" -> "Experimental";
                default -> ""; //User settings.
            };
        }
        return AI;
    }

    private FBufferedImage getFBEnemyAvatar() {
        return new FBufferedImage(120, 120) {
            @Override
            protected void draw(Graphics g, float w, float h) {
                if (FSkin.getAvatars().get(enemyAvatarKey) != null)
                    g.drawImage(FSkin.getAvatars().get(enemyAvatarKey), 0, 0, w, h);
            }
        };
    }
}
