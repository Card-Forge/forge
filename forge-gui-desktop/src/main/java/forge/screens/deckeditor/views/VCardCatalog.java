package forge.screens.deckeditor.views;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.item.InventoryItem;
import forge.itemmanager.ItemManager;
import forge.itemmanager.ItemManagerContainer;
import forge.screens.deckeditor.controllers.CCardCatalog;

/**
 * Assembles Swing components of card catalog in deck editor.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum VCardCatalog implements IVDoc<CCardCatalog> {
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Card Catalog");

    private final ItemManagerContainer itemManagerContainer = new ItemManagerContainer();
    private ItemManager<? extends InventoryItem> itemManager;

    //========== Constructor
    /** */
    private VCardCatalog() {
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
    public void setParentCell(final DragCell cell0) {
        this.parentCell = cell0;
    }

    @Override
    public DragCell getParentCell() {
        return this.parentCell;
    }

    @Override
    public void populate() {
        final JPanel parentBody = parentCell.getBody();
        parentBody.setLayout(new MigLayout("insets 5, gap 0, wrap, hidemode 3"));
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
}
