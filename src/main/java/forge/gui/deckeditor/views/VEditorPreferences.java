package forge.gui.deckeditor.views;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import net.miginfocom.swing.MigLayout;
import forge.gui.deckeditor.controllers.CEditorPreferences;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.gui.toolbox.FCheckBox;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FSkin;

/** 
 * Assembles Swing components of deck editor analysis tab.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VEditorPreferences implements IVDoc<CEditorPreferences> {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Preferences");

    private JLabel lblStats = new FLabel.Builder()
        .text("General").tooltip("Configure high-level UI components")
        .fontSize(12).build();

    private JLabel lblCatalog = new FLabel.Builder()
        .text("Card Catalog Columns").tooltip("Toggle columns in card catalog panel")
        .fontSize(12).build();

    private JLabel lblDeck = new FLabel.Builder()
        .text("Current Deck Columns").tooltip("Toggle columns in current deck panel")
        .fontSize(12).build();

    private JLabel lblDisplay = new FLabel.Builder()
        .text("Card Catalog Options").tooltip("Toggle card catalog display options")
        .fontSize(12).build();

    private JCheckBox chbCatalogColor = new FCheckBox("Color");
    private JCheckBox chbCatalogRarity = new FCheckBox("Rarity");
    private JCheckBox chbCatalogCMC = new FCheckBox("CMC");
    private JCheckBox chbCatalogSet = new FCheckBox("Set");
    private JCheckBox chbCatalogAI = new FCheckBox("AI");
    private JCheckBox chbCatalogRanking = new FCheckBox("Ranking");
    private JCheckBox chbCatalogPower = new FCheckBox("Power");
    private JCheckBox chbCatalogToughness = new FCheckBox("Toughness");
    private JCheckBox chbCatalogOwned = new FCheckBox("Owned (Spell shop)");

    private JCheckBox chbDeckColor = new FCheckBox("Color");
    private JCheckBox chbDeckRarity = new FCheckBox("Rarity");
    private JCheckBox chbDeckCMC = new FCheckBox("CMC");
    private JCheckBox chbDeckSet = new FCheckBox("Set");
    private JCheckBox chbDeckAI = new FCheckBox("AI");
    private JCheckBox chbDeckRanking = new FCheckBox("Ranking");
    private JCheckBox chbDeckPower = new FCheckBox("Power");
    private JCheckBox chbDeckToughness = new FCheckBox("Toughness");

    private JCheckBox chbDeckStats = new FCheckBox("Show stats in current deck");
    private JCheckBox chbElasticColumns = new FCheckBox("Use elastic resizing when changing column widths");

    private JCheckBox chbCardDisplayUnique = new FCheckBox("Show unique cards only (only affects Constructed)");

    private JPanel pnl = new JPanel(new MigLayout("insets 0, gap 0, wrap 2, ax center"));
    private JScrollPane scroller = new JScrollPane(pnl);

    //========== Constructor
    private VEditorPreferences() {
        FSkin.get(lblStats).setMatteBorder(0, 0, 1, 0, FSkin.getColor(FSkin.Colors.CLR_BORDERS));
        FSkin.get(lblCatalog).setMatteBorder(0, 0, 1, 0, FSkin.getColor(FSkin.Colors.CLR_BORDERS));
        FSkin.get(lblDeck).setMatteBorder(0, 0, 1, 0, FSkin.getColor(FSkin.Colors.CLR_BORDERS));
        FSkin.get(lblDisplay).setMatteBorder(0, 0, 1, 0, FSkin.getColor(FSkin.Colors.CLR_BORDERS));

        FSkin.SkinFont font = FSkin.getFont(12);

        FSkin.get(chbCatalogColor).setFont(font);
        FSkin.get(chbCatalogRarity).setFont(font);
        FSkin.get(chbCatalogCMC).setFont(font);
        FSkin.get(chbCatalogSet).setFont(font);
        FSkin.get(chbCatalogAI).setFont(font);
        FSkin.get(chbCatalogRanking).setFont(font);
        FSkin.get(chbCatalogPower).setFont(font);
        FSkin.get(chbCatalogToughness).setFont(font);
        FSkin.get(chbCatalogOwned).setFont(font);

        FSkin.get(chbDeckColor).setFont(font);
        FSkin.get(chbDeckRarity).setFont(font);
        FSkin.get(chbDeckCMC).setFont(font);
        FSkin.get(chbDeckSet).setFont(font);
        FSkin.get(chbDeckAI).setFont(font);
        FSkin.get(chbDeckRanking).setFont(font);
        FSkin.get(chbDeckPower).setFont(font);
        FSkin.get(chbDeckToughness).setFont(font);

        FSkin.get(chbDeckStats).setFont(font);
        FSkin.get(chbElasticColumns).setFont(font);
        chbDeckStats.setSelected(true);
        chbElasticColumns.setSelected(false);

        FSkin.get(chbCardDisplayUnique).setFont(font);
        chbCardDisplayUnique.setSelected(false);

        pnl.add(lblStats, "h 25px!, gap 5px 5px 5px 5px, ax center, span 2 1");
        pnl.add(chbDeckStats, "h 25px!, gap 5px 5px 5px 5px, ax center, span 2 1");
        pnl.add(chbElasticColumns, "h 25px!, gap 5px 5px 5px 5px, ax center, span 2 1");

        final String constraints = "w 75px, h 25px!, gap 5px 5px 5px 5px, ax center";
        pnl.add(lblCatalog, constraints + ", span 2 1");
        pnl.add(chbCatalogColor, constraints);
        pnl.add(chbCatalogRarity, constraints);
        pnl.add(chbCatalogCMC, constraints);
        pnl.add(chbCatalogSet, constraints);
        pnl.add(chbCatalogPower, constraints);
        pnl.add(chbCatalogToughness, constraints);
        pnl.add(chbCatalogAI, constraints);
        pnl.add(chbCatalogRanking, constraints);
        pnl.add(chbCatalogOwned, constraints + ", wrap");

        pnl.add(lblDeck, constraints + ", span 2 1");
        pnl.add(chbDeckColor, constraints);
        pnl.add(chbDeckRarity, constraints);
        pnl.add(chbDeckCMC, constraints);
        pnl.add(chbDeckSet, constraints);
        pnl.add(chbDeckPower, constraints);
        pnl.add(chbDeckToughness, constraints);
        pnl.add(chbDeckAI, constraints + ", wrap");
        pnl.add(chbDeckRanking, constraints + ", wrap");

        pnl.add(lblDisplay, constraints + ", span 2 1");
        pnl.add(chbCardDisplayUnique, "h 25px!, gap 5px 5px 5px 5px, ax center, span 2 1");

        pnl.setOpaque(false);
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
        return EDocID.EDITOR_PREFERENCES;
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
    public CEditorPreferences getLayoutControl() {
        return CEditorPreferences.SINGLETON_INSTANCE;
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
    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getChbCatalogColor() {
        return chbCatalogColor;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getChbCatalogRarity() {
        return chbCatalogRarity;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getChbCatalogCMC() {
        return chbCatalogCMC;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getChbCatalogSet() {
        return chbCatalogSet;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getChbCatalogAI() {
        return chbCatalogAI;
    }
    
    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getChbCatalogRanking() {
        return chbCatalogRanking;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getChbCatalogPower() {
        return chbCatalogPower;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getChbCatalogToughness() {
        return chbCatalogToughness;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getChbCatalogOwned() {
        return chbCatalogOwned;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getChbDeckColor() {
        return chbDeckColor;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getChbDeckRarity() {
        return chbDeckRarity;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getChbDeckCMC() {
        return chbDeckCMC;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getChbDeckSet() {
        return chbDeckSet;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getChbDeckAI() {
        return chbDeckAI;
    }
    
    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getChbDeckRanking() {
        return chbDeckRanking;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getChbDeckPower() {
        return chbDeckPower;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getChbDeckToughness() {
        return chbDeckToughness;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getChbDeckStats() {
        return chbDeckStats;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getChbElasticColumns() {
        return chbElasticColumns;
    }
    
    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getChbCardDisplayUnique() {
        return chbCardDisplayUnique;
    }
}
