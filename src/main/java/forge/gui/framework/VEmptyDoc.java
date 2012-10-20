package forge.gui.framework;

import forge.gui.match.nonsingleton.CEmptyDoc;

/** 
 * An intentionally empty IVDoc to fill field slots unused
 * by the current layout of a match UI.
 */
public class VEmptyDoc implements IVDoc<CEmptyDoc> {
    // Fields used with interface IVDoc
    private final CEmptyDoc control;
    private final EDocID docID;

    /**
     * An intentionally empty IVDoc to fill field slots unused
     * by the current layout of a match UI.
     *
     * @param id0 EDocID
     */
    public VEmptyDoc(final EDocID id0) {
        id0.setDoc(this);
        docID = id0;
        control = new CEmptyDoc();
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return docID;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getLayoutControl()
     */
    @Override
    public CEmptyDoc getLayoutControl() {
        return control;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#setParentCell(forge.gui.framework.DragCell)
     */
    @Override
    public void setParentCell(DragCell cell0) {
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getParentCell()
     */
    @Override
    public DragCell getParentCell() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#populate()
     */
    @Override
    public void populate() {
    }
}
