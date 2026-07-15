package forge.screens.deckeditor.views;

import javax.swing.JPanel;

import forge.deck.io.DeckPreferences;
import forge.game.GameType;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.itemmanager.DeckManager;
import forge.itemmanager.ItemManagerContainer;
import forge.screens.deckeditor.controllers.CDandanDecks;
import forge.screens.match.controllers.CDetailPicture;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

/**
 * Deck list tab for DanDan decks in the deck editor.
 */
public enum VDandanDecks implements IVDoc<CDandanDecks> {
    SINGLETON_INSTANCE;

    private DragCell parentCell;
    final Localizer localizer = Localizer.getInstance();
    private final DragTab tab = new DragTab(localizer.getMessage("lblDanDan"));

    private DeckManager lstDecks;

    @Override
    public EDocID getDocumentID() {
        return EDocID.EDITOR_DANDAN;
    }

    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    @Override
    public CDandanDecks getLayoutControl() {
        return CDandanDecks.SINGLETON_INSTANCE;
    }

    @Override
    public void setParentCell(DragCell cell0) {
        this.parentCell = cell0;
    }

    @Override
    public DragCell getParentCell() {
        return this.parentCell;
    }

    @Override
    public void populate() {
        CDandanDecks.SINGLETON_INSTANCE.refresh();
        String preferredDeck = DeckPreferences.getDanDanDeck();

        JPanel parentBody = parentCell.getBody();
        parentBody.setLayout(new MigLayout("insets 5, gap 0, wrap, hidemode 3"));
        parentBody.add(new ItemManagerContainer(lstDecks), "push, grow");

        VAllDecks.editPreferredDeck(lstDecks, preferredDeck);
    }

    public DeckManager getLstDecks() {
        return lstDecks;
    }

    public void setCDetailPicture(final CDetailPicture cDetailPicture) {
        this.lstDecks = new DeckManager(GameType.DanDan, cDetailPicture);
        this.lstDecks.setCaption(localizer.getMessage("lblDanDanDecks"));
    }
}
