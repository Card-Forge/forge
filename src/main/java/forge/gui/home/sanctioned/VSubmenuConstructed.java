package forge.gui.home.sanctioned;

import javax.swing.JButton;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import forge.gui.deckchooser.FDeckChooser;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.home.EMenuGroup;
import forge.gui.home.IVSubmenu;
import forge.gui.home.LblHeader;
import forge.gui.home.StartButton;
import forge.gui.home.VHomeUI;
import forge.gui.home.sanctioned.CSubmenuConstructed.GamePlayers;
import forge.gui.toolbox.FComboBox;
import forge.gui.toolbox.FComboBox.TextAlignment;
import forge.gui.toolbox.FSkin;

/**
 * Assembles Swing components of constructed submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum VSubmenuConstructed implements IVSubmenu<CSubmenuConstructed> {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Constructed Mode");

    /** */
    private final LblHeader lblTitle = new LblHeader("Sanctioned Format: Constructed");
    private final FComboBox<GamePlayers> cboGamePlayers = new FComboBox<GamePlayers>();
    private final StartButton btnStart  = new StartButton();

    private final FDeckChooser dcLeft = new FDeckChooser("%s Deck", false, true);
    private final FDeckChooser dcRight = new FDeckChooser("%s Deck", true, true);

    // CTR
    private VSubmenuConstructed() {
        FSkin.get(lblTitle).setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        cboGamePlayers.setButtonVisible(true);
        cboGamePlayers.setTextAlignment(TextAlignment.CENTER);
        FSkin.get(cboGamePlayers).setFont(FSkin.getBoldFont(16));
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getGroupEnum()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.SANCTIONED;
    }

    public final FDeckChooser getDcRight() {
        return dcRight;
    }

    public final FDeckChooser getDcLeft() {
        return dcLeft;
    }


    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuTitle()
     */
    @Override
    public String getMenuTitle() {
        return "Constructed";
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getItemEnum()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_CONSTRUCTED;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#populate()
     */
    @Override
    public void populate() {

        JPanel container = VHomeUI.SINGLETON_INSTANCE.getPnlDisplay();

        container.removeAll();
        container.setLayout(new MigLayout("insets 0, gap 0, wrap 2"));
        container.add(lblTitle, "w 80%, h 40px!, gap 0 0 15px 15px, span 2, al right, pushx");
        container.add(cboGamePlayers, "w 400px!, h 30px!, gap 0 0 15px 5px, span 2, al center");
        container.add(dcLeft, "w 50%, gap 40px 20px 20px 5px, growy, pushy");
        container.add(dcRight, "w 50%, gap 20px 40px 20px 5px, growy, pushy");
        container.add(btnStart, "span 2, gap 0 0 0px 20px, center");

        dcLeft.populate();
        dcRight.populate();

        if (container.isShowing()) {
            container.validate();
            container.repaint();
        }
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnStart() {
        return this.btnStart;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public boolean isLeftPlayerAi() {
        return dcLeft.isAi();
    }
    public boolean isRightPlayerAi() {
        return dcRight.isAi();
    }


    //========== Overridden from IVDoc

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_CONSTRUCTED;
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
    public CSubmenuConstructed getLayoutControl() {
        return CSubmenuConstructed.SINGLETON_INSTANCE;
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

    public FComboBox<GamePlayers> getGamePlayersComboBox() {
        return cboGamePlayers;
    }


}
