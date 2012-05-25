package forge.gui.home.utilities;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.framework.IVDoc;
import forge.gui.home.EMenuGroup;
import forge.gui.home.ICSubmenu;
import forge.gui.home.IVSubmenu;
import forge.gui.toolbox.FLabel;

/** 
 * Assembles Swing components of exit submenu option singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum VSubmenuExit implements IVSubmenu, IVDoc {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Exit");

    /** */
    private final JPanel pnl = new JPanel();

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        pnl.setLayout(new MigLayout("insets 0, gap 0, align center"));
        pnl.setOpaque(false);

        pnl.add(new FLabel.Builder().text("Open Deck Editor").opaque(true)
                .hoverable(true).cmdClick(CSubmenuDeckEditor.SINGLETON_INSTANCE.getMenuCommand())
                .fontSize(16).build(), "w 200px!, h 40px!");
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getGroup()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.UTILITIES;
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getPanel()
     */
    @Override
    public JPanel getPanel() {
        return pnl;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuTitle()
     */
    @Override
    public String getMenuTitle() {
        return "Exit Forge";
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getItemEnum()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_EXIT;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getSubmenuControl()
     */
    @Override
    public ICSubmenu getSubmenuControl() {
        return CSubmenuExit.SINGLETON_INSTANCE;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_EXIT;
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
    public ICDoc getLayoutControl() {
        return CSubmenuExit.SINGLETON_INSTANCE;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#setParentCell(forge.gui.framework.DragCell)
     */
    @Override
    public void setParentCell(DragCell cell0) {
        parentCell = cell0;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getParentCell()
     */
    @Override
    public DragCell getParentCell() {
        return parentCell;
    }
}
