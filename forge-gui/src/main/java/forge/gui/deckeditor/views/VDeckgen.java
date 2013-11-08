package forge.gui.deckeditor.views;

import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;
import forge.gui.deckeditor.controllers.CDeckgen;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.gui.toolbox.FLabel;

/** 
 * Assembles Swing components of deck editor analysis tab.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VDeckgen implements IVDoc<CDeckgen> {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Deck Generation");

    // Deckgen buttons
    private final JLabel btnRandCardpool = new FLabel.Builder()
        .tooltip("Generate random constructed cardpool in current deck area")
        .text("Random Cardpool").fontSize(14)
        .opaque(true).hoverable(true).build();

    private final JLabel btnRandDeck2 = new FLabel.Builder()
        .tooltip("Generate 2 color constructed deck in current deck area")
        .text("Constructed (2 color)").fontSize(14)
        .opaque(true).hoverable(true).build();

    private final JLabel btnRandDeck3 = new FLabel.Builder()
        .tooltip("Generate 3 color constructed deck in current deck area")
        .text("Constructed (3 color)").fontSize(14)
        .opaque(true).hoverable(true).build();

    private final JLabel btnRandDeck5 = new FLabel.Builder()
        .tooltip("Generate 5 color constructed deck in current deck area")
        .text("Constructed (5 color)").fontSize(14)
        .opaque(true).hoverable(true).build();

    //========== Constructor
    private VDeckgen() {
    }

    //========== Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.EDITOR_DECKGEN;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getLayoutControl()
     */
    @Override
    public CDeckgen getLayoutControl() {
        return CDeckgen.SINGLETON_INSTANCE;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#setParentCell(forge.gui.framework.DragCell)
     */
    @Override
    public void setParentCell(final DragCell cell0) {
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

        final String constraints = "w 80%!, h 30px!, gap 0 0 10px 0";
        parentCell.getBody().add(btnRandCardpool, constraints);
        parentCell.getBody().add(btnRandDeck2, constraints);
        parentCell.getBody().add(btnRandDeck3, constraints);
        parentCell.getBody().add(btnRandDeck5, constraints);
    }

    //========== Retrieval methods
    /** @return {@link javax.swing.JLabel} */
    public JLabel getBtnRandCardpool() {
        return btnRandCardpool;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getBtnRandDeck2() {
        return btnRandDeck2;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getBtnRandDeck3() {
        return btnRandDeck3;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getBtnRandDeck5() {
        return btnRandDeck5;
    }
}
