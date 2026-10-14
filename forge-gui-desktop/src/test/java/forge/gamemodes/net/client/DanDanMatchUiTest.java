package forge.gamemodes.net.client;

import com.google.common.eventbus.Subscribe;
import forge.GuiDesktop;
import forge.Singletons;
import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.ai.PlayerControllerAi;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.game.ability.AbilityKey;
import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.GameView;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.combat.CombatUtil;
import forge.game.event.GameEventSpellAbilityCast;
import forge.game.mana.Mana;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.player.RegisteredPlayer;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.gamemodes.net.ProtocolMethod;
import forge.gamemodes.net.event.GuiGameEvent;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.SOverlayUtils;
import forge.gui.control.FControlGameEventHandler;
import forge.gui.download.CdnUuidCache;
import forge.gui.framework.EDocID;
import forge.gui.framework.FScreen;
import forge.gui.framework.IVDoc;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.match.input.Input;
import forge.gamemodes.match.input.InputAttack;
import forge.gamemodes.match.input.InputConfirm;
import forge.gamemodes.match.input.InputConfirmMulligan;
import forge.gamemodes.match.input.InputPassPriority;
import forge.gamemodes.match.input.InputPayMana;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.LobbyPlayerHuman;
import forge.player.PlayerControllerHuman;
import forge.screens.match.CMatchUI;
import forge.screens.match.views.VDetail;
import forge.screens.match.views.VHand;
import forge.screens.match.views.VLog;
import forge.screens.match.views.VPrompt;
import forge.screens.match.views.VZone;
import forge.trackable.TrackableCollection;
import forge.view.arcane.CardArea;
import forge.view.arcane.CardPanel;
import forge.view.arcane.FloatingZone;
import forge.view.arcane.PlayArea;
import forge.toolbox.special.PhaseLabel;
import forge.util.CardTranslation;
import forge.util.Localizer;
import io.netty.channel.embedded.EmbeddedChannel;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import javax.swing.JDialog;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/** Exercises the real desktop match consumers with a serialized, remote-style GameView. */
public class DanDanMatchUiTest extends AITest {
    private static boolean desktopUiInitialized;
    private static String originalSoundPreference;
    private static boolean soundPreferenceCaptured;

    @Test
    public void serializedDanDanSnapshotRendersSharedZonesAndAuthorizedHand() throws Exception {
        ensureDesktopUiInitialized();

        final Game game = newDanDanGame();
        final Player first = game.getPlayers().get(0);
        final Player second = game.getPlayers().get(1);
        addCardToZone("Island", first, ZoneType.Library);
        addCardToZone("Memory Lapse", second, ZoneType.Library);
        addCardToZone("Mental Note", first, ZoneType.Graveyard);
        addCardToZone("Dandân", first, ZoneType.Hand);
        addCardToZone("Dandân", second, ZoneType.Hand);

        final Game ordinary = newOrdinaryGame();
        addCardToZone("Island", ordinary.getPlayers().get(0), ZoneType.Library);
        addCardToZone("Memory Lapse", ordinary.getPlayers().get(1), ZoneType.Library);
        final Player ordinaryFirst = ordinary.getPlayers().get(0);
        final Player ordinarySecond = ordinary.getPlayers().get(1);
        final PlayerView ordinaryFirstView = ordinaryFirst.getView();
        final PlayerView ordinarySecondView = ordinarySecond.getView();
        assertFalse(ordinary.getView().isDanDan());
        assertEquals(ordinaryFirstView.getCards(ZoneType.Library).size(), 1,
                "ordinary client views retain separate libraries");
        assertEquals(ordinarySecondView.getCards(ZoneType.Library).size(), 1);
        assertFalse(ordinaryFirstView.getCards(ZoneType.Library).get(0).getId()
                == ordinarySecondView.getCards(ZoneType.Library).get(0).getId());

        final GameView initialSnapshot = roundTrip(game.getView());
        final List<PlayerView> players = new ArrayList<>(initialSnapshot.getPlayers());
        final CMatchUI[] uiHolder = new CMatchUI[1];
        final FScreen[] previousScreen = new FScreen[1];
        FThreads.invokeInEdtAndWait(() -> {
            previousScreen[0] = Singletons.getControl().getCurrentScreen();
            uiHolder[0] = new CMatchUI();
        });
        final CMatchUI matchUI = uiHolder[0];
        final FGameClient client = new FGameClient("DanDan UI test", matchUI, "127.0.0.1", 1);
        final EmbeddedChannel channel = new EmbeddedChannel(new GameClientHandler(client));

        try {
            channel.writeInbound(new GuiGameEvent(ProtocolMethod.setGameView, initialSnapshot, 0L));
            FThreads.invokeInEdtAndWait(() -> { }); // wait for the real handler's EDT setGameView
            assertNotNull(initialSnapshot.getTracker(), "the client handler hydrates deserialized trackables");
            assertNotNull(initialSnapshot.getGameLog(), "the client handler initializes the remote game log");
            FThreads.invokeInEdtAndWait(() -> assertSame(matchUI.getGameView(), initialSnapshot));
            assertNull(initialSnapshot.getGame(), "the remote snapshot must not retain its server Game");
            assertNull(initialSnapshot.getMatch(), "the remote snapshot must not retain its server Match");

            channel.writeInbound(new GuiGameEvent(ProtocolMethod.openView,
                    new TrackableCollection<>(players.get(0))));
            FThreads.invokeInEdtAndWait(() -> { }); // wait for the real handler's EDT openView
            FThreads.invokeInEdtAndWait(() -> {
                assertEquals(matchUI.getCurrentPlayer().getId(), players.get(0).getId(),
                        "the client handler installs the first local player before opening the view");
                assertTrue(matchUI.getGameController() instanceof NetGameController,
                        "the client handler installs its normal network controller");
                assertTrue(matchUI.isCurrentScreen(), "the client view is installed as the active screen");
            });
            final CardView[] permittedHandCard = new CardView[1];
            final VHand[] localHand = new VHand[1];
            final CardPanel[] permittedPanel = new CardPanel[1];
            FThreads.invokeInEdtAndWait(() -> {
                permittedHandCard[0] = players.get(0).getCards(ZoneType.Hand).get(0);
                localHand[0] = matchUI.getHandFor(players.get(0));
                permittedPanel[0] = localHand[0].getHandArea().getCardPanel(permittedHandCard[0].getId());
                assertNotNull(permittedPanel[0], "the authorized own-hand card has a rendered panel");
            });
            // The actual remote openView reveals initial hand panels through Forge's normal draw animation.
            awaitHandCardReveal(localHand[0], permittedHandCard[0].getId(), permittedPanel[0]);
            assertTrue(channel.isOpen(), "the embedded client channel remains open through view setup");

            assertTrue(initialSnapshot.isDanDan());
            assertNull(matchUI.getGameView().getGame(), "the rendered snapshot must not retain its server Game");
            assertNull(matchUI.getGameView().getMatch(), "the rendered snapshot must not retain its server Match");
            FThreads.invokeInEdtAndWait(() -> {
                assertEquals(matchUI.getHandViews().size(), 1,
                        "only this client's authorized local hand gets a hand panel");
                assertEquals(matchUI.getHandViews().get(0).getPlayer().getId(), players.get(0).getId());
                assertNull(matchUI.getHandFor(players.get(1)), "the other seat has no hand panel on this client");

                assertEquals(libraryCount(matchUI, players.get(0)), 2);
                assertEquals(libraryCount(matchUI, players.get(1)), 2);
                assertEquals(graveyardCount(matchUI, players.get(0)), 1);
                assertEquals(graveyardCount(matchUI, players.get(1)), 1);
                assertTrue(zoneTooltip(matchUI, players.get(0), ZoneType.Library)
                        .contains(sharedZoneLabel(ZoneType.Library)));
                assertTrue(zoneTooltip(matchUI, players.get(1), ZoneType.Library)
                        .contains(sharedZoneLabel(ZoneType.Library)));
                assertTrue(zoneTooltip(matchUI, players.get(0), ZoneType.Graveyard)
                        .contains(sharedZoneLabel(ZoneType.Graveyard)));
                assertTrue(zoneTooltip(matchUI, players.get(1), ZoneType.Graveyard)
                        .contains(sharedZoneLabel(ZoneType.Graveyard)));

                final VZone firstLibraryTab = new VZone(matchUI, players.get(0), ZoneType.Library);
                final VZone secondLibraryTab = new VZone(matchUI, players.get(1), ZoneType.Library);
                final VZone firstGraveyardTab = new VZone(matchUI, players.get(0), ZoneType.Graveyard);
                final VZone secondGraveyardTab = new VZone(matchUI, players.get(1), ZoneType.Graveyard);
                firstLibraryTab.refresh();
                secondLibraryTab.refresh();
                firstGraveyardTab.refresh();
                secondGraveyardTab.refresh();
                assertTrue(firstLibraryTab.getTabLabel().getText().contains(sharedZoneLabel(ZoneType.Library)));
                assertTrue(secondLibraryTab.getTabLabel().getText().contains(sharedZoneLabel(ZoneType.Library)));
                assertTrue(firstGraveyardTab.getTabLabel().getText().contains(sharedZoneLabel(ZoneType.Graveyard)));
                assertTrue(secondGraveyardTab.getTabLabel().getText().contains(sharedZoneLabel(ZoneType.Graveyard)));
                for (final CardView card : players.get(0).getCards(ZoneType.Library)) {
                    assertNotNull(firstLibraryTab.getCardPanel(card));
                }
                for (final CardView card : players.get(1).getCards(ZoneType.Library)) {
                    assertNotNull(secondLibraryTab.getCardPanel(card));
                }

                FloatingZone.show(matchUI, players.get(0), ZoneType.Library);
                FloatingZone.show(matchUI, players.get(1), ZoneType.Library);
                FloatingZone.show(matchUI, players.get(0), ZoneType.Graveyard);
                FloatingZone.show(matchUI, players.get(1), ZoneType.Graveyard);

                assertEquals(cardCountInWindow(players.get(0), ZoneType.Library), 2);
                assertEquals(cardCountInWindow(players.get(1), ZoneType.Library), 2);
                assertEquals(cardCountInWindow(players.get(0), ZoneType.Graveyard), 1);
                assertEquals(cardCountInWindow(players.get(1), ZoneType.Graveyard), 1);
                assertTrue(floatingZoneTitle(players.get(0), ZoneType.Library)
                        .contains(sharedZoneLabel(ZoneType.Library)));
                assertTrue(floatingZoneTitle(players.get(1), ZoneType.Library)
                        .contains(sharedZoneLabel(ZoneType.Library)));
                assertTrue(floatingZoneTitle(players.get(0), ZoneType.Graveyard)
                        .contains(sharedZoneLabel(ZoneType.Graveyard)));
                assertTrue(floatingZoneTitle(players.get(1), ZoneType.Graveyard)
                        .contains(sharedZoneLabel(ZoneType.Graveyard)));

                hoverCard(localHand[0].getHandArea(), permittedPanel[0]);
                final VDetail detail = (VDetail) EDocID.CARD_DETAIL.getDoc();
                final String permittedName = CardTranslation.getTranslatedName(permittedHandCard[0].getName()).trim();
                assertTrue(detail.getPnlDetail().getNameCostLabel().getText().contains(permittedName),
                        "the real hand hover renders a permitted own-card name before the hidden-card control");

                final CardView unknownLibraryCard = players.get(0).getCards(ZoneType.Library).get(0);
                final String unknownName = CardTranslation.getTranslatedName(unknownLibraryCard.getName()).trim();
                final CardArea sharedLibraryArea = cardAreaInWindow(players.get(0), ZoneType.Library);
                final CardPanel unknownPanel = sharedLibraryArea.getCardPanel(unknownLibraryCard.getId());
                assertNotNull(unknownPanel, "an unknown shared-library card has a rendered card-back panel");
                assertFalse(matchUI.mayView(unknownLibraryCard), "the local seat is not authorized to view an unknown library card");

                hoverCard(sharedLibraryArea, unknownPanel);
                assertTrue(unknownPanel.isSelected(),
                        "the real shared-library mouse dispatch selects the hidden card panel");
                // CDetailPicture passes null to CDetail when mayView is false, clearing the detail panel.
                assertEquals(detail.getPnlDetail().getNameCostLabel().getText(), "",
                        "the real shared-library hover clears details for an unknown card");
                assertFalse(detail.getPnlDetail().getNameCostLabel().getText().contains(unknownName),
                        "the unknown library name is absent from the rendered detail label");

                final IVDoc<?> reportLog = EDocID.REPORT_LOG.getDoc();
                assertTrue(reportLog instanceof VLog, "the normal match view registers its real VLog consumer");
                assertNotNull(reportLog.getParentCell(), "the normal match layout attaches the report log");
                assertSame(reportLog.getParentCell().getSelected(), reportLog,
                        "openView selects the real report-log tab before the marker update");
            });

            final String renderedPublicLogMarker = "DanDan rendered public log marker";
            // This exercises the real selected log observer and renderer. It does not model a game event
            // that could carry private card text, so it is not a textual-log privacy proof.
            initialSnapshot.getGameLog().add(GameLogEntryType.TURN, renderedPublicLogMarker);
            FThreads.invokeInEdtAndWait(() -> { }); // wait for CLog's observer update and VLog render on the EDT
            FThreads.invokeInEdtAndWait(() -> {
                final IVDoc<?> reportLog = EDocID.REPORT_LOG.getDoc();
                final List<JTextArea> renderedEntries = new ArrayList<>();
                collectTextAreas(reportLog.getParentCell().getBody(), renderedEntries);
                final String renderedLogText = renderedEntries.stream()
                        .map(JTextArea::getText)
                        .reduce("", (left, right) -> left + "\n" + right);
                assertTrue(renderedLogText.contains(renderedPublicLogMarker),
                        "the real selected report-log consumer renders an observer-delivered public marker");
            });

            // Reuse the match UI after the ordinary fresh-view lifecycle; this is not a
            // network next-game/Tracker-rehydration assertion.
            cleanup(matchUI, previousScreen[0]);
            final PlayerControllerHuman ordinaryController = (PlayerControllerHuman) ordinaryFirst.getController();
            FThreads.invokeInEdtAndWait(() -> {
                matchUI.setGameView(null);
                ordinaryController.setGui(matchUI);
                matchUI.setGameView(ordinary.getView());
                matchUI.setOriginalGameController(ordinaryFirstView, ordinaryController);
                matchUI.openView(new TrackableCollection<>(ordinaryFirstView));
                assertSame(matchUI.getGameView(), ordinary.getView());
                assertFalse(matchUI.getGameView().isDanDan());
                assertEquals(matchUI.getCurrentPlayer().getId(), ordinaryFirstView.getId());
                assertTrue(matchUI.getGameController() instanceof PlayerControllerHuman);
                assertTrue(matchUI.isCurrentScreen());
                assertEquals(matchUI.getHandViews().size(), 1);
                assertEquals(matchUI.getHandViews().get(0).getPlayer().getId(), ordinaryFirstView.getId());
            });
            FThreads.invokeInEdtAndWait(() -> {
                assertEquals(libraryCount(matchUI, ordinaryFirstView), 1);
                assertEquals(libraryCount(matchUI, ordinarySecondView), 1);
                assertEquals(graveyardCount(matchUI, ordinaryFirstView), 0);
                assertEquals(graveyardCount(matchUI, ordinarySecondView), 0);
                assertFalse(zoneTooltip(matchUI, ordinaryFirstView, ZoneType.Library)
                        .contains(sharedZoneLabel(ZoneType.Library)));
                assertFalse(zoneTooltip(matchUI, ordinaryFirstView, ZoneType.Graveyard)
                        .contains(sharedZoneLabel(ZoneType.Graveyard)));
                final VZone ordinaryLibraryTab = new VZone(matchUI, ordinaryFirstView, ZoneType.Library);
                final VZone ordinaryGraveyardTab = new VZone(matchUI, ordinaryFirstView, ZoneType.Graveyard);
                ordinaryLibraryTab.refresh();
                ordinaryGraveyardTab.refresh();
                assertFalse(ordinaryLibraryTab.getTabLabel().getText().contains(sharedZoneLabel(ZoneType.Library)));
                assertFalse(ordinaryGraveyardTab.getTabLabel().getText().contains(sharedZoneLabel(ZoneType.Graveyard)));
                FloatingZone.show(matchUI, ordinaryFirstView, ZoneType.Library);
                FloatingZone.show(matchUI, ordinaryFirstView, ZoneType.Graveyard);
                assertFalse(floatingZoneTitle(ordinaryFirstView, ZoneType.Library)
                        .contains(sharedZoneLabel(ZoneType.Library)));
                assertFalse(floatingZoneTitle(ordinaryFirstView, ZoneType.Graveyard)
                        .contains(sharedZoneLabel(ZoneType.Graveyard)));
            });

        } finally {
            try {
                cleanup(matchUI, previousScreen[0]);
            } finally {
                client.close();
                channel.finishAndReleaseAll();
            }
        }
    }

    @Test
    public void localSharedZoneEventRefreshesBothSeatDisplays() throws InterruptedException {
        assertLocalZoneEventRefreshesSeatDisplays(newDanDanGame(), true);
    }

    @Test
    public void clientInitializesGameLogForSecondGameViewOnSameChannel() throws Exception {
        ensureDesktopUiInitialized();

        final GameView firstSnapshot = roundTrip(newDanDanGame().getView());
        final List<PlayerView> firstPlayers = new ArrayList<>(firstSnapshot.getPlayers());
        final CMatchUI[] uiHolder = new CMatchUI[1];
        final FScreen[] previousScreen = new FScreen[1];
        FThreads.invokeInEdtAndWait(() -> {
            previousScreen[0] = Singletons.getControl().getCurrentScreen();
            uiHolder[0] = new CMatchUI();
        });
        final CMatchUI matchUI = uiHolder[0];
        final FGameClient client = new FGameClient("DanDan next-game log test", matchUI, "127.0.0.1", 1);
        final EmbeddedChannel channel = new EmbeddedChannel(new GameClientHandler(client));

        try {
            channel.writeInbound(new GuiGameEvent(ProtocolMethod.setGameView, firstSnapshot, 0L));
            FThreads.invokeInEdtAndWait(() -> { });
            assertNotNull(firstSnapshot.getGameLog(), "the first full game view receives a client log");
            channel.writeInbound(new GuiGameEvent(ProtocolMethod.openView,
                    new TrackableCollection<>(firstPlayers.get(0))));
            FThreads.invokeInEdtAndWait(() -> { });
            FThreads.invokeInEdtAndWait(() -> assertTrue(matchUI.isCurrentScreen(),
                    "the first actual openView callback completes its UI transition"));
            FThreads.invokeInEdtAndWait(() ->
                    assertTrue(Singletons.getControl().setCurrentScreen(FScreen.HOME_SCREEN)));

            final GameView secondSnapshot = roundTrip(newDanDanGame().getView());
            assertFalse(firstSnapshot.getId() == secondSnapshot.getId(),
                    "the second serialized GameView belongs to a distinct game");
            assertNull(secondSnapshot.getGameLog(), "serialization drops the new game's transient client log");
            final List<PlayerView> secondPlayers = new ArrayList<>(secondSnapshot.getPlayers());
            channel.writeInbound(new GuiGameEvent(ProtocolMethod.setGameView, secondSnapshot, 0L));
            FThreads.invokeInEdtAndWait(() -> { });
            assertNotNull(secondSnapshot.getGameLog(), "the second full game view receives its own client log");
            assertNotSame(secondSnapshot.getGameLog(), firstSnapshot.getGameLog(),
                    "the new game receives its own log instead of retaining prior game history");
            channel.writeInbound(new GuiGameEvent(ProtocolMethod.openView,
                    new TrackableCollection<>(secondPlayers.get(0))));
            FThreads.invokeInEdtAndWait(() -> { });
            FThreads.invokeInEdtAndWait(() -> {
                assertSame(matchUI.getGameView(), secondSnapshot);
                assertTrue(matchUI.isCurrentScreen(), "the actual second openView callback completes the UI transition");
            });
        } finally {
            try {
                cleanup(matchUI, previousScreen[0]);
            } finally {
                client.close();
                channel.finishAndReleaseAll();
            }
        }
    }

    @Test
    public void localOrdinaryZoneEventRetainsSeparateSeatDisplays() throws InterruptedException {
        assertLocalZoneEventRefreshesSeatDisplays(newOrdinaryGame(), false);
    }

    @Test(timeOut = 360_000)
    public void renderedDanDanAttacksAfterNormalTurnMaturation() throws Throwable {
        runRenderedDanDanAttack(false);
    }

    @Test(timeOut = 360_000)
    public void renderedDanDanAttacksWithAutoPassNoActionsEnabled() throws Throwable {
        runRenderedDanDanAttack(true);
    }

    private void runRenderedDanDanAttack(final boolean autoPassNoActions) throws Throwable {
        ensureDesktopUiInitialized();

        final ForgePreferences preferences = FModel.getPreferences();
        final FPref[] phasePreferences = {
                FPref.PHASE_HUMAN_MAIN1,
                FPref.PHASE_HUMAN_BEGINCOMBAT,
                FPref.PHASE_HUMAN_DECLAREATTACKERS,
                FPref.PHASE_HUMAN_DECLAREBLOCKERS,
                FPref.YIELD_AUTO_PASS_NO_ACTIONS
        };
        final Map<FPref, String> previousPreferences = new EnumMap<>(FPref.class);
        previousPreferences.put(FPref.PLAYER_NAME, preferences.getPref(FPref.PLAYER_NAME));
        for (final FPref preference : phasePreferences) {
            previousPreferences.put(preference, preferences.getPref(preference));
            preferences.setPref(preference, Boolean.toString(preference != FPref.YIELD_AUTO_PASS_NO_ACTIONS
                    || autoPassNoActions));
        }

        final CMatchUI[] uiHolder = new CMatchUI[1];
        final FScreen[] previousScreen = new FScreen[1];
        FThreads.invokeInEdtAndWait(() -> {
            previousScreen[0] = Singletons.getControl().getCurrentScreen();
            uiHolder[0] = (CMatchUI) GuiBase.getInterface().getNewGuiGame();
        });
        final CMatchUI matchUI = uiHolder[0];
        final RegisteredPlayer aiRegistered = new RegisteredPlayer(officialDanDanDeck())
                .setPlayer(new LobbyPlayerAi("DanDan stock AI", null));
        final RegisteredPlayer humanRegistered = new RegisteredPlayer(officialDanDanDeck())
                .setPlayer(new LobbyPlayerHuman("DanDan local human"));
        final HostedMatch hosted = new HostedMatch();
        final CountDownLatch fixtureReady = new CountDownLatch(1);
        final CountDownLatch gameLoopReturned = new CountDownLatch(1);
        final AtomicReference<Throwable> setupFailure = new AtomicReference<>();
        final AtomicReference<Throwable> hostedStartupFailure = new AtomicReference<>();
        final AtomicReference<Player> aiPlayerRef = new AtomicReference<>();
        final AtomicReference<Player> humanPlayerRef = new AtomicReference<>();
        final AtomicReference<PlayerControllerHuman> humanUiControllerRef = new AtomicReference<>();
        final AtomicReference<Integer> dandanIdRef = new AtomicReference<>();
        final AtomicReference<DanDanCastReceipt> dandanCastReceipt = new AtomicReference<>();
        final AtomicReference<Integer> castTurnRef = new AtomicReference<>();
        final AtomicReference<List<Integer>> physicalCardIdsRef = new AtomicReference<>();
        final GameRules rules = new GameRules(GameType.Constructed);
        rules.setAppliedVariants(EnumSet.of(GameType.DanDan));
        rules.setGamesPerMatch(1);
        hosted.setEndGameHook(gameLoopReturned::countDown);

        hosted.setStartGameHook(() -> {
            try {
                final Game game = hosted.getGame();
                final Player ai = game.getPlayers().stream()
                        .filter(player -> player.getController() instanceof PlayerControllerAi)
                        .findFirst().orElseThrow();
                final Player human = game.getPlayers().stream()
                        .filter(player -> player.getController() instanceof PlayerControllerHuman)
                        .findFirst().orElseThrow();
                assertSame(ai.getZone(ZoneType.Library), human.getZone(ZoneType.Library));
                assertSame(ai.getZone(ZoneType.Graveyard), human.getZone(ZoneType.Graveyard));
                final Player libraryOwner = human.getZone(ZoneType.Library).getPlayer();
                final List<Integer> originalPhysicalCardIds = game.getCardsInGame().stream()
                        .map(Card::getId).sorted().toList();
                assertEquals(originalPhysicalCardIds.stream().distinct().count(), 80L);
                assertEquals(game.getCardsInGame().size(), 80,
                        "the normal HostedMatch retains the 80 physical official DanDan cards");

                // Return the opening hand to the shared library so the only local card drawn before
                // the attack is the fish acquired below. This avoids unrelated cleanup prompts.
                for (final Card card : new ArrayList<>(human.getCardsIn(ZoneType.Hand))) {
                    game.getAction().moveToBottomOfLibrary(card, null);
                }

                // Keep stock AI decision making, but remove real steal spells from the AI's opening
                // hand and its next four draws. The turns still run through the normal AI controller.
                for (final Card card : new ArrayList<>(ai.getCardsIn(ZoneType.Hand))) {
                    if (!"Island".equals(card.getName())) {
                        game.getAction().moveToBottomOfLibrary(card, null);
                    }
                }
                assertTrue(ai.getCardsIn(ZoneType.Hand).stream().allMatch(card -> "Island".equals(card.getName())),
                        "the stock AI keeps its real basic Island opening cards");

                final Card dandan = findOwnedSharedCard(libraryOwner, "Dandân");
                final Card humanIslandOne = findOwnedSharedCard(libraryOwner, "Island", dandan);
                final Card humanIslandTwo = findOwnedSharedCard(libraryOwner, "Island", dandan, humanIslandOne);
                final Card defenderIsland = findOwnedSharedCard(libraryOwner, "Island", dandan, humanIslandOne, humanIslandTwo);
                final Card sanctuary = findOwnedSharedCard(libraryOwner, "Mystic Sanctuary");
                assertNotNull(dandan, "the official shared library contains the actual Dandân creature");
                assertNotNull(humanIslandOne);
                assertNotNull(humanIslandTwo);
                assertNotNull(defenderIsland);
                assertNotNull(sanctuary);
                game.getAction().moveToPlay(humanIslandOne, human, null, AbilityKey.newMap());
                game.getAction().moveToPlay(humanIslandTwo, human, null, AbilityKey.newMap());

                final Card aiIslandInHand = game.getAction().moveTo(ai.getZone(ZoneType.Hand), defenderIsland, null);
                final Card sanctuaryInHand = game.getAction().moveTo(ai.getZone(ZoneType.Hand), sanctuary, null);
                assertSame(aiIslandInHand.getOwner(), ai);
                assertSame(sanctuaryInHand.getOwner(), ai);
                final Card aiIslandOnBattlefield = game.getAction().moveToPlay(aiIslandInHand, ai, null, AbilityKey.newMap());
                final Card sanctuaryOnBattlefield = game.getAction().moveToPlay(sanctuaryInHand, ai, null, AbilityKey.newMap());
                assertSame(aiIslandOnBattlefield.getController(), ai);
                assertSame(sanctuaryOnBattlefield.getController(), ai);

                final Card acquiredDandan = game.getAction().moveTo(human.getZone(ZoneType.Hand), dandan, null);
                assertSame(acquiredDandan.getOwner(), human,
                        "the real shared-library acquisition gives the local Human the creature");
                assertSame(acquiredDandan.getController(), human);
                final int acquiredDandanId = acquiredDandan.getId();
                game.subscribeToEvents(new Object() {
                    @Subscribe
                    public void onSpellAbilityCast(final GameEventSpellAbilityCast event) {
                        if (event.sa() == null || !event.sa().isSpell() || event.sa().getHostCard() == null
                                || event.sa().getHostCard().getId() != acquiredDandanId || event.si() == null) {
                            return;
                        }
                        SpellAbilityStackInstance matchingStackItem = null;
                        for (final SpellAbilityStackInstance item : game.getStack()) {
                            if (item.getId() == event.si().getId()) {
                                matchingStackItem = item;
                                break;
                            }
                        }
                        if (matchingStackItem == null) return;
                        final var spell = matchingStackItem.getSpellAbility();
                        final List<Mana> payingMana = List.copyOf(spell.getPayingMana());
                        dandanCastReceipt.set(new DanDanCastReceipt(spell.getHostCard().getId(),
                                spell.getActivatingPlayer().getId(), game.getPhaseHandler().getTurn(), payingMana.size(),
                                payingMana.stream().map(Mana::getColor).toList()));
                    }
                });

                final List<Card> libraryIslands = new ArrayList<>();
                while (libraryIslands.size() < 4) {
                    final Card nextIsland = findOwnedSharedCard(libraryOwner, "Island", libraryIslands.toArray(new Card[0]));
                    assertNotNull(nextIsland, "the official shared library supplies four real basic Islands for AI draws");
                    libraryIslands.add(nextIsland);
                }
                for (int index = libraryIslands.size() - 1; index >= 0; index--) {
                    game.getAction().moveToLibrary(libraryIslands.get(index), 0, null);
                }
                assertEquals(human.getCardsIn(ZoneType.Library).stream().limit(4).map(Card::getId).toList(),
                        libraryIslands.stream().map(Card::getId).toList(),
                        "only the real official Island cards form the deterministic four-card draw prefix");
                assertTrue(human.getCardsIn(ZoneType.Library).stream().limit(4)
                        .allMatch(card -> "Island".equals(card.getName())));
                assertEquals(game.getCardsInGame().size(), 80);
                assertEquals(game.getCardsInGame().stream().map(Card::getId).sorted().toList(), originalPhysicalCardIds,
                        "fixture arrangement preserves every original physical card ID");
                dandanIdRef.set(acquiredDandan.getId());
                physicalCardIdsRef.set(originalPhysicalCardIds);
                aiPlayerRef.set(ai);
                humanPlayerRef.set(human);
            } catch (Throwable failure) {
                setupFailure.set(failure);
            } finally {
                fixtureReady.countDown();
            }
        });

        boolean startScheduled = false;
        boolean gameLoopReturnedNormally = false;
        Throwable primaryFailure = null;
        InputAttack acceptedAttackInput = null;
        try {
            preferences.setPref(FPref.PLAYER_NAME, "DanDan test player");
            FThreads.invokeInEdtAndWait(() -> {
                try {
                    hosted.startMatch(rules, null, List.of(humanRegistered, aiRegistered), Map.of(humanRegistered, matchUI), null);
                    final Game startedGame = hosted.getGame();
                    final Player startedHuman = startedGame.getPlayers().stream()
                            .filter(player -> player.getOriginalLobbyPlayer() == humanRegistered.getPlayer())
                            .findFirst().orElseThrow();
                    final Object uiController = matchUI.getGameController(startedHuman.getView());
                    assertTrue(uiController instanceof PlayerControllerHuman,
                            "the rendered local seat retains its original Human controller in the UI map");
                    final PlayerControllerHuman humanController = (PlayerControllerHuman) uiController;
                    assertSame(humanController.getPlayer(), startedHuman);
                    assertSame(humanController.getGui(), matchUI);
                    humanPlayerRef.set(startedHuman);
                    humanUiControllerRef.set(humanController);
                } catch (Throwable failure) {
                    hostedStartupFailure.set(failure);
                }
            });
            if (hostedStartupFailure.get() != null) {
                throw new AssertionError("HostedMatch could not start through its normal EDT UI lifecycle",
                        hostedStartupFailure.get());
            }
            startScheduled = true;
            boolean combatStopsConfigured = false;
            Input lastOpeningInput = null;
            final long setupDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(90);
            while (fixtureReady.getCount() != 0 && System.nanoTime() < setupDeadline) {
                final Game setupGame = hosted.getGame();
                if (setupGame != null) {
                    final Player setupHuman = setupGame.getPlayers().stream()
                            .filter(player -> player.getOriginalLobbyPlayer() == humanRegistered.getPlayer())
                            .findFirst().orElse(null);
                    if (setupHuman != null) {
                        final PlayerControllerHuman setupController = humanUiControllerRef.get();
                        assertNotNull(setupController, "the local Human UI controller is cached from HostedMatch setup");
                        if (!combatStopsConfigured && matchUI.isCurrentScreen()) {
                            disableRenderedCombatStops(matchUI, setupHuman, setupController);
                            combatStopsConfigured = true;
                        }
                        final Input openingInput = setupController.getInputQueue().getInput();
                        if (openingInput == null) {
                            lastOpeningInput = null;
                        } else if ((openingInput instanceof InputConfirm || openingInput instanceof InputConfirmMulligan)
                                && openingInput != lastOpeningInput
                                && renderedPromptReady(matchUI, openingInput, setupController)) {
                            lastOpeningInput = openingInput;
                            clickRenderedPromptOk(matchUI, openingInput, setupController);
                        }
                    }
                }
                if (gameLoopReturned.getCount() == 0 && fixtureReady.getCount() != 0) {
                    throw new AssertionError("HostedMatch ended before the attack fixture completed");
                }
                Thread.sleep(10);
            }
            assertEquals(fixtureReady.getCount(), 0L,
                    "the real opening-hand prompt and HostedMatch setup hook complete");
            assertTrue(combatStopsConfigured, "the actual human BC, DA, and DB labels were toggled through Swing input");
            if (setupFailure.get() != null) {
                throw new AssertionError("DanDan attack fixture setup failed", setupFailure.get());
            }

            final Game game = hosted.getGame();
            final Player ai = aiPlayerRef.get();
            final Player human = humanPlayerRef.get();
            final PlayerControllerHuman controller = humanUiControllerRef.get();
            assertNotNull(controller, "the rendered Human input driver uses its retained local controller");
            final int dandanId = dandanIdRef.get();
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(180);
            Input lastActedInput = null;
            boolean dandanSelectionInitiated = false;
            boolean mainOnePromptObserved = false;
            boolean mainOnePassedAfterResolution = false;
            boolean attackAccepted = false;
            int attackTurn = -1;

            while (System.nanoTime() < deadline && !attackAccepted) {
                final Input input = controller.getInputQueue().getInput();
                if (input == null) {
                    lastActedInput = null;
                    if (gameLoopReturned.getCount() == 0) {
                        throw new AssertionError("HostedMatch finished before the actual Dandân attack input");
                    }
                    Thread.sleep(10);
                    continue;
                }

                final PhaseType phase = game.getPhaseHandler().getPhase();
                final Player turnPlayer = game.getPhaseHandler().getPlayerTurn();
                final Player priorityPlayer = game.getPhaseHandler().getPriorityPlayer();
                if (input == lastActedInput || !renderedPromptReady(matchUI, input, controller)) {
                    Thread.sleep(10);
                    continue;
                }
                lastActedInput = input;
                assertSame(input.getOwner(), human.getView(),
                        "only the live local Human input is answered by the rendered desktop controls");
                assertSame(controller.getInputQueue().getInput(), input,
                        "the native desktop action is bound to the actual current game input");

                if (input instanceof InputConfirm || input instanceof InputConfirmMulligan
                        || input instanceof InputPayMana) {
                    clickRenderedPromptOk(matchUI, input, controller);
                    continue;
                }

                if (input instanceof InputAttack) {
                    final Card dandan = game.findById(dandanId);
                    assertEquals(phase, PhaseType.COMBAT_DECLARE_ATTACKERS,
                            "native phase progression reached the real attacker declaration step");
                    final DanDanCastReceipt castReceipt = dandanCastReceipt.get();
                    assertNotNull(castReceipt, "the actual Dandân cast was recorded from its live stack event");
                    assertEquals(castReceipt.cardId(), dandanId);
                    assertEquals(castReceipt.casterId(), human.getId());
                    assertEquals(castReceipt.turn(), castTurnRef.get());
                    assertEquals(castReceipt.paidCount(), 2,
                            "the actual Dandân cast paid exactly two mana");
                    assertEquals(castReceipt.paidColors().size(), castReceipt.paidCount());
                    assertTrue(castReceipt.paidColors().stream()
                                    .allMatch(color -> color == forge.card.mana.ManaAtom.BLUE),
                            "the actual Dandân stack item records exactly UU paid");
                    assertTrue(mainOnePromptObserved);
                    assertTrue(mainOnePassedAfterResolution,
                            "the local Human advanced normally from Main 1 with the real OK button");
                    assertTrue(game.getPhaseHandler().getTurn() > castTurnRef.get(),
                            "normal progression advanced beyond the turn in which Dandân was cast");
                    assertEquals(game.getPhaseHandler().getTurn(), castTurnRef.get() + 2,
                            "the witness attacks on the next natural Human turn within the four-Island draw prefix");
                    assertEquals(game.getCardsInGame().size(), 80);
                    assertEquals(game.getCardsInGame().stream().map(Card::getId).sorted().toList(),
                            physicalCardIdsRef.get(), "the exact original physical card IDs remain in the match");
                    assertSame(dandan.getZone(), human.getZone(ZoneType.Battlefield),
                            "the actual Dandân spell resolved to the Human battlefield before later combat");
                    assertSame(dandan.getOwner(), human);
                    assertSame(dandan.getController(), human);
                    assertFalse(dandan.isSick(), "normal turn progression matured the acquired Dandân");
                    assertSame(turnPlayer, human);
                    assertSame(priorityPlayer, human);
                    assertTrue(CombatUtil.canAttack(dandan, ai),
                            "the AI controls the actual basic Island that permits DanDan to attack");
                    assertEquals(ai.getCardsIn(ZoneType.Battlefield).stream()
                            .filter(card -> "Mystic Sanctuary".equals(card.getName())).count(), 1L,
                            "the defender also controls its real Mystic Sanctuary");
                    assertTrue(matchUI.getGameController() instanceof PlayerControllerHuman,
                            "the rendered match uses the actual local Human game controller");

                    FThreads.invokeInEdtAndWait(() -> {
                        final PhaseLabel beginCombat = matchUI.getFieldViewFor(human.getView())
                                .getPhaseIndicator().getLabelFor(PhaseType.COMBAT_BEGIN);
                        final PhaseLabel attackers = matchUI.getFieldViewFor(human.getView())
                                .getPhaseIndicator().getLabelFor(PhaseType.COMBAT_DECLARE_ATTACKERS);
                        final PhaseLabel blockers = matchUI.getFieldViewFor(human.getView())
                                .getPhaseIndicator().getLabelFor(PhaseType.COMBAT_DECLARE_BLOCKERS);
                        assertFalse(beginCombat.getEnabled(), "the rendered BC stop is visibly disabled");
                        assertFalse(attackers.getEnabled(), "the rendered DA stop is visibly disabled");
                        assertFalse(blockers.getEnabled(), "the rendered DB stop is visibly disabled");
                        assertTrue(beginCombat.isShowing() && attackers.isShowing() && blockers.isShowing(),
                                "the three actual combat labels are visible on the rendered human field");
                        assertTrue(attackers.getActive(), "the rendered DA label identifies the current phase");
                        assertTrue(controller.isUiSetToSkipPhase(human.getView(), PhaseType.COMBAT_BEGIN));
                        assertTrue(controller.isUiSetToSkipPhase(human.getView(), PhaseType.COMBAT_DECLARE_ATTACKERS));
                        assertTrue(controller.isUiSetToSkipPhase(human.getView(), PhaseType.COMBAT_DECLARE_BLOCKERS));
                        assertTrue(matchUI.isCurrentScreen(), "the actual desktop match remains the active screen");
                        final VPrompt prompt = (VPrompt) EDocID.REPORT_MESSAGE.getDoc();
                        assertTrue(prompt.getBtnOK().isShowing(),
                                "the current combat prompt has its real rendered OK control");
                    });

                    final PlayArea battlefield = matchUI.getFieldViewFor(human.getView()).getTabletop();
                    final CardPanel attackerPanel = battlefield.getCardPanel(dandanId);
                    assertNotNull(attackerPanel, "the matured Dandân has a real rendered battlefield panel");
                    clickRenderedCard(battlefield, attackerPanel, battlefield::getCardPanel);
                    assertTrue(game.getPhaseHandler().getCombat().isAttacking(dandan),
                            "the real rendered battlefield click is accepted by InputAttack");
                    acceptedAttackInput = (InputAttack) input;
                    clickRenderedPromptOk(matchUI, input, controller);
                    attackTurn = game.getPhaseHandler().getTurn();
                    attackAccepted = true;
                    continue;
                }

                if (input instanceof InputPassPriority) {
                    if (turnPlayer == human && phase == PhaseType.MAIN1 && !dandanSelectionInitiated) {
                        final VHand hand = matchUI.getHandFor(human.getView());
                        final CardPanel panel = hand.getHandArea().getCardPanel(dandanId);
                        assertNotNull(panel, "the actual acquired Dandân has a rendered local hand panel");
                        awaitHandCardReveal(hand, dandanId, panel);
                        castTurnRef.set(game.getPhaseHandler().getTurn());
                        mainOnePromptObserved = true;
                        clickRenderedCard(hand.getHandArea(), panel, hand.getHandArea()::getCardPanel);
                        dandanSelectionInitiated = true;
                        continue;
                    }

                    if (dandanCastReceipt.get() != null
                            && game.findById(dandanId).getZone() == human.getZone(ZoneType.Battlefield)
                            && turnPlayer == human && phase == PhaseType.MAIN1
                            && game.getStack().isEmpty() && game.getPhaseHandler().getTurn() > castTurnRef.get()) {
                        mainOnePassedAfterResolution = true;
                    }

                    clickRenderedPromptOk(matchUI, input, controller);
                    continue;
                }

                throw new AssertionError("Unhandled native Human input while progressing to DanDan attack: "
                        + input.getClass().getName() + " at " + phase);
            }

            assertTrue(attackAccepted, "native phase progression reached the real accepted attack");
            assertTrue(attackTurn > castTurnRef.get());
        } catch (Throwable failure) {
            primaryFailure = failure;
            throw failure;
        } finally {
            try {
                final Game game = hosted.getGame();
                if (startScheduled && game != null && !game.isGameOver()) {
                    final Player human = humanPlayerRef.get() != null ? humanPlayerRef.get()
                            : game.getPlayers().stream()
                                    .filter(player -> player.getOriginalLobbyPlayer() == humanRegistered.getPlayer())
                                    .findFirst().orElse(null);
                    final PlayerControllerHuman controller = humanUiControllerRef.get();
                    if (human != null && controller != null) {
                        assertSame(controller.getPlayer(), human);
                        if (acceptedAttackInput != null) {
                            awaitNextHumanPriorityAndConcede(game, human, controller);
                        } else {
                            FThreads.invokeInEdtAndWait(controller::concede);
                        }
                    }
                }
                if (startScheduled) {
                    gameLoopReturnedNormally = gameLoopReturned.await(30, TimeUnit.SECONDS);
                    if (!gameLoopReturnedNormally) {
                        throw new AssertionError("the HostedMatch game loop did not return after the real Human conceded");
                    }
                }
            } catch (Throwable cleanupFailure) {
                if (primaryFailure != null && primaryFailure != cleanupFailure) primaryFailure.addSuppressed(cleanupFailure);
                else throw cleanupFailure;
            } finally {
                if (!startScheduled || gameLoopReturnedNormally) {
                    FThreads.invokeInEdtAndWait(() -> {
                        matchUI.afterGameEnd();
                        SOverlayUtils.hideOverlay();
                    });
                    cleanup(matchUI, previousScreen[0]);
                }
                for (final Map.Entry<FPref, String> preference : previousPreferences.entrySet()) {
                    preferences.setPref(preference.getKey(), preference.getValue());
                }
            }
        }
    }

    private void assertLocalZoneEventRefreshesSeatDisplays(final Game game, final boolean sharedZones)
            throws InterruptedException {
        ensureDesktopUiInitialized();

        final Player first = game.getPlayers().get(0);
        final Player second = game.getPlayers().get(1);
        final Zone sharedLibrary = first.getZone(ZoneType.Library);
        final Zone sharedGraveyard = first.getZone(ZoneType.Graveyard);
        assertEquals(game.getView().isDanDan(), sharedZones);
        assertEquals(second.getZone(ZoneType.Library) == sharedLibrary, sharedZones,
                "only DanDan seats share library model zones");
        assertEquals(second.getZone(ZoneType.Graveyard) == sharedGraveyard, sharedZones,
                "only DanDan seats share graveyard model zones");
        assertSame(sharedLibrary.getPlayer(), first,
                "the shared library's natural zone events identify its canonical seat");
        assertSame(sharedGraveyard.getPlayer(), first,
                "the shared graveyard's natural zone events identify its canonical seat");
        addCardToZone("Island", first, ZoneType.Library);
        addCardToZone("Memory Lapse", second, ZoneType.Library);
        addCardToZone("Mental Note", first, ZoneType.Graveyard);
        addCardToZone("Dandân", first, ZoneType.Hand);
        addCardToZone("Dandân", second, ZoneType.Hand);

        final PlayerView firstView = first.getView();
        final PlayerView secondView = second.getView();
        final PlayerControllerHuman localController = (PlayerControllerHuman) first.getController();
        final CMatchUI[] uiHolder = new CMatchUI[1];
        final FScreen[] previousScreen = new FScreen[1];
        FThreads.invokeInEdtAndWait(() -> {
            previousScreen[0] = Singletons.getControl().getCurrentScreen();
            uiHolder[0] = new CMatchUI();
            localController.setGui(uiHolder[0]);
            uiHolder[0].setGameView(game.getView());
            uiHolder[0].setOriginalGameController(firstView, localController);
        });
        final CMatchUI matchUI = uiHolder[0];
        game.subscribeToEvents(new FControlGameEventHandler(localController));

        try {
            FThreads.invokeInEdtAndWait(() -> matchUI.openView(new TrackableCollection<>(firstView)));
            FThreads.invokeInEdtAndWait(() -> {
                FloatingZone.show(matchUI, firstView, ZoneType.Library);
                FloatingZone.show(matchUI, secondView, ZoneType.Library);
                FloatingZone.show(matchUI, firstView, ZoneType.Graveyard);
                FloatingZone.show(matchUI, secondView, ZoneType.Graveyard);
                assertEquals(cardCountInWindow(firstView, ZoneType.Library), sharedZones ? 2 : 1);
                assertEquals(cardCountInWindow(secondView, ZoneType.Library), sharedZones ? 2 : 1);
                assertEquals(cardCountInWindow(firstView, ZoneType.Graveyard), 1);
                assertEquals(cardCountInWindow(secondView, ZoneType.Graveyard), sharedZones ? 1 : 0);
            });

            final Card moved = sharedLibrary.getCards().get(0);
            game.getAction().moveTo(sharedGraveyard, moved, null);
            FThreads.invokeInEdtAndWait(() -> { }); // drain the real game-event UI update queued on the EDT

            assertEquals(firstView.getZoneSize(ZoneType.Library), sharedZones ? 1 : 0);
            assertEquals(secondView.getZoneSize(ZoneType.Library), 1);
            assertEquals(firstView.getZoneSize(ZoneType.Graveyard), 2);
            assertEquals(secondView.getZoneSize(ZoneType.Graveyard), sharedZones ? 2 : 0);

            FThreads.invokeInEdtAndWait(() -> {
                assertEquals(libraryCount(matchUI, firstView), sharedZones ? 1 : 0);
                assertEquals(libraryCount(matchUI, secondView), 1);
                assertEquals(graveyardCount(matchUI, firstView), 2);
                assertEquals(graveyardCount(matchUI, secondView), sharedZones ? 2 : 0);
                assertEquals(cardCountInWindow(firstView, ZoneType.Library), sharedZones ? 1 : 0);
                assertEquals(cardCountInWindow(secondView, ZoneType.Library), 1,
                        "the second seat's displayed library count remains correct");
                assertEquals(cardCountInWindow(firstView, ZoneType.Graveyard), 2);
                assertEquals(cardCountInWindow(secondView, ZoneType.Graveyard), sharedZones ? 2 : 0);
            });

            if (sharedZones) {
                final Card acquired = sharedLibrary.getCards().stream()
                        .filter(card -> card.getOwner() == second)
                        .findFirst().orElseThrow();
                assertSame(acquired.getOwner(), second, "the card starts owned by the opposing seat");
                final Card returned = game.getAction().moveTo(first.getZone(ZoneType.Hand), acquired, null);
                FThreads.invokeInEdtAndWait(() -> { });
                final CardView acquiredView = returned.getView();
                assertEquals(acquiredView.getZone(), ZoneType.Hand);
                assertEquals(acquiredView.getController().getId(), firstView.getId());
                assertEquals(acquiredView.getOwner().getId(), firstView.getId());
                assertTrue(matchUI.mayView(acquiredView), "the acquiring local player may view its hand card");
                assertFalse(matchUI.mayView(secondView.getCards(ZoneType.Hand).get(0)),
                        "the other seat's hand card remains hidden from this client");

                final String acquiredName = CardTranslation.getTranslatedName(acquiredView.getName()).trim();
                final VHand[] localHand = new VHand[1];
                final CardPanel[] acquiredPanel = new CardPanel[1];
                FThreads.invokeInEdtAndWait(() -> {
                    localHand[0] = matchUI.getHandFor(firstView);
                    assertNotNull(localHand[0]);
                    assertNull(matchUI.getHandFor(secondView));
                    acquiredPanel[0] = localHand[0].getHandArea().getCardPanel(acquiredView.getId());
                    assertNotNull(acquiredPanel[0], "the natural zone event refreshes the local hand panel");
                    assertTrue(matchUI.mayView(acquiredPanel[0].getCard()));
                    assertTrue(localHand[0].getHandArea().isShowing());
                    final VDetail detail = (VDetail) EDocID.CARD_DETAIL.getDoc();
                    assertTrue(detail.getPnlDetail().getNameCostLabel().isShowing(),
                            "the normal match layout shows the card detail label");
                    final String previousDetailName = detail.getPnlDetail().getNameCostLabel().getText();
                    assertTrue(previousDetailName == null || !previousDetailName.contains(acquiredName),
                            "the acquired card has not already been selected for the detail panel");
                });

                awaitHandCardReveal(localHand[0], acquiredView.getId(), acquiredPanel[0]);

                FThreads.invokeInEdtAndWait(() -> {
                    assertTrue(acquiredPanel[0].isDisplayEnabled(),
                            "the normal hand acquisition animation reveals the card panel");
                    assertTrue(acquiredPanel[0].getCardWidth() > 0 && acquiredPanel[0].getCardHeight() > 0,
                            "the acquired card panel has real laid-out bounds");
                    final VDetail detail = (VDetail) EDocID.CARD_DETAIL.getDoc();
                    final String previousDetailName = detail.getPnlDetail().getNameCostLabel().getText();
                    assertTrue(previousDetailName == null || !previousDetailName.contains(acquiredName),
                            "the acquired card is not already selected before the dispatched hover");
                    Point hoverPoint = null;
                    final Rectangle visibleCardBounds = new Rectangle(acquiredPanel[0].getCardX(), acquiredPanel[0].getCardY(),
                            acquiredPanel[0].getCardWidth(), acquiredPanel[0].getCardHeight())
                            .intersection(localHand[0].getHandArea().getVisibleRect());
                    assertFalse(visibleCardBounds.isEmpty(), "the acquired card has visible bounds in the real HandArea");
                    final int hoverY = visibleCardBounds.y + visibleCardBounds.height / 2;
                    for (int x = visibleCardBounds.x; x < visibleCardBounds.x + visibleCardBounds.width; x++) {
                        if (localHand[0].getHandArea().getCardPanel(x, hoverY) == acquiredPanel[0]) {
                            hoverPoint = new Point(x, hoverY);
                            break;
                        }
                    }
                    assertNotNull(hoverPoint, "the acquired card has an exposed hit area in the real HandArea");
                    assertSame(localHand[0].getHandArea().getCardPanel(hoverPoint.x, hoverPoint.y), acquiredPanel[0],
                            "the chosen hover coordinates resolve to the acquired card panel");
                    localHand[0].getHandArea().dispatchEvent(new MouseEvent(localHand[0].getHandArea(),
                            MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0,
                            hoverPoint.x, hoverPoint.y, 0, false));
                    // GuiDesktop.invokeInEdtNow runs the CMatchUI.setCard detail update inline here.
                    assertNotNull(detail.getPnlDetail().getNameCostLabel().getText(),
                            "the real hover listener populates card details");
                    assertTrue(detail.getPnlDetail().getNameCostLabel().getText().contains(acquiredName),
                            "hovering the acquired local hand card displays its permitted card details");
                });
            }
        } finally {
            cleanup(matchUI, previousScreen[0]);
        }
    }

    private Game newDanDanGame() {
        final List<RegisteredPlayer> players = List.of(
                new RegisteredPlayer(new Deck()).setPlayer(human("First seat")),
                new RegisteredPlayer(new Deck()).setPlayer(human("Second seat")));
        final GameRules rules = new GameRules(GameType.Constructed);
        rules.setAppliedVariants(EnumSet.of(GameType.DanDan));
        return new Match(rules, players, "serialized DanDan match UI test").createGame();
    }

    private Game newOrdinaryGame() {
        final List<RegisteredPlayer> players = List.of(
                new RegisteredPlayer(new Deck()).setPlayer(human("Ordinary first")),
                new RegisteredPlayer(new Deck()).setPlayer(human("Ordinary second")));
        final GameRules rules = new GameRules(GameType.Constructed);
        return new Match(rules, players, "ordinary match UI control").createGame();
    }

    private static LobbyPlayerHuman human(final String name) {
        return new LobbyPlayerHuman(name) {
            @Override
            public Player createIngamePlayer(final Game game, final int id) {
                final Player player = new Player(getName(), game, id);
                player.setFirstController(new PlayerControllerHuman(game, player, this));
                return player;
            }
        };
    }

    private static GameView roundTrip(final GameView source) throws Exception {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(source);
        }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (GameView) input.readObject();
        }
    }

    private static void clickRenderedPromptOk(final CMatchUI matchUI, final Input input,
            final PlayerControllerHuman controller) {
        FThreads.invokeInEdtAndWait(() -> {
            assertSame(controller.getInputQueue().getInput(), input,
                    "the real rendered OK click is bound to the current Human input");
            assertSame(controller.getInputProxy().getInput(), input,
                    "the real rendered OK click reaches the matching Human input proxy");
            assertSame(input.getOwner(), controller.getPlayer().getView());
            final VPrompt prompt = (VPrompt) EDocID.REPORT_MESSAGE.getDoc();
            assertTrue(prompt.getBtnOK().isShowing(), "the real match prompt OK control is visible");
            assertTrue(prompt.getBtnOK().isEnabled(), "the real match prompt OK control accepts this input");
            assertTrue(matchUI.isCurrentScreen(), "the native match UI is active when its prompt is clicked");
            prompt.getBtnOK().doClick();
        });
    }

    private static boolean renderedPromptReady(final CMatchUI matchUI, final Input input,
            final PlayerControllerHuman controller) {
        final boolean[] ready = {false};
        FThreads.invokeInEdtAndWait(() -> {
            final VPrompt prompt = (VPrompt) EDocID.REPORT_MESSAGE.getDoc();
            ready[0] = matchUI.isCurrentScreen()
                    && controller.getInputQueue().getInput() == input
                    && controller.getInputProxy().getInput() == input
                    && prompt.getBtnOK().isShowing()
                    && prompt.getBtnOK().isEnabled();
        });
        return ready[0];
    }

    private static void awaitNextHumanPriorityAndConcede(final Game game, final Player human,
            final PlayerControllerHuman controller)
            throws InterruptedException {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        final boolean[] conceded = {false};
        while (!conceded[0] && System.nanoTime() < deadline) {
            FThreads.invokeInEdtAndWait(() -> {
                final Input candidate = controller.getInputQueue().getInput();
                if (candidate instanceof InputPassPriority && candidate.getOwner() == human.getView()
                        && controller.getInputQueue().getInput() == candidate
                        && controller.getInputProxy().getInput() == candidate && !game.isGameOver()) {
                    final VPrompt prompt = (VPrompt) EDocID.REPORT_MESSAGE.getDoc();
                    if (prompt.getBtnOK().isShowing() && prompt.getBtnOK().isEnabled()) {
                        controller.concede();
                        conceded[0] = true;
                    }
                }
            });
            if (!conceded[0]) {
                Thread.sleep(10);
            }
        }
        if (!conceded[0]) {
            final Input current = controller.getInputQueue().getInput();
            throw new AssertionError("Timed out waiting for the next rendered Human priority input; queue="
                    + (current == null ? "null" : current.getClass().getSimpleName())
                    + ", proxy=" + (controller.getInputProxy().getInput() == null ? "null"
                            : controller.getInputProxy().getInput().getClass().getSimpleName()));
        }
    }

    private static void disableRenderedCombatStops(final CMatchUI matchUI, final Player human,
            final PlayerControllerHuman controller) {
        FThreads.invokeInEdtAndWait(() -> {
            for (final PhaseType phase : List.of(PhaseType.COMBAT_BEGIN,
                    PhaseType.COMBAT_DECLARE_ATTACKERS, PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                final PhaseLabel label = matchUI.getFieldViewFor(human.getView())
                        .getPhaseIndicator().getLabelFor(phase);
                assertTrue(label.isShowing(), "the real combat phase label is visible before it is toggled");
                assertTrue(label.getEnabled(), "the actual combat phase stop begins enabled");
                label.dispatchEvent(new MouseEvent(label, MouseEvent.MOUSE_PRESSED,
                        System.currentTimeMillis(), MouseEvent.BUTTON1_DOWN_MASK,
                        Math.max(1, label.getWidth() / 2), Math.max(1, label.getHeight() / 2),
                        1, false, MouseEvent.BUTTON1));
                assertFalse(label.getEnabled(), "the actual phase label toggles to disabled from mouse input");
                assertTrue(controller.isUiSetToSkipPhase(human.getView(), phase),
                        "the local Human controller receives the real phase-label toggle");
            }
        });
    }

    private static void clickRenderedCard(final JComponent cardArea, final CardPanel expectedPanel,
            final BiFunction<Integer, Integer, CardPanel> panelAt) {
        FThreads.invokeInEdtAndWait(() -> {
            assertTrue(cardArea.isShowing(), "the actual card area is visible before the native click");
            assertTrue(expectedPanel.isDisplayEnabled(), "the real card panel accepts native input");
            final Rectangle visibleBounds = new Rectangle(expectedPanel.getCardX(), expectedPanel.getCardY(),
                    expectedPanel.getCardWidth(), expectedPanel.getCardHeight()).intersection(cardArea.getVisibleRect());
            assertFalse(visibleBounds.isEmpty(), "the target card has visible bounds in its real card area");
            final int y = visibleBounds.y + visibleBounds.height / 2;
            Point clickPoint = null;
            for (int x = visibleBounds.x; x < visibleBounds.x + visibleBounds.width; x++) {
                if (panelAt.apply(x, y) == expectedPanel) {
                    clickPoint = new Point(x, y);
                    break;
                }
            }
            assertNotNull(clickPoint, "the native mouse coordinates resolve to the expected card panel");
            cardArea.dispatchEvent(new MouseEvent(cardArea, MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(), MouseEvent.BUTTON1_DOWN_MASK,
                    clickPoint.x, clickPoint.y, 1, false, MouseEvent.BUTTON1));
            cardArea.dispatchEvent(new MouseEvent(cardArea, MouseEvent.MOUSE_RELEASED,
                    System.currentTimeMillis(), 0, clickPoint.x, clickPoint.y, 1, false, MouseEvent.BUTTON1));
        });
    }

    private static Card findOwnedSharedCard(final Player libraryOwner, final String name, final Card... excluded) {
        for (final Card card : libraryOwner.getCardsIn(ZoneType.Library)) {
            if (name.equals(card.getName())
                    && Arrays.stream(excluded).noneMatch(other -> other == card)) {
                return card;
            }
        }
        return null;
    }

    private static Deck officialDanDanDeck() {
        for (final DeckProxy proxy : DeckProxy.getAllDanDanDecks()) {
            if ("DanDan_SecretLair".equals(proxy.getName())) return proxy.getDeck();
        }
        throw new AssertionError("The official DanDan_SecretLair deck was not loaded");
    }

    private record DanDanCastReceipt(int cardId, int casterId, int turn, int paidCount, List<Byte> paidColors) { }

    private static int libraryCount(final CMatchUI ui, final PlayerView player) {
        return Integer.parseInt(((JLabel) ui.getFieldViewFor(player).getDetailsPanel().getLblLibrary()).getText());
    }

    private static int graveyardCount(final CMatchUI ui, final PlayerView player) {
        for (final JLabel label : labels(ui.getFieldViewFor(player).getDetailsPanel())) {
            final String tooltip = label.getToolTipText();
            if (tooltip != null && (tooltip.startsWith("Graveyard (") || tooltip.startsWith("Shared Graveyard ("))) {
                return Integer.parseInt(label.getText());
            }
        }
        throw new AssertionError("rendered graveyard count label was not found");
    }

    private static String zoneTooltip(final CMatchUI ui, final PlayerView player, final ZoneType zone) {
        if (zone == ZoneType.Library) {
            return ((JLabel) ui.getFieldViewFor(player).getDetailsPanel().getLblLibrary()).getToolTipText();
        }
        for (final JLabel label : labels(ui.getFieldViewFor(player).getDetailsPanel())) {
            final String tooltip = label.getToolTipText();
            if (tooltip != null && (tooltip.startsWith("Graveyard (") || tooltip.startsWith("Shared Graveyard ("))) {
                return tooltip;
            }
        }
        throw new AssertionError("rendered zone tooltip was not found for " + player.getName() + " " + zone);
    }

    private static String sharedZoneLabel(final ZoneType zone) {
        final String key = zone == ZoneType.Library ? "lblDanDanSharedLibrary" : "lblDanDanSharedGraveyard";
        final String fallback = zone == ZoneType.Library ? "Shared Library" : "Shared Graveyard";
        return Localizer.getInstance().getMessageorUseDefault(key, fallback);
    }

    private static String floatingZoneTitle(final PlayerView player, final ZoneType zone) {
        for (final Window window : Window.getWindows()) {
            if (!(window instanceof JDialog dialog) || !window.isShowing()) {
                continue;
            }
            final String title = dialog.getTitle();
            if (title != null && title.contains(player.getName())
                    && (title.contains(zone.getTranslatedName()) || title.contains(sharedZoneLabel(zone)))) {
                return title;
            }
        }
        throw new AssertionError("visible floating window not found for " + player.getName() + " " + zone);
    }

    private static CardArea cardAreaInWindow(final PlayerView player, final ZoneType zone) {
        for (final Window window : Window.getWindows()) {
            if (!(window instanceof JDialog dialog) || !window.isShowing()) {
                continue;
            }
            final String title = dialog.getTitle();
            if (title != null && title.contains(player.getName())
                    && (title.contains(zone.getTranslatedName()) || title.contains(sharedZoneLabel(zone)))) {
                final CardArea cardArea = findCardArea(dialog);
                if (cardArea != null) {
                    return cardArea;
                }
            }
        }
        throw new AssertionError("visible card area not found for " + player.getName() + " " + zone);
    }

    private static CardArea findCardArea(final Container parent) {
        if (parent instanceof CardArea cardArea) {
            return cardArea;
        }
        for (final Component child : parent.getComponents()) {
            if (child instanceof Container container) {
                final CardArea cardArea = findCardArea(container);
                if (cardArea != null) {
                    return cardArea;
                }
            }
        }
        return null;
    }

    private static void hoverCard(final CardArea cardArea, final CardPanel expectedPanel) {
        assertTrue(cardArea.isShowing(), "the real card area is visible before hover dispatch");
        assertTrue(expectedPanel.isDisplayEnabled(), "the real card panel accepts hover input");
        final Rectangle visibleCardBounds = new Rectangle(expectedPanel.getCardX(), expectedPanel.getCardY(),
                expectedPanel.getCardWidth(), expectedPanel.getCardHeight()).intersection(cardArea.getVisibleRect());
        assertFalse(visibleCardBounds.isEmpty(), "the card panel has visible bounds in its real card area");
        final int hoverY = visibleCardBounds.y + visibleCardBounds.height / 2;
        Point hoverPoint = null;
        for (int x = visibleCardBounds.x; x < visibleCardBounds.x + visibleCardBounds.width; x++) {
            if (cardArea.getCardPanel(x, hoverY) == expectedPanel) {
                hoverPoint = new Point(x, hoverY);
                break;
            }
        }
        assertNotNull(hoverPoint, "the dispatched coordinates hit the expected card panel");
        cardArea.dispatchEvent(new MouseEvent(cardArea, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0,
                hoverPoint.x, hoverPoint.y, 0, false));
    }

    private static void collectTextAreas(final Container parent, final List<JTextArea> result) {
        for (final Component child : parent.getComponents()) {
            if (child instanceof JTextArea textArea) {
                result.add(textArea);
            } else if (child instanceof Container container) {
                collectTextAreas(container, result);
            }
        }
    }

    private static int cardCountInWindow(final PlayerView player, final ZoneType zone) {
        for (final Window window : Window.getWindows()) {
            if (!(window instanceof JDialog dialog) || !window.isShowing()) {
                continue;
            }
            final String title = dialog.getTitle();
            if (title != null && title.contains(player.getName())
                    && (title.contains(zone.getTranslatedName()) || title.contains(sharedZoneLabel(zone)))) {
                return cardPanels(window).size();
            }
        }
        throw new AssertionError("visible floating window not found for " + player.getName() + " " + zone);
    }

    private static void cleanup(final CMatchUI matchUI, final FScreen previousScreen) {
        FThreads.invokeInEdtAndWait(() -> {
            FloatingZone.closeAll();
            matchUI.resetForNewMatch();
            Singletons.getControl().setCurrentScreen(previousScreen == null ? FScreen.HOME_SCREEN : previousScreen);
        });
    }

    private static void awaitHandCardReveal(final VHand hand, final int cardId, final CardPanel expectedPanel)
            throws InterruptedException {
        assertFalse(FThreads.isGuiThread(), "the hand animation wait must leave the EDT free to run");
        final long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            final boolean[] displayEnabled = new boolean[1];
            FThreads.invokeInEdtAndWait(() -> displayEnabled[0] =
                    hand.getHandArea().getCardPanel(cardId) == expectedPanel && expectedPanel.isDisplayEnabled());
            if (displayEnabled[0]) {
                return;
            }
            Thread.sleep(15);
        }
        throw new AssertionError("the real hand acquisition animation did not reveal the card panel within 5 seconds");
    }

    private static List<CardPanel> cardPanels(final Container parent) {
        final List<CardPanel> result = new ArrayList<>();
        for (final Component child : parent.getComponents()) {
            if (child instanceof CardPanel cardPanel) {
                result.add(cardPanel);
            }
            if (child instanceof Container nested) {
                result.addAll(cardPanels(nested));
            }
        }
        return result;
    }

    private static List<JLabel> labels(final Container parent) {
        final List<JLabel> result = new ArrayList<>();
        for (final Component child : parent.getComponents()) {
            if (child instanceof JLabel label) {
                result.add(label);
            }
            if (child instanceof Container nested) {
                result.addAll(labels(nested));
            }
        }
        return result;
    }

    private static void ensureDesktopUiInitialized() {
        final ForgePreferences soundPreferences = FModel.getPreferences();
        if (!soundPreferenceCaptured && soundPreferences != null) {
            originalSoundPreference = soundPreferences.getPref(FPref.UI_ENABLE_SOUNDS);
            soundPreferenceCaptured = true;
        }
        if (soundPreferences != null) {
            soundPreferences.setPref(FPref.UI_ENABLE_SOUNDS, Boolean.toString(false));
        }

        if (!desktopUiInitialized) {
            if (Singletons.getView() == null) {
                Singletons.initializeOnce(true);
            }

            final ForgePreferences preferences = FModel.getPreferences();
            final boolean checkForSnapshots = preferences.getPrefBoolean(FPref.CHECK_SNAPSHOT_AT_STARTUP);
            final boolean enableMusic = preferences.getPrefBoolean(FPref.UI_ENABLE_MUSIC);
            try {
                preferences.setPref(FPref.CHECK_SNAPSHOT_AT_STARTUP, Boolean.toString(false));
                preferences.setPref(FPref.UI_ENABLE_MUSIC, Boolean.toString(false));

                // The isolated test profile must not open the unattended first-run download prompt.
                CdnUuidCache.markBulkSyncPromptAnswered();
                assertFalse(CdnUuidCache.shouldPromptForBulkSync());

                // Follow the normal desktop bootstrap: this loads the full skin, sets the
                // controller's display, then queues FView.initialize() on the EDT.
                Singletons.getControl().initialize();
                FThreads.invokeInEdtAndWait(() -> {
                    assertNotNull(Singletons.getView().getNavigationBar());
                    assertNotNull(Singletons.getView().getPnlInsets());
                    assertNotNull(Singletons.getControl().getForgeMenu());
                    assertEquals(Singletons.getControl().getCurrentScreen(), FScreen.HOME_SCREEN);
                    assertNull(Singletons.getView().getSplash());
                    assertTrue(Singletons.getView().getFrame().isShowing());
                    assertTrue(Singletons.getView().getFrame().getWidth() > 0);
                    assertTrue(Singletons.getView().getFrame().getHeight() > 0);
                    assertTrue(Singletons.getView().getLpnDocument().getWidth() > 0);
                    assertTrue(Singletons.getView().getLpnDocument().getHeight() > 0);
                });
                desktopUiInitialized = true;
            } finally {
                preferences.setPref(FPref.CHECK_SNAPSHOT_AT_STARTUP, Boolean.toString(checkForSnapshots));
                preferences.setPref(FPref.UI_ENABLE_MUSIC, Boolean.toString(enableMusic));
            }
        }
        assertNotNull(Singletons.getView());
        assertTrue(FModel.getPreferences() != null);
        assertTrue(GuiBase.getInterface() instanceof GuiDesktop);
    }

    @AfterClass(alwaysRun = true)
    public void disposeDesktopUi() {
        try {
            if (Singletons.getView() != null) {
                FThreads.invokeInEdtAndWait(() -> {
                    FloatingZone.closeAll();
                    if (Singletons.getView().getSplash() != null) {
                        Singletons.getView().getSplash().dispose();
                    }
                    Singletons.getView().getFrame().dispose();
                });
                forge.sound.SoundSystem.instance.dispose();
            }
        } finally {
            if (soundPreferenceCaptured) {
                FModel.getPreferences().setPref(FPref.UI_ENABLE_SOUNDS, originalSoundPreference);
                soundPreferenceCaptured = false;
                originalSoundPreference = null;
            }
        }
    }
}
