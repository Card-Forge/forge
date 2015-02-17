package forge.screens.workshop.views;

import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import net.miginfocom.swing.MigLayout;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.screens.workshop.controllers.CCardScript;
import forge.toolbox.FTextPane;

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

    private final FTextPane txtScript = new FTextPane();
    private final StyledDocument doc;
    private final Style error;

    //========== Constructor
    private VCardScript() {
        txtScript.setEditable(true);
        txtScript.setFocusable(true);
        doc = new DefaultStyledDocument();
        txtScript.setDocument(doc);
        error = doc.addStyle("error", null);
        error.addAttribute(StyleConstants.Background, Color.red);
        error.addAttribute(StyleConstants.Bold, new Boolean(true));
    }

    public JTextPane getTxtScript() {
        return txtScript;
    }

    public StyledDocument getDoc() {
        return doc;
    }

    public Style getErrorStyle() {
        return error;
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
    	body.add(txtScript, "w 100%, h 100%");
    }
}
