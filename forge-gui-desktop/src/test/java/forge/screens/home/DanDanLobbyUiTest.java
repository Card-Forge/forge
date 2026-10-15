package forge.screens.home;

import forge.GuiDesktop;
import forge.LobbyPlayer;
import forge.Singletons;
import forge.ai.LobbyPlayerAi;
import forge.ai.PlayerControllerAi;
import forge.ai.AITest;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckProxy;
import forge.deck.DeckSection;
import forge.deck.DeckType;
import forge.deckchooser.FDeckChooser;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.match.GameLobby;
import forge.gamemodes.match.GameLobby.GameLobbyData;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.match.LocalLobby;
import forge.gamemodes.match.LobbySlot;
import forge.gamemodes.match.LobbySlotType;
import forge.gamemodes.net.client.ClientGameLobby;
import forge.gamemodes.net.event.UpdateLobbyPlayerEvent;
import forge.gamemodes.quest.QuestController;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.interfaces.IUpdateable;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.skin.FSkinProp;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.player.LobbyPlayerHuman;
import forge.toolbox.FCheckBox;
import forge.toolbox.FComboBoxPanel;
import forge.toolbox.FSkin;
import forge.util.Localizer;
import org.testng.annotations.Test;

import java.awt.Component;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class DanDanLobbyUiTest extends AITest {
    @Test
    public void swappedHumanSeatSelectsBundledAndCustomSharedDeckAndOnlyOwnerPersistsSleeve() {
        ensureSkinLoaded();
        final LobbyTestState oldState = LobbyTestState.capture();
        final Deck official = getOfficialDeck();
        final Deck custom = new Deck(official, "DanDan lobby swapped seat regression");
        FModel.getDecks().getConstructed().add(custom);
        try {
            FThreads.invokeInEdtAndWait(() -> {
                final LocalLobby lobby = new LocalLobby();
                lobby.getSlot(0).setType(LobbySlotType.AI);
                lobby.getSlot(1).setType(LobbySlotType.LOCAL);
                lobby.applyVariant(GameType.DanDan);
                final VLobby view = new VLobby(lobby);
                lobby.setListener(view);
                view.setPlayerChangeListener(lobby::applyToSlot);
                view.update(true);
                assertEquals(lobby.getDanDanDeckOwnerSlot(), 1);
                final FDeckChooser chooser = view.getDeckChoosers().get(1);
                assertSame(chooser, view.getDeckChoosers().get(0), "both seats show the shared chooser");
                chooser.setSelectedDeckType(DeckType.CUSTOM_DECK);
                assertTrue(chooser.getLstDecks().setSelectedString(custom.getName()));
                chooser.getLstDecks().getSelectCommand().run();
                assertEquals(lobby.getSlot(1).getDeck().getName(), custom.getName());
                assertSame(lobby.getSlot(0).getDeck(), lobby.getSlot(1).getDeck());

                final String originalSleeve = custom.getSleeveArtKey();
                final int originalOffset = custom.getSleeveArtOffset();
                try {
                    persistSleeve(view.getPlayerPanels().get(0), "Unauthorized sleeve", 2);
                    assertEquals(custom.getSleeveArtKey(), originalSleeve,
                            "the non-owner cannot mutate shared deck metadata before broadcast");
                    assertEquals(custom.getSleeveArtOffset(), originalOffset);
                    persistSleeve(view.getPlayerPanels().get(1), "Authorized sleeve", 3);
                    assertEquals(custom.getSleeveArtKey(), "Authorized sleeve");
                    assertEquals(FModel.getDecks().getConstructed().get(custom.getName()).getSleeveArtOffset(), 3);
                } finally {
                    custom.setSleeveArtKey(originalSleeve);
                    custom.setSleeveArtOffset(originalOffset);
                }

                chooser.setSelectedDeckType(DeckType.DAN_DAN_DECK);
                assertTrue(chooser.getLstDecks().setSelectedString(official.getName()));
                chooser.getLstDecks().getSelectCommand().run();
                assertEquals(lobby.getSlot(1).getDeck().getName(), official.getName());
                assertSame(lobby.getSlot(0).getDeck(), lobby.getSlot(1).getDeck());
            });
        } finally {
            FModel.getDecks().getConstructed().delete(custom.getName());
            oldState.restore();
        }
    }

    @Test
    public void formatDropdownReplacesExclusiveFormatsButRejectsOverlayConflict() {
        ensureSkinLoaded();
        final LobbyTestState oldState = LobbyTestState.capture();
        final GuiDesktop previousGui = (GuiDesktop) GuiBase.getInterface();
        GuiBase.setInterface(new DialogCapturingGui());
        try {
            FThreads.invokeInEdtAndWait(() -> {
                final LocalLobby lobby = new LocalLobby();
                final VLobby view = new VLobby(lobby);
                lobby.setListener(view);
                view.setPlayerChangeListener(lobby::applyToSlot);
                view.update(true);
                final FComboBoxPanel<GameType> formats = formatCombo(view);
                formats.setSelectedItem(GameType.Commander);
                assertTrue(lobby.hasVariant(GameType.Commander));
                formats.setSelectedItem(GameType.DanDan);
                assertTrue(lobby.hasVariant(GameType.DanDan));
                assertFalse(lobby.hasVariant(GameType.Commander));
                formats.setSelectedItem(GameType.MomirBasic);
                assertTrue(lobby.hasVariant(GameType.MomirBasic));
                assertFalse(lobby.hasVariant(GameType.DanDan));
                formats.setSelectedItem(GameType.DanDan);
                assertTrue(lobby.hasVariant(GameType.DanDan));
                assertFalse(lobby.hasVariant(GameType.MomirBasic));

                formats.setSelectedItem(GameType.Commander);
                lobby.applyVariant(GameType.Archenemy);
                formats.setSelectedItem(GameType.DanDan);
                assertTrue(lobby.hasVariant(GameType.Commander), "rejected replacement retains the old format");
                assertTrue(lobby.hasVariant(GameType.Archenemy));
                assertFalse(lobby.hasVariant(GameType.DanDan));
                assertEquals(formats.getSelectedItem(), GameType.Commander);
            });
        } finally {
            oldState.restore();
            GuiBase.setInterface(previousGui);
        }
    }

    private static Deck getOfficialDeck() {
        for (final DeckProxy proxy : DeckProxy.getAllDanDanDecks()) {
            if (proxy.getName().equals("DanDan_SecretLair")) return proxy.getDeck();
        }
        throw new AssertionError("bundled DanDan deck was absent");
    }

    @Test
    public void onlySharedDeckOwnerMayPersistSleeveAndCustomDeckRemainsWritable() {
        ensureSkinLoaded();
        final LobbyTestState oldState = LobbyTestState.capture();
        final Deck custom = new Deck(getOfficialDeck(), "DanDan lobby sleeve authority regression");
        final Path customFile = Path.of(ForgeConstants.DECK_CONSTRUCTED_DIR, custom.getName() + ".dck");
        FModel.getDecks().getConstructed().add(custom);
        try {
            FThreads.invokeInEdtAndWait(() -> {
                final TestLobby lobby = new TestLobby(null);
                lobby.applyVariant(GameType.DanDan);
                final VLobby view = new VLobby(lobby);
                lobby.setListener(view);
                view.setPlayerChangeListener(lobby::applyToSlot);
                view.update(true);
                final FDeckChooser chooser = view.getDeckChoosers().get(0);
                chooser.setSelectedDeckType(DeckType.CUSTOM_DECK);
                assertTrue(chooser.getLstDecks().setSelectedString(custom.getName()));
                chooser.getLstDecks().getSelectCommand().run();
                assertSame(lobby.getSlot(0).getDeck(), custom);
                assertSame(lobby.getSlot(1).getDeck(), custom);

                final String beforeKey = custom.getSleeveArtKey();
                final int beforeOffset = custom.getSleeveArtOffset();
                persistSleeve(view.getPlayerPanels().get(1), "Rejected remote sleeve", 7);
                assertEquals(custom.getSleeveArtKey(), beforeKey);
                assertEquals(custom.getSleeveArtOffset(), beforeOffset);
                persistSleeve(view.getPlayerPanels().get(0), "Saved custom sleeve", 5);
                assertEquals(custom.getSleeveArtKey(), "Saved custom sleeve");
                try {
                    assertTrue(Files.readString(customFile).contains("Saved custom sleeve"),
                            "an authoritative custom deck sleeve is written to user storage");
                } catch (final IOException exception) {
                    throw new AssertionError("could not read the custom deck save", exception);
                }
            });
        } finally {
            FModel.getDecks().getConstructed().delete(custom.getName());
            oldState.restore();
        }
    }

    private static void persistSleeve(final PlayerPanel panel, final String key, final int offset) {
        try {
            final Method method = PlayerPanel.class.getDeclaredMethod("persistSleeveToDeck", String.class, int.class);
            method.setAccessible(true);
            method.invoke(panel, key, offset);
        } catch (final ReflectiveOperationException exception) {
            throw new AssertionError("could not exercise the sleeve persistence path", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static FComboBoxPanel<GameType> formatCombo(final VLobby view) {
        try {
            final Field field = VLobby.class.getDeclaredField("cboFormatPanel");
            field.setAccessible(true);
            return (FComboBoxPanel<GameType>) field.get(view);
        } catch (final ReflectiveOperationException exception) {
            throw new AssertionError("could not reach the existing format dropdown", exception);
        }
    }

    private static final class DialogCapturingGui extends GuiDesktop {
        @Override
        public int showOptionDialog(final String message, final String title, final FSkinProp icon,
                                    final List<String> options, final int defaultOption) {
            return 0;
        }
    }

    @Test
    public void actualChooserRemoteReadyAndStartUseOneAuthoritativeDeck() {
        ensureSkinLoaded();
        final LobbyTestState oldState = LobbyTestState.capture();
        final GuiDesktop previousGui = (GuiDesktop) GuiBase.getInterface();
        final CapturingGui gui = new CapturingGui();
        GuiBase.setInterface(gui);
        try {
            FThreads.invokeInEdtAndWait(() -> {
                // Establish an ordinary saved selection through its real chooser first.
                // Use a separate lobby so that saved UI state does not become an already
                // chosen deck that DanDan should preserve in the following lobby.
                final TestLobby ordinaryLobby = new TestLobby(null);
                final VLobby ordinaryView = new VLobby(ordinaryLobby);
                ordinaryLobby.setListener(ordinaryView);
                ordinaryView.update(true);
                final FDeckChooser ordinaryChooser = ordinaryView.getDeckChoosers().get(0);
                ordinaryChooser.setSelectedDeckType(DeckType.COLOR_DECK);
                assertTrue(ordinaryChooser.getLstDecks().getItemCount() > 1);
                ordinaryChooser.getLstDecks().setSelectedIndex(1);
                final String savedConstructedState = FModel.getPreferences().getPref(FPref.CONSTRUCTED_DECK_STATES[0]);
                assertTrue(savedConstructedState.startsWith("COLOR_DECK;"));

                final TestLobby lobby = new TestLobby(null);
                lobby.applyVariant(GameType.DanDan);
                final VLobby view = new VLobby(lobby);
                lobby.setListener(view);
                final List<UpdateLobbyPlayerEvent> sentEvents = new ArrayList<>();
                view.setPlayerChangeListener((index, event) -> {
                    sentEvents.add(event);
                    lobby.applyToSlot(index, event);
                });
                view.update(true);

                final Deck official = lobby.getSlot(0).getDeck();
                assertNotNull(official);
                assertEquals(official.getName(), "DanDan_SecretLair");
                assertSame(lobby.getSlot(1).getDeck(), official,
                        "a fresh lobby chooser publishes one selected deck to both seats");
                assertEquals(view.getDeckChoosers().get(0).getSelectedDeckType(), DeckType.DAN_DAN_DECK);
                assertEquals(FModel.getPreferences().getPref(FPref.CONSTRUCTED_DECK_STATES[0]), savedConstructedState,
                        "DanDan selection does not overwrite the ordinary chooser's saved state");

                clickReady(view.getPlayerPanels().get(1));
                assertTrue(lobby.getSlot(1).isReady(),
                        "the remote Ready control accepts the host's authoritative deck without a local selection");
                assertTrue(sentEvents.stream().anyMatch(e -> Boolean.TRUE.equals(e.getReady()) && e.getDeck() == null),
                        "the guest sends readiness without uploading a second deck");

                final FDeckChooser chooser = view.getDeckChoosers().get(0);
                chooser.setSelectedDeckType(DeckType.PRECONSTRUCTED_DECK);
                final DeckProxy explicitProxy = getCompatiblePreconstructedDeck(chooser.getDeck().getName());
                assertNotNull(explicitProxy, "a compatible preconstructed deck is available through the chooser");
                assertTrue(chooser.getLstDecks().setSelectedString(explicitProxy.toString()));
                assertEquals(chooser.getDeck().getName(), explicitProxy.getName());
                final Deck explicitlyChosen = lobby.getSlot(0).getDeck();
                assertNotNull(explicitlyChosen);
                assertEquals(explicitlyChosen.getName(), explicitProxy.getName());
                assertSame(lobby.getSlot(1).getDeck(), explicitlyChosen);
                assertFalse(lobby.getSlot(0).isReady());
                assertFalse(lobby.getSlot(1).isReady(), "changing the host selection resets online readiness");

                view.update(true);
                assertSame(lobby.getSlot(0).getDeck(), explicitlyChosen,
                        "full refresh preserves the compatible deck chosen through the real chooser");
                assertSame(lobby.getSlot(1).getDeck(), explicitlyChosen);
                assertEquals(view.getDeckChoosers().get(0).getSelectedDeckType(), DeckType.PRECONSTRUCTED_DECK);
                clickReady(view.getPlayerPanels().get(0));
                assertTrue(lobby.getSlot(0).isReady(), "the local host can ready the shared deck");
                clickReady(view.getPlayerPanels().get(1));
                assertTrue(lobby.getSlot(1).isReady(), "the remote can ready again after the host changes decks");

                gui.captured.clear();
                lobby.startGame().run();
                assertEquals(gui.captured.size(), 2);
                assertEquals(gui.captured.get(0).getDeck().getName(), explicitlyChosen.getName());
                assertEquals(gui.captured.get(1).getDeck().get(DeckSection.Main),
                        explicitlyChosen.get(DeckSection.Main));
                assertEquals(gui.captured.get(0).getDeck().get(DeckSection.Main),
                        explicitlyChosen.get(DeckSection.Main));

                lobby.applyVariant(GameType.Planechase);
                assertTrue(lobby.getSlot(0).getDeck().get(DeckSection.Planes).countAll() > 0,
                        "DanDan full refresh still initializes the compatible Planechase auxiliary section");
                assertSame(lobby.getSlot(1).getDeck(), lobby.getSlot(0).getDeck());

                assertClientSnapshotsDoNotUploadSharedPlanechaseDeck(lobby);

                final TestLobby client = new TestLobby(null);
                client.setGameType(GameType.DanDan);
                client.setData(lobby.getData());
                assertEquals(client.getGameType(), GameType.DanDan);
                lobby.removeVariant(GameType.Planechase);
                lobby.removeVariant(GameType.DanDan);
                client.setData(lobby.getData());
                assertEquals(client.getGameType(), GameType.Constructed,
                        "an incoming host snapshot that removes DanDan also resets the client's current chooser mode");

                lobby.removeVariant(GameType.DanDan);
                view.update(true);
                assertEquals(view.getDeckChoosers().get(0).getSelectedDeckType(), DeckType.COLOR_DECK,
                        "the ordinary chooser restores its prior saved selection after DanDan");
                assertEquals(FModel.getPreferences().getPref(FPref.CONSTRUCTED_DECK_STATES[0]), savedConstructedState);

                // Verify the serialized state can initialize a brand new ordinary chooser,
                // rather than only observing the original chooser retained in this view's cache.
                final TestLobby freshOrdinaryLobby = new TestLobby(null);
                final VLobby freshOrdinaryView = new VLobby(freshOrdinaryLobby);
                freshOrdinaryLobby.setListener(freshOrdinaryView);
                freshOrdinaryView.update(true);
                assertEquals(freshOrdinaryView.getDeckChoosers().get(0).getSelectedDeckType(), DeckType.COLOR_DECK,
                        "a new ordinary chooser restores the complete saved state written before DanDan");
            });
        } finally {
            oldState.restore();
            GuiBase.setInterface(previousGui);
        }
    }

    @Test
    public void localLobbyHumanSeatsKeepAutomaticReadinessAndStart() {
        ensureSkinLoaded();
        final LobbyTestState oldState = LobbyTestState.capture();
        final GuiDesktop previousGui = (GuiDesktop) GuiBase.getInterface();
        final CapturingGui gui = new CapturingGui();
        GuiBase.setInterface(gui);
        try {
            FThreads.invokeInEdtAndWait(() -> {
                final LocalLobby lobby = new LocalLobby();
                final VLobby view = new VLobby(lobby);
                lobby.setListener(view);
                view.setPlayerChangeListener(lobby::applyToSlot);
                view.update(true);
                lobby.applyVariant(GameType.DanDan);

                final PlayerPanel secondSeat = view.getPlayerPanels().get(1);
                secondSeat.setType(LobbySlotType.LOCAL);
                view.firePlayerChangeListener(1);

                assertTrue(lobby.getSlot(0).isReady());
                assertTrue(lobby.getSlot(1).isReady(), "a newly human local seat is auto-ready without a Ready control");
                final Runnable start = lobby.startGame();
                assertNotNull(start, "the actual LocalLobby can start two human DanDan seats");
                start.run();
                assertEquals(gui.captured.size(), 2);
                assertEquals(gui.captured.get(0).getDeck().getName(), gui.captured.get(1).getDeck().getName());
                assertEquals(gui.captured.get(0).getDeck().get(DeckSection.Main),
                        gui.captured.get(1).getDeck().get(DeckSection.Main));
            });
        } finally {
            oldState.restore();
            GuiBase.setInterface(previousGui);
        }
    }

    @Test
    public void localLobbyStartsWithItsExistingAiSeat() {
        ensureSkinLoaded();
        final LobbyTestState oldState = LobbyTestState.capture();
        final GuiDesktop previousGui = (GuiDesktop) GuiBase.getInterface();
        final CapturingGui gui = new CapturingGui();
        GuiBase.setInterface(gui);
        try {
            FThreads.invokeInEdtAndWait(() -> {
                final LocalLobby lobby = new LocalLobby();
                final VLobby view = new VLobby(lobby);
                lobby.setListener(view);
                view.setPlayerChangeListener(lobby::applyToSlot);
                view.update(true);
                lobby.applyVariant(GameType.DanDan);

                assertEquals(lobby.getSlot(0).getType(), LobbySlotType.LOCAL);
                assertEquals(lobby.getSlot(1).getType(), LobbySlotType.AI);
                final Runnable start = lobby.startGame();
                assertNotNull(start, "the desktop LocalLobby accepts its one local human and existing AI seat");
                start.run();

                assertEquals(gui.captured.size(), 2);
                assertTrue(gui.captured.get(0).getPlayer() instanceof LobbyPlayerHuman);
                assertTrue(gui.captured.get(1).getPlayer() instanceof LobbyPlayerAi,
                        "the ordinary lobby factory constructs the existing AI player");
                assertNotNull(gui.match.game);
                assertTrue(gui.match.game.getPlayers().get(1).getController() instanceof PlayerControllerAi,
                        "the started match uses the normal AI controller");
            });
        } finally {
            oldState.restore();
            GuiBase.setInterface(previousGui);
        }
    }

    private static void assertClientSnapshotsDoNotUploadSharedPlanechaseDeck(final TestLobby host) {
        final ClientGameLobby client = new ClientGameLobby();
        client.setLocalPlayer(1);
        final VLobby clientView = new VLobby(client);
        client.setListener(clientView);
        final List<UpdateLobbyPlayerEvent> outbound = new ArrayList<>();
        clientView.setPlayerChangeListener((index, event) -> outbound.add(event));

        final GameLobbyData snapshot = copyLobbyData(host.getData());
        for (int i = 0; i < 3; i++) {
            client.setData(copyLobbyData(snapshot));
        }
        assertEquals(client.getSlot(0).getDeck().getName(), host.getSlot(0).getDeck().getName());
        assertEquals(client.getSlot(1).getDeck().getName(), host.getSlot(0).getDeck().getName(),
                "repeated guest snapshots keep the host's shared deck name visible on both seats");
        final String outboundSummary = outbound.stream()
                .map(event -> "deck=" + (event.getDeck() == null ? "" : event.getDeck().getName())
                        + ",section=" + event.getSection() + ",deckName=" + event.getDeckName()
                        + ",planes=" + event.getPlanarDeckName())
                .toList().toString();
        assertTrue(outbound.stream().noneMatch(DanDanLobbyUiTest::isDeckOrSectionUpdate),
                "rendering repeated DanDan + Planechase snapshots must not upload a deck or section: " + outboundSummary);

        clickReady(clientView.getPlayerPanels().get(1));
        assertTrue(outbound.stream().anyMatch(event -> Boolean.TRUE.equals(event.getReady())
                        && !isDeckOrSectionUpdate(event)),
                "a guest can still send Ready while rendering the shared deck");
        assertTrue(outbound.stream().noneMatch(DanDanLobbyUiTest::isDeckOrSectionUpdate));

        final int[] hostUpdates = {0};
        host.setListener(new IUpdateable() {
            @Override public void update(final boolean fullUpdate) { hostUpdates[0]++; }
            @Override public void update(final int slot, final LobbySlotType type) { hostUpdates[0]++; }
        });
        final int beforeRejectedDeck = hostUpdates[0];
        host.applyToSlot(1, UpdateLobbyPlayerEvent.deckUpdate(DeckSection.Planes, new CardPool()));
        assertEquals(hostUpdates[0], beforeRejectedDeck,
                "an ignored guest section upload alone must not publish another lobby snapshot");
        assertEquals(host.getSlot(1).getDeck().getName(), host.getSlot(0).getDeck().getName());

        host.applyToSlot(1, UpdateLobbyPlayerEvent.nameUpdate("Guest renamed"));
        assertEquals(host.getSlot(1).getName(), "Guest renamed");
        assertEquals(hostUpdates[0], beforeRejectedDeck + 1,
                "legitimate guest name updates still publish");
        final boolean readyAfterUpdate = !host.getSlot(1).isReady();
        host.applyToSlot(1, UpdateLobbyPlayerEvent.isReadyUpdate(readyAfterUpdate));
        assertEquals(host.getSlot(1).isReady(), readyAfterUpdate);
        assertEquals(hostUpdates[0], beforeRejectedDeck + 2,
                "legitimate guest readiness updates still publish");
    }

    private static boolean isDeckOrSectionUpdate(final UpdateLobbyPlayerEvent event) {
        return event.getDeck() != null || event.getSection() != null;
    }

    private static GameLobbyData copyLobbyData(final GameLobbyData snapshot) {
        try {
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
                output.writeObject(snapshot);
            }
            try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                return (GameLobbyData) input.readObject();
            }
        } catch (final Exception exception) {
            throw new AssertionError("could not copy a serialized lobby snapshot", exception);
        }
    }

    private static void clickReady(final PlayerPanel panel) {
        for (final Component component : panel.getComponents()) {
            if (component instanceof FCheckBox && ((FCheckBox) component).getText()
                    .equals(Localizer.getInstance().getMessage("lblReady"))) {
                ((FCheckBox) component).doClick();
                return;
            }
        }
        throw new AssertionError("network lobby Ready control was not present");
    }

    private static void ensureSkinLoaded() {
        if (!FSkin.isLoaded()) {
            if (Singletons.getView() == null) {
                Singletons.initializeOnce(true);
            }
            FSkin.loadFull(true);
        }
    }

    private static DeckProxy getCompatiblePreconstructedDeck(final String excludedName) {
        for (final DeckProxy proxy : DeckProxy.getAllPreconstructedDecks(QuestController.getPrecons())) {
            if (!proxy.getName().equals(excludedName)
                    && DeckFormat.DanDan.getDeckConformanceProblem(proxy.getDeck()) == null) return proxy;
        }
        return null;
    }

    private static final class TestLobby extends GameLobby {
        private TestLobby(final Deck deck) {
            addSlot(new LobbySlot(LobbySlotType.LOCAL, "Host", 0, 0, 0, false, false, Collections.emptySet()));
            addSlot(new LobbySlot(LobbySlotType.REMOTE, "Guest", 1, 1, 1, false, false, Collections.emptySet()));
            getSlot(0).setDeck(deck);
        }

        @Override public boolean hasControl() { return true; }
        @Override public boolean mayEdit(final int index) { return true; }
        @Override public boolean mayControl(final int index) { return true; }
        @Override public boolean mayRemove(final int index) { return false; }
        @Override protected IGuiGame getGui(final int index) { return null; }
        @Override protected void onGameStarted() { }
    }

    private static final class CapturingGui extends GuiDesktop {
        private final List<RegisteredPlayer> captured = new ArrayList<>();
        private final CapturingMatch match = new CapturingMatch(captured);

        @Override public HostedMatch hostMatch() { return match; }
        @Override public IGuiGame getNewGuiGame() { return null; }
    }

    private static final class CapturingMatch extends HostedMatch {
        private final List<RegisteredPlayer> captured;
        private Game game;

        private CapturingMatch(final List<RegisteredPlayer> captured) {
            this.captured = captured;
        }

        @Override
        public void startMatch(final GameType gameType, final Set<GameType> variants,
                               final List<RegisteredPlayer> players, final Map<RegisteredPlayer, IGuiGame> guis) {
            captured.clear();
            captured.addAll(players);
            final GameRules rules = new GameRules(gameType);
            rules.setAppliedVariants(variants == null ? EnumSet.noneOf(GameType.class) : variants);
            game = new Match(rules, players, "DanDan lobby boundary test").createGame();
        }

        @Override public Game getGame() { return game; }
    }

    private static final class LobbyTestState {
        private final LobbyPlayer human;
        private final String humanName;
        private final int avatarIndex;
        private final int sleeveIndex;
        private final String sleeveArtKey;
        private final int sleeveArtOffset;
        private final String playerName;
        private final String avatars;
        private final String sleeves;
        private final String constructedState0;
        private final String constructedState1;
        private final Path preferencesPath = Path.of(ForgeConstants.MAIN_PREFS_FILE);
        private final boolean preferencesFileExisted;
        private final byte[] preferencesFileBytes;

        private LobbyTestState() {
            human = GamePlayerUtil.getGuiPlayer();
            humanName = human.getName();
            avatarIndex = human.getAvatarIndex();
            sleeveIndex = human.getSleeveIndex();
            sleeveArtKey = human.getSleeveArtKey();
            sleeveArtOffset = human.getSleeveArtOffset();
            playerName = FModel.getPreferences().getPref(FPref.PLAYER_NAME);
            avatars = FModel.getPreferences().getPref(FPref.UI_AVATARS);
            sleeves = FModel.getPreferences().getPref(FPref.UI_SLEEVES);
            constructedState0 = FModel.getPreferences().getPref(FPref.CONSTRUCTED_DECK_STATES[0]);
            constructedState1 = FModel.getPreferences().getPref(FPref.CONSTRUCTED_DECK_STATES[1]);
            try {
                preferencesFileExisted = Files.exists(preferencesPath);
                preferencesFileBytes = preferencesFileExisted ? Files.readAllBytes(preferencesPath) : null;
            } catch (final IOException exception) {
                throw new AssertionError("could not snapshot persisted Forge preferences", exception);
            }
        }

        static LobbyTestState capture() { return new LobbyTestState(); }

        void restore() {
            human.setName(humanName);
            human.setAvatarIndex(avatarIndex);
            human.setSleeveIndex(sleeveIndex);
            human.setSleeveArtKey(sleeveArtKey);
            human.setSleeveArtOffset(sleeveArtOffset);
            FModel.getPreferences().setPref(FPref.PLAYER_NAME, playerName);
            FModel.getPreferences().setPref(FPref.UI_AVATARS, avatars);
            FModel.getPreferences().setPref(FPref.UI_SLEEVES, sleeves);
            FModel.getPreferences().setPref(FPref.CONSTRUCTED_DECK_STATES[0], constructedState0);
            FModel.getPreferences().setPref(FPref.CONSTRUCTED_DECK_STATES[1], constructedState1);
            try {
                if (preferencesFileExisted) {
                    Files.write(preferencesPath, preferencesFileBytes);
                } else {
                    Files.deleteIfExists(preferencesPath);
                }
            } catch (final IOException exception) {
                throw new AssertionError("could not restore persisted Forge preferences", exception);
            }
        }
    }
}
