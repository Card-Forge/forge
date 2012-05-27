package forge.gui.deckeditor.views;

import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.MatteBorder;

import net.miginfocom.swing.MigLayout;
import forge.Singletons;
import forge.card.CardEdition;
import forge.game.GameFormat;
import forge.gui.deckeditor.SFilterUtil;
import forge.gui.deckeditor.controllers.CFilters;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.framework.IVDoc;
import forge.gui.toolbox.FCheckBox;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FTextField;

/** 
 * Assembles Swing components of deck editor filter tab.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VFilters implements IVDoc {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Filters");

    // Text filter components
    private final JTextField txfContains = new FTextField();
    private final JTextField txfWithout = new FTextField();
    private final JLabel lblContains = new FLabel.Builder()
        .text("Contains:").fontSize(14).build();
    private final JLabel lblWithout = new FLabel.Builder()
        .text("Without:").fontSize(14).build();

    private final JCheckBox chbName = new FCheckBox("Name");
    private final JCheckBox chbType = new FCheckBox("Type");
    private final JCheckBox chbText = new FCheckBox("Text");

    // Interval filter components
    private final JComboBox cbxSets = new JComboBox();
    private final JComboBox cbxPLow = new JComboBox();
    private final JComboBox cbxPHigh = new JComboBox();
    private final JComboBox cbxTLow = new JComboBox();
    private final JComboBox cbxTHigh = new JComboBox();
    private final JComboBox cbxCMCLow = new JComboBox();
    private final JComboBox cbxCMCHigh = new JComboBox();

    private final JLabel lblP = new FLabel.Builder()
        .fontSize(12).text(" <= Power <= ").build();

    private final JLabel lblT = new FLabel.Builder()
        .fontSize(12).text(" <= Toughness <= ").build();

    private final JLabel lblCMC = new FLabel.Builder()
        .fontSize(12).text(" <= CMC <= ").build();

    // Title labels
    private final JLabel lblProperties = new FLabel.Builder()
        .text("Properties").tooltip("Filter by color, type, set, or format. Click to toggle.")
        .hoverable(true).fontSize(14).build();

    private final JLabel lblText = new FLabel.Builder()
        .text("Card Text").tooltip("Filter by card name, type, and text, space delimited. Click to reset.")
        .hoverable(true).fontSize(14).build();

    private final JLabel lblIntervals = new FLabel.Builder()
        .text("Intervals").tooltip("Filter by power, toughness, or converted mana cost. Click to reset.")
        .hoverable(true).fontSize(14).build();

    // Container components
    private final JPanel pnlText = new JPanel(new MigLayout(
            "insets 0, gap 0, wrap 3, ax center"));
    private final JPanel pnlIntervals = new JPanel(new MigLayout(
            "insets 0, gap 0, wrap 3, ax center"));

    private JPanel pnlContainer = new JPanel(new MigLayout("insets 0, gap 0, wrap"));
    private JScrollPane scroller = new JScrollPane(pnlContainer);

    //========== Constructor
    private VFilters() {
        String constraints = "";

        // Sets/formats combo box
        cbxSets.addItem("All sets and formats");
        for (final GameFormat s : Singletons.getModel().getFormats()) {
            cbxSets.addItem(s);
        }
        for (final CardEdition s : Singletons.getModel().getEditions()) {
            cbxSets.addItem(s);
        }

        // Color/type searches
        lblProperties.setBorder(new MatteBorder(0, 0, 1, 0,
                FSkin.getColor(FSkin.Colors.CLR_BORDERS)));
        lblText.setBorder(new MatteBorder(0, 0, 1, 0,
                FSkin.getColor(FSkin.Colors.CLR_BORDERS)));
        lblIntervals.setBorder(new MatteBorder(0, 0, 1, 0,
                FSkin.getColor(FSkin.Colors.CLR_BORDERS)));

        // Text search
        txfContains.setMargin(new Insets(5, 5, 5, 5));
        txfContains.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        txfContains.setOpaque(true);
        txfContains.setEditable(true);
        txfContains.setFocusable(true);
        txfContains.setOpaque(true);
        txfContains.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        txfWithout.setMargin(new Insets(5, 5, 5, 5));
        txfWithout.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        txfWithout.setOpaque(true);
        txfWithout.setEditable(true);
        txfWithout.setFocusable(true);
        txfWithout.setOpaque(true);
        txfWithout.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        pnlText.setOpaque(false);
        pnlText.add(lblText, "w 210px!, h 25px!");
        pnlText.add(lblContains, "w 80px!, h 30px!");
        pnlText.add(txfContains, "pushx, growx, span 2 1, gap 5px 5px 2px 2px, h 30px!");
        pnlText.add(lblWithout, "w 80px!, h 30px!");
        pnlText.add(txfWithout, "pushx, growx, span 2 1, gap 5px 5px 2px 2px, h 30px!");
        pnlText.add(chbName, "pushx, growx, w 70px!, h 25px!, gap 5px 5px 2px 2px");
        pnlText.add(chbType, "pushx, growx, w 70px!, h 25px!, gap 0 5px 2px 2px");
        pnlText.add(chbText, "w 60px!, h 25px!, gap 0 5px 2px 2px");

        chbName.setSelected(true);
        chbType.setSelected(true);
        chbText.setSelected(true);

        cbxPLow.addItem("*");
        cbxTLow.addItem("*");
        cbxCMCLow.addItem("*");

        // Interval search
        for (int i = 0; i < 10; i++) {
            cbxPLow.addItem(i);
            cbxTLow.addItem(i);
            cbxCMCLow.addItem(i);
            cbxPHigh.addItem(i);
            cbxTHigh.addItem(i);
            cbxCMCHigh.addItem(i);
        }

        cbxPHigh.addItem("10+");
        cbxTHigh.addItem("10+");
        cbxCMCHigh.addItem("10+");

        cbxPLow.setSelectedItem("*");
        cbxTLow.setSelectedItem("*");
        cbxCMCLow.setSelectedItem("*");
        cbxPHigh.setSelectedItem("10+");
        cbxTHigh.setSelectedItem("10+");
        cbxCMCHigh.setSelectedItem("10+");

        constraints = "w 80px!, h 25px!, gap 0 0 0 0";
        pnlIntervals.add(cbxPLow, constraints);
        pnlIntervals.add(lblP, "w 100px!, h 25px!");
        pnlIntervals.add(cbxPHigh, constraints);

        pnlIntervals.add(cbxTLow, constraints);
        pnlIntervals.add(lblT, "w 100px!, h 25px!");
        pnlIntervals.add(cbxTHigh, constraints);

        pnlIntervals.add(cbxCMCLow, constraints);
        pnlIntervals.add(lblCMC, "w 100px!, h 25px!");
        pnlIntervals.add(cbxCMCHigh, constraints);

        pnlIntervals.setOpaque(false);

        // Core layout
        final String constraints2 = "w 90%!, gap 5% 0 1% 0";
        pnlContainer.add(lblProperties, "w 90%!, h 25px!, gap 5% 0 1% 0");
        pnlContainer.add(SFilterUtil.populateColorFilters(), constraints2);
        pnlContainer.add(SFilterUtil.populateTypeFilters(), constraints2);
        pnlContainer.add(cbxSets, constraints2 + ", h 25px!");

        pnlContainer.add(lblText, "w 90%!, h 25px!, gap 5% 0 15px 0");
        pnlContainer.add(pnlText, constraints2);

        pnlContainer.add(lblIntervals, "w 90%!, h 25px!, gap 5% 0 15px 0");
        pnlContainer.add(pnlIntervals, constraints2);

        pnlContainer.setOpaque(false);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.setBorder(null);
        scroller.getViewport().setBorder(null);
    }

    //========== Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.EDITOR_FILTERS;
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
        return CFilters.SINGLETON_INSTANCE;
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
        parentCell.getBody().setLayout(new MigLayout("insets 0, gap 0, wrap"));
        parentCell.getBody().add(scroller, "w 96%!, h 96%, gap 2% 0 2% 0");
    }

    //========== Retrieval methods
    /** @return {javax.swing.JLabel} */
    public JLabel getBtnToggle() {
        return lblProperties;
    }

    /** @return {javax.swing.JLabel} */
    public JLabel getBtnResetText() {
        return lblText;
    }

    /** @return {javax.swing.JLabel} */
    public JLabel getBtnResetIntervals() {
        return lblIntervals;
    }

    /** @return {javax.swing.JComboBox} */
    public JComboBox getCbxSets() {
        return cbxSets;
    }

    /** @return {javax.swing.JTextField} */
    public JTextField getTxfContains() {
        return txfContains;
    }

    /** @return {javax.swing.JTextField} */
    public JTextField getTxfWithout() {
        return txfWithout;
    }

    /** @return {javax.swing.JComboBox} */
    public JComboBox getCbxPLow() {
        return cbxPLow;
    }

    /** @return {javax.swing.JComboBox} */
    public JComboBox getCbxPHigh() {
        return cbxPHigh;
    }

    /** @return {javax.swing.JComboBox} */
    public JComboBox getCbxTLow() {
        return cbxTLow;
    }

    /** @return {javax.swing.JComboBox} */
    public JComboBox getCbxTHigh() {
        return cbxTHigh;
    }

    /** @return {javax.swing.JComboBox} */
    public JComboBox getCbxCMCLow() {
        return cbxCMCLow;
    }

    /** @return {javax.swing.JComboBox} */
    public JComboBox getCbxCMCHigh() {
        return cbxCMCHigh;
    }

    /** @return {javax.swing.JCheckBox} */
    public JCheckBox getChbTextName() {
        return chbName;
    }

    /** @return {javax.swing.JCheckBox} */
    public JCheckBox getChbTextType() {
        return chbType;
    }

    /** @return {javax.swing.JCheckBox} */
    public JCheckBox getChbTextText() {
        return chbText;
    }
    //========== Custom class handling

}
