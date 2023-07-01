package forge.screens.deckeditor.views;

import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.screens.deckeditor.controllers.CDeckgen;
import forge.toolbox.FLabel;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

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
    final Localizer localizer = Localizer.getInstance();
    private final DragTab tab = new DragTab(localizer.getMessage("lblDeckGeneration"));
    // Deckgen buttons
    private final FLabel btnRandCardpool = new FLabel.Builder()
        .tooltip(localizer.getMessage("ttbtnRandCardpool"))
        .text(localizer.getMessage("btnRandCardpool")).fontSize(14)
        .opaque(true).hoverable(true).build();

    private final FLabel btnRandDeck2 = new FLabel.Builder()
        .tooltip(localizer.getMessage("ttbtnRandDeck2"))
        .text(localizer.getMessage("btnRandDeck2")).fontSize(14)
        .opaque(true).hoverable(true).build();

    private final FLabel btnRandDeck3 = new FLabel.Builder()
        .tooltip(localizer.getMessage("ttbtnRandDeck3"))
        .text(localizer.getMessage("btnRandDeck3")).fontSize(14)
        .opaque(true).hoverable(true).build();

    private final FLabel btnRandDeck5 = new FLabel.Builder()
        .tooltip(localizer.getMessage("ttbtnRandDeck5"))
        .text(localizer.getMessage("btnRandDeck5")).fontSize(14)
        .opaque(true).hoverable(true).build();

    //========== Constructor
    VDeckgen() {
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
    public FLabel getBtnRandCardpool() {
        return btnRandCardpool;
    }

    /** @return {@link javax.swing.JLabel} */
    public FLabel getBtnRandDeck2() {
        return btnRandDeck2;
    }

    /** @return {@link javax.swing.JLabel} */
    public FLabel getBtnRandDeck3() {
        return btnRandDeck3;
    }

    /** @return {@link javax.swing.JLabel} */
    public FLabel getBtnRandDeck5() {
        return btnRandDeck5;
    }
}
