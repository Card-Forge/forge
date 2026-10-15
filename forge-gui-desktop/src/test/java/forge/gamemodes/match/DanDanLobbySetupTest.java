package forge.gamemodes.match;

import forge.GuiDesktop;
import forge.LobbyPlayer;
import forge.ai.AITest;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.game.GameType;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.interfaces.IUpdateable;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.skin.FSkinProp;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.gamemodes.net.event.UpdateLobbyPlayerEvent;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class DanDanLobbySetupTest extends AITest {
    @Test
    public void bundledDeckSleeveSaveDoesNotWritePackagedResource() throws IOException {
        final Path bundledFile = Path.of(ForgeConstants.DECK_DANDAN_DIR, "DanDan_SecretLair.dck");
        final byte[] before = Files.readAllBytes(bundledFile);
        final DeckProxy proxy = findOfficialProxy();
        final Deck deck = proxy.getDeck();
        final String oldKey = deck.getSleeveArtKey();
        final int oldOffset = deck.getSleeveArtOffset();
        try {
            deck.setSleeveArtKey("Storage regression sleeve");
            deck.setSleeveArtOffset(4);
            assertFalse(proxy.saveDeck(), "bundled catalog must refuse persistence");
            assertEquals(Files.readAllBytes(bundledFile), before,
                    "the shipped deck file must remain byte-for-byte unchanged");
        } finally {
            deck.setSleeveArtKey(oldKey);
            deck.setSleeveArtOffset(oldOffset);
            Files.write(bundledFile, before);
        }
    }

    private static DeckProxy findOfficialProxy() {
        for (final DeckProxy proxy : DeckProxy.getAllDanDanDecks()) {
            if (proxy.getName().equals("DanDan_SecretLair")) return proxy;
        }
        throw new AssertionError("bundled DanDan deck was absent");
    }

    @Test
    public void hostDeckIsSharedAndRemoteCannotUploadASecondDeck() {
        final GuiPlayerState oldPlayerState = GuiPlayerState.capture();
        try {
            final Deck rejectedRemoteDeck = new Deck("remote choice");
            final Deck explicitCompatibleDeck = new Deck("explicit compatible deck");
            final TestLobby lobby = new TestLobby(explicitCompatibleDeck);
            final CountingUpdateable updates = new CountingUpdateable();
            lobby.setListener(updates);

            lobby.applyVariant(GameType.DanDan);
            assertSame(lobby.getSlot(0).getDeck(), explicitCompatibleDeck,
                "choosing DanDan must preserve an explicitly selected compatible deck");
            assertSame(lobby.getSlot(1).getDeck(), explicitCompatibleDeck);
            assertFalse(lobby.getSlot(0).isReady());
            assertFalse(lobby.getSlot(1).isReady());

            lobby.applyToSlot(1, UpdateLobbyPlayerEvent.isReadyUpdate(true));
            assertTrue(lobby.getSlot(1).isReady(), "remote may ready using the host's shared deck");
            lobby.applyToSlot(1, UpdateLobbyPlayerEvent.deckUpdate(rejectedRemoteDeck));
            assertSame(lobby.getSlot(1).getDeck(), explicitCompatibleDeck, "remote deck uploads must not replace the host deck");

            final Deck replacement = getOfficialDeckFromChooserSource();
            lobby.applyToSlot(0, UpdateLobbyPlayerEvent.deckUpdate(replacement));
            assertSame(lobby.getSlot(0).getDeck(), replacement);
            assertSame(lobby.getSlot(1).getDeck(), replacement);
            assertEquals(lobby.getSlot(1).getDeckName(), replacement.getName());
            assertFalse(lobby.getSlot(0).isReady(), "changing the shared deck resets host readiness");
            assertFalse(lobby.getSlot(1).isReady(), "changing the shared deck resets remote readiness");

            lobby.applyToSlot(0, UpdateLobbyPlayerEvent.isReadyUpdate(true));
            lobby.applyToSlot(1, UpdateLobbyPlayerEvent.isReadyUpdate(true));
            assertNotNull(lobby.startGame(), "one ready local and one ready remote seat can start");
            assertTrue(updates.fullOrPartialUpdates > 0, "authoritative deck changes are published through the lobby update path");
        } finally {
            oldPlayerState.restore();
        }
    }

    @Test
    public void unsupportedSeatsAreRejectedAndPlanechaseRemainsCompatible() {
        final Deck official = getOfficialDeckFromChooserSource();
        final GuiDesktop originalGui = (GuiDesktop) GuiBase.getInterface();
        final CapturingGui testGui = new CapturingGui();
        GuiBase.setInterface(testGui);
        try {
            final TestLobby thirdSeat = new TestLobby(official);
            thirdSeat.applyVariant(GameType.DanDan);
            thirdSeat.addSeat(LobbySlotType.AI, "Computer", 2);
            assertNullStart(thirdSeat);

            final TestLobby openSeat = new TestLobby(official);
            openSeat.applyVariant(GameType.DanDan);
            openSeat.getSlot(1).setType(LobbySlotType.OPEN);
            assertNullStart(openSeat);

            final TestLobby aiSeat = new TestLobby(official);
            aiSeat.applyVariant(GameType.DanDan);
            aiSeat.getSlot(1).setType(LobbySlotType.AI);
            assertNullStart(aiSeat);

            final LocalLobby aiOnly = new LocalLobby();
            aiOnly.applyVariant(GameType.DanDan);
            aiOnly.getSlot(0).setType(LobbySlotType.AI);
            assertNullStart(aiOnly);

            final TestLobby conflict = new TestLobby(official);
            conflict.applyVariant(GameType.Commander);
            conflict.applyVariant(GameType.DanDan);
            assertTrue(conflict.hasVariant(GameType.Commander));
            assertFalse(conflict.hasVariant(GameType.DanDan), "a verified deck/start-rule conflict is rejected before mode mutation");
            assertTrue(testGui.lastMessage.contains("Commander"));

            final TestLobby compatible = new TestLobby(official);
            compatible.applyVariant(GameType.DanDan);
            compatible.applyVariant(GameType.Planechase);
            assertTrue(compatible.hasVariant(GameType.DanDan));
            assertTrue(compatible.hasVariant(GameType.Planechase));

        } finally {
            GuiBase.setInterface(originalGui);
        }
    }

    private static void assertNullStart(final GameLobby lobby) {
        final Deck originalDeck = lobby.getSlot(0).getDeck();
        assertTrue(lobby.startGame() == null, "unsupported DanDan seating cannot produce a start action");
        assertSame(lobby.getSlot(0).getDeck(), originalDeck, "rejection must happen before deck/player mutation");
    }

    private static Deck getOfficialDeckFromChooserSource() {
        for (DeckProxy proxy : DeckProxy.getAllDanDanDecks()) {
            if (proxy.getName().equals("DanDan_SecretLair")) return proxy.getDeck();
        }
        return null;
    }

    private static final class TestLobby extends GameLobby {
        private TestLobby(final Deck initialDeck) {
            addSlot(new LobbySlot(LobbySlotType.LOCAL, "Human", 0, 0, 0, false, false, Collections.emptySet()));
            addSlot(new LobbySlot(LobbySlotType.REMOTE, "Guest", 1, 1, 1, false, false, Collections.emptySet()));
            getSlot(0).setDeck(initialDeck);
        }

        private void addSeat(final LobbySlotType type, final String name, final int index) {
            addSlot(new LobbySlot(type, name, index, index, index, false, true, Collections.emptySet()));
        }

        @Override public boolean hasControl() { return true; }
        @Override public boolean mayEdit(final int index) { return true; }
        @Override public boolean mayControl(final int index) { return true; }
        @Override public boolean mayRemove(final int index) { return false; }
        @Override protected IGuiGame getGui(final int index) { return null; }
        @Override protected void onGameStarted() { }
    }

    private static final class CapturingGui extends GuiDesktop {
        private String lastMessage = "";

        @Override
        public int showOptionDialog(final String message, final String title, final FSkinProp icon,
                                    final List<String> options, final int defaultOption) {
            lastMessage = message;
            return 0;
        }
    }

    private static final class CountingUpdateable implements IUpdateable {
        private int fullOrPartialUpdates;

        @Override public void update(final boolean fullUpdate) { fullOrPartialUpdates++; }
        @Override public void update(final int slot, final LobbySlotType type) { fullOrPartialUpdates++; }
    }

    private static final class GuiPlayerState {
        private final LobbyPlayer human = GamePlayerUtil.getGuiPlayer();
        private final String name = human.getName();
        private final int avatar = human.getAvatarIndex();
        private final int sleeve = human.getSleeveIndex();
        private final String sleeveArtKey = human.getSleeveArtKey();
        private final int sleeveArtOffset = human.getSleeveArtOffset();
        private final String playerName = FModel.getPreferences().getPref(FPref.PLAYER_NAME);
        private final String avatars = FModel.getPreferences().getPref(FPref.UI_AVATARS);
        private final String sleeves = FModel.getPreferences().getPref(FPref.UI_SLEEVES);
        private final Path preferencesPath = Path.of(ForgeConstants.MAIN_PREFS_FILE);
        private final boolean preferencesFileExisted;
        private final byte[] preferencesFileBytes;

        private GuiPlayerState() {
            try {
                preferencesFileExisted = Files.exists(preferencesPath);
                preferencesFileBytes = preferencesFileExisted ? Files.readAllBytes(preferencesPath) : null;
            } catch (final IOException exception) {
                throw new AssertionError("could not snapshot persisted Forge preferences", exception);
            }
        }

        static GuiPlayerState capture() { return new GuiPlayerState(); }

        void restore() {
            human.setName(name);
            human.setAvatarIndex(avatar);
            human.setSleeveIndex(sleeve);
            human.setSleeveArtKey(sleeveArtKey);
            human.setSleeveArtOffset(sleeveArtOffset);
            FModel.getPreferences().setPref(FPref.PLAYER_NAME, playerName);
            FModel.getPreferences().setPref(FPref.UI_AVATARS, avatars);
            FModel.getPreferences().setPref(FPref.UI_SLEEVES, sleeves);
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
