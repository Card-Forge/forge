package forge.gui.home.sanctioned;

import java.awt.Font;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import forge.game.GameType;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.home.EMenuGroup;
import forge.gui.home.IVSubmenu;
import forge.gui.home.LblHeader;
import forge.gui.home.StartButton;
import forge.gui.home.VHomeUI;
import forge.gui.toolbox.DeckLister;
import forge.gui.toolbox.ExperimentalLabel;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FList;
import forge.gui.toolbox.FRadioButton;
import forge.gui.toolbox.FScrollPane;
import forge.gui.toolbox.FSkin;

/** 
 * Assembles Swing components of draft submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VSubmenuDraft implements IVSubmenu {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Draft Mode");

    /** */
    private final LblHeader lblTitle = new LblHeader("Sanctioned Format: Draft");

    private final JPanel pnlStart = new JPanel();
    private final StartButton btnStart  = new StartButton();

    private final DeckLister lstHumanDecks = new DeckLister(GameType.Draft);
    private final JList lstAI = new FList();

    private final JRadioButton radSingle = new FRadioButton("Play one opponent");
    private final JRadioButton radAll = new FRadioButton("Play all 8 opponents");

    private final JLabel lblHuman = new FLabel.Builder()
        .fontAlign(SwingConstants.LEFT).fontSize(16).fontStyle(Font.BOLD)
        .text("Build or select a deck").build();

    private final FLabel lblDirections = new FLabel.Builder()
        .text("In Draft mode, three booster packs are rotated around eight players. Build a deck from the cards you choose.")
        .fontSize(12).build();

    private final ExperimentalLabel btnBuildDeck = new ExperimentalLabel("Start A New Draft");


    /**
     * Constructor.
     */
    private VSubmenuDraft() {
        lstAI.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        btnStart.setEnabled(false);

        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        final ButtonGroup grp = new ButtonGroup();
        grp.add(radSingle);
        grp.add(radAll);
        radSingle.setSelected(true);

        pnlStart.setLayout(new MigLayout("insets 0, gap 0, wrap 2"));
        pnlStart.setOpaque(false);
        pnlStart.add(radSingle, "w 200px!, h 30px!, gap 0 20px 0 0");
        pnlStart.add(btnStart, "span 1 2, growx, pushx");
        pnlStart.add(radAll, "w 200px!, h 30px!, gap 0 20px 0 0");
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getGroup()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.SANCTIONED;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuTitle()
     */
    @Override
    public String getMenuTitle() {
        return "Draft Mode";
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getItemEnum()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_DRAFT;
    }

    /** @return {@link forge.gui.toolbox.ExperimentalLabel} */
    public ExperimentalLabel getBtnBuildDeck() {
        return this.btnBuildDeck;
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnStart() {
        return this.btnStart;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadSingle() {
        return this.radSingle;
    }

    /** @return {@link forge.gui.toolbox.DeckLister} */
    public DeckLister getLstHumanDecks() {
        return lstHumanDecks;
    }

    //========== Overridden from IVDoc

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0, wrap, ax right"));
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitle, "w 80%!, h 40px!, gap 0 0 15px 15px, ax right");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblHuman, "w 80%!, h 30px!, gap 0 10% 20px 0");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblDirections, "gap 0 0 0 20px");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(btnBuildDeck, "w 200px!, h 30px!, ax center, gap 0 10% 0 20px");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(new FScrollPane(lstHumanDecks), "w 80%!, gap 0 10% 0 0, pushy, growy");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlStart, "gap 0 10% 50px 50px, ax center");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaint();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_DRAFT;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return tab;
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
        return parentCell;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getLayoutControl()
     */
    @Override
    public ICDoc getLayoutControl() {
        return CSubmenuDraft.SINGLETON_INSTANCE;
    }
}
