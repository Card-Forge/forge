package forge.screens.deckeditor.views;


import forge.StaticData;
import forge.itemmanager.SItemManagerUtil;
import forge.toolbox.FLabel;
import forge.toolbox.FPanel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public class VStatisticsImporter {

    private static VStatisticsImporter lastInstance = null;

    // Global stats
    private FLabel lblCardCountHeader = new FLabel.Builder()
            .text(Localizer.getInstance().getMessage("lblCardByColorTypeCMC")).tooltip(Localizer.getInstance().getMessage("lblBreakdownOfColorTypeCMC"))
            .fontStyle(Font.BOLD).fontSize(11).fontStyle(Font.BOLD).build();
    private FLabel lblShardCountHeader = new FLabel.Builder()
            .text(Localizer.getInstance().getMessage("lblColoredManaSymbolsINManaCost")).tooltip(Localizer.getInstance().getMessage("lblAmountOfManaSymbolsInManaCostOfCards"))
            .fontStyle(Font.BOLD).fontSize(11).fontStyle(Font.BOLD).build();

    // Total and color count labels
    private final JPanel pnlStats = new JPanel();
    private final FLabel lblMulti = buildLabel(SItemManagerUtil.StatTypes.MULTICOLOR, true);
    private final FLabel lblBlack = buildLabel(SItemManagerUtil.StatTypes.BLACK, false);
    private final FLabel lblBlue = buildLabel(SItemManagerUtil.StatTypes.BLUE, true);
    private final FLabel lblGreen = buildLabel(SItemManagerUtil.StatTypes.GREEN, false);
    private final FLabel lblRed = buildLabel(SItemManagerUtil.StatTypes.RED, true);
    private final FLabel lblWhite = buildLabel(SItemManagerUtil.StatTypes.WHITE, false);
    private final FLabel lblColorless = buildLabel(SItemManagerUtil.StatTypes.COLORLESS, true);

    // Colored mana symbol count labels
    private final FLabel lblWhiteShard = buildLabel(SItemManagerUtil.StatTypes.WHITE, true);
    private final FLabel lblBlueShard = buildLabel(SItemManagerUtil.StatTypes.BLUE, true);
    private final FLabel lblBlackShard = buildLabel(SItemManagerUtil.StatTypes.BLACK, true);
    private final FLabel lblRedShard = buildLabel(SItemManagerUtil.StatTypes.RED, false);
    private final FLabel lblGreenShard = buildLabel(SItemManagerUtil.StatTypes.GREEN, false);
    private final FLabel lblColorlessShard = buildLabel(SItemManagerUtil.StatTypes.COLORLESS, false);

    // Card type labels
    private final FLabel lblArtifact = buildLabel(SItemManagerUtil.StatTypes.ARTIFACT, true);
    private final FLabel lblCreature = buildLabel(SItemManagerUtil.StatTypes.CREATURE, false);
    private final FLabel lblEnchantment = buildLabel(SItemManagerUtil.StatTypes.ENCHANTMENT, true);
    private final FLabel lblInstant = buildLabel(SItemManagerUtil.StatTypes.INSTANT, false);
    private final FLabel lblLand = buildLabel(SItemManagerUtil.StatTypes.LAND, true);
    private final FLabel lblPlaneswalker = buildLabel(SItemManagerUtil.StatTypes.PLANESWALKER, false);
    private final FLabel lblSorcery = buildLabel(SItemManagerUtil.StatTypes.SORCERY, true);

    // CMC labels
    private final FLabel lblCMC0 = buildLabel(SItemManagerUtil.StatTypes.CMC_0, true);
    private final FLabel lblCMC1 = buildLabel(SItemManagerUtil.StatTypes.CMC_1, false);
    private final FLabel lblCMC2 = buildLabel(SItemManagerUtil.StatTypes.CMC_2, true);
    private final FLabel lblCMC3 = buildLabel(SItemManagerUtil.StatTypes.CMC_3, false);
    private final FLabel lblCMC4 = buildLabel(SItemManagerUtil.StatTypes.CMC_4, true);
    private final FLabel lblCMC5 = buildLabel(SItemManagerUtil.StatTypes.CMC_5, false);
    private final FLabel lblCMC6 = buildLabel(SItemManagerUtil.StatTypes.CMC_6, true);

    // Layout containers
    private final FScrollPane scroller = new FScrollPane(pnlStats, false);

    private static FLabel buildLabel(final FSkin.SkinImage icon, final boolean zebra) {
        final FLabel lbl = new FLabel.Builder().text("0 (0%)")
                .icon(icon).iconScaleAuto(false)
                .fontSize(11).build();

        if (zebra) {
            lbl.setOpaque(true);
            lbl.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        }

        return lbl;
    }

    private static FLabel buildLabel(final SItemManagerUtil.StatTypes statType, final boolean zebra) {
        return buildLabel(FSkin.getImage(statType.skinProp, 18, 18), zebra);
    }

    private VStatisticsImporter() {
        scroller.getViewport().setBorder(null);

        // Color stats
        lblMulti.setToolTipText(Localizer.getInstance().getMessage("lblMulticolorCardCount"));
        lblBlack.setToolTipText(Localizer.getInstance().getMessage("lblBlackCardCount"));
        lblBlue.setToolTipText(Localizer.getInstance().getMessage("lblBlueCardCount"));
        lblGreen.setToolTipText(Localizer.getInstance().getMessage("lblGreenCardCount"));
        lblRed.setToolTipText(Localizer.getInstance().getMessage("lblRedCardCount"));
        lblWhite.setToolTipText(Localizer.getInstance().getMessage("lblWhiteCardCount"));
        lblColorless.setToolTipText(Localizer.getInstance().getMessage("lblColorlessCardCount"));

        // Colored mana symbol count stats
        lblBlackShard.setToolTipText(Localizer.getInstance().getMessage("lblBlackManaSymbolCount"));
        lblBlueShard.setToolTipText(Localizer.getInstance().getMessage("lblBlueManaSymbolCount"));
        lblGreenShard.setToolTipText(Localizer.getInstance().getMessage("lblGreenManaSymbolCount"));
        lblRedShard.setToolTipText(Localizer.getInstance().getMessage("lblRedManaSymbolCount"));
        lblWhiteShard.setToolTipText(Localizer.getInstance().getMessage("lblWhiteManaSymbolCount"));

        // Type stats
        lblArtifact.setToolTipText(Localizer.getInstance().getMessage("lblArtifactCardCount"));
        lblCreature.setToolTipText(Localizer.getInstance().getMessage("lblCreatureCardCount"));
        lblEnchantment.setToolTipText(Localizer.getInstance().getMessage("lblEnchantmentCardCount"));
        lblInstant.setToolTipText(Localizer.getInstance().getMessage("lblInstantCardCount"));
        lblLand.setToolTipText(Localizer.getInstance().getMessage("lblLandCardCount"));
        lblPlaneswalker.setToolTipText(Localizer.getInstance().getMessage("lblPlaneswalkerCardCount"));
        lblSorcery.setToolTipText(Localizer.getInstance().getMessage("lblSorceryCardCount"));

        // CMC stats
        lblCMC0.setToolTipText(Localizer.getInstance().getMessage("lblCMCNCardCount", String.valueOf(0)));
        lblCMC1.setToolTipText(Localizer.getInstance().getMessage("lblCMCNCardCount", String.valueOf(1)));
        lblCMC2.setToolTipText(Localizer.getInstance().getMessage("lblCMCNCardCount", String.valueOf(2)));
        lblCMC3.setToolTipText(Localizer.getInstance().getMessage("lblCMCNCardCount", String.valueOf(3)));
        lblCMC4.setToolTipText(Localizer.getInstance().getMessage("lblCMCNCardCount", String.valueOf(4)));
        lblCMC5.setToolTipText(Localizer.getInstance().getMessage("lblCMCNCardCount", String.valueOf(5)));
        lblCMC6.setToolTipText(Localizer.getInstance().getMessage("lblCMCNCardCount", "6+"));

        // Stats container
        pnlStats.setOpaque(false);
        pnlStats.setLayout(new MigLayout("insets 0, gap 0, ax center, wrap 3"));

        // Add labels to container
        final String constraints = "w 35%!, h 35px!";
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
        pnlStats.add(lblColorlessShard, constraints);
    }

    public static VStatisticsImporter instance() {
        if (lastInstance == null)
            // singleton
            lastInstance = new VStatisticsImporter();
        return lastInstance;
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
    public FLabel getLblColorlessShard() { return lblColorlessShard; }

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

    /** @return {@link javax.swing.JPanel} */
    public JPanel getMainPanel() { return this.pnlStats; }
}
