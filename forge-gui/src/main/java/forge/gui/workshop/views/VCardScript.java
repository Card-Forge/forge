package forge.gui.workshop.views;

import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import net.miginfocom.swing.MigLayout;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.gui.toolbox.FSkin;
import forge.gui.workshop.controllers.CCardScript;

/** 
 * Assembles Swing components of workshop card script tab.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VCardScript implements IVDoc<CCardScript> {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Card Script");

    private final JTextArea tarScript = new JTextArea();
    private final JScrollPane scroller;

    //========== Constructor
    private VCardScript() {
        FSkin.JTextComponentSkin<JTextArea> txtScriptSkin = FSkin.get(tarScript);
        txtScriptSkin.setFont(FSkin.getFixedFont(16));
        txtScriptSkin.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        txtScriptSkin.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        txtScriptSkin.setCaretColor(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        tarScript.setMargin(new Insets(3, 3, 3, 3));

        scroller = new JScrollPane(tarScript);
        scroller.setBorder(null);
        scroller.setOpaque(false);
    }
    
    public JTextArea getTarScript() {
    	return tarScript;
    }

    //========== Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.WORKSHOP_CARDSCRIPT;
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
    public CCardScript getLayoutControl() {
        return CCardScript.SINGLETON_INSTANCE;
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
    	body.setLayout(new MigLayout("insets 1, gap 0, wrap"));
    	body.add(scroller, "w 100%, h 100%");
    }
}
