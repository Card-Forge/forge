package forge.gui.deckeditor.views;

import javax.swing.JLabel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;
import forge.game.GameType;
import forge.gui.deckeditor.controllers.CAllDecks;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.framework.IVDoc;
import forge.gui.toolbox.DeckLister;
import forge.gui.toolbox.FLabel;

/** 
 * Assembles Swing components of all deck viewer in deck editor.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VAllDecks implements IVDoc {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("All Decks");

    private final DeckLister lstDecks = new DeckLister(GameType.Constructed);
    private JScrollPane scroller = new JScrollPane(lstDecks);

    private final JLabel btnImport = new FLabel.Builder()
        .fontScaleAuto(false).fontSize(14)
        .text("Import Deck").tooltip("Attempt to import a deck from a non-Forge format")
        .opaque(true).hoverable(true).build();

    //========== Constructor
    private VAllDecks() {
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.setBorder(null);
        scroller.getViewport().setBorder(null);
    }

    //========== Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.EDITOR_ALLDECKS;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getControl()
     */
    @Override
    public ICDoc getControl() {
        return CAllDecks.SINGLETON_INSTANCE;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#setParentCell(forge.gui.framework.DragCell)
     */
    @Override
    public void setParentCell(DragCell cell0) {
        this.parentCell = cell0;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getParentCell()
     */
    @Override
    public DragCell getParentCell() {
        return this.parentCell;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#populate()
     */
    @Override
    public void populate() {
        parentCell.getBody().setLayout(new MigLayout("insets 0, gap 0, wrap, ax center"));

        parentCell.getBody().add(btnImport, "w 120px!, h 30px!, gap 0 0 5px 5px");

        //parentCell.getBody().add(scroller, "w 96%!, h 96%!, gap 2% 0 2% 0");
    }

    //========== Retrieval methods
    /** @return {@link javax.swing.JPanel} */
    public DeckLister getLstDecks() {
        return lstDecks;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getBtnImport() {
        return btnImport;
    }
}
