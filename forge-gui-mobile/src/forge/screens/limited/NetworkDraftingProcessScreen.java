package forge.screens.limited;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.deck.FDeckEditor;
import forge.deck.FDeckEditor.DeckEditorConfig;
import forge.deck.FDeckEditor.FDraftLog;
import forge.game.GameType;
import forge.gamemodes.net.EventParticipant;
import forge.gamemodes.net.event.DraftPickEvent;
import forge.gui.FThreads;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.screens.FScreen;
import forge.screens.match.views.VChat;
import forge.toolbox.DraftTimerRope;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.util.Utils;

/**
 * Push-model network draft picker. Packs arrive via the lobby's forwarded
 * draftPackArrived call (this screen does NOT implement IDraftEventHandler —
 * the lobby owns the single handler registration).
 *
 * Extends FDeckEditor for the Pack N / Main (N) / Side (N) tab layout, deck
 * header, and draft-log dropdown.
 */
public final class NetworkDraftingProcessScreen extends FDeckEditor {

    private final int seatIndex;
    // Pod seats for the direction strip and log; from the host's NetworkEvent or, on a
    // client, the broadcast NetworkEventView (a client has no NetworkEvent).
    private final List<EventParticipant> participants;
    private final Consumer<DraftPickEvent> pickSender;
    private final Runnable onLeave;
    private final NetworkDraftLog draftLog;

    private final FDraftLog draftLogBuffer = new FDraftLog();
    private final VChat chat;

    private NetworkDraftPackPage networkPackPage;
    private PicksDeckSectionPage mainPicksPage;
    private int currentPackNumber;
    private int currentPickNumber;
    private boolean draftComplete;
    private PaperCard pendingPickCard;
    private int[] lastQueueDepths;

    public NetworkDraftingProcessScreen(int seatIndex, List<EventParticipant> participants,
            Consumer<DraftPickEvent> pickSender, Runnable onLeave) {
        super(new NetworkDraftEditorConfig(), new PicksDeckController(new Deck()));

        this.seatIndex  = seatIndex;
        this.participants = participants;
        this.pickSender = pickSender;
        this.onLeave    = onLeave;
        this.draftLog   = new NetworkDraftLog(seatIndex);

        draftLog.setSink(draftLogBuffer);
        deckHeader.initDraftLog(draftLogBuffer, this);
        chat = deckHeader.initChat(this);

        for (TabPage<FDeckEditor> page : tabPages) {
            if (page instanceof NetworkDraftPackPage ndpp) {
                networkPackPage = ndpp;
            } else if (page instanceof PicksDeckSectionPage pdsp && pdsp.deckSection == DeckSection.Main) {
                mainPicksPage = pdsp;
            }
        }
        networkPackPage.setPickHandler(this::onPackCardActivated);
    }

    @Override
    public FScreen getLandscapeBackdropScreen() {
        return null;
    }

    @Override
    public boolean isDrafting() {
        return !draftComplete;
    }

    void onPackCardActivated() {
        PaperCard picked = networkPackPage.getSelectedCard();
        if (picked == null) return;
        pendingPickCard = picked;
        draftLog.recordPendingSelfPick(picked, currentPackNumber, currentPickNumber);
        networkPackPage.clearPack();
        pickSender.accept(new DraftPickEvent(seatIndex, picked));
    }

    public void onPackArrived(List<PaperCard> pack, int packNumber, int pickNumber,
            int timerSeconds) {
        FThreads.invokeInEdtNowOrLater(() -> {
            pendingPickCard = null; // defensive clear in case prior onSeatPicked never fired
            currentPackNumber = packNumber;
            currentPickNumber = pickNumber;
            int podSize = participants.size();
            if (lastQueueDepths == null || lastQueueDepths.length != podSize) {
                // Seed each seat with one pack until the first SeatPicked broadcast arrives
                lastQueueDepths = new int[podSize];
                Arrays.fill(lastQueueDepths, 1);
            }
            networkPackPage.setPushedPack(pack, packNumber, timerSeconds);
            networkPackPage.updateDirection(seatIndex, participants,
                    lastQueueDepths, isPassingRight(packNumber));
            setSelectedPage(networkPackPage);
        });
    }

    public void onSeatPicked(int seat, int[] queueDepths) {
        draftLog.recordSeatPicked(seat, queueDepths, participants);
        lastQueueDepths = queueDepths.clone();
        networkPackPage.updateDirection(seatIndex, participants,
                lastQueueDepths, isPassingRight(currentPackNumber));
        if (seat == seatIndex && pendingPickCard != null) {
            addPickToMain(pendingPickCard);
            pendingPickCard = null;
        }
    }

    /** Odd packs pass right, even packs pass left (conventional booster draft). */
    private static boolean isPassingRight(int packNumber) {
        return packNumber % 2 == 1;
    }

    public void onAutoPicked(int seat, PaperCard card, int packNumber, int pickInPack) {
        draftLog.recordAutoPicked(seat, card, packNumber, pickInPack, participants);
        if (seat == seatIndex) {
            addPickToMain(card);
            networkPackPage.stopTimer();
        }
    }

    public void onDraftCompleted() {
        draftComplete = true;
        networkPackPage.stopTimer();
    }

    private void addPickToMain(PaperCard card) {
        FThreads.invokeInEdtNowOrLater(() -> {
            if (mainPicksPage != null) {
                mainPicksPage.addPick(card);
            }
        });
    }

    @Override
    public void onClose(Consumer<Boolean> canCloseCallback) {
        if (draftComplete || canCloseCallback == null) {
            chat.unsubscribe();
            super.onClose(canCloseCallback);
            return;
        }
        FOptionPane.showConfirmDialog(
                Forge.getLocalizer().getMessage("lblEndDraftConfirm"),
                Forge.getLocalizer().getMessage("lblLeaveDraft"),
                Forge.getLocalizer().getMessage("lblLeave"),
                Forge.getLocalizer().getMessage("lblCancel"),
                false,
                confirmed -> {
                    if (confirmed) {
                        chat.unsubscribe();
                        onLeave.run();
                    }
                    canCloseCallback.accept(confirmed);
                });
    }

    // Push-model pack catalog with an integrated pick timer
    static final class NetworkDraftPackPage extends DraftPackPage {
        private static final float ROPE_HEIGHT = Utils.scale(6);
        private static final float LABEL_HEIGHT = Utils.scale(18);
        private static final float COUNTDOWN_WIDTH = Utils.scale(44);
        private static final float GAP = Utils.scale(4);
        private static final float STRIP_HEIGHT = Utils.scale(20);

        private final DraftTimerRope timerRope = new DraftTimerRope();
        private final FLabel lblCountdown = new FLabel.Builder()
                .font(FSkinFont.get(11)).align(Align.left).build();
        private final DraftDirectionStrip directionStrip = new DraftDirectionStrip();
        private Runnable pickHandler;

        NetworkDraftPackPage() {
            super(new CardManager(false));
            cardManager.setShowRanking(true);
            add(timerRope);
            add(lblCountdown);
            add(directionStrip);
        }

        void updateDirection(int mySeat, List<EventParticipant> participants, int[] depths, boolean passingRight) {
            directionStrip.update(mySeat, participants, depths, passingRight);
        }

        void setPickHandler(Runnable handler) {
            this.pickHandler = handler;
            cardManager.setItemActivateHandler(e -> {
                if (pickHandler != null) pickHandler.run();
            });
        }

        void setPushedPack(List<PaperCard> pack, int packNumber, int timerSeconds) {
            String label = Forge.getLocalizer().getMessage("lblPackN", String.valueOf(packNumber));
            caption = label;
            cardManager.setCaption(label);
            cardManager.setPool(pack);
            cardManager.setEnabled(true);
            showTab();
            if (timerSeconds > 0) {
                timerRope.start(timerSeconds);
                lblCountdown.setVisible(true);
            } else {
                timerRope.stop();
                lblCountdown.setVisible(false);
            }
        }

        void clearPack() {
            cardManager.setPool(Collections.emptyList());
            cardManager.setEnabled(false);
            timerRope.stop();
            lblCountdown.setVisible(false);
        }

        void stopTimer() {
            timerRope.stop();
            lblCountdown.setVisible(false);
        }

        PaperCard getSelectedCard() {
            return cardManager.getSelectedItem();
        }

        /** No-op — pool arrives via push, not pulled from a BoosterDraft. */
        @Override
        public void refresh() {}

        /** No-op — picks are confirmed server-side; addPickToMain handles the local state. */
        @Override
        public void moveCard(PaperCard card, CardManagerPage destination, int qty) {}

        @Override
        public void draw(Graphics g) {
            int secs = timerRope.getRemainingSeconds();
            lblCountdown.setText(String.format("%02d:%02d", secs / 60, secs % 60));
            super.draw(g);
        }

        @Override
        protected void doLayout(float width, float height) {
            lblCountdown.setBounds(GAP, 0, COUNTDOWN_WIDTH, LABEL_HEIGHT);
            float ropeX = COUNTDOWN_WIDTH + 2 * GAP;
            timerRope.setBounds(ropeX, (LABEL_HEIGHT - ROPE_HEIGHT) / 2f, width - ropeX - GAP, ROPE_HEIGHT);
            directionStrip.setBounds(0, LABEL_HEIGHT, width, STRIP_HEIGHT);
            float top = LABEL_HEIGHT + STRIP_HEIGHT;
            cardManager.setBounds(0, top, width, height - top);
        }
    }

    // Neighbor pack-passing strip: left / YOU / right names, queue-depth icons on each
    // seat's incoming side, and arrows showing the pass direction for the current pack.
    static final class DraftDirectionStrip extends FDisplayObject {
        private static final float ICON_HEIGHT = Utils.scale(15);
        private static final float ARROW_WIDTH = Utils.scale(7);
        private static final float ARROW_HALF_HEIGHT = Utils.scale(4);
        private static final float SEG_GAP = Utils.scale(4);
        private static final FSkinFont FONT = FSkinFont.get(11);

        private enum Kind { TEXT, ICONS, ARROW }

        private final List<Item> items = new ArrayList<>(8);
        private float[] widths = new float[0];
        private float totalWidth;
        private boolean hasData;

        /** One drawable element: a name run, a stack of {@code depth} pack icons, or a direction arrow. */
        private static final class Item {
            final Kind kind;
            final String text;
            final int depth;
            final boolean arrowRight;

            private Item(Kind kind, String text, int depth, boolean arrowRight) {
                this.kind = kind;
                this.text = text;
                this.depth = depth;
                this.arrowRight = arrowRight;
            }

            static Item text(String t)       { return new Item(Kind.TEXT, t, 0, false); }
            static Item icons(int depth)     { return new Item(Kind.ICONS, null, depth, false); }
            static Item arrow(boolean right) { return new Item(Kind.ARROW, null, 0, right); }
        }

        void update(int mySeat, List<EventParticipant> participants, int[] depths, boolean passingRight) {
            int podSize = participants == null ? 0 : participants.size();
            if (podSize < 2 || depths == null || depths.length != podSize
                    || mySeat < 0 || mySeat >= podSize) {
                hasData = false;
                return;
            }
            int leftIdx  = (mySeat - 1 + podSize) % podSize;
            int rightIdx = (mySeat + 1) % podSize;
            String you   = Forge.getLocalizer().getMessage("lblDraftOverlayYou");
            String left  = EventParticipant.resolveName(leftIdx,  participants, null);
            String right = EventParticipant.resolveName(rightIdx, participants, null);

            // Icons sit on each seat's incoming side: left when passing right, right when passing left
            items.clear();
            if (passingRight) {
                addIcons(depths[leftIdx]);
                items.add(Item.text(left));
                items.add(Item.arrow(true));
                addIcons(depths[mySeat]);
                items.add(Item.text(you));
                items.add(Item.arrow(true));
                addIcons(depths[rightIdx]);
                items.add(Item.text(right));
            } else {
                items.add(Item.text(left));
                addIcons(depths[leftIdx]);
                items.add(Item.arrow(false));
                items.add(Item.text(you));
                addIcons(depths[mySeat]);
                items.add(Item.arrow(false));
                items.add(Item.text(right));
                addIcons(depths[rightIdx]);
            }

            widths = new float[items.size()];
            totalWidth = 0;
            for (int i = 0; i < items.size(); i++) {
                widths[i] = itemWidth(items.get(i));
                totalWidth += widths[i] + (i > 0 ? SEG_GAP : 0);
            }
            hasData = true;
        }

        private void addIcons(int depth) {
            if (depth > 0) items.add(Item.icons(depth));
        }

        private static float iconWidth() {
            float h = FSkinImage.PACK.getHeight();
            float aspect = h > 0 ? FSkinImage.PACK.getWidth() / h : 18f / 25f;
            return ICON_HEIGHT * aspect;
        }

        private static float itemWidth(Item it) {
            switch (it.kind) {
                case TEXT:
                    return FONT.getBounds(it.text).width;
                case ARROW:
                    return ARROW_WIDTH;
                default:
                    float w = iconWidth();
                    if (it.depth > 1) {
                        w += SEG_GAP + FONT.getBounds("x" + it.depth).width;
                    }
                    return w;
            }
        }

        @Override
        public void draw(Graphics g) {
            if (!hasData) return;
            float h = getHeight();
            FSkinColor color = FSkinColor.get(FSkinColor.Colors.CLR_TEXT);
            float x = Math.max(0, (getWidth() - totalWidth) / 2f);
            for (int i = 0; i < items.size(); i++) {
                Item it = items.get(i);
                switch (it.kind) {
                    case TEXT:
                        g.drawText(it.text, FONT, color, x, 0, widths[i], h, false, Align.left, true);
                        break;
                    case ARROW:
                        drawArrow(g, color, x, h, it.arrowRight);
                        break;
                    default:
                        g.drawImage(FSkinImage.PACK, x, (h - ICON_HEIGHT) / 2f, iconWidth(), ICON_HEIGHT);
                        if (it.depth > 1) {
                            String cnt = "x" + it.depth;
                            g.drawText(cnt, FONT, color, x + iconWidth() + SEG_GAP, 0,
                                    FONT.getBounds(cnt).width, h, false, Align.left, true);
                        }
                        break;
                }
                x += widths[i] + SEG_GAP;
            }
        }

        private static void drawArrow(Graphics g, FSkinColor color, float x, float h, boolean right) {
            float midY = h / 2f;
            if (right) {
                g.fillTriangle(color, x, midY - ARROW_HALF_HEIGHT, x, midY + ARROW_HALF_HEIGHT, x + ARROW_WIDTH, midY);
            } else {
                g.fillTriangle(color, x + ARROW_WIDTH, midY - ARROW_HALF_HEIGHT, x + ARROW_WIDTH, midY + ARROW_HALF_HEIGHT, x, midY);
            }
        }
    }

    static final class PicksDeckSectionPage extends DeckSectionPage {
        PicksDeckSectionPage(CardManager cm, DeckSection section, ItemManagerConfig config) {
            super(cm, section, config);
        }

        void addPick(PaperCard card) {
            cardManager.addItem(card, 1);
            updateCaption();
        }
    }

    private static final class NetworkDraftEditorConfig extends DeckEditorConfig {
        @Override
        public GameType getGameType() {
            return GameType.Draft;
        }

        @Override
        public boolean isLimited() { return true; }

        @Override
        public boolean isDraft() { return true; }

        @Override
        public boolean hasInfiniteCardPool() { return false; }

        @Override
        protected IDeckController getController() {
            throw new UnsupportedOperationException("NetworkDraftEditorConfig uses a directly supplied controller");
        }

        @Override
        protected DeckEditorPage[] getInitialPages() {
            NetworkDraftPackPage packPage = new NetworkDraftPackPage();
            PicksDeckSectionPage mainPage = new PicksDeckSectionPage(new CardManager(false), DeckSection.Main, ItemManagerConfig.DRAFT_POOL);
            PicksDeckSectionPage sidePage = new PicksDeckSectionPage(new CardManager(false), DeckSection.Sideboard, ItemManagerConfig.DRAFT_POOL);
            return new DeckEditorPage[]{ packPage, mainPage, sidePage };
        }
    }

    // Minimal in-memory controller for the picks deck
    private static final class PicksDeckController implements IDeckController {
        private Deck deck;

        PicksDeckController(Deck initial) {
            this.deck = initial;
        }

        @Override public void setEditor(FDeckEditor editor) {
            if (deck != null) { editor.setDeck(deck); }
        }
        @Override public Deck getDeck() { return deck; }
        @Override public void setDeck(Deck deck) { this.deck = deck; }
        @Override public void newDeck() { this.deck = new Deck(); }
        @Override public String getDeckDisplayName() { return ""; }
        @Override public void notifyModelChanged() {}
        @Override public void exitWithoutSaving() {}
    }
}
