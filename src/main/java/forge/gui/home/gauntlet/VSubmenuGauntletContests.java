package forge.gui.home.gauntlet;

import java.awt.Font;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.home.EMenuGroup;
import forge.gui.home.IVSubmenu;
import forge.gui.home.StartButton;
import forge.gui.home.VHomeUI;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FList;
import forge.gui.toolbox.FRadioButton;
import forge.gui.toolbox.FScrollPane;
import forge.gui.toolbox.FSkin;

/** 
 * Assembles Swing components of "build gauntlet" submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum VSubmenuGauntletContests implements IVSubmenu<CSubmenuGauntletContests> {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Gauntlet Contests");

    // Other fields
    private final FLabel lblTitle = new FLabel.Builder()
        .text("Gauntlet Contests").fontAlign(SwingConstants.CENTER)
        .opaque(true).fontSize(16).build();

    private final StartButton btnStart  = new StartButton();

    private final FLabel btnRandom = new FLabel.Builder()
        .text("Random").hoverable(true).build();

    private final JPanel pnlLoad = new JPanel(new MigLayout("insets 0, gap 0, wrap"));
    private final JPanel pnlDecks = new JPanel();

    private final ContestGauntletLister gauntletList = new ContestGauntletLister();
    private final JList lstDecks = new FList();

    private final JScrollPane scrLeft  = new FScrollPane(gauntletList,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    private final JScrollPane scrRight  = new FScrollPane(lstDecks,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    private final JRadioButton radUserDecks = new FRadioButton("Custom user decks");
    private final JRadioButton radQuestDecks = new FRadioButton("Quest Decks");
    private final JRadioButton radColorDecks = new FRadioButton("Fully random color decks");
    private final JRadioButton radThemeDecks = new FRadioButton("Semi-random theme decks");

    private final JLabel lblDeck = new FLabel.Builder().fontSize(16)
            .fontStyle(Font.BOLD).text("DECK").fontAlign(SwingConstants.CENTER).build();
    private final JLabel lblLoad = new FLabel.Builder().fontSize(16)
            .fontStyle(Font.BOLD).text("PICK A CONTEST").fontAlign(SwingConstants.CENTER).build();

    private final JLabel lblDesc1 = new FLabel.Builder()
        .text("A gauntlet that has been started will keep the same deck until it is finished.").build();

    private VSubmenuGauntletContests() {
        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        // Radio button grouping
        final ButtonGroup grpRight = new ButtonGroup();
        grpRight.add(radUserDecks);
        grpRight.add(radQuestDecks);
        grpRight.add(radColorDecks);
        grpRight.add(radThemeDecks);

        scrLeft.setBorder(null);
        pnlLoad.setLayout(new MigLayout("insets 0, gap 0, wrap"));
        pnlLoad.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        pnlLoad.add(lblLoad, "h 30px!, w 94%!, gap 1% 0 0 5px, ax center");
       // pnlLoad.add(new FLabel.Builder().text("If a gauntlet has been started, its deck is frozen.").build(),
         //       "gap 0 0 0 5px, ax center");
        pnlLoad.add(scrLeft, "w 94%!, pushy, growy, gap 3% 0 0 10px");

        btnRandom.setOpaque(true);
        pnlDecks.setLayout(new MigLayout("insets 0, gap 0, wrap"));
        pnlDecks.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        pnlDecks.add(lblDeck, "h 30px!, w 94%!, gap 1% 0 0 5px, ax center");
        pnlDecks.add(radUserDecks, "w 96%!, h 30px!, gap 2% 0 0 5px");
        pnlDecks.add(radQuestDecks, "w 96%!, h 30px!, gap 2% 0 0 5px");
        pnlDecks.add(radColorDecks, "w 96%!, h 30px!, gap 2% 0 0 5px");
        pnlDecks.add(radThemeDecks, "w 96%!, h 30px!, gap 2% 0 0 5px");
        pnlDecks.add(btnRandom, "h 30px!, w 200px!, gap 25% 0 0 10px");
        pnlDecks.add(scrRight, "w 94%!, pushy, growy, gap 3% 0 0 10px");
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
        return "Gauntlet Contests";
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getItemEnum()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_GAUNTLETCONTESTS;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#populate()
     */
    @Override
    public void populate() {
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0, wrap 2"));

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitle, "w 98%!, h 30px!, gap 1% 0 15px 15px, span 2");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblDesc1, "gap 0 0 0 15px, ax center, span 2");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlLoad, "w 56%!, gap 1% 2% 0 15px, pushy, growy");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlDecks, "w 40%!, gap 0 0 0 15px, pushy, growy");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(btnStart, "w 98%!, ax center, gap 1% 0 20px 20px, span 2");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
    }


    /** @return {@link javax.swing.JButton} */
    public JButton getBtnStart() {
        return this.btnStart;
    }

    /** @return {@link forge.gui.toolbox.FLabel} */
    public FLabel getBtnRandom() {
        return this.btnRandom;
    }

    /** @return {@link forge.gui.home.gauntlet.ContestGauntletLister} */
    public ContestGauntletLister getGauntletLister() {
        return this.gauntletList;
    }

    /** @return {@link javax.swing.JList} */
    public JList getLstDecks() {
        return this.lstDecks;
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
        return this.radColorDecks;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadThemeDecks() {
        return this.radThemeDecks;
    }

    //========== Overridden from IVDoc

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_GAUNTLETCONTESTS;
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
    public CSubmenuGauntletContests getLayoutControl() {
        return CSubmenuGauntletContests.SINGLETON_INSTANCE;
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
