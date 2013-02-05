package forge.gui.deckeditor.views;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import forge.gui.WrapLayout;
import forge.gui.deckeditor.SEditorUtil;
import forge.gui.deckeditor.controllers.CCurrentDeck;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FTextField;


/** 
 * Assembles Swing components of current deck being edited in deck editor.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VCurrentDeck implements IVDoc<CCurrentDeck>, ITableContainer {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Current Deck");

    // Other fields

    private final JLabel btnSave = new FLabel.Builder()
            .fontSize(14)
            .tooltip("Save (in default directory)")
            .iconInBackground(true)
            .iconAlignX(SwingConstants.CENTER).iconAlpha(1.0f)
            .icon(FSkin.getIcon(FSkin.InterfaceIcons.ICO_SAVE))
            .text(" ").hoverable(true).build();

    private final JLabel btnExport = new FLabel.Builder()
            .fontSize(14)
            .tooltip("Save As")
            .iconInBackground(true)
            .iconAlignX(SwingConstants.CENTER).iconAlpha(1.0f)
            .icon(FSkin.getIcon(FSkin.InterfaceIcons.ICO_SAVEAS))
            .text(" ").hoverable(true).build();

    private final JLabel btnImport = new FLabel.Builder()
            .fontSize(14)
            .tooltip("Load")
            .iconInBackground(true)
            .iconAlignX(SwingConstants.CENTER).iconAlpha(1.0f)
            .icon(FSkin.getIcon(FSkin.InterfaceIcons.ICO_OPEN))
            .text(" ").hoverable(true).build();

    private final JLabel btnNew = new FLabel.Builder()
            .fontSize(14)
            .tooltip("New Deck")
            .iconInBackground(true)
            .iconAlignX(SwingConstants.CENTER).iconAlpha(1.0f)
            .icon(FSkin.getIcon(FSkin.InterfaceIcons.ICO_NEW))
            .text(" ").hoverable(true).build();

    private final JLabel btnPrintProxies = new FLabel.Builder()
            .fontSize(14)
            .tooltip("Print Proxies")
            .iconInBackground(true)
            .iconAlignX(SwingConstants.CENTER).iconAlpha(1.0f)
            .icon(FSkin.getIcon(FSkin.InterfaceIcons.ICO_PRINT))
            .text(" ").hoverable(true).build();

    private final JPanel pnlRemoveButtons =
            new JPanel(new MigLayout("insets 0, gap 0, ax center, hidemode 3"));

    private final FLabel btnRemove = new FLabel.Builder()
            .fontSize(14)
            .text("Remove card")
            .tooltip("Remove selected card from current deck (or double click the row or hit the spacebar)")
            .icon(FSkin.getIcon(FSkin.InterfaceIcons.ICO_MINUS))
            .iconScaleAuto(false).hoverable(true).build();

    private final FLabel btnRemove4 = new FLabel.Builder()
            .fontSize(14)
            .text("Remove 4 of card")
            .tooltip("Remove up to 4 of selected card to current deck")
            .icon(FSkin.getIcon(FSkin.InterfaceIcons.ICO_MINUS))
            .iconScaleAuto(false).hoverable(true).build();

    private final JLabel btnDoSideboard = new FLabel.Builder()
            .fontSize(14)
            .text("Deck/Sideboard")
            .tooltip("Edit the sideboard for this deck")
            .icon(FSkin.getIcon(FSkin.InterfaceIcons.ICO_EDIT))
            .iconScaleAuto(false).hoverable(true).build();

    private final JTextField txfTitle = new FTextField.Builder().text("[New Deck]").build();

    private final JPanel pnlRemove = new JPanel();
    private final JPanel pnlHeader = new JPanel();

    private final JLabel lblTitle = new FLabel.Builder().text("Title")
            .fontSize(14).build();

    // Total and color count labels/filter toggles
    private final JPanel pnlStats = new JPanel(new MigLayout("insets 0, gap 0, ax center"));
    private final JPanel pnlStatsWrap = new JPanel(new WrapLayout(FlowLayout.LEFT));
    private final Map<SEditorUtil.StatTypes, FLabel> statLabels =
            new HashMap<SEditorUtil.StatTypes, FLabel>();

    private JTable tblCards = null;
    private final JScrollPane scroller = new JScrollPane(tblCards);

    //========== Constructor

    private VCurrentDeck() {
        // Header area
        pnlHeader.setOpaque(false);
        pnlHeader.setLayout(new MigLayout("insets 0, gap 0, ax center, hidemode 3"));

        pnlHeader.add(lblTitle, "w 80px!, h 30px!, gap 5px 5px 0 0");
        pnlHeader.add(txfTitle, "pushx, growx, gap 0 5px 0 0");
        pnlHeader.add(btnSave, "w 26px!, h 26px!, gap 0 5px 0 0");
        pnlHeader.add(btnNew, "w 26px!, h 26px!, gap 0 5px 0 0");

        pnlHeader.add(btnImport, "w 26px!, h 26px!, gap 0 5px 0 0");
        pnlHeader.add(btnExport, "w 26px!, h 26px!, gap 0 5px 0 0");
        pnlHeader.add(btnPrintProxies, "w 26px!, h 26px!, gap 0 20px 0 0");

        pnlRemove.setOpaque(false);
        pnlRemove.setLayout(new MigLayout("insets 0, gap 0, ax center"));
        pnlRemove.add(btnRemove, "w 30%!, h 30px!, gap 10 10 5 5");
        pnlRemove.add(btnRemove4, "w 30%!, h 30px!, gap 10 10 5 5");
        pnlRemove.add(btnDoSideboard, "w 30%!, h 30px!, gap 10 10 5 5");

        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.setBorder(null);
        scroller.getViewport().setBorder(null);

        pnlStats.setOpaque(false);
        pnlStatsWrap.setOpaque(false);
        
        for (SEditorUtil.StatTypes s : SEditorUtil.StatTypes.values()) {
            FLabel label = buildLabel(s);
            statLabels.put(s, label);
            if (SEditorUtil.StatTypes.TOTAL == s) {
                pnlStats.add(label, "align right");
            } else {
                pnlStatsWrap.add(label);
            }
        }
        pnlStats.add(pnlStatsWrap, "w 30:480:480, align left");

        pnlRemoveButtons.setOpaque(false);
        pnlRemoveButtons.add(btnRemove, "w 30%!, h 30px!, gap 0 0 5px 5px");
        pnlRemoveButtons.add(btnRemove4, "w 30%!, h 30px!, gap 0 0 5px 5px");
        pnlRemoveButtons.add(btnDoSideboard, "w 30%!, h 30px!, gap 0 0 5px 5px");
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
        parentBody.setLayout(new MigLayout("insets 0, gap 0, wrap, hidemode 3"));
        parentBody.add(pnlHeader, "w 98%!, gap 1% 1% 5 0");
        parentBody.add(pnlStats, "w 96%, gap 1% 1% 5 0, center");
        parentBody.add(pnlRemoveButtons, "w 96%!, gap 2% 0 0 0");
        parentBody.add(scroller, "w 98%!, h 10:100%:100%, gap 1% 0 0 1%");
    }

    //========== Retrieval methods

    //========== Custom class handling

    //========== Overridden from ITableContainer
    /* (non-Javadoc)
     * @see forge.gui.deckeditor.views.ITableContainer#setTableView()
     */
    @Override
    public void setTableView(final JTable tbl0) {
        this.tblCards = tbl0;
        scroller.setViewportView(tblCards);
    }

    public JLabel getLblTitle() { return lblTitle; }

    @Override
    public FLabel getStatLabel(SEditorUtil.StatTypes s) {
        return statLabels.get(s);
    }

    //========== Retrieval

    /** @return {@link javax.swing.JLabel} */
    public FLabel getBtnRemove() {
        return btnRemove;
    }

    /** @return {@link javax.swing.JLabel} */
    public FLabel getBtnRemove4() {
        return btnRemove4;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getBtnSave() {
        return btnSave;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getBtnSaveAs() {
        return btnExport;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getBtnPrintProxies() {
        return btnPrintProxies;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getBtnOpen() {
        return btnImport;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getBtnNew() {
        return btnNew;
    }

    /** @return {@link javax.swing.JTextField} */
    public JTextField getTxfTitle() {
        return txfTitle;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getPnlHeader() {
        return pnlHeader;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getPnlStats() {
        return pnlStats;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getPnlRemButtons() {
        return pnlRemoveButtons;
    }

    /** @return {@link javax.swing.JPanel} */
    public JLabel getBtnDoSideboard() {
        return btnDoSideboard;
    }

    //========== Other methods

    private FLabel buildLabel(SEditorUtil.StatTypes s) {
        FLabel label = new FLabel.Builder()
            .icon(s.img).iconScaleAuto(false)
            .text("0").fontSize(11)
            .tooltip(s.toLabelString())
            .build();

        Dimension labelSize = new Dimension(60, 20);
        label.setPreferredSize(labelSize);
        label.setMinimumSize(labelSize);
        
        return label;
    }
}
