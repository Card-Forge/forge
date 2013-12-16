package forge.gui.deckeditor.views;

import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

import forge.gui.deckeditor.controllers.CCardCatalog;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.gui.toolbox.itemmanager.ItemManagerContainer;
import forge.item.InventoryItem;

/** 
 * Assembles Swing components of card catalog in deck editor.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 * 
 */
public enum VCardCatalog implements IVDoc<CCardCatalog> {
    /** */
    SINGLETON_INSTANCE;

    public static final int SEARCH_MODE_INVERSE_INDEX = 1;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Card Catalog");

    // panel where special instructions appear
    private final JPanel pnlHeader = new JPanel(new MigLayout("insets 0, gap 0, center"));
    private final FLabel lblTitle = new FLabel.Builder().fontSize(14).build();

    private final ItemManagerContainer itemManagerContainer = new ItemManagerContainer();
    private ItemManager<? extends InventoryItem> itemManager;

    //========== Constructor
    /** */
    private VCardCatalog() {
        pnlHeader.setOpaque(false);
        pnlHeader.add(lblTitle, "center, gap 0 0 10 5");
    }

    //========== Overridden from IVDoc

    @Override
    public EDocID getDocumentID() {
        return EDocID.EDITOR_CATALOG;
    }

    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    @Override
    public CCardCatalog getLayoutControl() {
        return CCardCatalog.SINGLETON_INSTANCE;
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
        JPanel parentBody = parentCell.getBody();
        parentBody.setLayout(new MigLayout("insets 5, gap 0, wrap, hidemode 3"));
        parentBody.add(pnlHeader, "pushx, growx");
        parentBody.add(itemManagerContainer, "push, grow");
    }

    public ItemManager<? extends InventoryItem> getItemManager() {
        return this.itemManager;
    }

    public void setItemManager(final ItemManager<? extends InventoryItem> itemManager0) {
        if (this.itemManager == itemManager0) { return; }
        this.itemManager = itemManager0;
        itemManagerContainer.setItemManager(itemManager0);
    }

    //========== Accessor/mutator methods
    public JPanel getPnlHeader()     { return pnlHeader;     }
    public FLabel getLblTitle()      { return lblTitle;      }
}
