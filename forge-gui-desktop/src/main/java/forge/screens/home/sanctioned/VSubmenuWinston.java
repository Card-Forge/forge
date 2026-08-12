package forge.screens.home.sanctioned;

import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import forge.game.GameType;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.itemmanager.DeckManager;
import forge.itemmanager.ItemManagerContainer;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.LblHeader;
import forge.screens.home.StartButton;
import forge.screens.home.VHomeUI;
import forge.screens.home.VHomeUI.PnlDisplay;
import forge.toolbox.FLabel;
import forge.toolbox.FList;
import forge.toolbox.FSkin;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

/**
 * Assembles Swing components of draft submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VSubmenuWinston implements IVSubmenu<CSubmenuWinston> {
    /** */
    SINGLETON_INSTANCE;

    private final Localizer localizer = Localizer.getInstance();

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab(localizer.getMessage("lblWinstonDraft"));

    /** */
    private final LblHeader lblTitle = new LblHeader(localizer.getMessage("lblSanctionedWinstonDraft"));

    private final JPanel pnlStart = new JPanel();
    private final StartButton btnStart  = new StartButton();

    private final DeckManager lstDecks = new DeckManager(GameType.Winston, CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture());
    private final JList<String> lstAI = new FList<>();

    private final JLabel lblInfo = new FLabel.Builder()
            .fontAlign(SwingConstants.LEFT).fontSize(16).fontStyle(Font.BOLD)
            .text(localizer.getMessage("lblBuildorselectadeck")).build();

    private final FLabel lblDir1 = new FLabel.Builder()
            .text(localizer.getMessage("lblWinstonDraftDescription"))
            .fontSize(12).build();

    private final FLabel lblDir2 = new FLabel.Builder()
            .text(localizer.getMessage("lblWinstonDraftBuildDescription"))
            .fontSize(12).build();

    private final FLabel lblDir3 = new FLabel.Builder()
            .text(localizer.getMessage("lblWinstonDraftPlayDescription"))
            .fontSize(12).build();

    private final FLabel btnBuildDeck = new FLabel.ButtonBuilder()
            .text(localizer.getMessage("lblNewWinstonDraftGame")).fontSize(16).build();

    /**
     * Constructor.
     */
    VSubmenuWinston() {
        lstAI.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        btnStart.setEnabled(false);

        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        lstDecks.setCaption(localizer.getMessage("lblWinstonDraftDecks"));

        pnlStart.setLayout(new MigLayout("insets 0, gap 0, wrap 2"));
        pnlStart.setOpaque(false);
        pnlStart.add(btnStart);
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
        return localizer.getMessage("lblWinstonDraft");
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getItemEnum()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_WINSTON;
    }

    /** @return {@link forge.gui.toolbox.FLabel} */
    public FLabel getBtnBuildDeck() {
        return this.btnBuildDeck;
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnStart() {
        return this.btnStart;
    }

    /** @return {@link forge.gui.toolbox.itemmanager.DeckManager} */
    public DeckManager getLstDecks() {
        return lstDecks;
    }

    //========== Overridden from IVDoc

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        PnlDisplay pnlDisplay = VHomeUI.SINGLETON_INSTANCE.getPnlDisplay();
        pnlDisplay.removeAll();
        pnlDisplay.setLayout(new MigLayout("insets 0, gap 0, wrap, ax right"));
        pnlDisplay.add(lblTitle, "w 80%!, h 40px!, gap 0 0 15px 15px, ax right");

        pnlDisplay.add(lblInfo, "w 80%!, h 30px!, gap 0 10% 20px 5px");
        pnlDisplay.add(lblDir1, "gap 0 0 0 5px");
        pnlDisplay.add(lblDir2, "gap 0 0 0 5px");
        pnlDisplay.add(lblDir3, "gap 0 0 0 20px");

        pnlDisplay.add(btnBuildDeck, "w 250px!, h 30px!, ax center, gap 0 10% 0 20px");
        pnlDisplay.add(new ItemManagerContainer(lstDecks), "w 80%!, gap 0 10% 0 0, pushy, growy");

        pnlDisplay.add(pnlStart, "gap 0 10% 50px 50px, ax center");

        pnlDisplay.repaint();
        pnlDisplay.revalidate();
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_WINSTON;
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
    public CSubmenuWinston getLayoutControl() {
        return CSubmenuWinston.SINGLETON_INSTANCE;
    }
}
