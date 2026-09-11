package forge.screens.deckeditor.controllers;

import forge.Singletons;
import forge.deck.Deck;
import forge.game.GameType;
import forge.gamemodes.limited.DraftAction;
import forge.gamemodes.net.event.DraftActivateEvent;
import forge.gamemodes.net.event.DraftPickEvent;
import forge.gamemodes.net.event.DraftSeatStateEvent;
import forge.gamemodes.net.event.NetEvent;
import forge.gui.FDraftOverlay;
import forge.gui.framework.DragCell;
import forge.gui.framework.FScreen;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.model.FModel;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.views.VAllDecks;
import forge.screens.deckeditor.views.VBrawlDecks;
import forge.screens.deckeditor.views.VCardCatalog;
import forge.screens.deckeditor.views.VCommanderDecks;
import forge.screens.deckeditor.views.VCurrentDeck;
import forge.screens.deckeditor.views.VDeckgen;
import forge.screens.deckeditor.views.VEditorLog;
import forge.screens.deckeditor.views.VOathbreakerDecks;
import forge.screens.deckeditor.views.VTinyLeadersDecks;
import forge.screens.match.controllers.CDetailPicture;
import forge.toolbox.FOptionPane;
import forge.util.ItemPool;
import forge.util.Localizer;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;

/**
 * Network draft editor controller. Works with a push model: packs arrive
 * via {@link forge.gamemodes.net.event.DraftPackArrivedEvent} and picks
 * are sent back via a {@link Consumer} callback.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public class CEditorNetworkDraft extends ACEditorBase<PaperCard, Deck> {

    private final int seatIndex;
    private final Consumer<NetEvent> pickSender;
    private final Runnable onLeave;
    private final Localizer localizer = Localizer.getInstance();

    private int currentPackNumber;
    private int currentPickNumber;
    private int currentSeq;
    private List<DraftAction> actions = List.of();
    private boolean hiddenPack;
    private String packCaption = "";
    private boolean draftComplete;

    private record PendingSelfPick(String cardName, int packNumber, int pickInPack, boolean auto) { }
    private PendingSelfPick pendingSelfPick;

    private String ccAddLabel;
    private DragCell constructedDecksParent;
    private DragCell commanderDecksParent;
    private DragCell oathbreakerDecksParent;
    private DragCell brawlDecksParent;
    private DragCell tinyLeadersDecksParent;
    private DragCell deckGenParent;

    /**
     * @param seatIndex   this player's seat in the draft pod
     * @param pickSender  callback to send draft events; for the host this calls
     *                    ServerGameLobby.routeDraftEvent directly, for
     *                    clients it sends via FGameClient
     * @param onLeave     callback fired when the user confirms "Leave" on the
     *                    mid-draft exit prompt — lets the lobby drop its
     *                    reference and dismiss the overlay
     * @param cDetailPicture0 the shared detail picture controller
     */
    public CEditorNetworkDraft(int seatIndex,
            Consumer<NetEvent> pickSender, Runnable onLeave,
            CDetailPicture cDetailPicture0) {
        super(FScreen.DRAFTING_PROCESS, cDetailPicture0, GameType.Draft);

        this.seatIndex = seatIndex;
        this.pickSender = pickSender;
        this.onLeave = onLeave;

        final CardManager catalogManager = new CardManager(cDetailPicture0, false, false, true);
        final CardManager deckManager = new CardManager(cDetailPicture0, false, false, true);

        // Hide filters so more of the pack is visible
        catalogManager.setHideViewOptions(1, true);

        deckManager.setCaption(localizer.getMessage("lblDraftPicks"));

        catalogManager.setAlwaysNonUnique(true);
        deckManager.setAlwaysNonUnique(true);

        this.setCatalogManager(catalogManager);
        this.setDeckManager(deckManager);

        // Seed state here so auto-picks arriving before update() runs don't NPE on a null pool
        catalogManager.setup(ItemManagerConfig.DRAFT_PACK);
        deckManager.setup(ItemManagerConfig.DRAFT_POOL);
        deckManager.setPool(Collections.<PaperCard>emptyList());
    }

    /**
     * Display a new pack for the player to pick from.
     *
     * @param pack       the cards in the pack
     * @param packNumber 1-based pack number
     * @param pickNumber 0-based pick number within the pack round
     * @param seq        the arrival number the pick must echo
     * @param hiddenCount when above zero, the pack is hidden and shows this many face-down cards
     */
    public void showPack(List<PaperCard> pack, int packNumber, int pickNumber, int seq, int hiddenCount) {
        this.currentPackNumber = packNumber;
        this.currentPickNumber = pickNumber;
        this.currentSeq = seq;
        this.hiddenPack = hiddenCount > 0;

        ItemPool<PaperCard> pool = new ItemPool<>(PaperCard.class);
        if (hiddenPack) {
            pool.add(PaperCard.FAKE_CARD, hiddenCount);
        } else {
            for (PaperCard card : pack) {
                pool.add(card, 1);
            }
        }

        packCaption = localizer.getMessage("lblPackNCards", String.valueOf(packNumber));
        this.getCatalogManager().setPool(pool);
        this.getCatalogManager().refresh();
        showAbilityHints(actions, packCaption);
    }

    @Override
    protected void onAddItems(Iterable<Entry<PaperCard, Integer>> items, boolean toAlternate) {
        if (toAlternate || draftComplete) {
            return;
        }

        // Only one card per invocation — draft flow picks single cards, not groups
        Iterator<Entry<PaperCard, Integer>> it = items.iterator();
        if (!it.hasNext()) return;
        submitPick(it.next().getKey(), null);
    }

    private void submitPick(PaperCard card, DraftAction variant) {
        // The host draws a hidden pick itself, so no card is sent
        pickSender.accept(new DraftPickEvent(seatIndex, currentSeq, hiddenPack ? null : card, variant));

        // Deferred log: flushed on the server's SeatPicked echo so queue-depth data is authoritative
        String name = hiddenPack ? localizer.getMessage("lblFaceDownCard") : card.getName();
        pendingSelfPick = new PendingSelfPick(name, currentPackNumber, currentPickNumber + 1, false);

        this.getCatalogManager().setPool(Collections.<PaperCard>emptyList());
        FDraftOverlay.SINGLETON_INSTANCE.onPickSubmitted();
    }

    public void addAutoPickedCard(PaperCard card, int packNumber, int pickInPack) {
        pendingSelfPick = new PendingSelfPick(card.getName(), packNumber, pickInPack, true);
        FDraftOverlay.SINGLETON_INSTANCE.onPickSubmitted();
    }

    /** The host owns the pool: picks, removals, returns and trades all arrive here. */
    public void applySeatState(DraftSeatStateEvent state) {
        if (state.isFull()) {
            getDeckManager().setPool(state.getPoolAdded());
        } else {
            state.getPoolAdded().forEach(c -> getDeckManager().addItem(c, 1));
            state.getPoolRemoved().forEach(c -> getDeckManager().removeItem(c, 1));
        }
        actions = state.getActions();
        showAbilityHints(actions, packCaption);
    }

    public void flushSelfPickLog(int queueDepth) {
        PendingSelfPick p = pendingSelfPick;
        if (p == null) return;
        pendingSelfPick = null;
        NetworkDraftLog.logMyPick(p.cardName, p.packNumber, p.pickInPack, queueDepth, p.auto);
    }

    @Override
    protected void onRemoveItems(Iterable<Entry<PaperCard, Integer>> items, boolean toAlternate) {
        // Cannot remove cards during draft
    }

    @Override
    protected void buildAddContextMenu(EditorContextMenuBuilder cmb) {
        cmb.addMoveItems(localizer.getMessage("lblDraft"), null);
        if (!draftComplete) {
            cmb.addDraftActionItems(actions, action -> submitPick(action.target(), action));
        }
    }

    @Override
    protected void buildRemoveContextMenu(EditorContextMenuBuilder cmb) {
        if (!draftComplete) {
            cmb.addDraftActionItems(actions, action -> pickSender.accept(new DraftActivateEvent(seatIndex, action)));
        }
    }

    /**
     * Called when the draft is complete and the pool arrives from the server.
     *
     * @param pool the drafted pool (Sideboard section holds picks + event metadata tags)
     */
    public void completeDraft(Deck pool) {
        draftComplete = true;
        FModel.getDecks().getNetworkEventDecks().add(pool);

        int totalCards = 0;
        if (getDeckManager().getPool() != null) {
            totalCards = getDeckManager().getPool().countAll();
        }
        NetworkDraftLog.logDraftComplete(totalCards);
        FDraftOverlay.SINGLETON_INSTANCE.reset();
        FScreen.DRAFTING_PROCESS.close();

        FScreen editScreen = FScreen.DECK_EDITOR_DRAFT;
        CEditorLimited<Deck> editorCtrl = new CEditorLimited<>(
                FModel.getDecks().getNetworkEventDecks(), Deck::new, editScreen, getCDetailPicture());
        Singletons.getControl().setCurrentScreen(editScreen);
        CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(editorCtrl);
        editorCtrl.getDeckController().load(null, pool.getName());

        FOptionPane.showMessageDialog(localizer.getMessage("lblDraftCompletePoolSaved", pool.getName()));
    }

    @Override
    protected CardLimit getCardLimit() {
        return CardLimit.None;
    }

    @Override
    public DeckController<Deck> getDeckController() {
        return null;
    }

    @Override
    public void resetTables() {
    }

    @Override
    public void update() {
        if (VEditorLog.SINGLETON_INSTANCE.getParentCell() == null) {
            VCardCatalog.SINGLETON_INSTANCE.getParentCell().addDoc(VEditorLog.SINGLETON_INSTANCE);
            VEditorLog.SINGLETON_INSTANCE.showView();
        }

        ccAddLabel = this.getBtnAdd().getText();

        this.getBtnAdd().setVisible(false);
        this.getBtnAdd4().setVisible(false);
        this.getBtnRemove().setVisible(false);
        this.getBtnRemove4().setVisible(false);

        this.getCbxSection().setVisible(false);

        VCurrentDeck.SINGLETON_INSTANCE.getPnlHeader().setVisible(false);

        deckGenParent = removeTab(VDeckgen.SINGLETON_INSTANCE);
        constructedDecksParent = removeTab(VAllDecks.SINGLETON_INSTANCE);
        commanderDecksParent = removeTab(VCommanderDecks.SINGLETON_INSTANCE);
        oathbreakerDecksParent = removeTab(VOathbreakerDecks.SINGLETON_INSTANCE);
        brawlDecksParent = removeTab(VBrawlDecks.SINGLETON_INSTANCE);
        tinyLeadersDecksParent = removeTab(VTinyLeadersDecks.SINGLETON_INSTANCE);

        // One pick per click — draft flow doesn't support group-picking
        getCatalogManager().setAllowMultipleSelections(false);
    }

    @Override
    public boolean canSwitchAway(boolean isClosing) {
        if (isClosing && !draftComplete) {
            String userPrompt = localizer.getMessage("lblEndDraftConfirm");
            boolean leaving = FOptionPane.showConfirmDialog(userPrompt,
                    localizer.getMessage("lblLeaveDraft"),
                    localizer.getMessage("lblLeave"),
                    localizer.getMessage("lblCancel"), false);
            if (leaving && onLeave != null) onLeave.run();
            return leaving;
        }
        return true;
    }

    @Override
    public void resetUIChanges() {
        this.getBtnAdd().setText(ccAddLabel);
        this.getBtnAdd4().setVisible(true);
        this.getBtnRemove().setVisible(true);
        this.getBtnRemove4().setVisible(true);

        VCurrentDeck.SINGLETON_INSTANCE.getPnlHeader().setVisible(true);
        VEditorLog.SINGLETON_INSTANCE.getParentCell().setVisible(true);

        if (deckGenParent != null) {
            deckGenParent.addDoc(VDeckgen.SINGLETON_INSTANCE);
        }
        if (constructedDecksParent != null) {
            constructedDecksParent.addDoc(VAllDecks.SINGLETON_INSTANCE);
        }
        if (commanderDecksParent != null) {
            commanderDecksParent.addDoc(VCommanderDecks.SINGLETON_INSTANCE);
        }
        if (oathbreakerDecksParent != null) {
            oathbreakerDecksParent.addDoc(VOathbreakerDecks.SINGLETON_INSTANCE);
        }
        if (brawlDecksParent != null) {
            brawlDecksParent.addDoc(VBrawlDecks.SINGLETON_INSTANCE);
        }
        if (tinyLeadersDecksParent != null) {
            tinyLeadersDecksParent.addDoc(VTinyLeadersDecks.SINGLETON_INSTANCE);
        }

        getCatalogManager().setAllowMultipleSelections(true);
    }
}
