package forge.gui.deckeditor.views;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import net.miginfocom.swing.MigLayout;
import forge.gui.deckeditor.SEditorUtil;
import forge.gui.deckeditor.controllers.CCardCatalog;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.framework.IVDoc;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FSkin;

/** 
 * Assembles Swing components of card catalog in deck editor.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 * 
 */
public enum VCardCatalog implements IVDoc, ITableContainer {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Card Catalog");

    // Total and color count labels
    private final JPanel pnlStats = new JPanel();
    private final JLabel lblTotal = buildLabel(SEditorUtil.ICO_TOTAL);
    private final JLabel lblBlack = buildLabel(SEditorUtil.ICO_BLACK);
    private final JLabel lblBlue = buildLabel(SEditorUtil.ICO_BLUE);
    private final JLabel lblGreen = buildLabel(SEditorUtil.ICO_GREEN);
    private final JLabel lblRed = buildLabel(SEditorUtil.ICO_RED);
    private final JLabel lblWhite = buildLabel(SEditorUtil.ICO_WHITE);
    private final JLabel lblColorless = buildLabel(SEditorUtil.ICO_COLORLESS);

    // Card type labels
    private final JLabel lblArtifact = buildLabel(SEditorUtil.ICO_ARTIFACT);
    private final JLabel lblCreature = buildLabel(SEditorUtil.ICO_CREATURE);
    private final JLabel lblEnchantment = buildLabel(SEditorUtil.ICO_ENCHANTMENT);
    private final JLabel lblInstant = buildLabel(SEditorUtil.ICO_INSTANT);
    private final JLabel lblLand = buildLabel(SEditorUtil.ICO_LAND);
    private final JLabel lblPlaneswalker = buildLabel(SEditorUtil.ICO_PLANESWALKER);
    private final JLabel lblSorcery = buildLabel(SEditorUtil.ICO_SORCERY);

    private final JLabel lblTitle = new FLabel.Builder()
            .fontScaleAuto(false).fontSize(14).build();

    private final JPanel pnlHeader = new JPanel(new MigLayout("insets 0, gap 0"));

    private final JPanel pnlAddButtons =
            new JPanel(new MigLayout("insets 0, gap 0, ax center, hidemode 3"));

    private final JLabel btnAdd = new FLabel.Builder()
            .fontScaleAuto(false).fontSize(14)
            .text("Add card")
            .tooltip("Add selected card to current deck (or double click the row)")
            .icon(FSkin.getIcon(FSkin.ForgeIcons.ICO_PLUS))
            .iconScaleAuto(false).hoverable(true).build();

    private final JLabel btnAdd4 = new FLabel.Builder()
            .fontScaleAuto(false).fontSize(14)
            .text("Add 4 of card")
            .tooltip("Add up to 4 of selected card to current deck")
            .icon(FSkin.getIcon(FSkin.ForgeIcons.ICO_PLUS))
            .iconScaleAuto(false).hoverable(true).build();

    private JTable tblCards = null;
    private final JScrollPane scroller = new JScrollPane();

    //========== Constructor
    /** */
    private VCardCatalog() {
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.setBorder(null);
        scroller.getViewport().setBorder(null);

        lblTotal.setToolTipText("Total Card Count");
        lblBlack.setToolTipText("Black Card Count");
        lblBlue.setToolTipText("Blue Card Count");
        lblGreen.setToolTipText("Green Card Count");
        lblRed.setToolTipText("Red Card Count");
        lblWhite.setToolTipText("White Card Count");
        lblColorless.setToolTipText("Total Card Count");
        lblArtifact.setToolTipText("Artiface Card Count");
        lblCreature.setToolTipText("Creature Card Count");
        lblColorless.setToolTipText("Colorless Card Count");
        lblEnchantment.setToolTipText("Enchantment Card Count");
        lblInstant.setToolTipText("Instant Card Count");
        lblLand.setToolTipText("Land Card Count");
        lblPlaneswalker.setToolTipText("Planeswalker Card Count");
        lblSorcery.setToolTipText("Sorcery Card Count");

        pnlStats.setOpaque(false);
        pnlStats.setLayout(new MigLayout("insets 0, gap 5px, ax center, wrap 7"));

        final String constraints = "w 55px!, h 20px!";
        pnlStats.add(lblTotal, constraints);
        pnlStats.add(lblBlack, constraints);
        pnlStats.add(lblBlue, constraints);
        pnlStats.add(lblGreen, constraints);
        pnlStats.add(lblRed, constraints);
        pnlStats.add(lblWhite, constraints);
        pnlStats.add(lblColorless, constraints);

        pnlStats.add(lblArtifact, constraints);
        pnlStats.add(lblCreature, constraints);
        pnlStats.add(lblEnchantment, constraints);
        pnlStats.add(lblInstant, constraints);
        pnlStats.add(lblLand, constraints);
        pnlStats.add(lblPlaneswalker, constraints);
        pnlStats.add(lblSorcery, constraints);

        pnlAddButtons.setOpaque(false);
        pnlAddButtons.add(btnAdd, "w 42%!, h 30px!, gap 0 0 5px 5px");
        pnlAddButtons.add(btnAdd4, "w 42%!, h 30px!, gap 5% 5% 5px 5px");

        pnlHeader.setOpaque(false);
        pnlHeader.add(lblTitle, "w 100%!, h 100%!");
    }

    //========== Overridden from IVDoc

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.EDITOR_CATALOG;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getControl()
     */
    @Override
    public ICDoc getControl() {
        return CCardCatalog.SINGLETON_INSTANCE;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#setParentCell(forge.gui.framework.DragCell)
     */
    @Override
    public void setParentCell(DragCell cell0) {
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
        parentCell.getBody().setLayout(new MigLayout("insets 0, gap 0, wrap, hidemode 3"));
        parentCell.getBody().add(pnlHeader, "w 98%!, h 30px!, gap 1% 0 1% 10px");
        parentCell.getBody().add(pnlStats, "w 96%, h 50px!, gap 2% 0 1% 1%");
        parentCell.getBody().add(pnlAddButtons, "w 96%!, gap 2% 0 0 0");
        parentCell.getBody().add(scroller, "w 98%!, h 100% - 35px, gap 1% 0 1% 1%");
    }

    //========== Overridden from ITableContainer
    /* (non-Javadoc)
     * @see forge.gui.deckeditor.views.ITableContainer#setTableView()
     */
    @Override
    public void setTableView(final JTable tbl0) {
        this.tblCards = tbl0;
        scroller.setViewportView(tblCards);
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.views.ITableContainer#getLblTotal()
     */
    @Override
    public JLabel getLblTotal() { return lblTotal; }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.views.ITableContainer#getLblBlack()
     */
    @Override
    public JLabel getLblBlack() { return lblBlack; }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.views.ITableContainer#getLblBlue()
     */
    @Override
    public JLabel getLblBlue() { return lblBlue; }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.views.ITableContainer#getLblGreen()
     */
    @Override
    public JLabel getLblGreen() { return lblGreen; }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.views.ITableContainer#getLblRed()
     */
    @Override
    public JLabel getLblRed() { return lblRed; }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.views.ITableContainer#getLblWhite()
     */
    @Override
    public JLabel getLblWhite() { return lblWhite; }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.views.ITableContainer#getLblColorless()
     */
    @Override
    public JLabel getLblColorless() { return lblColorless; }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.views.ITableContainer#getLblArtifact()
     */
    @Override
    public JLabel getLblArtifact() { return lblArtifact; }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.views.ITableContainer#getLblEnchantment()
     */
    @Override
    public JLabel getLblEnchantment() { return lblEnchantment; }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.views.ITableContainer#getLblCreature()
     */
    @Override
    public JLabel getLblCreature() { return lblCreature; }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.views.ITableContainer#getLblSorcery()
     */
    @Override
    public JLabel getLblSorcery() { return lblSorcery; }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.views.ITableContainer#getLblInstant()
     */
    @Override
    public JLabel getLblInstant() { return lblInstant; }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.views.ITableContainer#getLblPlaneswalker()
     */
    @Override
    public JLabel getLblPlaneswalker() { return lblPlaneswalker; }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.views.ITableContainer#getLblLand()
     */
    @Override
    public JLabel getLblLand() { return lblLand; }

    //========== Accessor/mutator methods

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblTitle() {
        return lblTitle;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getBtnAdd() {
        return btnAdd;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getBtnAdd4() {
        return btnAdd4;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getPnlHeader() {
        return pnlHeader;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getPnlStats() {
        return pnlStats;
    }

    //========== Other methods

    private JLabel buildLabel(final ImageIcon icon0) {
        final JLabel lbl = new FLabel.Builder().text("0")
                .icon(icon0).iconScaleAuto(false)
                .fontSize(11).fontScaleAuto(false)
                .build();

            return lbl;
    }
}
