package forge.gamemodes.match;

import com.google.common.eventbus.Subscribe;
import forge.GuiDesktop;
import forge.StaticData;
import forge.ai.LobbyPlayerAi;
import forge.ai.PlayerControllerAi;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.game.ability.AbilityKey;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.combat.CombatUtil;
import forge.game.event.GameEventPlayerPriority;
import forge.game.event.GameEventSnapshotRestored;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.SpellAbilityView;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.input.Input;
import forge.gamemodes.match.input.InputAttack;
import forge.gamemodes.match.input.InputConfirm;
import forge.gamemodes.match.input.InputConfirmMulligan;
import forge.gamemodes.match.input.InputPassPriority;
import forge.gamemodes.match.input.InputPayMana;
import forge.gamemodes.match.input.InputSelectCardsFromList;
import forge.gamemodes.net.IRemote;
import forge.gamemodes.net.ProtocolGuiGame;
import forge.gamemodes.net.ProtocolMethod;
import forge.gamemodes.net.event.GuiGameEvent;
import forge.gamemodes.net.event.IdentifiableNetEvent;
import forge.gamemodes.net.event.NetEvent;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.interfaces.IGameController;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.LobbyPlayerHuman;
import forge.player.PlayerControllerHuman;
import forge.util.MyRandom;
import forge.util.ThreadUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Exercises actual human inputs for a paid cast from an acquired DanDan common-library card. */
public class DanDanAcquiredCardPlayTest {
    private static final long GAME_TIMEOUT_SECONDS = 90;

    @Test(timeOut = 180_000)
    public void namedRecipientPaysForAndResolvesAcquiredCommonLibraryCard() throws Throwable {
        runFixture(false);
    }

    @Test(timeOut = 180_000)
    public void recipientCanUndoOwnManaAbilityButOpponentCannot() throws Throwable {
        runFixture(true);
    }

    @Test(timeOut = 180_000)
    public void localAiTurnAllowsAcquiredDanDanToAttackThroughNormalCombatInput() throws Throwable {
        runFixture(false, true);
    }

    private static void runFixture(boolean exerciseManaUndo) throws Throwable {
        runFixture(exerciseManaUndo, false);
    }

    private static void runFixture(boolean exerciseManaUndo, boolean aiOpponent) throws Throwable {
        final IGuiBase previousGui = GuiBase.getInterface();
        final Random previousRandom = MyRandom.getRandom();
        ForgePreferences preferences = null;
        final EnumMap<FPref, String> previousPreferences = new EnumMap<>(FPref.class);
        final List<FPref> changedPreferences = List.of(
                FPref.LOAD_CARD_SCRIPTS_LAZILY,
                FPref.UI_LANGUAGE,
                FPref.ENFORCE_DECK_LEGALITY,
                FPref.UI_ENABLE_MUSIC,
                FPref.UI_UNDO_MODE,
                FPref.YIELD_AUTO_PASS_NO_ACTIONS,
                FPref.YIELD_DECLINE_SCOPE_STACK_YIELD,
                FPref.YIELD_DECLINE_SCOPE_NO_ACTIONS);
        try {
            GuiBase.setInterface(new GuiDesktop());
            preferences = FModel.getPreferences();
            for (FPref pref : changedPreferences) previousPreferences.put(pref, preferences.getPref(pref));
            if (StaticData.instance() == null) {
                FModel.initialize(null, prefs -> {
                    prefs.setPref(FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
                    prefs.setPref(FPref.UI_LANGUAGE, "en-US");
                    prefs.setPref(FPref.ENFORCE_DECK_LEGALITY, false);
                    return null;
                });
            }
            preferences.setPref(FPref.ENFORCE_DECK_LEGALITY, false);
            preferences.setPref(FPref.UI_ENABLE_MUSIC, false);
            // This fixture asserts that one completed undo exhausts the available history.
            preferences.setPref(FPref.UI_UNDO_MODE, ForgeConstants.UNDO_MODE_SINGLE);
            preferences.setPref(FPref.YIELD_AUTO_PASS_NO_ACTIONS, false);
            preferences.setPref(FPref.YIELD_DECLINE_SCOPE_STACK_YIELD, "NEVER");
            preferences.setPref(FPref.YIELD_DECLINE_SCOPE_NO_ACTIONS, "NEVER");
            MyRandom.setRandom(new Random(8108));
            runPaidCastFixture(exerciseManaUndo, aiOpponent);
        } finally {
            try {
                if (preferences != null) {
                    for (FPref pref : changedPreferences) {
                        if (previousPreferences.containsKey(pref)) preferences.setPref(pref, previousPreferences.get(pref));
                    }
                }
            } finally {
                try {
                    MyRandom.setRandom(previousRandom);
                } finally {
                    GuiBase.setInterface(previousGui);
                }
            }
        }
    }

    private static void runPaidCastFixture(boolean exerciseManaUndo, boolean aiOpponent) throws Throwable {
        final CountDownLatch acquiredInStartHook = new CountDownLatch(1);
        final CountDownLatch hostedTaskFinished = new CountDownLatch(1);
        final AtomicReference<Throwable> hostedTaskFailure = new AtomicReference<>();
        final AtomicReference<Throwable> setupFailure = new AtomicReference<>();
        final AtomicReference<Acquisition> acquisition = new AtomicReference<>();
        final AtomicBoolean awaitingManaUndoRestore = new AtomicBoolean();
        final CountDownLatch manaUndoRestored = new CountDownLatch(1);
        final AtomicBoolean aiTurnPriorityObserved = new AtomicBoolean();
        final AtomicBoolean aiReceivedCastPriority = new AtomicBoolean();
        final AtomicBoolean aiReceivedDanDanCastPriority = new AtomicBoolean();
        final BlockingQueue<Prompt> prompts = new LinkedBlockingQueue<>();
        final Endpoint sourceEndpoint = new Endpoint(aiOpponent ? 1 : 0, prompts);
        final Endpoint recipientEndpoint = new Endpoint(aiOpponent ? 0 : 1, prompts);
        final ProtocolGuiGame sourceGui = aiOpponent ? null : new ProtocolGuiGame(sourceEndpoint);
        final ProtocolGuiGame recipientGui = new ProtocolGuiGame(recipientEndpoint);
        if (sourceGui != null) sourceEndpoint.setGui(sourceGui);
        recipientEndpoint.setGui(recipientGui);

        final RegisteredPlayer sourceRegistered = new RegisteredPlayer(officialDanDanDeck())
                .setPlayer(aiOpponent ? new LobbyPlayerAi("AI seat", null) : new LobbyPlayerHuman("source seat"));
        final RegisteredPlayer recipientRegistered = new RegisteredPlayer(officialDanDanDeck())
                .setPlayer(new LobbyPlayerHuman("recipient seat"));
        final Map<RegisteredPlayer, IGuiGame> guis = aiOpponent
                ? Map.of(recipientRegistered, recipientGui)
                : Map.of(sourceRegistered, sourceGui, recipientRegistered, recipientGui);
        final HostedMatch hosted = new HostedMatch();
        hosted.setStartGameHook(() -> {
            try {
                final Game game = hosted.getGame();
                final Player source = game.getPlayers().get(aiOpponent ? 1 : 0);
                final Player recipient = game.getPlayers().get(aiOpponent ? 0 : 1);
                final Player libraryOwner = aiOpponent ? recipient : source;
                if (exerciseManaUndo) {
                    game.subscribeToEvents(new Object() {
                        @Subscribe
                        public void onSnapshotRestored(GameEventSnapshotRestored event) {
                            if (!event.start() && awaitingManaUndoRestore.compareAndSet(true, false)) {
                                manaUndoRestored.countDown();
                            }
                        }
                    });
                }
                if (aiOpponent) {
                    Assert.assertTrue(source.getController() instanceof PlayerControllerAi,
                            "the normal HostedMatch AI seat has its real AI controller");
                    Assert.assertSame(source.getZone(ZoneType.Library), recipient.getZone(ZoneType.Library),
                            "both seats use the same physical DanDan library");
                    Assert.assertSame(source.getZone(ZoneType.Graveyard), recipient.getZone(ZoneType.Graveyard),
                            "both seats use the same physical DanDan graveyard");
                    Assert.assertFalse(source.getCardsIn(ZoneType.Hand).isEmpty(),
                            "the normal AI mulligan flow leaves an actual opening hand");
                    for (Card card : source.getCardsIn(ZoneType.Hand)) {
                        Assert.assertSame(card.getOwner(), source,
                                "the AI's actual opening cards are assigned to the AI");
                    }
                    Assert.assertEquals(game.getCardsInGame().size(), 80,
                            "the local AI game retains all 80 physical official cards");
                    Assert.assertEquals(game.getCardsInGame().stream().map(Card::getId).distinct().count(), 80L,
                            "the shared-zone aliases do not duplicate any physical card");
                    final int aiId = source.getId();
                    game.subscribeToEvents(new Object() {
                        @Subscribe
                        public void onPriority(GameEventPlayerPriority event) {
                            if (event.turn().getId() == aiId && event.priority().getId() == aiId) {
                                aiTurnPriorityObserved.set(true);
                            }
                            if (event.priority().getId() != aiId || game.getStack().isEmpty()) return;
                            final SpellAbilityStackInstance top = game.getStack().peek();
                            final Acquisition cards = acquisition.get();
                            if (cards != null && top.getSpellAbility().getHostCard().getId() == cards.acquiredId()) {
                                aiReceivedCastPriority.set(true);
                            }
                            if (cards != null && cards.dandanId() >= 0
                                    && top.getSpellAbility().getHostCard().getId() == cards.dandanId()) {
                                aiReceivedDanDanCastPriority.set(true);
                            }
                        }
                    });
                }
                final Card acquired = findOwnedSharedCard(libraryOwner, "Mental Note");
                final Card island = findOwnedSharedCard(libraryOwner, "Island");
                final Card dandan = aiOpponent ? findOwnedSharedCard(libraryOwner, "Dandân") : null;
                final Card secondRecipientIsland = aiOpponent ? findOwnedSharedCard(libraryOwner, "Island", island) : null;
                final Card thirdRecipientIsland = aiOpponent
                        ? findOwnedSharedCard(libraryOwner, "Island", island, secondRecipientIsland) : null;
                final Card defenderIsland = aiOpponent
                        ? findOwnedSharedCard(libraryOwner, "Island", island, secondRecipientIsland, thirdRecipientIsland) : null;
                final Card sanctuary = aiOpponent ? findOwnedSharedCard(libraryOwner, "Mystic Sanctuary") : null;
                Assert.assertNotNull(acquired, "the official shared library contains Mental Note");
                Assert.assertNotNull(island, "the official shared library contains Island");
                if (aiOpponent) {
                    Assert.assertNotNull(dandan, "the shared library contains the actual DanDan creature");
                    Assert.assertNotNull(secondRecipientIsland, "the shared library supplies a second actual recipient Island");
                    Assert.assertNotNull(thirdRecipientIsland, "the shared library supplies a third actual recipient Island");
                    Assert.assertNotNull(defenderIsland, "the shared library supplies an actual defender Island");
                    Assert.assertNotNull(sanctuary, "the shared library supplies the actual Mystic Sanctuary");
                    game.getAction().moveToPlay(secondRecipientIsland, recipient, null, AbilityKey.newMap());
                    game.getAction().moveToPlay(thirdRecipientIsland, recipient, null, AbilityKey.newMap());
                    final Card defenderIslandForAi = game.getAction().moveTo(source.getZone(ZoneType.Hand), defenderIsland, null);
                    final Card sanctuaryForAi = game.getAction().moveTo(source.getZone(ZoneType.Hand), sanctuary, null);
                    Assert.assertSame(defenderIslandForAi.getOwner(), source,
                            "the real shared-library acquisition assigns the defender Island to the AI");
                    Assert.assertSame(sanctuaryForAi.getOwner(), source,
                            "the real shared-library acquisition assigns Mystic Sanctuary to the AI");
                    Assert.assertSame(defenderIslandForAi.getController(), source);
                    Assert.assertSame(sanctuaryForAi.getController(), source);
                    final Card defenderIslandOnBattlefield = game.getAction().moveToPlay(defenderIslandForAi, source, null, AbilityKey.newMap());
                    final Card sanctuaryOnBattlefield = game.getAction().moveToPlay(sanctuaryForAi, source, null, AbilityKey.newMap());
                    Assert.assertSame(defenderIslandOnBattlefield.getController(), source,
                            "the battlefield Island ETB belongs to the AI battlefield and controller");
                    Assert.assertSame(sanctuaryOnBattlefield.getController(), source,
                            "the Mystic Sanctuary ETB belongs to the AI battlefield and controller");
                    Assert.assertSame(defenderIslandOnBattlefield.getZone(), source.getZone(ZoneType.Battlefield));
                    Assert.assertSame(sanctuaryOnBattlefield.getZone(), source.getZone(ZoneType.Battlefield));
                    game.getAction().moveTo(recipient.getZone(ZoneType.Hand), dandan, null);
                }
                Assert.assertSame(acquired.getOwner(), libraryOwner,
                        "the card begins in the owner of the one physical shared library");
                Assert.assertSame(island.getOwner(), libraryOwner);

                if (exerciseManaUndo) {
                    final List<Card> graveyardSeeds = new ArrayList<>();
                    for (Card card : new ArrayList<>(source.getCardsIn(ZoneType.Library))) {
                        if (card != acquired && card != island && card.getOwner() == source) {
                            graveyardSeeds.add(card);
                            if (graveyardSeeds.size() == 2) break;
                        }
                    }
                    Assert.assertEquals(graveyardSeeds.size(), 2,
                            "the official common library supplies two distinct cards for a nonempty graveyard oracle");
                    for (Card card : graveyardSeeds) {
                        game.getAction().moveTo(source.getZone(ZoneType.Graveyard), card, null);
                    }
                }

                final int acquiredId = acquired.getId();
                final int islandId = island.getId();
                game.getAction().moveTo(recipient.getZone(ZoneType.Hand), acquired, null);
                game.getAction().moveTo(recipient.getZone(ZoneType.Hand), island, null);

                final Card acquiredInHand = game.findById(acquiredId);
                final Card islandInHand = game.findById(islandId);
                Assert.assertSame(acquiredInHand.getZone(), recipient.getZone(ZoneType.Hand));
                Assert.assertSame(acquiredInHand.getOwner(), recipient,
                        "the common-library-to-named-hand move assigns the actual recipient as owner");
                Assert.assertSame(acquiredInHand.getController(), recipient);
                Assert.assertSame(islandInHand.getZone(), recipient.getZone(ZoneType.Hand));
                Assert.assertSame(islandInHand.getOwner(), recipient);
                acquisition.set(new Acquisition(source, recipient, acquiredId, islandId, dandan == null ? -1 : dandan.getId()));
                if (aiOpponent) {
                    final List<Integer> physicalCardIdsBefore = game.getCardsInGame().stream()
                            .map(Card::getId).sorted().toList();
                    final List<Card> aiOpeningHand = new ArrayList<>(source.getCardsIn(ZoneType.Hand));
                    Assert.assertFalse(aiOpeningHand.isEmpty(), "the normal AI mulligan leaves an actual opening hand");
                    for (Card card : aiOpeningHand) {
                        if (!("Island".equals(card.getName()) && card.isBasicLand())) {
                            game.getAction().moveToBottomOfLibrary(card, null);
                        }
                    }
                    Assert.assertTrue(source.getCardsIn(ZoneType.Hand).stream()
                                    .allMatch(card -> "Island".equals(card.getName()) && card.isBasicLand()),
                            "the bounded normal-AI opening hand contains only actual basic Islands");

                    final List<Card> topIslands = source.getCardsIn(ZoneType.Library).stream()
                            .filter(card -> "Island".equals(card.getName()) && card.isBasicLand())
                            .limit(8).toList();
                    Assert.assertEquals(topIslands.size(), 8,
                            "the official shared library supplies the bounded normal-turn draw prefix");
                    for (int i = topIslands.size() - 1; i >= 0; i--) {
                        game.getAction().moveToLibrary(topIslands.get(i), 0, null);
                    }
                    final List<Card> actualTop = source.getCardsIn(ZoneType.Library).subList(0, topIslands.size());
                    Assert.assertEquals(actualTop.stream().map(Card::getId).toList(),
                            topIslands.stream().map(Card::getId).toList(),
                            "the bounded normal-turn draw prefix contains the selected physical Island cards");
                    Assert.assertTrue(actualTop.stream()
                                    .allMatch(card -> "Island".equals(card.getName()) && card.isBasicLand()),
                            "the bounded normal-turn draw prefix contains only actual basic Islands");
                    Assert.assertEquals(game.getCardsInGame().stream().map(Card::getId).sorted().toList(),
                            physicalCardIdsBefore,
                            "normalizing the AI scenario preserves every physical official card identity");
                    Assert.assertEquals(game.getCardsInGame().size(), 80,
                            "the bounded normal-AI scenario retains the complete official card pool");
                    Assert.assertEquals(game.getCardsInGame().stream().map(Card::getId).distinct().count(), 80L,
                            "the shared-zone aliases do not duplicate any physical card");
                }
            } catch (Throwable failure) {
                setupFailure.set(failure);
            } finally {
                acquiredInStartHook.countDown();
            }
        });

        Throwable primaryFailure = null;
        boolean startScheduled = false;
        try {
            final GameRules rules = new GameRules(GameType.Constructed);
            rules.setAppliedVariants(EnumSet.of(GameType.DanDan));
            ThreadUtil.invokeInGameThread(() -> {
                try {
                    hosted.startMatch(rules, null, List.of(sourceRegistered, recipientRegistered), guis, null);
                } catch (Throwable failure) {
                    hostedTaskFailure.set(failure);
                } finally {
                    hostedTaskFinished.countDown();
                }
            });
            startScheduled = true;

            boolean acquired = false;
            boolean islandPlayed = false;
            boolean undoManaAbilitySelected = false;
            boolean manaUndoComplete = !exerciseManaUndo;
            boolean spellSelected = false;
            boolean manaSourceSelected = false;
            Input payingInput = null;
            int mentalNoteManaSourceId = -1;
            boolean stackObserved = false;
            boolean sourcePassedStackPriority = false;
            boolean recipientPassedStackPriority = false;
            boolean resolved = false;
            boolean dandanSelected = false;
            boolean dandanAutoPayRequested = false;
            boolean dandanStackObserved = false;
            boolean dandanResolved = false;
            boolean attackPromptObserved = false;
            int libraryBeforeCast = -1;
            int handBeforeCast = -1;
            List<Integer> topThreeBeforeCast = List.of();
            List<Integer> commonLibraryBeforeManaUndo = List.of();
            List<Integer> commonGraveyardBeforeManaUndo = List.of();
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(GAME_TIMEOUT_SECONDS);

            while ((!resolved || (aiOpponent && !attackPromptObserved)) && System.nanoTime() < deadline) {
                final Prompt prompt = prompts.poll(250, TimeUnit.MILLISECONDS);
                if (prompt == null) {
                    final Throwable workerFailure = hostedTaskFailure.get();
                    if (workerFailure != null) {
                        throw new AssertionError("HostedMatch game task failed", workerFailure);
                    }
                    if (hostedTaskFinished.getCount() == 0 && (!resolved || (aiOpponent && !attackPromptObserved))) {
                        throw new AssertionError("HostedMatch returned before the paid-cast scenario resolved");
                    }
                    continue;
                }
                if (!isCurrentPrompt(prompt)) continue;

                if (!acquired && (prompt.input() instanceof InputConfirm
                        || prompt.input() instanceof InputConfirmMulligan)) {
                    selectOk(prompt);
                    continue;
                }
                if (!acquired && acquiredInStartHook.getCount() == 0) {
                    if (setupFailure.get() != null) {
                        throw new AssertionError("DanDan common-library acquisition fixture failed", setupFailure.get());
                    }
                    Assert.assertNotNull(acquisition.get(), "start hook retained the exact acquired card IDs");
                    acquired = true;
                } else if (!acquired) {
                    continue;
                }

                final Acquisition cards = acquisition.get();
                final Game game = hosted.getGame();
                final Player owner = prompt.owner().getId() == cards.source().getId()
                        ? cards.source() : cards.recipient();

                if (aiOpponent && prompt.input() instanceof InputAttack) {
                    final Card dandan = game.findById(cards.dandanId());
                    Assert.assertTrue(resolved, "the shared-library Mental Note resolved before the attack step");
                    Assert.assertEquals(game.getPhaseHandler().getPhase(), PhaseType.COMBAT_DECLARE_ATTACKERS);
                    Assert.assertSame(game.getPhaseHandler().getPlayerTurn(), cards.recipient());
                    Assert.assertSame(owner, cards.recipient());
                    Assert.assertSame(prompt.controller(), cards.recipient().getController());
                    Assert.assertSame(dandan.getZone(), cards.recipient().getZone(ZoneType.Battlefield));
                    Assert.assertSame(dandan.getController(), cards.recipient());
                    Assert.assertFalse(dandan.isSick(), "normal turn progression matured the acquired creature");
                    Assert.assertTrue(CombatUtil.canAttack(dandan, cards.source()),
                            "the AI controls an actual basic Island, so Dandân has a legal defender");
                    selectCard(prompt, dandan.getView());
                    Assert.assertTrue(game.getPhaseHandler().getCombat().isAttacking(dandan),
                            "the real InputAttack accepted the human's Dandân selection");
                    selectOk(prompt);
                    attackPromptObserved = true;
                    continue;
                }

                if (aiOpponent && owner == cards.recipient() && islandPlayed && !dandanSelected
                        && prompt.input() instanceof InputPassPriority
                        && game.getPhaseHandler().getPlayerTurn() == cards.recipient()
                        && game.getPhaseHandler().getPhase() == PhaseType.MAIN1) {
                    final PlayerView recipientView = cards.recipient().getView();
                    FThreads.invokeInEdtAndWait(() -> {
                        Assert.assertFalse(prompt.controller().isUiSetToSkipPhase(recipientView, PhaseType.MAIN1),
                                "the actual recipient MAIN1 remains a stop");
                        for (PhaseType phase : List.of(PhaseType.COMBAT_BEGIN,
                                PhaseType.COMBAT_DECLARE_ATTACKERS, PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                            prompt.controller().applyYieldUpdate(new YieldUpdate.SkipPhase(recipientView, phase, true));
                            Assert.assertTrue(prompt.controller().isUiSetToSkipPhase(recipientView, phase),
                                    "the recipient's public phase UI action marks " + phase + " to skip");
                        }
                    });
                    final Card dandan = game.findById(cards.dandanId());
                    Assert.assertSame(dandan.getZone(), cards.recipient().getZone(ZoneType.Hand));
                    selectCard(prompt, dandan.getView());
                    dandanSelected = true;
                    continue;
                }

                if (aiOpponent && owner == cards.recipient() && dandanSelected && !dandanResolved
                        && prompt.input() instanceof InputPayMana) {
                    if (!dandanAutoPayRequested && prompt.autoPayEnabled()) {
                        Assert.assertSame(prompt.controller(), cards.recipient().getController(),
                                "the recipient human controller owns the live Dandân mana payment");
                        Assert.assertTrue(isCurrentPrompt(prompt), "the human controller owns the live mana payment input");
                        selectOk(prompt);
                        dandanAutoPayRequested = true;
                    }
                    continue;
                }

                if (aiOpponent && dandanSelected && prompt.input() instanceof InputPassPriority
                        && !game.getStack().isEmpty()
                        && game.getStack().peek().getSpellAbility().getHostCard().getId() == cards.dandanId()) {
                    final SpellAbility spell = game.getStack().peek().getSpellAbility();
                    Assert.assertSame(spell.getActivatingPlayer(), cards.recipient());
                    Assert.assertEquals(spell.getPayingMana().size(), 2, "the real DanDan spell paid its two blue mana");
                    Assert.assertTrue(spell.getPayingMana().stream().allMatch(mana -> mana.getColor() == ManaAtom.BLUE));
                    dandanStackObserved = true;
                    selectOk(prompt);
                    continue;
                }

                if (aiOpponent && dandanStackObserved && !dandanResolved && prompt.input() instanceof InputPassPriority
                        && game.getStack().isEmpty() && owner == cards.recipient()) {
                    final Card dandan = game.findById(cards.dandanId());
                    Assert.assertSame(dandan.getZone(), cards.recipient().getZone(ZoneType.Battlefield));
                    Assert.assertSame(dandan.getController(), cards.recipient());
                    dandanResolved = true;
                }

                if (aiOpponent && owner == cards.recipient() && !aiTurnPriorityObserved.get()
                        && prompt.input() instanceof InputPassPriority) {
                    selectOk(prompt);
                    continue;
                }

                if (prompt.input() instanceof InputPassPriority
                        && owner == cards.recipient()
                        && !islandPlayed
                        && game.getPhaseHandler().getPlayerTurn() == cards.recipient()
                        && game.getPhaseHandler().getPhase() == PhaseType.MAIN1) {
                    final Card island = game.findById(cards.islandId());
                    Assert.assertSame(island.getZone(), cards.recipient().getZone(ZoneType.Hand));
                    selectCard(prompt, island.getView());
                    islandPlayed = true;
                    continue;
                }

                if (prompt.input() instanceof InputPassPriority
                        && owner == cards.recipient()
                        && islandPlayed
                        && exerciseManaUndo
                        && !undoManaAbilitySelected
                        && game.getPhaseHandler().getPlayerTurn() == cards.recipient()
                        && game.getPhaseHandler().getPriorityPlayer() == cards.recipient()
                        && game.getPhaseHandler().getPhase() == PhaseType.MAIN1) {
                    final Card island = game.findById(cards.islandId());
                    Assert.assertFalse(island.isTapped(), "recipient's Island is untapped before the separate mana activation");
                    Assert.assertEquals(cards.recipient().getManaPool().totalMana(), 0,
                            "recipient begins the mana-undo witness with an empty pool");
                    Assert.assertTrue(game.getStack().isEmpty(), "the common library and graveyard snapshot precedes casting");
                    commonLibraryBeforeManaUndo = cards.recipient().getCardsIn(ZoneType.Library).stream()
                            .map(Card::getId).toList();
                    commonGraveyardBeforeManaUndo = cards.recipient().getCardsIn(ZoneType.Graveyard).stream()
                            .map(Card::getId).toList();
                    Assert.assertEquals(commonGraveyardBeforeManaUndo.size(), 2,
                            "the undo witness begins with two actual cards in the shared graveyard");
                    Assert.assertEquals(commonGraveyardBeforeManaUndo.stream().distinct().count(), 2L,
                            "the shared graveyard snapshot contains two distinct card IDs");
                    Assert.assertEquals(cards.source().getCardsIn(ZoneType.Library).stream().map(Card::getId).toList(),
                            commonLibraryBeforeManaUndo, "both seats reference the same ordered common library");
                    Assert.assertEquals(cards.source().getCardsIn(ZoneType.Graveyard).stream().map(Card::getId).toList(),
                            commonGraveyardBeforeManaUndo, "both seats reference the same ordered common graveyard");
                    selectCard(prompt, island.getView());
                    undoManaAbilitySelected = true;
                    continue;
                }

                if (prompt.input() instanceof InputPassPriority
                        && owner == cards.recipient()
                        && exerciseManaUndo
                        && undoManaAbilitySelected
                        && !manaUndoComplete
                        && game.getPhaseHandler().getPlayerTurn() == cards.recipient()
                        && game.getPhaseHandler().getPriorityPlayer() == cards.recipient()
                        && game.getPhaseHandler().getPhase() == PhaseType.MAIN1) {
                    final Card island = game.findById(cards.islandId());
                    final PlayerControllerHuman recipientController = (PlayerControllerHuman) cards.recipient().getController();
                    final PlayerControllerHuman sourceController = (PlayerControllerHuman) cards.source().getController();
                    Assert.assertEquals(commonLibraryBeforeManaUndo, cards.source().getCardsIn(ZoneType.Library).stream()
                            .map(Card::getId).toList(), "common library remains mirrored after Island activation");
                    Assert.assertEquals(commonGraveyardBeforeManaUndo, cards.source().getCardsIn(ZoneType.Graveyard).stream()
                            .map(Card::getId).toList(), "common graveyard remains mirrored after Island activation");
                    Assert.assertEquals(commonLibraryBeforeManaUndo, cards.recipient().getCardsIn(ZoneType.Library).stream()
                            .map(Card::getId).toList(), "Island activation leaves exact common-library order and IDs");
                    Assert.assertEquals(commonGraveyardBeforeManaUndo, cards.recipient().getCardsIn(ZoneType.Graveyard).stream()
                            .map(Card::getId).toList(), "Island activation leaves exact common-graveyard order and IDs");
                    Assert.assertSame(prompt.controller(), recipientController,
                            "the live priority input belongs to the recipient Human controller");
                    Assert.assertEquals(cards.recipient().getManaPool().getAmountOfColor(MagicColor.BLUE), 1,
                            "the real Island activation added one blue mana");
                    Assert.assertEquals(cards.recipient().getManaPool().totalMana(), 1,
                            "the real Island activation added exactly one mana");
                    Assert.assertTrue(island.isTapped(), "the real Island mana ability tapped its source");
                    Assert.assertTrue(recipientController.canUndoLastAction(),
                            "the recipient owns the undoable mana ability and currently has priority");
                    Assert.assertFalse(sourceController.canUndoLastAction(),
                            "the other Human controller cannot undo the recipient's action");

                    FThreads.invokeInEdtAndWait(() -> {
                        Assert.assertTrue(isCurrentPrompt(prompt), "the opposing undo attempt uses the live recipient priority input");
                        sourceController.undoLastAction();
                    });
                    Assert.assertEquals(cards.recipient().getManaPool().getAmountOfColor(MagicColor.BLUE), 1,
                            "opponent undo leaves the blue mana unchanged");
                    Assert.assertTrue(island.isTapped(), "opponent undo leaves the recipient's Island tapped");
                    Assert.assertEquals(cards.recipient().getCardsIn(ZoneType.Library).stream().map(Card::getId).toList(),
                            commonLibraryBeforeManaUndo, "opponent undo leaves exact common-library order and IDs");
                    Assert.assertEquals(cards.recipient().getCardsIn(ZoneType.Graveyard).stream().map(Card::getId).toList(),
                            commonGraveyardBeforeManaUndo, "opponent undo leaves exact common-graveyard order and IDs");

                    Assert.assertTrue(recipientController.canUndoLastAction(),
                            "the recipient still has its own undoable action after the opposing attempt");
                    final Input previousPromptInput = prompt.input();
                    awaitingManaUndoRestore.set(true);
                    FThreads.invokeInEdtAndWait(() -> {
                        Assert.assertTrue(isCurrentPrompt(prompt), "the recipient undo uses the same live priority input");
                        recipientController.undoLastAction();
                    });
                    final Prompt restoredPrompt = recipientEndpoint.awaitPromptAfter(previousPromptInput, manaUndoRestored);
                    Assert.assertTrue(isCurrentPrompt(restoredPrompt), "the restored game has a fresh current priority input");
                    Assert.assertNotSame(restoredPrompt.input(), previousPromptInput,
                            "the game rebuilt the Human priority input after undo");
                    Assert.assertSame(restoredPrompt.controller(), recipientController,
                            "the restored priority remains with the recipient Human controller");
                    Assert.assertEquals(cards.recipient().getManaPool().totalMana(), 0,
                            "the recipient's real undo refunded the mana ability");
                    Assert.assertFalse(island.isTapped(), "the recipient's real undo untapped its Island");
                    Assert.assertFalse(recipientController.canUndoLastAction(), "the undoable action was removed");
                    manaUndoComplete = true;
                    Assert.assertSame(game.findById(cards.acquiredId()).getZone(),
                            cards.recipient().getZone(ZoneType.Hand), "the acquired spell remains in hand before its paid cast");
                    Assert.assertEquals(cards.recipient().getCardsIn(ZoneType.Library).stream().map(Card::getId).toList(),
                            commonLibraryBeforeManaUndo, "recipient undo preserves exact common-library order and IDs");
                    Assert.assertEquals(cards.recipient().getCardsIn(ZoneType.Graveyard).stream().map(Card::getId).toList(),
                            commonGraveyardBeforeManaUndo, "recipient undo preserves exact common-graveyard order and IDs");
                    continue;
                }

                if (prompt.input() instanceof InputPassPriority
                        && owner == cards.recipient()
                        && islandPlayed
                        && manaUndoComplete
                        && !spellSelected
                        && game.getPhaseHandler().getPlayerTurn() == cards.recipient()
                        && game.getPhaseHandler().getPhase() == PhaseType.MAIN1) {
                    final Card spell = game.findById(cards.acquiredId());
                    Assert.assertSame(spell.getZone(), cards.recipient().getZone(ZoneType.Hand));
                    Assert.assertSame(spell.getOwner(), cards.recipient());
                    Assert.assertFalse(cards.source().getCardsIn(ZoneType.Hand).contains(spell),
                            "the other seat's actual hand does not contain the acquired card");
                    Assert.assertFalse(spell.getView().canBeShownTo(cards.source().getView()),
                            "the opponent's player view cannot see the acquired card face in the recipient's hand");
                    for (CardView sourceHandCard : cards.source().getView().getHand()) {
                        Assert.assertNotEquals(sourceHandCard.getId(), cards.acquiredId(),
                                "the source seat's authorized hand view has no acquired-card entry");
                    }
                    libraryBeforeCast = cards.recipient().getCardsIn(ZoneType.Library).size();
                    handBeforeCast = cards.recipient().getCardsIn(ZoneType.Hand).size();
                    final List<Integer> topThree = new ArrayList<>();
                    for (Card card : cards.recipient().getCardsIn(ZoneType.Library)) {
                        topThree.add(card.getId());
                        if (topThree.size() == 3) break;
                    }
                    Assert.assertEquals(topThree.size(), 3, "Mental Note has two cards to mill and one to draw");
                    topThreeBeforeCast = List.copyOf(topThree);
                    selectCard(prompt, spell.getView());
                    spellSelected = true;
                    continue;
                }

                if (prompt.input() instanceof InputPayMana && !manaSourceSelected) {
                    Assert.assertTrue(islandPlayed, "the card was played as a land before the spell was cast");
                    final Card island;
                    if (aiOpponent) {
                        island = cards.recipient().getCardsIn(ZoneType.Battlefield).stream()
                                .filter(card -> "Island".equals(card.getName()) && !card.isTapped())
                                .findFirst().orElseThrow(() -> new AssertionError(
                                        "the recipient has an untapped Island remaining after Dandân's Auto payment"));
                    } else {
                        island = game.findById(cards.islandId());
                    }
                    Assert.assertFalse(island.isTapped(), "Island is untapped before its real mana ability is activated");
                    mentalNoteManaSourceId = island.getId();
                    selectCard(prompt, island.getView());
                    manaSourceSelected = true;
                    payingInput = prompt.input();
                    continue;
                }

                if (prompt.input() instanceof InputPassPriority && spellSelected && !stackObserved
                        && !game.getStack().isEmpty()) {
                    final SpellAbilityStackInstance top = game.getStack().peek();
                    final SpellAbility spell = top.getSpellAbility();
                    Assert.assertEquals(spell.getHostCard().getId(), cards.acquiredId(),
                            "the acquired Mental Note itself is on the real game stack");
                    Assert.assertSame(spell.getHostCard().getOwner(), cards.recipient());
                    Assert.assertSame(spell.getActivatingPlayer(), cards.recipient());
                    Assert.assertEquals(spell.getPayingMana().size(), 1,
                            "exactly one mana was paid for the official one-blue spell");
                    Assert.assertEquals(spell.getPayingMana().get(0).getColor(), ManaAtom.BLUE);
                    Assert.assertTrue(mentalNoteManaSourceId >= 0,
                            "the actual Island source was recorded before its ability was activated");
                    final int paidManaSourceId = mentalNoteManaSourceId;
                    Assert.assertEquals(spell.getPayingMana().get(0).getSourceCard().getId(), paidManaSourceId);
                    Assert.assertTrue(spell.getPayingManaAbilities().stream()
                                    .anyMatch(ability -> ability.getHostCard().getId() == paidManaSourceId),
                            "the actual Island mana ability supplied the spell's blue mana");
                    Assert.assertTrue(game.findById(paidManaSourceId).isTapped(),
                            "the recipient's Island is tapped after paying the spell cost");
                    stackObserved = true;
                    sourcePassedStackPriority = owner == cards.source();
                    recipientPassedStackPriority = owner == cards.recipient();
                    selectOk(prompt);
                    continue;
                }

                if (prompt.input() instanceof InputPassPriority && stackObserved && !resolved) {
                    if (game.getStack().isEmpty()) {
                        if (!aiOpponent) {
                            Assert.assertTrue(sourcePassedStackPriority,
                                    "the source seat passed its actual priority while Mental Note was on the stack");
                        }
                        Assert.assertTrue(recipientPassedStackPriority,
                                "the recipient passed its actual priority while Mental Note was on the stack");
                        final Card resolvedSpell = game.findById(cards.acquiredId());
                        Assert.assertSame(resolvedSpell.getZone(), cards.recipient().getZone(ZoneType.Graveyard),
                                "the normally paid Mental Note resolved into the common graveyard");
                        Assert.assertSame(resolvedSpell.getOwner(), cards.recipient());
                        Assert.assertSame(resolvedSpell.getController(), cards.recipient());
                        Assert.assertEquals(cards.recipient().getCardsIn(ZoneType.Library).size(), libraryBeforeCast - 3,
                                "the actual Mental Note script milled two cards and drew one");
                        Assert.assertEquals(cards.recipient().getCardsIn(ZoneType.Hand).size(), handBeforeCast,
                                "casting one card and drawing one leaves the recipient's hand count unchanged");
                        Assert.assertSame(game.findById(topThreeBeforeCast.get(0)).getZone(),
                                cards.recipient().getZone(ZoneType.Graveyard), "Mental Note milled the first actual top card");
                        Assert.assertSame(game.findById(topThreeBeforeCast.get(1)).getZone(),
                                cards.recipient().getZone(ZoneType.Graveyard), "Mental Note milled the second actual top card");
                        Assert.assertSame(game.findById(topThreeBeforeCast.get(2)).getZone(),
                                cards.recipient().getZone(ZoneType.Hand), "Mental Note drew the next actual top card");
                        if (aiOpponent) {
                            Assert.assertEquals(game.getCardsInGame().size(), 80,
                                    "the real AI/human spell resolution preserves all physical cards");
                            Assert.assertEquals(game.getCardsInGame().stream().map(Card::getId).distinct().count(), 80L,
                                    "the resolution preserves one identity for each physical card");
                        }
                        resolved = true;
                        if (aiOpponent) selectOk(prompt);
                        continue;
                    }
                    sourcePassedStackPriority |= owner == cards.source();
                    recipientPassedStackPriority |= owner == cards.recipient();
                    selectOk(prompt);
                    continue;
                }

                if (aiOpponent && owner == cards.recipient()
                        && prompt.input() instanceof InputSelectCardsFromList discard
                        && game.getPhaseHandler().getPhase() == PhaseType.CLEANUP
                        && game.getStack().isEmpty()) {
                    final List<Card> hand = new ArrayList<>(cards.recipient().getCardsIn(ZoneType.Hand));
                    final int discardCount = hand.size() - cards.recipient().getMaxHandSize();
                    Assert.assertTrue(discardCount > 0, "the live cleanup input is for the actual oversized hand");
                    Assert.assertEquals(discard.getValidChoices().size(), hand.size(),
                            "the cleanup input offers the actual recipient hand");
                    final List<Card> chosen = hand.stream()
                            .filter(card -> card.getId() != cards.acquiredId() && card.getId() != cards.islandId()
                                    && card.getId() != cards.dandanId())
                            .limit(discardCount).toList();
                    Assert.assertEquals(chosen.size(), discardCount,
                            "the real cleanup discard can preserve the human's acquired spell and Island");
                    final List<Integer> chosenCardIds = chosen.stream().map(Card::getId).toList();
                    Assert.assertEquals(chosenCardIds.stream().distinct().count(), (long) discardCount,
                            "the cleanup selection contains exactly the required distinct card IDs");
                    for (Card card : chosen) selectCard(prompt, card.getView());
                    final List<Integer> selectedCardIds = discard.getSelected().stream().map(Card::getId).toList();
                    Assert.assertEquals(selectedCardIds.size(), discardCount,
                            "the live cleanup input accepted exactly the required number of cards");
                    Assert.assertEquals(selectedCardIds, chosenCardIds,
                            "the live cleanup input accepted the exact recorded card IDs");
                    Assert.assertSame(game.findById(cards.acquiredId()).getZone(),
                            cards.recipient().getZone(ZoneType.Graveyard),
                            "the resolved Mental Note remains in the recipient's graveyard through cleanup");
                    Assert.assertSame(game.findById(cards.islandId()).getZone(),
                            cards.recipient().getZone(ZoneType.Battlefield),
                            "the played fixture Island remains on the recipient's battlefield through cleanup");
                    Assert.assertSame(game.findById(cards.dandanId()).getZone(),
                            cards.recipient().getZone(ZoneType.Battlefield),
                            "the resolved Dandân remains on the recipient's battlefield through cleanup");
                    continue;
                }

                if (prompt.input() instanceof InputConfirm || prompt.input() instanceof InputConfirmMulligan) {
                    selectOk(prompt);
                } else if (prompt.input() instanceof InputPassPriority) {
                    selectOk(prompt);
                } else if (prompt.input() instanceof InputPayMana && manaSourceSelected
                        && prompt.input() != payingInput) {
                    Assert.fail("a new mana input appeared before the actual Island payment completed");
                }
            }

            Assert.assertTrue(acquired, "the actual game reached the DanDan acquisition setup hook");
            Assert.assertTrue(islandPlayed, "the recipient played the fixture Island through a real Human Input");
            if (exerciseManaUndo) {
                Assert.assertTrue(undoManaAbilitySelected, "the recipient activated the actual Island mana ability before casting");
                Assert.assertTrue(manaUndoComplete, "opponent denial and recipient undo completed before paid casting");
            }
            Assert.assertTrue(spellSelected, "the recipient selected the acquired card through a real Human Input");
            Assert.assertTrue(manaSourceSelected, "the recipient activated the Island through InputPayMana");
            Assert.assertTrue(stackObserved, "the acquired spell was observed on the stack with its actual payment");
            Assert.assertTrue(recipientPassedStackPriority,
                    "the human recipient passed its actual priority before resolution");
            Assert.assertTrue(resolved, "the spell resolved after real priority passes");
            Assert.assertTrue(recipientEndpoint.acquiredSpellAbilityChoices > 0,
                    "the normal getAbilityToPlay callback chose Mental Note's available spell ability");
            if (aiOpponent) {
                Assert.assertTrue(aiTurnPriorityObserved.get(),
                        "the actual AI turn priority was observed before the later human MAIN1 cast");
                Assert.assertTrue(aiReceivedCastPriority.get(),
                        "the actual AI controller received priority on the human's paid Mental Note stack");
                Assert.assertTrue(dandanSelected, "the human selected the actual acquired DanDan from hand");
                Assert.assertTrue(dandanStackObserved, "the actual DanDan was cast and paid for on the normal stack");
                Assert.assertTrue(dandanResolved, "the actual DanDan resolved before the next human combat");
                Assert.assertTrue(attackPromptObserved, "normal phase progression reached the actual attacker input");
                Assert.assertTrue(aiReceivedDanDanCastPriority.get(), "the actual AI received priority on the DanDan spell");
            } else {
                Assert.assertTrue(sourcePassedStackPriority, "the source seat passed priority before resolution");
            }
        } catch (Throwable failure) {
            primaryFailure = failure;
            throw failure;
        } finally {
            try {
                final Game game = hosted.getGame();
                if (game != null && !game.isGameOver()) {
                    final PlayerControllerHuman controller = game.getPlayers().get(1).getController() instanceof PlayerControllerHuman human
                            ? human : game.getPlayers().get(0).getController() instanceof PlayerControllerHuman human
                            ? human : null;
                    if (controller != null) FThreads.invokeInEdtAndWait(controller::concede);
                }
                if (startScheduled && !hostedTaskFinished.await(30, TimeUnit.SECONDS)) {
                    throw new AssertionError("the complete HostedMatch game task did not return after the real Human concede");
                }
                final Throwable workerFailure = hostedTaskFailure.get();
                final Game completedGame = hosted.getGame();
                final AssertionError workerError = workerFailure == null ? null
                        : new AssertionError("HostedMatch game task failed", workerFailure);
                if (completedGame != null) {
                    try {
                        hosted.endCurrentGame();
                    } catch (Throwable endFailure) {
                        if (workerError != null) workerError.addSuppressed(endFailure);
                        else throw endFailure;
                    }
                }
                if (workerError != null) {
                    if (primaryFailure != null && primaryFailure != workerError) primaryFailure.addSuppressed(workerError);
                    else throw workerError;
                }
            } catch (Throwable cleanupFailure) {
                if (primaryFailure != null && primaryFailure != cleanupFailure) primaryFailure.addSuppressed(cleanupFailure);
                else throw cleanupFailure;
            }
        }
    }

    private static boolean isCurrentPrompt(Prompt prompt) {
        return prompt.controller().getInputProxy().getInput() == prompt.input()
                && prompt.controller().getInputQueue().getInput() == prompt.input()
                && prompt.input().getOwner() != null
                && prompt.input().getOwner().getId() == prompt.owner().getId();
    }

    private static void selectCard(Prompt prompt, CardView card) {
        FThreads.invokeInEdtAndWait(() -> {
            Assert.assertTrue(isCurrentPrompt(prompt), "a stale event cannot answer a newer Human Input");
            Assert.assertTrue(prompt.controller().selectCard(card, null, null),
                    "the actual IGameController accepts the card for its current Human Input");
        });
    }

    private static void selectOk(Prompt prompt) {
        FThreads.invokeInEdtAndWait(() -> {
            Assert.assertTrue(isCurrentPrompt(prompt), "a stale event cannot answer a newer Human Input");
            prompt.controller().selectButtonOk();
        });
    }

    private static Card findOwnedSharedCard(Player source, String name, Card... excluded) {
        for (Card card : source.getCardsIn(ZoneType.Library)) {
            if (name.equals(card.getName()) && card.getOwner() == source
                    && java.util.Arrays.stream(excluded).noneMatch(other -> other == card)) return card;
        }
        return null;
    }

    private static Deck officialDanDanDeck() {
        for (DeckProxy proxy : DeckProxy.getAllDanDanDecks()) {
            if ("DanDan_SecretLair".equals(proxy.getName())) return proxy.getDeck();
        }
        throw new AssertionError("The official DanDan_SecretLair deck was not loaded");
    }

    private record Acquisition(Player source, Player recipient, int acquiredId, int islandId, int dandanId) { }
    private record Prompt(int seat, PlayerView owner, Input input, PlayerControllerHuman controller,
                          boolean autoPayEnabled) { }

    private static final class Endpoint implements IRemote {
        private final int seat;
        private final BlockingQueue<Prompt> prompts;
        private volatile ProtocolGuiGame gui;
        private volatile int acquiredSpellAbilityChoices;

        private Endpoint(int seat, BlockingQueue<Prompt> prompts) {
            this.seat = seat;
            this.prompts = prompts;
        }

        private void setGui(ProtocolGuiGame gui) {
            this.gui = gui;
        }

        private Prompt awaitPromptAfter(Input previousInput, CountDownLatch restoreCompleted) throws InterruptedException {
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(GAME_TIMEOUT_SECONDS);
            final long restoreWait = deadline - System.nanoTime();
            if (restoreWait <= 0 || !restoreCompleted.await(restoreWait, TimeUnit.NANOSECONDS)) {
                throw new AssertionError("The game thread did not complete the typed undo snapshot restore");
            }
            while (System.nanoTime() < deadline) {
                final long remaining = deadline - System.nanoTime();
                final Prompt prompt = prompts.poll(Math.min(remaining, TimeUnit.MILLISECONDS.toNanos(250)),
                        TimeUnit.NANOSECONDS);
                if (prompt == null) continue;
                if (prompt.seat() == seat && prompt.input() != previousInput
                        && prompt.input() instanceof InputPassPriority && isCurrentPrompt(prompt)) {
                    prompts.offer(prompt);
                    return prompt;
                }
            }
            throw new AssertionError("The undo did not produce a fresh current priority input for seat " + seat);
        }

        @Override
        public void send(NetEvent event) {
            final GuiGameEvent guiEvent = (GuiGameEvent) event;
            if (guiEvent.getMethod() != ProtocolMethod.updateButtons) return;
            final Object[] args = guiEvent.getObjects();
            final PlayerView owner = (PlayerView) args[0];
            if (owner == null) return;
            final IGameController controller = gui.getGameController(owner);
            if (controller instanceof PlayerControllerHuman human) {
                final Input input = human.getInputProxy().getInput();
                if (input != null && input.getOwner() != null && input.getOwner().getId() == owner.getId()) {
                    prompts.offer(new Prompt(seat, owner, input, human, Boolean.TRUE.equals(args[3])));
                }
            }
        }

        @Override
        public Object sendAndWait(IdentifiableNetEvent event) {
            final GuiGameEvent guiEvent = (GuiGameEvent) event;
            final Object[] args = guiEvent.getObjects();
            return switch (guiEvent.getMethod()) {
                case getAbilityToPlay -> {
                    final CardView card = (CardView) args[0];
                    @SuppressWarnings("unchecked") final List<SpellAbilityView> abilities = (List<SpellAbilityView>) args[1];
                    if (abilities == null || abilities.isEmpty()) {
                        throw new AssertionError("Human ability prompt has no choices for " + card.getName());
                    }
                    final SpellAbilityView chosen = abilities.stream().filter(SpellAbilityView::canPlay).findFirst()
                            .orElseThrow(() -> new AssertionError("No legal ability for " + card.getName()));
                    if ("Mental Note".equals(card.getName())) acquiredSpellAbilityChoices++;
                    yield chosen;
                }
                case showOptionDialog -> {
                    final int defaultOption = (Integer) args[4];
                    yield defaultOption >= 0 ? defaultOption : 0;
                }
                case showConfirmDialog -> args[4];
                case confirm -> args[2];
                case showInputDialog -> args[3];
                case getChoices -> {
                    @SuppressWarnings("unchecked") final List<Object> choices = (List<Object>) args[3];
                    final int minimum = (Integer) args[1];
                    final int maximum = (Integer) args[2];
                    final int count = Math.max(0, Math.min(choices.size(), maximum < 0 ? choices.size()
                            : Math.max(minimum, Math.min(maximum, 1))));
                    yield new ArrayList<>(choices.subList(0, count));
                }
                default -> throw new AssertionError("Unexpected blocking Human prompt for seat " + seat + ": "
                        + guiEvent.getMethod());
            };
        }
    }
}
