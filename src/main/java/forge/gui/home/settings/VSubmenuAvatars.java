package forge.gui.home.settings;

import java.awt.Dimension;
import java.awt.Image;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;
import forge.Command;
import forge.Singletons;
import forge.gui.WrapLayout;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.home.EMenuGroup;
import forge.gui.home.IVSubmenu;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FScrollPane;
import forge.gui.toolbox.FSkin;
import forge.properties.ForgePreferences.FPref;

/** 
 * Assembles Swing components of avatars submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VSubmenuAvatars implements IVSubmenu {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Avatars");

    /** */
    private final JPanel pnlAvatars = new JPanel();
    private final FScrollPane scrContent = new FScrollPane(pnlAvatars,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    private final FLabel lblAvatarHuman = new FLabel.Builder().hoverable(true).selectable(true)
            .iconScaleFactor(0.99f).iconInBackground(true).build();
    private final FLabel lblAvatarAI = new FLabel.Builder().hoverable(true).selectable(true)
            .iconScaleFactor(0.99f).iconInBackground(true).build();

    /**
     * Constructor.
     */
    private VSubmenuAvatars() {
        populateAvatars();
        scrContent.setBorder(null);

    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        parentCell.getBody().setLayout(new MigLayout("insets 0, gap 0"));
        parentCell.getBody().add(scrContent, "w 98%!, h 98%!, gap 1% 0 1% 0");
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getGroup()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.SETTINGS;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuTitle()
     */
    @Override
    public String getMenuTitle() {
        return "Avatars";
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuName()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_AVATARS;
    }

    @SuppressWarnings("serial")
    private void populateAvatars() {
        final Map<Integer, Image> avatarMap = FSkin.getAvatars();
        final JPanel pnlAvatarPics = new JPanel(new WrapLayout());
        final JPanel pnlAvatarUsers = new JPanel(new MigLayout("insets 0, gap 0, align center"));

        pnlAvatars.setOpaque(false);
        pnlAvatarUsers.setOpaque(false);
        pnlAvatarPics.setOpaque(false);

        pnlAvatarUsers.add(new FLabel.Builder().fontSize(12).text("Human").build(),
                "w 100px!, h 20px!, gap 0 20px 0 0");
        pnlAvatarUsers.add(new FLabel.Builder().fontSize(12).text("AI").build(),
                "w 100px!, h 20px!, wrap");

        pnlAvatarUsers.add(lblAvatarHuman, "w 100px!, h 100px!, gap 0 20px 0 0");
        pnlAvatarUsers.add(lblAvatarAI, "w 100px!, h 100px!");

        for (final Integer i : avatarMap.keySet()) {
            pnlAvatarPics.add(makeAvatarLabel(avatarMap.get(i), i));
        }

        pnlAvatars.removeAll();
        pnlAvatars.setLayout(new MigLayout("insets 0, gap 0"));
        pnlAvatars.add(pnlAvatarUsers, "w 90%!, h 150px!, wrap");
        pnlAvatars.add(new FScrollPane(pnlAvatarPics,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
                "w 90%!, pushy, growy, gap 5% 0 0 0");

        final Command cmdHuman = new Command() { @Override
            public void execute() { lblAvatarAI.setSelected(false); } };

        final Command cmdAI = new Command() { @Override
            public void execute() { lblAvatarHuman.setSelected(false); } };

        lblAvatarHuman.setCommand(cmdHuman);
        lblAvatarAI.setCommand(cmdAI);

        lblAvatarHuman.setSelected(true);

        final String[] indexes = Singletons.getModel().getPreferences().getPref(FPref.UI_AVATARS).split(",");
        int aiIndex = Integer.parseInt(indexes[0]);
        int humanIndex = Integer.parseInt(indexes[1]);

        if (humanIndex >= FSkin.getAvatars().size()) { humanIndex = 0; }
        if (aiIndex >= FSkin.getAvatars().size()) { aiIndex = 0; }

        lblAvatarAI.setIcon(new ImageIcon(FSkin.getAvatars().get(aiIndex)));
        lblAvatarHuman.setIcon(new ImageIcon(FSkin.getAvatars().get(humanIndex)));

        Singletons.getModel().getPreferences().setPref(FPref.UI_AVATARS, aiIndex + "," + humanIndex);
        Singletons.getModel().getPreferences().save();
    }

    @SuppressWarnings("serial")
    private FLabel makeAvatarLabel(final Image img0, final int index0) {
        final FLabel lbl = new FLabel.Builder().icon(new ImageIcon(img0)).iconScaleFactor(1.0)
                .iconAlpha(0.7f).iconInBackground(true).hoverable(true).build();

        final Dimension size = new Dimension(100, 100);
        lbl.setPreferredSize(size);
        lbl.setMaximumSize(size);
        lbl.setMinimumSize(size);

        final Command cmd = new Command() {
            @Override
            public void execute() {
                String[] indices = Singletons.getModel().getPreferences()
                        .getPref(FPref.UI_AVATARS).split(",");

                if (lblAvatarAI.isSelected()) {
                    lblAvatarAI.setIcon(new ImageIcon(FSkin.getAvatars().get(index0)));
                    lblAvatarAI.repaintSelf();
                    indices[0] = String.valueOf(index0);
                }
                else {
                    lblAvatarHuman.setIcon(new ImageIcon(FSkin.getAvatars().get(index0)));
                    lblAvatarHuman.repaintSelf();
                    indices[1] = String.valueOf(index0);
                }

                Singletons.getModel().getPreferences().setPref(FPref.UI_AVATARS, indices[0] + "," + indices[1]);
                Singletons.getModel().getPreferences().save();
            }
        };

        lbl.setCommand(cmd);
        return lbl;
    }

    //========== Overridden from IVDoc

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_AVATARS;
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
        return CSubmenuAvatars.SINGLETON_INSTANCE;
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
