package forge.gamemodes.match;

import com.google.common.eventbus.Subscribe;
import forge.GuiDesktop;
import forge.StaticData;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.card.Card;
import forge.game.card.CardView;
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
import forge.gamemodes.match.input.InputConfirm;
import forge.gamemodes.match.input.InputConfirmMulligan;
import forge.gamemodes.match.input.InputPassPriority;
import forge.gamemodes.match.input.InputPayMana;
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
    public void recipientManaUndoPreservesAcquiredCardsAndSharedZones() throws Throwable {
        runFixture(true);
    }

    private static void runFixture(boolean exerciseManaUndo) throws Throwable {
        final IGuiBase previousGui = GuiBase.getInterface();
        final Random previousRandom = MyRandom.getRandom();
        ForgePreferences preferences = null;
        final EnumMap<FPref, String> previousPreferences = new EnumMap<>(FPref.class);
        final List<FPref> changedPreferences = List.of(
                FPref.LOAD_CARD_SCRIPTS_LAZILY,
                FPref.UI_LANGUAGE,
                FPref.ENFORCE_DECK_LEGALITY,
                FPref.UI_ENABLE_MUSIC,
                FPref.PLAYER_NAME,
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
            preferences.setPref(FPref.PLAYER_NAME, "DanDan test player");
            // This fixture asserts that one completed undo exhausts the available history.
            preferences.setPref(FPref.UI_UNDO_MODE, ForgeConstants.UNDO_MODE_SINGLE);
            preferences.setPref(FPref.YIELD_AUTO_PASS_NO_ACTIONS, false);
            preferences.setPref(FPref.YIELD_DECLINE_SCOPE_STACK_YIELD, "NEVER");
            preferences.setPref(FPref.YIELD_DECLINE_SCOPE_NO_ACTIONS, "NEVER");
            MyRandom.setRandom(new Random(8108));
            runPaidCastFixture(exerciseManaUndo);
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

    private static void runPaidCastFixture(boolean exerciseManaUndo) throws Throwable {
        final CountDownLatch acquiredInStartHook = new CountDownLatch(1);
        final CountDownLatch hostedTaskFinished = new CountDownLatch(1);
        final AtomicReference<Throwable> hostedTaskFailure = new AtomicReference<>();
        final AtomicReference<Throwable> setupFailure = new AtomicReference<>();
        final AtomicReference<Acquisition> acquisition = new AtomicReference<>();
        final AtomicBoolean awaitingManaUndoRestore = new AtomicBoolean();
        final CountDownLatch manaUndoRestored = new CountDownLatch(1);
        final BlockingQueue<Prompt> prompts = new LinkedBlockingQueue<>();
        final Endpoint sourceEndpoint = new Endpoint(0, prompts);
        final Endpoint recipientEndpoint = new Endpoint(1, prompts);
        final ProtocolGuiGame sourceGui = new ProtocolGuiGame(sourceEndpoint);
        final ProtocolGuiGame recipientGui = new ProtocolGuiGame(recipientEndpoint);
        sourceEndpoint.setGui(sourceGui);
        recipientEndpoint.setGui(recipientGui);

        final RegisteredPlayer sourceRegistered = new RegisteredPlayer(officialDanDanDeck())
                .setPlayer(new LobbyPlayerHuman("source seat"));
        final RegisteredPlayer recipientRegistered = new RegisteredPlayer(officialDanDanDeck())
                .setPlayer(new LobbyPlayerHuman("recipient seat"));
        final Map<RegisteredPlayer, IGuiGame> guis = Map.of(sourceRegistered, sourceGui, recipientRegistered, recipientGui);
        final HostedMatch hosted = new HostedMatch();
        hosted.setStartGameHook(() -> {
            try {
                final Game game = hosted.getGame();
                final Player source = game.getPlayers().get(0);
                final Player recipient = game.getPlayers().get(1);
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
                final Card acquired = findOwnedSharedCard(source, "Mental Note");
                final Card island = findOwnedSharedCard(source, "Island");
                Assert.assertNotNull(acquired, "the official shared library contains Mental Note");
                Assert.assertNotNull(island, "the official shared library contains Island");
                Assert.assertSame(acquired.getOwner(), source,
                        "the card begins in the owner of the one physical shared library");
                Assert.assertSame(island.getOwner(), source);

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
                acquisition.set(new Acquisition(source, recipient, acquiredId, islandId));
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
            int libraryBeforeCast = -1;
            int handBeforeCast = -1;
            List<Integer> topThreeBeforeCast = List.of();
            List<Integer> commonLibraryBeforeManaUndo = List.of();
            List<Integer> commonGraveyardBeforeManaUndo = List.of();
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(GAME_TIMEOUT_SECONDS);

            while (!resolved && System.nanoTime() < deadline) {
                final Prompt prompt = prompts.poll(250, TimeUnit.MILLISECONDS);
                if (prompt == null) {
                    final Throwable workerFailure = hostedTaskFailure.get();
                    if (workerFailure != null) {
                        throw new AssertionError("HostedMatch game task failed", workerFailure);
                    }
                    if (hostedTaskFinished.getCount() == 0 && !resolved) {
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
                    final Card island = game.findById(cards.islandId());
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
                        Assert.assertTrue(sourcePassedStackPriority,
                                "the source seat passed its actual priority while Mental Note was on the stack");
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
                        resolved = true;
                        continue;
                    }
                    sourcePassedStackPriority |= owner == cards.source();
                    recipientPassedStackPriority |= owner == cards.recipient();
                    selectOk(prompt);
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
                Assert.assertTrue(manaUndoComplete, "recipient undo completed before paid casting");
            }
            Assert.assertTrue(spellSelected, "the recipient selected the acquired card through a real Human Input");
            Assert.assertTrue(manaSourceSelected, "the recipient activated the Island through InputPayMana");
            Assert.assertTrue(stackObserved, "the acquired spell was observed on the stack with its actual payment");
            Assert.assertTrue(recipientPassedStackPriority,
                    "the human recipient passed its actual priority before resolution");
            Assert.assertTrue(resolved, "the spell resolved after real priority passes");
            Assert.assertTrue(recipientEndpoint.acquiredSpellAbilityChoices > 0,
                    "the normal getAbilityToPlay callback chose Mental Note's available spell ability");
            Assert.assertTrue(sourcePassedStackPriority, "the source seat passed priority before resolution");
        } catch (Throwable failure) {
            primaryFailure = failure;
            throw failure;
        } finally {
            try {
                final Game game = hosted.getGame();
                if (game != null && !game.isGameOver()) {
                    final PlayerControllerHuman controller = (PlayerControllerHuman) game.getPlayers().get(1).getController();
                    FThreads.invokeInEdtAndWait(controller::concede);
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

    private static Card findOwnedSharedCard(Player source, String name) {
        for (Card card : source.getCardsIn(ZoneType.Library)) {
            if (name.equals(card.getName()) && card.getOwner() == source) return card;
        }
        return null;
    }

    private static Deck officialDanDanDeck() {
        for (DeckProxy proxy : DeckProxy.getAllDanDanDecks()) {
            if ("DanDan_SecretLair".equals(proxy.getName())) return proxy.getDeck();
        }
        throw new AssertionError("The official DanDan_SecretLair deck was not loaded");
    }

    private record Acquisition(Player source, Player recipient, int acquiredId, int islandId) { }
    private record Prompt(int seat, PlayerView owner, Input input, PlayerControllerHuman controller) { }

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
                    prompts.offer(new Prompt(seat, owner, input, human));
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
