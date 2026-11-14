/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.screens.deckeditor.controllers;

import forge.util.Localizer;

import java.awt.event.ActionListener;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import forge.card.MagicColor;
import forge.Singletons;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.GameType;
import forge.gamemodes.rogue.RogueConfig;
import forge.gamemodes.rogue.RogueIO;
import forge.gamemodes.rogue.RogueRun;
import forge.gui.GuiBase;
import forge.gui.UiCommand;
import forge.gui.framework.DragCell;
import forge.gui.framework.EDocID;
import forge.gui.framework.FScreen;
import forge.localinstance.achievements.RogueCommanderAchievements;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.screens.deckeditor.AddBasicLandsDialog;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.views.VAllDecks;
import forge.screens.deckeditor.views.VBrawlDecks;
import forge.screens.deckeditor.views.VCardCatalog;
import forge.screens.deckeditor.views.VCommanderDecks;
import forge.screens.deckeditor.views.VCurrentDeck;
import forge.screens.deckeditor.views.VDeckgen;
import forge.screens.deckeditor.views.VOathbreakerDecks;
import forge.screens.deckeditor.views.VTinyLeadersDecks;
import forge.screens.home.CHomeUI;
import forge.screens.home.rogue.CSubmenuRogueMap;
import forge.screens.match.controllers.CDetailPicture;
import forge.sound.SoundEffectType;
import forge.sound.SoundSystem;
import forge.toolbox.FButton;
import forge.util.ImageFetcher;
import forge.util.ItemPool;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.eclipse.jetty.util.StringUtil;

/**
 * Deck editor for Rogue Commander mode.
 * Allows viewing the current deck with restrictions:
 * - Can remove cards up to the number added during the run
 * - Can freely add/remove basic lands
 * - Cannot add other cards (they must come from rewards)
 */
public final class CEditorRogue extends CDeckEditor<Deck> {

    private static final String WASTES = "Wastes";
    private final DeckController<Deck> controller;
    private final List<DeckSection> allSections = new ArrayList<>();
    private final ItemPool<PaperCard> basicLandPool;
    private final ItemPool<PaperCard> emptyCatalogPool = new ItemPool<>(PaperCard.class);
    private DragCell allDecksParent;
    private DragCell commanderDecksParent;
    private DragCell oathbreakerDecksParent;
    private DragCell brawlDecksParent;
    private DragCell tinyLeadersDecksParent;
    private DragCell deckGenParent;
    private final Localizer localizer = Localizer.getInstance();
    private final RogueRun rogueRun;

    // Rogue-specific UI elements
    private forge.toolbox.FLabel lblRemovalCredits;
    private FButton btnUndo;
    private FButton btnBackToPath;


    // Undo action tracking
    private static class UndoAction {
        enum Type { ADD, REMOVE }
        final Type type;
        final java.util.List<java.util.Map.Entry<PaperCard, Integer>> items;
        final int nonBasicLandsCount; // For restoring removal credits

        UndoAction(Type type, Iterable<java.util.Map.Entry<PaperCard, Integer>> items, int nonBasicLandsCount) {
            this.type = type;
            this.items = new java.util.ArrayList<>();
            for (java.util.Map.Entry<PaperCard, Integer> entry : items) {
                this.items.add(entry);
            }
            this.nonBasicLandsCount = nonBasicLandsCount;
        }
    }
    private final java.util.Stack<UndoAction> undoStack = new java.util.Stack<>();

    public CEditorRogue(final RogueRun rogueRun0, final FScreen screen0, final CDetailPicture cDetailPicture0) {
        super(screen0, cDetailPicture0, GameType.RogueCommander);
        this.rogueRun = rogueRun0;
        allSections.add(DeckSection.Main);
        allSections.add(DeckSection.Commander);

        // Create infinite pool of basic lands from the commander's edition
        basicLandPool = new ItemPool<>(PaperCard.class);

        // Get commander's edition
        PaperCard commanderCard = rogueRun0.getSelectedRogueDeck().getStartDeck().getCommanders().get(0);
        String selectedEdition = RogueConfig.getBasicLandEdition(
            rogueRun.getSelectedRogueDeck().getLandEdition(), commanderCard);

        List<PaperCard> preferredSetBasics = RogueConfig.getBasicLandsFromEdition(selectedEdition);
        List<PaperCard> filteredCandidates = new ArrayList<>(
            rogueRun.filterCardsByCommanderColorIdentity(preferredSetBasics));
        addBasicLandsToPool(filteredCandidates);
        ensureRequiredBasicLandsPresent(filteredCandidates);

        // Create managers with empty catalog (no cards to add except basic lands)
        final CardManager catalogManager = new CardManager(cDetailPicture0, false, false, false);
        final CardManager deckManager = new CardManager(cDetailPicture0, false, false, false);

        catalogManager.setCaption(localizer.getMessage("lblCardCatalog"));
        catalogManager.setAlwaysNonUnique(true);
        deckManager.setAlwaysNonUnique(true);

        this.setCatalogManager(catalogManager);
        this.setDeckManager(deckManager);

        this.controller = new DeckController<>(null, this, Deck::new);

        // Setup Add Basic Lands button
        getBtnAddBasicLands().setCommand((UiCommand) () -> addBasicLands(this));
    }

    /**
     * Add basic lands to the deck (unrestricted).
     */
    private static void addBasicLands(CEditorRogue editor) {
        if (!editor.isCurrentRun()) {
            return;
        }

        AddBasicLandsDialog dialog = new AddBasicLandsDialog(editor.getHumanDeck(), null);
        CardPool landsToAdd = dialog.show();

        if (landsToAdd != null && !landsToAdd.isEmpty() && editor.isCurrentRun()) {
            for (Entry<PaperCard, Integer> entry : landsToAdd) {
                editor.getHumanDeck().getMain().add(entry.getKey(), entry.getValue());
            }
            editor.getDeckController().notifyModelChanged();
            editor.resetTables();
        }
    }

    @Override
    protected CardLimit getCardLimit() {
        return CardLimit.None;  // No card limit restrictions for Commander
    }

    @Override
    protected void onAddItems(Iterable<Entry<PaperCard, Integer>> items, boolean toAlternate) {
        if (toAlternate || sectionMode != DeckSection.Main || !isCurrentRun()) { return; }

        // Only allow adding if all items are basic lands
        for (Entry<PaperCard, Integer> entry : items) {
            if (!entry.getKey().getRules().getType().isBasicLand()) {
                // Non-basic land detected - reject all additions
                return;
            }
        }

        // Get allowed additions (applies card limits if needed)
        ItemPool<PaperCard> itemsToAdd = getAllowedAdditions(items);
        if (itemsToAdd.isEmpty()) { return; }

        // Add to deck manager
        this.getDeckManager().addItems(itemsToAdd);
        SoundSystem.instance.play(SoundEffectType.Draw, SoundEffectType.Draw.isSynced());

        // Push ADD action onto undo stack (basic lands don't affect removal credits)
        undoStack.push(new UndoAction(UndoAction.Type.ADD, itemsToAdd, 0));

        // Since catalog is infinite, just select the added items
        // (don't remove them from catalog)
        this.getCatalogManager().selectItemEntrys(itemsToAdd);

        this.getDeckController().notifyModelChanged();

        // Update the undo button state
        updateRemovalCreditsLabel();
    }

    @Override
    protected void onRemoveItems(Iterable<Entry<PaperCard, Integer>> items, boolean toAlternate) {
        if (toAlternate || sectionMode != DeckSection.Main || !isCurrentRun()) { return; }

        // Count how many non-basic lands are being removed
        int nonBasicLandsToRemove = 0;
        for (Entry<PaperCard, Integer> entry : items) {
            if (!entry.getKey().getRules().getType().isBasicLand()) {
                nonBasicLandsToRemove += entry.getValue();
            }
        }

        // Check if player has enough removal credits
        if (nonBasicLandsToRemove > rogueRun.getRemovalCredits()) {
            // Not enough removal credits
            String message = localizer.getMessage("lblRogueRemovalLimitHint");
            javax.swing.JOptionPane.showMessageDialog(
                null,
                message,
                localizer.getMessage("lblRogueRemovalLimitReached"),
                javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // Remove from deck manager
        this.getDeckManager().removeItems(items);
        SoundSystem.instance.play(SoundEffectType.FlipCard, SoundEffectType.FlipCard.isSynced());

        // Deduct removal credits from RogueRun (persisted)
        rogueRun.setRemovalCredits(rogueRun.getRemovalCredits() - nonBasicLandsToRemove);

        // Push REMOVE action onto undo stack with non-basic land count for credit restoration
        undoStack.push(new UndoAction(UndoAction.Type.REMOVE, items, nonBasicLandsToRemove));

        // Since catalog is infinite, don't add items back to catalog
        // (they're always available)

        this.getDeckController().notifyModelChanged();

        // Save the run to persist credit changes
        RogueIO.saveRun(rogueRun);

        // Update the removal credits label
        updateRemovalCreditsLabel();
    }

    @Override
    protected void buildAddContextMenu(EditorContextMenuBuilder cmb) {
        if (sectionMode != DeckSection.Main) {
            return;
        }
        cmb.addMoveItems(localizer.getMessage("lblAdd"), localizer.getMessage("lbltodeck"));
    }

    @Override
    protected void buildRemoveContextMenu(EditorContextMenuBuilder cmb) {
        if (sectionMode != DeckSection.Main) {
            return;
        }
        // Standard remove context menu
        cmb.addMoveItems(localizer.getMessage("lblRemove"), null);
    }

    @Override
    public void resetTables() {
        DeckSection selectedSection = (DeckSection) getCbxSection().getSelectedItem();
        if (selectedSection == null) {
            selectedSection = DeckSection.Main;
            getCbxSection().setSelectedItem(selectedSection);
        }
        setEditorMode(selectedSection);
    }

    @Override
    public DeckController<Deck> getDeckController() {
        return this.controller;
    }


    @Override
    protected void resetUI() {
        super.resetUI();

        // Hide add buttons (can't add cards from catalog)
        getBtnAdd().setVisible(false);
        getBtnAdd4().setVisible(false);

        // Hide basic lands button (card catalog provides this functionality)
        getBtnAddBasicLands().setVisible(false);

        // Keep remove button visible, hide remove4
        getBtnRemove().setVisible(true);
        getBtnRemove4().setVisible(false);

        // Hide deck management buttons (can't save/load in Rogue mode)
        VCurrentDeck.SINGLETON_INSTANCE.getBtnSave().setVisible(false);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnSaveAs().setVisible(false);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnNew().setVisible(false);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnOpen().setVisible(false);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnImport().setVisible(false);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnPrintProxies().setVisible(false);

        // Disable deck title editing
        VCurrentDeck.SINGLETON_INSTANCE.getTxfTitle().setEnabled(false);

        // Set title
        VCurrentDeck.SINGLETON_INSTANCE.getLblTitle().setText(localizer.getMessage("lblRogueDeckTitle"));
        VCardCatalog.SINGLETON_INSTANCE.getTabLabel().setText(localizer.getMessage("lblRogueBasicLands"));
        getCbxSection().setVisible(true);

        final JPanel buttonPanel = this.getDeckManager().getPnlButtons();
        if (lblRemovalCredits == null) {
            lblRemovalCredits = new forge.toolbox.FLabel.Builder()
                .text(localizer.getMessage("lblRogueRemovalCredits", rogueRun.getRemovalCredits()))
                .tooltip(localizer.getMessage("lblRogueRemovalCreditsTooltip", rogueRun.getRemovalCredits()))
                .fontSize(14)
                .build();
        }
        if (lblRemovalCredits.getParent() != buttonPanel) {
            buttonPanel.add(lblRemovalCredits, "w 22%!, h 30px!, gapx 5");
        }

        if (btnUndo == null) {
            btnUndo = new FButton(localizer.getMessage("lblUndo"));
            btnUndo.setToolTipText(localizer.getMessage("lblRogueUndoTooltip"));
            btnUndo.setCommand(this::undoLastRemoval);
        }
        if (btnUndo.getParent() != buttonPanel) {
            buttonPanel.add(btnUndo, "w 12%!, h 26px!, ay bottom, gapx 60");
        }

        if (btnBackToPath == null) {
            btnBackToPath = new FButton(localizer.getMessage("lblRogueBackToMap"));
            btnBackToPath.setToolTipText(localizer.getMessage("lblRogueReturnToMapTooltip"));
            btnBackToPath.setCommand(this::navigateBackToPath);
        }
        if (btnBackToPath.getParent() != buttonPanel) {
            buttonPanel.add(btnBackToPath, "w 18%!, h 26px!, ay bottom, gapx 5");
        }

        // Update label text and button state
        updateRemovalCreditsLabel();
    }

    @Override
    public Boolean isSectionImportable(DeckSection section) {
        return false;  // No importing in Rogue mode
    }

    @Override
    public boolean canSwitchAway(boolean isClosing) {
        // Save the RogueRun to persist deck changes
        if (isCurrentRun() && rogueRun.getHostedMatch() == null) {
            RogueIO.saveRun(rogueRun);
            RogueCommanderAchievements.getInstance().evaluateRunAchievements(rogueRun);
            // Reload to ensure we're working with the saved version
            // This prevents stale data if the editor is opened again
            resetTables();
        }
        return true;
    }

    @Override
    public void resetUIChanges() {
        // Reset UI to default state
        VCurrentDeck.SINGLETON_INSTANCE.getBtnSave().setVisible(true);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnSaveAs().setVisible(true);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnNew().setVisible(true);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnOpen().setVisible(true);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnImport().setVisible(true);
        VCurrentDeck.SINGLETON_INSTANCE.getTxfTitle().setEnabled(true);
        getBtnAdd().setVisible(true);
        getBtnAdd4().setVisible(true);

        final JPanel buttonPanel = this.getDeckManager().getPnlButtons();
        if (lblRemovalCredits != null) {
            buttonPanel.remove(lblRemovalCredits);
        }
        if (btnUndo != null) {
            buttonPanel.remove(btnUndo);
        }
        if (btnBackToPath != null) {
            buttonPanel.remove(btnBackToPath);
        }
        buttonPanel.revalidate();
        buttonPanel.repaint();

        if (allDecksParent != null) {
            allDecksParent.addDoc(VAllDecks.SINGLETON_INSTANCE);
            allDecksParent = null;
        }
        if (commanderDecksParent != null) {
            commanderDecksParent.addDoc(VCommanderDecks.SINGLETON_INSTANCE);
            commanderDecksParent = null;
        }
        if (oathbreakerDecksParent != null) {
            oathbreakerDecksParent.addDoc(VOathbreakerDecks.SINGLETON_INSTANCE);
            oathbreakerDecksParent = null;
        }
        if (brawlDecksParent != null) {
            brawlDecksParent.addDoc(VBrawlDecks.SINGLETON_INSTANCE);
            brawlDecksParent = null;
        }
        if (tinyLeadersDecksParent != null) {
            tinyLeadersDecksParent.addDoc(VTinyLeadersDecks.SINGLETON_INSTANCE);
            tinyLeadersDecksParent = null;
        }
        if (deckGenParent != null) {
            deckGenParent.addDoc(VDeckgen.SINGLETON_INSTANCE);
            deckGenParent = null;
        }
    }

    @Override
    public void update() {
        if (!isCurrentRun()) {
            // Let the tab finish opening before switching back to Rogue Commander.
            SwingUtilities.invokeLater(() -> {
                if (Singletons.getControl().getCurrentScreen() == getScreen()
                        && CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController() == this
                        && !isCurrentRun()) {
                    navigateBackToPath();
                }
            });
            return;
        }

        this.getCatalogManager().setup(ItemManagerConfig.CARD_CATALOG);
        this.getDeckManager().setup(ItemManagerConfig.DECK_EDITOR);

        // Clear undo stack for new editing session
        undoStack.clear();

        this.getDeckController().setModel(rogueRun.getCurrentDeck());

        resetUI();
        allDecksParent = removeTab(VAllDecks.SINGLETON_INSTANCE);
        commanderDecksParent = removeTab(VCommanderDecks.SINGLETON_INSTANCE);
        oathbreakerDecksParent = removeTab(VOathbreakerDecks.SINGLETON_INSTANCE);
        brawlDecksParent = removeTab(VBrawlDecks.SINGLETON_INSTANCE);
        tinyLeadersDecksParent = removeTab(VTinyLeadersDecks.SINGLETON_INSTANCE);
        deckGenParent = removeTab(VDeckgen.SINGLETON_INSTANCE);

        getCbxSection().removeAllItems();
        for (DeckSection section : allSections) {
            getCbxSection().addItem(section);
        }
        for (ActionListener listener : getCbxSection().getActionListeners()) {
            getCbxSection().removeActionListener(listener);
        }
        getCbxSection().addActionListener(actionEvent -> {
            DeckSection ds = (DeckSection) getCbxSection().getSelectedItem();
            setEditorMode(ds);
        });
        resetTables();
        fetchEditorCardImages();

        // Set deck name
        if (rogueRun.getCurrentDeck() != null) {
            String deckName = rogueRun.getSelectedRogueDeck().getName() + " - Rogue Run";
            rogueRun.getCurrentDeck().setName(deckName);
            VCurrentDeck.SINGLETON_INSTANCE.getTxfTitle().setText(deckName);
        }
    }

    private void fetchEditorCardImages() {
        Deck deck = rogueRun.getCurrentDeck();
        if (deck == null) {
            return;
        }

        Set<String> imageKeys = new LinkedHashSet<>();
        for (Entry<PaperCard, Integer> entry : basicLandPool) {
            addCardImageKeys(imageKeys, entry.getKey());
        }
        for (Entry<PaperCard, Integer> entry : deck.getAllCardsInASinglePool()) {
            addCardImageKeys(imageKeys, entry.getKey());
        }

        ImageFetcher.Callback repaintCallback = () -> {
            getCatalogManager().getCurrentView().getComponent().repaint();
            getDeckManager().getCurrentView().getComponent().repaint();
        };
        for (String imageKey : imageKeys) {
            GuiBase.getInterface().getImageFetcher().fetchImage(imageKey, repaintCallback);
        }
    }

    private static void addCardImageKeys(Set<String> imageKeys, PaperCard card) {
        String frontImageKey = card.getImageKey(false);
        if (!StringUtil.isBlank(frontImageKey)) {
            imageKeys.add(frontImageKey);
        }
        if (card.hasBackFace()) {
            String backImageKey = card.getImageKey(true);
            if (!StringUtil.isBlank(backImageKey)) {
                imageKeys.add(backImageKey);
            }
        }
    }

    @Override
    protected ItemPool<PaperCard> getAllowedAdditions(Iterable<Entry<PaperCard, Integer>> itemsToAdd) {
        // Only basic lands can be added
        final ItemPool<PaperCard> additions = new ItemPool<>(PaperCard.class);

        for (final Entry<PaperCard, Integer> itemEntry : itemsToAdd) {
            final PaperCard card = itemEntry.getKey();
            if (card.getRules().getType().isBasicLand()) {
                additions.add(card, itemEntry.getValue());
            }
        }

        return additions;
    }

    private void setEditorMode(DeckSection sectionMode) {
        Deck deck = rogueRun.getCurrentDeck();
        if (deck == null || sectionMode == null) {
            return;
        }

        switch (sectionMode) {
        case Commander:
            getCatalogManager().setup(ItemManagerConfig.CARD_CATALOG);
            getCatalogManager().setPool(emptyCatalogPool, true);
            getDeckManager().setPool(deck.getOrCreate(DeckSection.Commander));
            getBtnRemove().setVisible(false);
            getBtnRemove4().setVisible(false);
            break;
        case Main:
        default:
            getCatalogManager().setup(ItemManagerConfig.CARD_CATALOG);
            getCatalogManager().setPool(basicLandPool, true);
            getDeckManager().setPool(deck.getMain());
            getBtnRemove().setVisible(true);
            getBtnRemove4().setVisible(false);
            break;
        }

        this.sectionMode = sectionMode;
        updateRemovalCreditsLabel();
        this.getDeckController().updateCaptions();
    }

    private void addBasicLandsToPool(List<PaperCard> cards) {
        for (PaperCard card : cards) {
            basicLandPool.add(card);
        }
    }

    private void ensureRequiredBasicLandsPresent(List<PaperCard> preferredCandidates) {
        Set<String> presentBasicLandTypes = new LinkedHashSet<>();
        for (PaperCard card : preferredCandidates) {
            presentBasicLandTypes.add(card.getName());
        }

        for (String requiredBasicLandType : getRequiredBasicLandTypes()) {
            if (presentBasicLandTypes.contains(requiredBasicLandType)) {
                continue;
            }

            PaperCard fallbackCard = RogueConfig.getFallbackBasicLandPrint(requiredBasicLandType);
            if (fallbackCard != null) {
                basicLandPool.add(fallbackCard);
                presentBasicLandTypes.add(requiredBasicLandType);
            }
        }
    }

    private Set<String> getRequiredBasicLandTypes() {
        Set<String> requiredBasicLandTypes = new LinkedHashSet<>(getCommanderColoredBasicLandTypes());
        if (shouldRequireWastes()) {
            requiredBasicLandTypes.add(WASTES);
        }
        return requiredBasicLandTypes;
    }

    private Set<String> getCommanderColoredBasicLandTypes() {
        Set<String> coloredBasicLandTypes = new LinkedHashSet<>();
        int colorIdentity = rogueRun.getCommanderColorIdentityMask();
        if ((colorIdentity & MagicColor.WHITE) != 0) { coloredBasicLandTypes.add("Plains"); }
        if ((colorIdentity & MagicColor.BLUE) != 0) { coloredBasicLandTypes.add("Island"); }
        if ((colorIdentity & MagicColor.BLACK) != 0) { coloredBasicLandTypes.add("Swamp"); }
        if ((colorIdentity & MagicColor.RED) != 0) { coloredBasicLandTypes.add("Mountain"); }
        if ((colorIdentity & MagicColor.GREEN) != 0) { coloredBasicLandTypes.add("Forest"); }
        return coloredBasicLandTypes;
    }

    private boolean shouldRequireWastes() {
        return rogueRun.getCommanderColorIdentityMask() == 0 ||
            rogueRun.getSelectedRogueDeck().shouldIncludeColorlessBasics();
    }

    /**
     * Updates the removal credits label to show available removals.
     */
    private void updateRemovalCreditsLabel() {
        if (lblRemovalCredits != null) {
            lblRemovalCredits.setText(localizer.getMessage("lblRogueRemovalCredits", rogueRun.getRemovalCredits()));
            lblRemovalCredits.setToolTipText(localizer.getMessage("lblRogueRemovalCreditsTooltip", rogueRun.getRemovalCredits()));

            // Update undo button state
            if (btnUndo != null) {
                btnUndo.setEnabled(sectionMode == DeckSection.Main && !undoStack.isEmpty());
            }
        }
    }

    private boolean isCurrentRun() {
        return rogueRun == CSubmenuRogueMap.SINGLETON_INSTANCE.getCurrentRun();
    }

    private void navigateBackToPath() {
        if (Singletons.getControl().setCurrentScreen(FScreen.HOME_SCREEN)) {
            CHomeUI.SINGLETON_INSTANCE.itemClick(CSubmenuRogueMap.SINGLETON_INSTANCE.getCurrentRun() == null
                ? EDocID.HOME_ROGUESTART : EDocID.HOME_ROGUEMAP);
        }
    }

    /**
     * Undoes the last action (either ADD or REMOVE).
     * - If last action was REMOVE: re-adds the cards and restores removal credits
     * - If last action was ADD: removes the cards that were added
     */
    private void undoLastRemoval() {
        if (undoStack.isEmpty() || !isCurrentRun()) {
            return;
        }

        // Pop the last action
        UndoAction action = undoStack.pop();

        // Convert items list to ItemPool for deck manager
        ItemPool<PaperCard> itemPool = new ItemPool<>(PaperCard.class);
        for (java.util.Map.Entry<PaperCard, Integer> entry : action.items) {
            itemPool.add(entry.getKey(), entry.getValue());
        }

        // Reverse the action
        if (action.type == UndoAction.Type.REMOVE) {
            // Last action was REMOVE → undo by adding cards back
            this.getDeckManager().addItems(itemPool);

            // Restore removal credits (only for non-basic lands)
            rogueRun.setRemovalCredits(rogueRun.getRemovalCredits() + action.nonBasicLandsCount);

        } else { // action.type == UndoAction.Type.ADD
            // Last action was ADD → undo by removing cards
            this.getDeckManager().removeItems(action.items);

            // No removal credits to restore (additions don't cost credits)
        }

        this.getDeckController().notifyModelChanged();

        // Save the run to persist credit changes
        RogueIO.saveRun(rogueRun);

        updateRemovalCreditsLabel();
    }
}
