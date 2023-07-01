package forge.screens.deckeditor.controllers;


import java.util.List;
import java.util.Map;

import forge.deck.DeckBase;
import forge.game.GameType;
import forge.gui.framework.DragCell;
import forge.gui.framework.FScreen;
import forge.item.PaperToken;
import forge.itemmanager.TokenManager;
import forge.model.FModel;
import forge.screens.deckeditor.views.VAllDecks;
import forge.screens.deckeditor.views.VCardCatalog;
import forge.screens.deckeditor.views.VCurrentDeck;
import forge.screens.deckeditor.views.VDeckgen;
import forge.screens.deckeditor.views.VProbabilities;
import forge.screens.home.quest.CSubmenuQuestDecks;
import forge.screens.match.controllers.CDetailPicture;
import forge.util.Localizer;

public class CEditorTokenViewer extends ACEditorBase<PaperToken, DeckBase> {

    private DragCell allDecksParent = null;
    private DragCell deckGenParent = null;
    private DragCell probsParent = null;

    private List<PaperToken> fullCatalogCards;

    // remember changed gui elements
    private String CCTabLabel = "";
    private String CCAddLabel = "";
    private String CDTabLabel = "";
    private String CDRemLabel = "";
    private String prevRem4Label = null;
    private String prevRem4Tooltip = null;
    private Runnable prevRem4Cmd = null;

    /**
     * Child controller for quest card shop UI.
     *
     */
    public CEditorTokenViewer(final CDetailPicture cDetailPicture0) {
        super(FScreen.TOKEN_VIEWER, cDetailPicture0, GameType.Quest);

        FModel.getMagicDb().getAllTokens().preloadTokens();
        fullCatalogCards = FModel.getMagicDb().getAllTokens().getAllTokens();

        final TokenManager catalogManager = new TokenManager(cDetailPicture0, false);
        final TokenManager deckManager = new TokenManager(cDetailPicture0, false);
        catalogManager.setCaption(Localizer.getInstance().getMessage("lblAllTokens"));
        catalogManager.setAlwaysNonUnique(true);
        this.setCatalogManager(catalogManager);
        this.setDeckManager(deckManager);
    }

    //=========== Overridden from ACEditorBase

    @Override
    protected ACEditorBase.CardLimit getCardLimit() {
        return ACEditorBase.CardLimit.None;
    }

    @Override
    protected void onAddItems(Iterable<Map.Entry<PaperToken, Integer>> items, boolean toAlternate) {

    }

    @Override
    protected void onRemoveItems(Iterable<Map.Entry<PaperToken, Integer>> items, boolean toAlternate) {

    }

    @Override
    protected void buildAddContextMenu(ACEditorBase.EditorContextMenuBuilder cmb) {
    }

    @Override
    protected void buildRemoveContextMenu(ACEditorBase.EditorContextMenuBuilder cmb) {
    }

    /*
     * (non-Javadoc)
     *
     * @see forge.gui.deckeditor.ACEditorBase#resetTables()
     */
    @Override
    public void resetTables() {
    }

    /*
     * (non-Javadoc)
     *
     * @see forge.gui.deckeditor.ACEditorBase#getController()
     */
    @Override
    public DeckController<DeckBase> getDeckController() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#show(forge.Command)
     */
    @SuppressWarnings("serial")
    @Override
    public void update() {
        resetUI();

        CCTabLabel = VCardCatalog.SINGLETON_INSTANCE.getTabLabel().getText();
        VCardCatalog.SINGLETON_INSTANCE.getTabLabel().setText(Localizer.getInstance().getMessage("lblAllTokens"));

        this.getBtnAdd().setVisible(false);
        this.getBtnAdd4().setVisible(false);
        this.getBtnRemove().setVisible(false);
        this.getBtnAddBasicLands().setVisible(false);

        VProbabilities.SINGLETON_INSTANCE.getTabLabel().setVisible(false);

        prevRem4Label = this.getBtnRemove4().getText();
        prevRem4Tooltip = this.getBtnRemove4().getToolTipText();
        prevRem4Cmd = this.getBtnRemove4().getCommand();

        VCurrentDeck.SINGLETON_INSTANCE.getPnlHeader().setVisible(false);

        this.getCatalogManager().setPool(fullCatalogCards);
        this.getDeckManager().setPool((Iterable<PaperToken>) null);

        this.getBtnRemove4().setVisible(false);


        deckGenParent = removeTab(VDeckgen.SINGLETON_INSTANCE);
        allDecksParent = removeTab(VAllDecks.SINGLETON_INSTANCE);
        probsParent = removeTab(VProbabilities.SINGLETON_INSTANCE);
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.controllers.ACEditorBase#canSwitchAway()
     */
    @Override
    public boolean canSwitchAway(boolean isClosing) {
        FModel.getQuest().save();
        return true;
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.controllers.ACEditorBase#resetUIChanges()
     */
    @Override
    public void resetUIChanges() {
        CSubmenuQuestDecks.SINGLETON_INSTANCE.update();


        this.getCatalogManager().getPnlButtons().add(this.getBtnAdd4());

        this.getBtnRemove4().setText(prevRem4Label);
        this.getBtnRemove4().setToolTipText(prevRem4Tooltip);
        this.getBtnRemove4().setCommand(prevRem4Cmd);

        VCardCatalog.SINGLETON_INSTANCE.getTabLabel().setText(CCTabLabel);
        VCurrentDeck.SINGLETON_INSTANCE.getTabLabel().setText(CDTabLabel);

        this.getBtnAdd().setText(CCAddLabel);
        this.getBtnRemove().setText(CDRemLabel);

        //TODO: Remove filter for SItemManagerUtil.StatTypes.PACK

        //Re-add tabs
        if (deckGenParent != null) {
            deckGenParent.addDoc(VDeckgen.SINGLETON_INSTANCE);
        }
        if (allDecksParent != null) {
            allDecksParent.addDoc(VAllDecks.SINGLETON_INSTANCE);
        }
        if (probsParent != null) {
            probsParent.addDoc(VProbabilities.SINGLETON_INSTANCE);
        }
    }

}
