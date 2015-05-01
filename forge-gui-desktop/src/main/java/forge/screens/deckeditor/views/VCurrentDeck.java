package forge.screens.deckeditor.views;

import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import forge.assets.FSkinProp;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.item.InventoryItem;
import forge.itemmanager.ItemManager;
import forge.itemmanager.ItemManagerContainer;
import forge.screens.deckeditor.controllers.CCurrentDeck;
import forge.toolbox.FLabel;
import forge.toolbox.FSkin;
import forge.toolbox.FTextField;

/**
 * Assembles Swing components of current deck being edited in deck editor.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VCurrentDeck implements IVDoc<CCurrentDeck> {
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Current Deck");

    // Other fields

    private final FLabel btnSave = new FLabel.Builder()
            .fontSize(14)
            .tooltip("Save Deck (Ctrl+S)")
            .iconInBackground(true)
            .iconAlignX(SwingConstants.CENTER)
            .icon(FSkin.getIcon(FSkinProp.ICO_SAVE))
            .text(" ").hoverable(true).build();

    private final FLabel btnSaveAs = new FLabel.Builder()
            .fontSize(14)
            .tooltip("Save Deck As (Ctrl+E)")
            .iconInBackground(true)
            .iconAlignX(SwingConstants.CENTER)
            .icon(FSkin.getIcon(FSkinProp.ICO_SAVEAS))
            .text(" ").hoverable(true).build();

    private final FLabel btnLoad = new FLabel.Builder()
            .fontSize(14)
            .tooltip("Open Deck (Ctrl+O)")
            .iconInBackground(true)
            .iconAlignX(SwingConstants.CENTER)
            .icon(FSkin.getIcon(FSkinProp.ICO_OPEN))
            .text(" ").hoverable(true).build();

    private final FLabel btnNew = new FLabel.Builder()
            .fontSize(14)
            .tooltip("New Deck (Ctrl+N)")
            .iconInBackground(true)
            .iconAlignX(SwingConstants.CENTER)
            .icon(FSkin.getIcon(FSkinProp.ICO_NEW))
            .text(" ").hoverable(true).build();

    private final FLabel btnPrintProxies = new FLabel.Builder()
            .fontSize(14)
            .tooltip("Print to HTML file (Ctrl+P)")
            .iconInBackground(true)
            .iconAlignX(SwingConstants.CENTER)
            .icon(FSkin.getIcon(FSkinProp.ICO_PRINT))
            .text(" ").hoverable(true).build();

    private final FLabel btnImport = new FLabel.Builder()
            .fontSize(14)
            .text("Import")
            .tooltip("Attempt to import a deck from a non-Forge format (Ctrl+I)")
            .opaque(true).hoverable(true).build();

    private final FTextField txfTitle = new FTextField.Builder().ghostText("[New Deck]").build();

    private final JPanel pnlHeader = new JPanel();

    private final FLabel lblTitle = new FLabel.Builder().text("Title").fontSize(14).build();

    private final ItemManagerContainer itemManagerContainer = new ItemManagerContainer();
    private ItemManager<? extends InventoryItem> itemManager;

    //========== Constructor

    private VCurrentDeck() {
        // Header area
        pnlHeader.setOpaque(false);
        pnlHeader.setLayout(new MigLayout("insets 3, gapx 3, hidemode 3"));

        pnlHeader.add(lblTitle, "h 26px!");
        pnlHeader.add(txfTitle, "pushx, growx");
        pnlHeader.add(btnSave, "w 26px!, h 26px!");
        pnlHeader.add(btnNew, "w 26px!, h 26px!");

        pnlHeader.add(btnLoad, "w 26px!, h 26px!");
        pnlHeader.add(btnSaveAs, "w 26px!, h 26px!");
        pnlHeader.add(btnPrintProxies, "w 26px!, h 26px!");
        pnlHeader.add(btnImport, "w 61px!, h 26px!");
    }

    //========== Overridden from IVDoc

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.EDITOR_CURRENTDECK;
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
    public CCurrentDeck getLayoutControl() {
        return CCurrentDeck.SINGLETON_INSTANCE;
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
        final JPanel parentBody = parentCell.getBody();
        parentBody.setLayout(new MigLayout("insets 5, gap 0 3, wrap, hidemode 3"));
        parentBody.add(pnlHeader, "pushx, growx");
        parentBody.add(itemManagerContainer, "push, grow");
    }

    public ItemManager<? extends InventoryItem> getItemManager() {
        return this.itemManager;
    }

    public void setItemManager(final ItemManager<? extends InventoryItem> itemManager0) {
        this.itemManager = itemManager0;
        itemManagerContainer.setItemManager(itemManager0);
    }

    public FLabel getLblTitle() { return lblTitle; }

    //========== Retrieval

    /** @return {@link javax.swing.JLabel} */
    public FLabel getBtnSave() {
        return btnSave;
    }

    /** @return {@link javax.swing.JLabel} */
    public FLabel getBtnSaveAs() {
        return btnSaveAs;
    }

    /** @return {@link javax.swing.JLabel} */
    public FLabel getBtnPrintProxies() {
        return btnPrintProxies;
    }

    /** @return {@link javax.swing.JLabel} */
    public FLabel getBtnOpen() {
        return btnLoad;
    }

    /** @return {@link javax.swing.JLabel} */
    public FLabel getBtnNew() {
        return btnNew;
    }

    /** @return {@link forge.gui.toolbar.FTextField} */
    public FTextField getTxfTitle() {
        return txfTitle;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getPnlHeader() {
        return pnlHeader;
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public FLabel getBtnImport() {
        return btnImport;
    }
}
