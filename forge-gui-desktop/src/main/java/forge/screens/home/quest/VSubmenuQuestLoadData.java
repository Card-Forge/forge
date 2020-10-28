package forge.screens.home.quest;

import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.properties.ForgeConstants;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.VHomeUI;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

/**
 * Assembles Swing components of quest data submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VSubmenuQuestLoadData implements IVSubmenu<CSubmenuQuestLoadData> {
    SINGLETON_INSTANCE;
    final Localizer localizer = Localizer.getInstance();

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab(localizer.getMessage("lblQuestData"));

    private final FLabel lblTitle = new FLabel.Builder()
    .text(localizer.getMessage("lblLoadQuestData")).fontAlign(SwingConstants.CENTER)
    .opaque(true).fontSize(16).build();

    String str= ForgeConstants.QUEST_SAVE_DIR.replace('\\', '/');
    private final FLabel lblOldQuests = new FLabel.Builder().text(localizer.getMessage("lblOldQuestData").replace("%s",str)).fontAlign(SwingConstants.CENTER).fontSize(12).build();
    private final QuestFileLister lstQuests = new QuestFileLister();
    private final FScrollPane scrQuests = new FScrollPane(lstQuests, false);
    private final JPanel pnlOptions = new JPanel();

    /**
     * Constructor.
     */
    @SuppressWarnings("unchecked")
    VSubmenuQuestLoadData() {

        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0, wrap"));

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitle, "w 98%!, h 30px!, gap 1% 0 15px 15px");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblOldQuests, "w 98%, h 30px!, gap 1% 0 0 5px");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(scrQuests, "w 98%!, growy, pushy, gap 1% 0 0 20px");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
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
        return localizer.getMessage("lblNewLoadQuest");
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuName()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_QUESTLOADDATA;
    }

    /**
     * @return {@link forge.screens.home.quest.QuestFileLister}
     */
    public QuestFileLister getLstQuests() {
        return this.lstQuests;
    }

    //========== Overridden from IVDoc

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_QUESTLOADDATA;
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
    public CSubmenuQuestLoadData getLayoutControl() {
        return CSubmenuQuestLoadData.SINGLETON_INSTANCE;
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
}
