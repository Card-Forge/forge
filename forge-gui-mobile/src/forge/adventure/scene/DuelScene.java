package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.google.common.collect.ImmutableList;
import forge.Forge;
import forge.Graphics;
import forge.LobbyPlayer;
import forge.card.CardRenderer;
import forge.card.CardRenderer.CardStackPosition;
import forge.card.CardZoom;
import forge.adventure.character.EnemySprite;
import forge.adventure.character.PlayerSprite;
import forge.adventure.data.*;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.stage.GameHUD;
import forge.adventure.stage.IAfterMatch;
import forge.adventure.util.AdventureEventController;
import forge.adventure.util.Config;
import forge.adventure.util.Current;
import forge.assets.FBufferedImage;
import forge.assets.FSkin;
import forge.card.ColorSet;
import forge.deck.*;
import forge.game.card.CardView;
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
import forge.toolbox.FCardPanel;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FOptionPane;
import forge.trackable.TrackableCollection;
import forge.util.Aggregates;
import forge.util.StreamUtil;
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
        List<PaperCard> anteWonCards = Collections.emptyList();
        List<PaperCard> anteLostCards = Collections.emptyList();
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
                anteWonCards = new ArrayList<>(anteResult.wonCards);
                anteLostCards = new ArrayList<>(anteResult.lostCards);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String enemyName = enemy.getName();
        String insult = enemy.getBossInsult();
        boolean showMessages = enemy.getData().boss || (enemy.getData().copyPlayerDeck && Current.player().isUsingCustomDeck());
        Current.player().clearBlessing();

        boolean finalWinner = winner;
        boolean isBossLoss = (chaosBattle || showMessages) && !finalWinner;
        boolean hasAnteResults = !anteWonCards.isEmpty() || !anteLostCards.isEmpty();

        // No popups needed, preserve original behavior
        if (!hasAnteResults && !isBossLoss) {
            afterGameEnd(enemyName, finalWinner);
            return;
        }

        // Build popup chain: ante results -> boss dialogue -> exit
        callbackExit = true;
        Runnable exitChain = () -> {
            afterGameEnd(enemyName, finalWinner);
            exitDuelScene();
        };

        Runnable afterAnte;
        if (isBossLoss) {
            afterAnte = () -> {
                final FBufferedImage fb = getFBEnemyAvatar();
                String bossInsultMsg = insult != null ? insult
                        : Forge.getLocalizer().getMessage("AdvBossInsult" + Aggregates.randomInt(1, 44));
                bossDialogue = createFOption(bossInsultMsg,
                        enemyName, fb, () -> {
                            exitChain.run();
                            fb.dispose();
                        });
                FThreads.invokeInEdtNowOrLater(() -> bossDialogue.show());
            };
        } else {
            afterAnte = exitChain;
        }

        if (hasAnteResults) {
            showAnteResults(anteWonCards, anteLostCards, afterAnte);
        } else {
            afterAnte.run();
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

    private void showAnteResults(List<PaperCard> wonCards, List<PaperCard> lostCards, Runnable onDone) {
        // Show won cards one at a time, then lost cards, then continue
        showAnteCardsSequentially(wonCards, 0, true, () ->
            showAnteCardsSequentially(lostCards, 0, false, onDone));
    }

    private void showAnteCardsSequentially(List<PaperCard> cards, int index, boolean won, Runnable onDone) {
        if (index >= cards.size()) {
            onDone.run();
            return;
        }
        PaperCard card = cards.get(index);
        Runnable next = () -> showAnteCardsSequentially(cards, index + 1, won, onDone);
        showAnteCardPopup(won ? "Card Gained" : "Card Lost", card, won, next);
    }

    private void showAnteCardPopup(String title, PaperCard card, boolean won, Runnable onDone) {
        CardView cardView = CardView.getCardForUi(card);

        FDisplayObject cardDisplay = new FDisplayObject() {
            @Override
            public boolean tap(float x, float y, int count) {
                CardZoom.show(cardView);
                return true;
            }
            @Override
            public boolean longPress(float x, float y) {
                CardZoom.show(cardView);
                return true;
            }
            @Override
            public void draw(Graphics g) {
                float h = getHeight();
                float w = h / FCardPanel.ASPECT_RATIO;
                float xPos = (getWidth() - w) / 2;
                CardRenderer.drawCard(g, cardView, xPos, 0, w, h, CardStackPosition.Top, true);
            }
        };
        cardDisplay.setHeight(Forge.getScreenHeight() / 3);

        String message = card.getName();
        List<String> buttons;
        if (won && eventData == null) {
            int sellPrice = Current.player().cardSellPrice(card);
            buttons = sellPrice > 0
                    ? ImmutableList.of(Forge.getLocalizer().getMessage("lblOK"), "Auto-Sell (" + sellPrice + " gold)")
                    : ImmutableList.of(Forge.getLocalizer().getMessage("lblOK"));
        } else {
            buttons = ImmutableList.of(Forge.getLocalizer().getMessage("lblOK"));
        }

        FOptionPane popup = new FOptionPane(message, null, title, null, cardDisplay, buttons, 0, result -> {
            if (won && result == 1) {
                Current.player().autoSellCards.add(card);
            }
            if (onDone != null) onDone.run();
        });
        FThreads.invokeInEdtNowOrLater(popup::show);
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
        } else if (AdventurePlayer.current().isCommanderMode()){
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
                if (Config.instance().getConfigData().enableGeneticAI) {
                    deck = Aggregates.random(DeckProxy.getAllGeneticAIDecks()).getDeck();
                } else {
                    deck = currentEnemy.generateDeck(Current.player().isFantasyMode(), false);
                }
            } else if (this.eventData != null) {
                deck = eventData.nextOpponent.getDeck();
            } else {
                boolean useGeneticAI = Config.instance().getConfigData().enableGeneticAI && (Current.player().isUsingCustomDeck() || Current.player().isHardorInsaneDifficulty());
                deck = currentEnemy.copyPlayerDeck ? this.playerDeck : currentEnemy.generateDeck(Current.player().isFantasyMode(), useGeneticAI);
            }
            if (deck == null) {
                isDeckMissing = true;
                boolean canUseGeneticAI = Config.instance().getConfigData().enableGeneticAI;
                isDeckMissingMsg = "Deck for " + currentEnemy.getName() + " is missing! " + (this.eventData == null ? (canUseGeneticAI ? "Genetic AI deck will be used." : "Player deck will be used.") : "Player deck will be used.");
                System.err.println(isDeckMissingMsg);
                deck = this.eventData == null && canUseGeneticAI ? Aggregates.random(DeckProxy.getAllGeneticAIDecks()).getDeck() : this.playerDeck;
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
            String Intro = enemy.getBossIntro();
            if (Intro != null){
                bossDialogue = createFOption((Intro), enemy.getName(), fb, fb::dispose);
                }
                else {
                bossDialogue = createFOption(isDeckMissing ? isDeckMissingMsg : Forge.getLocalizer().getMessage("AdvBossIntro" + Aggregates.randomInt(1, 35)),
                enemy.getName(), fb, fb::dispose);
                }
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
    private static final String PLACEHOLDER_COMMANDER = "Atogatog";
    private static final String PLACEHOLDER_ATTRACTION = "Coin-Operated Pony";
    private static final String PLACEHOLDER_CONTRAPTION = "Automatic Fidget Spinner";

    private void applyAdventureDeckRules(DeckFormat format) {
        if(FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.DEV_MODE_ENABLED)
                && !FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.ENFORCE_DECK_LEGALITY))
            return;

        //Can't just keep the player from entering a battle if their deck is invalid. So instead we'll just edit their deck.
        CardPool mainSection = playerDeck.getMain(), attractions = playerDeck.get(DeckSection.Attractions), contraptions = playerDeck.get(DeckSection.Contraptions);

        if(format.hasCommander()) {
            applyAdventureCommandZoneRules(playerDeck, format);
        }

        removeExcessCopies(mainSection, format);
        removeExcessCopies(attractions, format);
        removeExcessCopies(contraptions, format);

        int mainSize = mainSection.countAll();

        int maxDeckSize = format == DeckFormat.Adventure ? Integer.MAX_VALUE : format.getMainRange().getMaximum();
        int minDeckSize = format == DeckFormat.Adventure ? Config.instance().getConfigData().minDeckSize : format.getMainRange().getMinimum();

        if(format.hasCommander() && playerDeck.has(DeckSection.Commander)) {
            //If they have a partner commander, it counts toward the 99.
            int commandExtras = Math.max(0, playerDeck.get(DeckSection.Commander).countAll() - 1);
            mainSize += commandExtras;
        }

        int excessCards = mainSize - maxDeckSize;
        if (excessCards > 0) {
            List<PaperCard> removals = Aggregates.random(mainSection.toFlatList(), excessCards);
            mainSection.removeAllFlat(removals);
        }

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
        Map<String, List<PaperCard>> removals = new HashMap<>();
        for(Map.Entry<PaperCard, Integer> e : section) {
            PaperCard card = e.getKey();
            String cardName = card.getCardName();
            if(removals.containsKey(cardName))
                continue; //Already processed.
            int amount = section.countByName(cardName);
            int limit = format.getMaxCardCopies(card);
            if(amount > limit) {
                removals.put(cardName, getItemsToRemove(section, cardName, amount - limit));
            }
        }
        for(List<PaperCard> list : removals.values())
            section.removeAllFlat(list);
    }

    private static List<PaperCard> getItemsToRemove(CardPool section, String cardName, int copies) {
        return section.toFlatList().stream()
                .filter(e -> e.getCardName().equals(cardName))
                .collect(StreamUtil.random(copies));
    }

    private static void applyAdventureCommandZoneRules(Deck playerDeck, DeckFormat format) {
        CardPool commandPool = playerDeck.getOrCreate(DeckSection.Commander);

        //1. Validate command section.
        List<PaperCard> removals = new ArrayList<>();
        List<PaperCard> commanders = playerDeck.getCommanders(); //ordered flat list
        if (commanders.size() > 2) {
            removals.addAll(commanders.subList(2, commanders.size()));
            commanders = commanders.subList(0, 2);
        }
        if (!commanders.isEmpty()) {
            PaperCard mainCommander = commanders.get(0);
            if (!format.isLegalCommander(mainCommander.getRules()))
                removals.add(mainCommander);
            if (commanders.size() > 1) {
                PaperCard partnerCommander = commanders.get(1);
                if (removals.contains(mainCommander)) {
                    if (!format.isLegalCommander(partnerCommander.getRules()))
                        removals.add(partnerCommander); //Main is invalid but partner is valid.
                } else if (!mainCommander.getRules().canBePartnerCommanders(partnerCommander.getRules()))
                    removals.add(partnerCommander); //Invalid partnership.
            }
        }
        commandPool.removeAllFlat(removals);
        CardPool mainPool = playerDeck.getMain();
        mainPool.add(removals); //Dump all the removed cards into the main pool.

        //2. If you're missing a commander, install a terrible one.
        if(commandPool.isEmpty()) {
            commandPool.add(PLACEHOLDER_COMMANDER, 1);
        }

        //3. Validate quantities across command zone and main section
        //In other words if it's your commander, make sure there isn't a copy in your main deck.
        for(Map.Entry<PaperCard, Integer> e : commandPool) {
            PaperCard card = e.getKey();
            int limit = format.getMaxCardCopies(card);
            int amountMain = mainPool.countByName(card);
            if(amountMain > 0) {
                int amountCommand = commandPool.countByName(card);
                int toRemove = Math.max(0, (amountMain + amountCommand) - limit);
                if(toRemove > 0) {
                    mainPool.removeAllFlat(getItemsToRemove(mainPool, card.getCardName(), toRemove));
                }
            }
        }

        //4. Filter for color identity.
        byte cmdCI = 0;
        int wildColors = 0; //For Prismatic Piper and friends.
        for(PaperCard commander : playerDeck.getCommanders()) {
            cmdCI |= commander.getRules().getColorIdentity().getColor();
            wildColors += commander.getRules().getAddsWildCardColor() ? 1 : 0;
        }
        for(Map.Entry<PaperCard, Integer> e : mainPool) {
            PaperCard card = e.getKey();
            ColorSet missingColors = card.getRules().getColorIdentity().getMissingColors(cmdCI);
            if (missingColors.countColors() > 0) {
                if (missingColors.countColors() <= wildColors) {
                    wildColors -= missingColors.countColors();
                    cmdCI |= missingColors.getColor();
                } else {
                    mainPool.removeAll(card);
                }
            }
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
            this.playerDeck = (Deck) eventData.registeredDeck.copyTo("EventDeckCopy");
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
