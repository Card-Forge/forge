package forge.gui.home.gauntlet;

import java.awt.Font;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.home.EMenuGroup;
import forge.gui.home.IVSubmenu;
import forge.gui.home.StartButton;
import forge.gui.home.VHomeUI;
import forge.gui.toolbox.FCheckBox;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FList;
import forge.gui.toolbox.FPanel;
import forge.gui.toolbox.FRadioButton;
import forge.gui.toolbox.FScrollPane;
import forge.gui.toolbox.FSkin;

/** 
 * Assembles Swing components of "quick gauntlet" submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum VSubmenuGauntletQuick implements IVSubmenu {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Quick Gauntlets");

    // Other fields
    private final FPanel pnlOptions = new FPanel(new MigLayout("insets 0, gap 0, wrap"));
    private final FPanel pnlDecks = new FPanel();
    private final FLabel lblTitle = new FLabel.Builder()
        .text("Quick Gauntlet Builder").fontAlign(SwingConstants.CENTER)
        .opaque(true).fontSize(16).build();

    private final FLabel lblDecklist = new FLabel.Builder()
        .text("Double click a non-random deck for its decklist.")
        .fontSize(12).build();

    private final JSlider sliOpponents = new JSlider(JSlider.HORIZONTAL, 5, 50, 20);
    //private JSlider sliGamesPerMatch = new JSlider(JSlider.HORIZONTAL, 1, 7, 3);

    private final JCheckBox boxUserDecks = new FCheckBox("Custom User Decks");
    private final JCheckBox boxQuestDecks = new FCheckBox("Quest Decks");
    private final JCheckBox boxColorDecks = new FCheckBox("Fully random color Decks");
    private final JCheckBox boxThemeDecks = new FCheckBox("Semi-random theme Decks");

    private final JRadioButton radUserDecks = new FRadioButton("Custom user decks");
    private final JRadioButton radQuestDecks = new FRadioButton("Quest Events");
    private final JRadioButton radRandomColor = new FRadioButton("Fully random colors");
    private final JRadioButton radThemeDecks = new FRadioButton("Semi-random themes");

    private final JList lstDecks = new FList();
    private final QuickGauntletLister gauntletList = new QuickGauntletLister();

    private final JLabel lblOptions = new FLabel.Builder().fontSize(16)
            .fontStyle(Font.BOLD).text("OPTIONS").fontAlign(SwingConstants.CENTER).build();

    private final JLabel lblDeck = new FLabel.Builder().fontSize(16)
            .fontStyle(Font.BOLD).text("DECK").fontAlign(SwingConstants.CENTER).build();

    private final FLabel btnRandom = new FLabel.Builder()
            .text("Random").hoverable(true).build();

    private final JScrollPane scrDecks = new FScrollPane(lstDecks);
    private final JScrollPane scrLoad = new JScrollPane(gauntletList,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

    private final JLabel lblDesc1 = new FLabel.Builder()
            .text("Matches per gauntlet").fontStyle(Font.ITALIC).build();

    private final JLabel lblDesc3 = new FLabel.Builder()
            .text("Allowed deck types").fontStyle(Font.ITALIC).build();

    private final JLabel lblDesc = new FLabel.Builder().text(
            "A new quick gauntlet is auto-saved. They can be loaded in the \"Load Gauntlet\" screen.")
            .fontSize(12).build();

    private final StartButton btnStart  = new StartButton();

    private VSubmenuGauntletQuick() {
        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        boxUserDecks.setSelected(true);
        boxQuestDecks.setSelected(true);
        boxThemeDecks.setSelected(true);
        boxColorDecks.setSelected(true);

        sliOpponents.setMajorTickSpacing(5);
        sliOpponents.setMinorTickSpacing(0);
        sliOpponents.setPaintTicks(false);
        sliOpponents.setPaintLabels(true);
        sliOpponents.setSnapToTicks(true);
        sliOpponents.setOpaque(false);
        sliOpponents.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        sliOpponents.setFont(FSkin.getFont(12));

        btnRandom.setOpaque(true);

        scrLoad.setOpaque(false);
        scrLoad.getViewport().setOpaque(false);
        scrLoad.setBorder(null);

        // Radio button grouping
        final ButtonGroup grpRadDecks = new ButtonGroup();
        grpRadDecks.add(radUserDecks);
        grpRadDecks.add(radQuestDecks);
        grpRadDecks.add(radRandomColor);
        grpRadDecks.add(radThemeDecks);

        lstDecks.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        pnlOptions.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        pnlOptions.add(lblOptions, "h 30px!, w 96%!, gap 2% 0 0 5px");
        pnlOptions.add(sliOpponents, "h 40px!, w 96%!, gap 2% 0 0 5px, ax center");
        pnlOptions.add(lblDesc1, "w 96%!, gap 2% 0 0 20px");
        pnlOptions.add(lblDesc3, "w 96%!, gap 2% 0 0 0");
        pnlOptions.setCornerDiameter(0);
        pnlOptions.add(boxUserDecks, "w 96%!, h 30px!, gap 2% 0 0 5px");
        pnlOptions.add(boxQuestDecks, "w 96%!, h 30px!, gap 2% 0 0 5px");
        pnlOptions.add(boxThemeDecks, "w 96%!, h 30px!, gap 2% 0 0 5px");
        pnlOptions.add(boxColorDecks, "w 96%!, h 30px!, gap 2% 0 0 0");

        pnlDecks.setLayout(new MigLayout("insets 0, gap 0, wrap"));
        pnlDecks.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        pnlDecks.setCornerDiameter(0);
        pnlDecks.add(lblDeck, "h 30px!, w 94%!, gap 1% 0 0 5px, ax center");
        pnlDecks.add(radUserDecks, "w 96%!, h 30px!, gap 2% 0 0 5px");
        pnlDecks.add(radQuestDecks, "w 96%!, h 30px!, gap 2% 0 0 5px");
        pnlDecks.add(radRandomColor, "w 96%!, h 30px!, gap 2% 0 0 5px");
        pnlDecks.add(radThemeDecks, "w 96%!, h 30px!, gap 2% 0 0 5px");
        pnlDecks.add(btnRandom, "h 30px!, w 200px!, gap 25% 0 0 10px");
        pnlDecks.add(scrDecks, "w 94%!, pushy, growy, gap 3% 0 0 10px");
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getGroupEnum()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.GAUNTLET;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuTitle()
     */
    @Override
    public String getMenuTitle() {
        return "Quick Gauntlet";
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getItemEnum()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_GAUNTLETQUICK;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#populate()
     */
    @Override
    public void populate() {
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0, wrap 2"));

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitle, "w 98%!, h 30px!, gap 1% 0 15px 15px, span 2");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblDesc, "ax center, gap 0 0 0 5px, span 2");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblDecklist, "ax center, gap 0 0 0 15px, span 2");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlOptions, "w 40%!, gap 1% 1% 0 0, pushy, growy");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlDecks, "w 57%!, pushy, growy");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(btnStart, "w 98%!, ax center, gap 1% 0 20px 20px, span 2");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
    }

    /** @return {@link javax.swing.JList} */
    public JList getLstDecks() {
        return this.lstDecks;
    }

    /** @return {@link forge.gui.home.gauntlet.QuickGauntletLister} */
    public QuickGauntletLister getGauntletLister() {
        return this.gauntletList;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getBoxUserDecks() {
        return boxUserDecks;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getBoxQuestDecks() {
        return boxQuestDecks;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getBoxColorDecks() {
        return boxColorDecks;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getBoxThemeDecks() {
        return boxThemeDecks;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadUserDecks() {
        return this.radUserDecks;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadQuestDecks() {
        return this.radQuestDecks;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadColorDecks() {
        return this.radRandomColor;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadThemeDecks() {
        return this.radThemeDecks;
    }

    /** @return {@link javax.swing.JSlider} */
    public JSlider getSliOpponents() {
        return this.sliOpponents;
    }

    /** @return {@link forge.gui.toolbox.FLabel} */
    public FLabel getBtnRandom() {
        return this.btnRandom;
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnStart() {
        return this.btnStart;
    }

    //========== Overridden from IVDoc

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_GAUNTLETQUICK;
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
        return CSubmenuGauntletQuick.SINGLETON_INSTANCE;
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
        return parentCell;
    }
}
