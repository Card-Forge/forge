package forge.screens.deckeditor.views;

import java.awt.Font;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.itemmanager.SItemManagerUtil.StatTypes;
import forge.screens.deckeditor.controllers.CStatistics;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinImage;

/**
 * Assembles Swing components of deck editor analysis tab.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VStatistics implements IVDoc<CStatistics> {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Statistics");

    // Global stats
    private FLabel lblTotal = new FLabel.Builder()
            .text("TOTAL CARDS: 0").tooltip("Total cards")
            .fontStyle(Font.BOLD).fontSize(11).fontStyle(Font.BOLD).build();
    private FLabel lblTMC = new FLabel.Builder()
            .text("TOTAL MANA COST: 0").tooltip("Total mana cost")
            .fontStyle(Font.BOLD).fontSize(11).fontStyle(Font.BOLD).build();
    private FLabel lblAMC = new FLabel.Builder()
            .text("AVERAGE MANA COST: 0.00").tooltip("Average mana cost")
            .fontStyle(Font.BOLD).fontSize(11).fontStyle(Font.BOLD).build();
    private FLabel lblCardCountHeader = new FLabel.Builder()
            .text("CARDS BY COLOR, TYPE AND CMC:").tooltip("Breakdown of cards by color, type and CMC")
            .fontStyle(Font.BOLD).fontSize(11).fontStyle(Font.BOLD).build();
    private FLabel lblShardCountHeader = new FLabel.Builder()
            .text("COLORED MANA SYMBOLS IN MANA COST:").tooltip("Amount of mana symbols in mana cost of cards")
            .fontStyle(Font.BOLD).fontSize(11).fontStyle(Font.BOLD).build();

    // Total and color count labels
    private final JPanel pnlStats = new JPanel();
    private final FLabel lblMulti = buildLabel(StatTypes.MULTICOLOR, true);
    private final FLabel lblBlack = buildLabel(StatTypes.BLACK, false);
    private final FLabel lblBlue = buildLabel(StatTypes.BLUE, true);
    private final FLabel lblGreen = buildLabel(StatTypes.GREEN, false);
    private final FLabel lblRed = buildLabel(StatTypes.RED, true);
    private final FLabel lblWhite = buildLabel(StatTypes.WHITE, false);
    private final FLabel lblColorless = buildLabel(StatTypes.COLORLESS, true);

    // Colored mana symbol count labels
    private final FLabel lblWhiteShard = buildLabel(StatTypes.WHITE, true);
    private final FLabel lblBlueShard = buildLabel(StatTypes.BLUE, true);
    private final FLabel lblBlackShard = buildLabel(StatTypes.BLACK, true);
    private final FLabel lblRedShard = buildLabel(StatTypes.RED, false);
    private final FLabel lblGreenShard = buildLabel(StatTypes.GREEN, false);

    // Card type labels
    private final FLabel lblArtifact = buildLabel(StatTypes.ARTIFACT, true);
    private final FLabel lblCreature = buildLabel(StatTypes.CREATURE, false);
    private final FLabel lblEnchantment = buildLabel(StatTypes.ENCHANTMENT, true);
    private final FLabel lblInstant = buildLabel(StatTypes.INSTANT, false);
    private final FLabel lblLand = buildLabel(StatTypes.LAND, true);
    private final FLabel lblPlaneswalker = buildLabel(StatTypes.PLANESWALKER, false);
    private final FLabel lblSorcery = buildLabel(StatTypes.SORCERY, true);

    // CMC labels
    private final FLabel lblCMC0 = buildLabel(StatTypes.CMC_0, true);
    private final FLabel lblCMC1 = buildLabel(StatTypes.CMC_1, false);
    private final FLabel lblCMC2 = buildLabel(StatTypes.CMC_2, true);
    private final FLabel lblCMC3 = buildLabel(StatTypes.CMC_3, false);
    private final FLabel lblCMC4 = buildLabel(StatTypes.CMC_4, true);
    private final FLabel lblCMC5 = buildLabel(StatTypes.CMC_5, false);
    private final FLabel lblCMC6 = buildLabel(StatTypes.CMC_6, true);

    // Layout containers
    private final FScrollPane scroller = new FScrollPane(pnlStats, false);

    //========== Constructor
    private VStatistics() {
        scroller.getViewport().setBorder(null);

        // Color stats
        lblMulti.setToolTipText("Multicolor Card Count");
        lblBlack.setToolTipText("Black Card Count");
        lblBlue.setToolTipText("Blue Card Count");
        lblGreen.setToolTipText("Green Card Count");
        lblRed.setToolTipText("Red Card Count");
        lblWhite.setToolTipText("White Card Count");
        lblColorless.setToolTipText("Colorless Card Count");

        // Colored mana symbol count stats
        lblBlackShard.setToolTipText("Black Mana Symbol Count");
        lblBlueShard.setToolTipText("Blue Mana Symbol Count");
        lblGreenShard.setToolTipText("Green Mana Symbol Count");
        lblRedShard.setToolTipText("Red Mana Symbol Count");
        lblWhiteShard.setToolTipText("White Mana Symbol Count");

        // Type stats
        lblArtifact.setToolTipText("Artifact Card Count");
        lblCreature.setToolTipText("Creature Card Count");
        lblEnchantment.setToolTipText("Enchantment Card Count");
        lblInstant.setToolTipText("Instant Card Count");
        lblLand.setToolTipText("Land Card Count");
        lblPlaneswalker.setToolTipText("Planeswalker Card Count");
        lblSorcery.setToolTipText("Sorcery Card Count");

        // CMC stats
        lblCMC0.setToolTipText("CMC 0 Card Count");
        lblCMC1.setToolTipText("CMC 1 Card Count");
        lblCMC2.setToolTipText("CMC 2 Card Count");
        lblCMC3.setToolTipText("CMC 3 Card Count");
        lblCMC4.setToolTipText("CMC 4 Card Count");
        lblCMC5.setToolTipText("CMC 5 Card Count");
        lblCMC6.setToolTipText("CMC 6+ Card Count");

        // Stats container
        pnlStats.setOpaque(false);
        pnlStats.setLayout(new MigLayout("insets 0, gap 0, ax center, wrap 3"));

        pnlStats.add(lblTotal, "w 96%!, h 20px!, span 3 1, gap 2% 0 0 0");
        pnlStats.add(lblTMC, "w 96%!, h 20px!, span 3 1, gap 2% 0 0 0");
        pnlStats.add(lblAMC, "w 96%!, h 20px!, span 3 1, gap 2% 0 0 0");

        // Add labels to container
        final String constraints = "w 32%!, h 35px!";
        pnlStats.add(lblCardCountHeader, "w 96%!, h 40px!, span 3 1, gap 2% 0 0 0");

        pnlStats.add(lblMulti, constraints);
        pnlStats.add(lblArtifact, constraints);
        pnlStats.add(lblCMC0, constraints);

        pnlStats.add(lblBlack, constraints);
        pnlStats.add(lblCreature, constraints);
        pnlStats.add(lblCMC1, constraints);

        pnlStats.add(lblBlue, constraints);
        pnlStats.add(lblEnchantment, constraints);
        pnlStats.add(lblCMC2, constraints);

        pnlStats.add(lblGreen, constraints);
        pnlStats.add(lblInstant, constraints);
        pnlStats.add(lblCMC3, constraints);

        pnlStats.add(lblRed, constraints);
        pnlStats.add(lblLand, constraints);
        pnlStats.add(lblCMC4, constraints);

        pnlStats.add(lblWhite, constraints);
        pnlStats.add(lblPlaneswalker, constraints);
        pnlStats.add(lblCMC5, constraints);

        pnlStats.add(lblColorless, constraints);
        pnlStats.add(lblSorcery, constraints);
        pnlStats.add(lblCMC6, constraints);

        // Shard count stats container
        pnlStats.add(lblShardCountHeader, "w 96%!, h 40px!, span 3 1, gap 2% 0 0 0");
        pnlStats.add(lblWhiteShard, constraints);
        pnlStats.add(lblBlueShard, constraints);
        pnlStats.add(lblBlackShard, constraints);
        pnlStats.add(lblRedShard, constraints);
        pnlStats.add(lblGreenShard, constraints);
    }

    //========== Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.EDITOR_STATISTICS;
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
    public CStatistics getLayoutControl() {
        return CStatistics.SINGLETON_INSTANCE;
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
        parentCell.getBody().setLayout(new MigLayout("insets 0, gap 0"));
        parentCell.getBody().add(scroller, "w 96%!, h 96%!, gap 2% 0 2% 0");
    }

    //========== Retrieval methods

    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getLblMulti() { return lblMulti; }
    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getLblBlack() { return lblBlack; }
    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getLblBlue() { return lblBlue; }
    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getLblGreen() { return lblGreen; }
    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getLblRed() { return lblRed; }
    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getLblWhite() { return lblWhite; }
    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getLblColorless() { return lblColorless; }

    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getLblBlackShard() { return lblBlackShard; }
    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getLblBlueShard() { return lblBlueShard; }
    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getLblGreenShard() { return lblGreenShard; }
    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getLblRedShard() { return lblRedShard; }
    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getLblWhiteShard() { return lblWhiteShard; }

    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getLblArtifact() { return lblArtifact; }
    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getLblEnchantment() { return lblEnchantment; }
    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getLblCreature() { return lblCreature; }
    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getLblSorcery() { return lblSorcery; }
    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getLblInstant() { return lblInstant; }
    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getLblPlaneswalker() { return lblPlaneswalker; }
    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getLblLand() { return lblLand; }

    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getLblCMC0() { return lblCMC0; }
    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getLblCMC1() { return lblCMC1; }
    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getLblCMC2() { return lblCMC2; }
    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getLblCMC3() { return lblCMC3; }
    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getLblCMC4() { return lblCMC4; }
    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getLblCMC5() { return lblCMC5; }
    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getLblCMC6() { return lblCMC6; }
    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getLblTotal() { return lblTotal; }
    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getLblTMC() { return lblTMC; }
    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getLblAMC() { return lblAMC; }

    //========== Other methods

    private static FLabel buildLabel(final SkinImage icon, final boolean zebra) {
        final FLabel lbl = new FLabel.Builder().text("0 (0%)")
                .icon(icon).iconScaleAuto(false)
                .fontSize(11).build();

        if (zebra) {
            lbl.setOpaque(true);
            lbl.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        }

        return lbl;
    }

    private static FLabel buildLabel(final StatTypes statType, final boolean zebra) {
        return buildLabel(FSkin.getImage(statType.skinProp, 18, 18), zebra);
    }
}
