package forge.gamemodes.net.client;

import forge.GuiDesktop;
import forge.Singletons;
import forge.ai.AITest;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.GameView;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.player.RegisteredPlayer;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.gamemodes.net.ProtocolMethod;
import forge.gamemodes.net.event.GuiGameEvent;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.control.FControlGameEventHandler;
import forge.gui.download.CdnUuidCache;
import forge.gui.framework.EDocID;
import forge.gui.framework.FScreen;
import forge.gui.framework.IVDoc;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.LobbyPlayerHuman;
import forge.player.PlayerControllerHuman;
import forge.screens.match.CMatchUI;
import forge.screens.match.views.VDetail;
import forge.screens.match.views.VHand;
import forge.screens.match.views.VLog;
import forge.screens.match.views.VZone;
import forge.trackable.TrackableCollection;
import forge.view.arcane.CardArea;
import forge.view.arcane.CardPanel;
import forge.view.arcane.FloatingZone;
import forge.util.CardTranslation;
import forge.util.Localizer;
import io.netty.channel.embedded.EmbeddedChannel;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import javax.swing.JDialog;
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
import java.util.EnumSet;
import java.util.List;

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
        assertLocalZoneEventRefreshesSeatDisplays(newDanDanGame());
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

    private void assertLocalZoneEventRefreshesSeatDisplays(final Game game)
            throws InterruptedException {
        ensureDesktopUiInitialized();

        final Player first = game.getPlayers().get(0);
        final Player second = game.getPlayers().get(1);
        final Zone sharedLibrary = first.getZone(ZoneType.Library);
        final Zone sharedGraveyard = first.getZone(ZoneType.Graveyard);
        assertTrue(game.getView().isDanDan());
        assertSame(second.getZone(ZoneType.Library), sharedLibrary,
                "DanDan seats share library model zones");
        assertSame(second.getZone(ZoneType.Graveyard), sharedGraveyard,
                "DanDan seats share graveyard model zones");
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
                assertEquals(cardCountInWindow(firstView, ZoneType.Library), 2);
                assertEquals(cardCountInWindow(secondView, ZoneType.Library), 2);
                assertEquals(cardCountInWindow(firstView, ZoneType.Graveyard), 1);
                assertEquals(cardCountInWindow(secondView, ZoneType.Graveyard), 1);
            });

            final Card moved = sharedLibrary.getCards().get(0);
            game.getAction().moveTo(sharedGraveyard, moved, null);
            FThreads.invokeInEdtAndWait(() -> { }); // drain the real game-event UI update queued on the EDT

            assertEquals(firstView.getZoneSize(ZoneType.Library), 1);
            assertEquals(secondView.getZoneSize(ZoneType.Library), 1);
            assertEquals(firstView.getZoneSize(ZoneType.Graveyard), 2);
            assertEquals(secondView.getZoneSize(ZoneType.Graveyard), 2);

            FThreads.invokeInEdtAndWait(() -> {
                assertEquals(libraryCount(matchUI, firstView), 1);
                assertEquals(libraryCount(matchUI, secondView), 1);
                assertEquals(graveyardCount(matchUI, firstView), 2);
                assertEquals(graveyardCount(matchUI, secondView), 2);
                assertEquals(cardCountInWindow(firstView, ZoneType.Library), 1);
                assertEquals(cardCountInWindow(secondView, ZoneType.Library), 1,
                        "the second seat's displayed library count remains correct");
                assertEquals(cardCountInWindow(firstView, ZoneType.Graveyard), 2);
                assertEquals(cardCountInWindow(secondView, ZoneType.Graveyard), 2);
            });

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
