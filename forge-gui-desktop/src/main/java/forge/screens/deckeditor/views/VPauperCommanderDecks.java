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
import forge.screens.deckeditor.controllers.CPauperCommanderDecks;
import forge.screens.match.controllers.CDetailPicture;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

/**
 * Assembles Swing components of the Pauper Commander deck viewer in the deck editor.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VPauperCommanderDecks implements IVDoc<CPauperCommanderDecks> {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    final Localizer localizer = Localizer.getInstance();
    private final DragTab tab = new DragTab(localizer.getMessage("lblPauperCommander"));

    private DeckManager lstDecks;

    //========== Overridden methods

    @Override
    public EDocID getDocumentID() {
        return EDocID.EDITOR_PAUPER_COMMANDER;
    }

    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    @Override
    public CPauperCommanderDecks getLayoutControl() {
        return CPauperCommanderDecks.SINGLETON_INSTANCE;
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
        CPauperCommanderDecks.SINGLETON_INSTANCE.refresh(); //ensure decks refreshed in case any deleted or added since last loaded
        String preferredDeck = DeckPreferences.getCommanderDeck();

        JPanel parentBody = parentCell.getBody();
        parentBody.setLayout(new MigLayout("insets 5, gap 0, wrap, hidemode 3"));
        parentBody.add(new ItemManagerContainer(lstDecks), "push, grow");

        VAllDecks.editPreferredDeck(lstDecks, preferredDeck);
    }

    //========== Retrieval methods
    /** @return {@link JPanel} */
    public DeckManager getLstDecks() {
        return lstDecks;
    }

    public void setCDetailPicture(final CDetailPicture cDetailPicture) {
        this.lstDecks = new DeckManager(GameType.PauperCommander, cDetailPicture);
        this.lstDecks.setCaption(localizer.getMessage("lblPauperCommanderDecks"));
    }
}
