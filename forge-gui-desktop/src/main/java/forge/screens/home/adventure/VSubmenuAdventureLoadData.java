package forge.screens.home.adventure;

import forge.adventure.AdventureApplicationConfiguration;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.localinstance.properties.ForgeConstants;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.VHomeUI;
import forge.toolbox.FLabel;
import forge.toolbox.FSkin;
import forge.util.Localizer;
import forge.screens.home.quest.QuestFileLister;
import forge.adventure.AdventureApplication;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

/**
 * Assembles Swing components of adventrue data submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VSubmenuAdventureLoadData implements IVSubmenu<CSubmenuAdventureLoadData> {
    SINGLETON_INSTANCE;
    final Localizer localizer = Localizer.getInstance();

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("todo");

    private final FLabel lblTitle = new FLabel.Builder()
    .text("Start Adventure").fontAlign(SwingConstants.CENTER)//TODO localizer
    .opaque(true).fontSize(16).build();

    String str= ForgeConstants.QUEST_SAVE_DIR.replace('\\', '/');
    private final QuestFileLister lstAdventures = new QuestFileLister();
    private final JComboBox<String> boxPlane = new JComboBox<String>(new String[]{"Shandalar"}); //TODO load list from a configuration file
    private final JButton bttnStart = new JButton("Start Adventure");//TODO localizer
    private final FLabel lblPlane = new FLabel.Builder().text("Plane").build();//TODO localizer
    private final JCheckBox boxFullScreen = new JCheckBox("Fullscreen");//TODO localizer

    /**
     * Constructor.
     */
    @SuppressWarnings("unchecked")
    VSubmenuAdventureLoadData() {

        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        bttnStart.addActionListener(arg0 -> start());
    }
    private final void start()
    {
        AdventureApplicationConfiguration config=new AdventureApplicationConfiguration();

        config.SetPlane(boxPlane.getSelectedItem().toString());
        config.setFullScreen(boxFullScreen.isSelected());

        AdventureApplication advanture=new AdventureApplication(config);


    }
    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("","[50%][50%]"));

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitle, "cell 0 0 4 1");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(bttnStart, "cell 0 1 2 1");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblPlane,"cell 0 2");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(boxPlane, "cell 1 2");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(boxFullScreen,"cell 0 3");


        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getGroup()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.ADVENTURE;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuTitle()
     */
    @Override
    public String getMenuTitle() { return "Start Adventure"; }//TODO localizer

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuName()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_ADVANTURELOADDATA;
    }


    //========== Overridden from IVDoc

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_ADVANTURELOADDATA;
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
    public CSubmenuAdventureLoadData getLayoutControl() {
        return CSubmenuAdventureLoadData.SINGLETON_INSTANCE;
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
