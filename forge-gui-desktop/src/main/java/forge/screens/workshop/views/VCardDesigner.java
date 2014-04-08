package forge.screens.workshop.views;

import forge.assets.FSkinProp;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.screens.workshop.controllers.CCardDesigner;
import forge.toolbox.FLabel;
import forge.toolbox.FSkin;

import javax.swing.*;

import java.awt.*;

/** 
 * Assembles Swing components of workshop card designer tab.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VCardDesigner implements IVDoc<CCardDesigner> {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Card Designer");
    
    private FLabel btnSaveCard = new FLabel.Builder()
    		.opaque(true).hoverable(true)
    		.text("Save and Apply Card Changes")
    		.icon(FSkin.getIcon(FSkinProp.ICO_SAVE))
    		.enabled(false) //disabled by default until card changes made
    		.build();

    //========== Constructor
    private VCardDesigner() {
    }
    
    public FLabel getBtnSaveCard() {
    	return btnSaveCard;
    }

    //========== Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.WORKSHOP_CARDDESIGNER;
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
    public CCardDesigner getLayoutControl() {
        return CCardDesigner.SINGLETON_INSTANCE;
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
    	JPanel body = parentCell.getBody();
    	SpringLayout layout = new SpringLayout();
    	body.setLayout(layout);
        layout.putConstraint(SpringLayout.SOUTH, btnSaveCard, -6, SpringLayout.SOUTH, body);
        layout.putConstraint(SpringLayout.WEST, btnSaveCard, 6, SpringLayout.WEST, body);
        layout.putConstraint(SpringLayout.EAST, btnSaveCard, -6, SpringLayout.EAST, body);
        btnSaveCard.setPreferredSize(new Dimension(60, 30));
        body.add(btnSaveCard);
    	//body.setLayout(new MigLayout("insets 1, gap 0, wrap"));
        //body.add(btnSaveCard, "w 100% - 12, h 30px!, ay bottom, gap 6");
    }
}
