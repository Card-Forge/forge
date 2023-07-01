package forge.screens.home.quest;

import java.awt.Font;

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
import forge.screens.home.VHomeUI;
import forge.screens.home.VHomeUI.PnlDisplay;
import forge.toolbox.FLabel;
import forge.toolbox.FSkin;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

/** 
 * Assembles Swing components of quest decks submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum VSubmenuQuestDecks implements IVSubmenu<CSubmenuQuestDecks> {
    /** */
    SINGLETON_INSTANCE;

    final Localizer localizer = Localizer.getInstance();

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab(localizer.getMessage("lblQuestDecks"));

    /** */
    private final LblHeader lblTitle = new LblHeader(localizer.getMessage("lblQuestDecks"));

    private final DeckManager lstDecks = new DeckManager(GameType.Quest, CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture());

    private final FLabel lblInfo = new FLabel.Builder()
        .fontAlign(SwingConstants.LEFT).fontSize(16).fontStyle(Font.BOLD)
        .text(localizer.getMessage("lblBuildorselectadeck")).build();

    private final FLabel lblDir1 = new FLabel.Builder()
        .text(localizer.getMessage("lblQuestDesc1"))
        .fontSize(12).build();

    private final FLabel lblDir2 = new FLabel.Builder()
        .text(localizer.getMessage("lblQuestDesc2"))
        .fontSize(12).build();

    private final FLabel lblDir3 = new FLabel.Builder()
        .text(localizer.getMessage("lblQuestDesc3"))
        .fontSize(12).build();

    private final FLabel btnNewDeck = new FLabel.ButtonBuilder().text(localizer.getMessage("lblBuildaNewDeck")).fontSize(16).build();

    /**
     * Constructor.
     */
    VSubmenuQuestDecks() {
        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        lstDecks.setCaption(localizer.getMessage("lblQuestDecks"));
    }

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

        pnlDisplay.add(btnNewDeck, "w 250px!, h 30px!, ax center, gap 0 10% 0 20px");
        pnlDisplay.add(new ItemManagerContainer(lstDecks), "w 80%!, gap 0 10% 0 0, pushy, growy, gapbottom 20px");

        pnlDisplay.repaintSelf();
        pnlDisplay.revalidate();
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getGroup()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.QUEST;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuTitle()
     */
    @Override
    public String getMenuTitle() {
        return localizer.getMessage("lblQuestDecks");
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuName()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_QUESTDECKS;
    }

    /** @return {@link forge.itemmanager.DeckManager} */
    public DeckManager getLstDecks() {
        return this.lstDecks;
    }

    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getBtnNewDeck() {
        return this.btnNewDeck;
    }

    //========== Overridden from IVDoc

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_QUESTDECKS;
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
    public CSubmenuQuestDecks getLayoutControl() {
        return CSubmenuQuestDecks.SINGLETON_INSTANCE;
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
