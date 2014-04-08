package forge.screens.home.settings;

import forge.UiCommand;
import forge.gui.WrapLayout;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.VHomeUI;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinImage;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

import java.awt.*;
import java.util.Map;

/** 
 * Assembles Swing components of avatars submenu singleton.
 */
public enum VSubmenuAvatars implements IVSubmenu<CSubmenuAvatars> {
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Avatars");

    private final JPanel pnlAvatars = new JPanel();

    private final FLabel lblAvatarHuman = new FLabel.Builder().hoverable(true).selectable(true)
            .iconScaleFactor(0.99f).iconInBackground(true).build();
    private final FLabel lblAvatarAI = new FLabel.Builder().hoverable(true).selectable(true)
            .iconScaleFactor(0.99f).iconInBackground(true).build();

    private VSubmenuAvatars() {
        populateAvatars();
    }

    public void focusHuman() {
        lblAvatarHuman.requestFocusInWindow();
    }
    
    @Override
    public void populate() {
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0"));
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlAvatars, "w 98%!, h 98%!, gap 1% 0 1% 0");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
    }

    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.SETTINGS;
    }

    @Override
    public String getMenuTitle() {
        return "Avatars";
    }

    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_AVATARS;
    }

    @SuppressWarnings("serial")
    private void populateAvatars() {
        final Map<Integer, SkinImage> avatarMap = FSkin.getAvatars();
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
        FScrollPane scroller = new FScrollPane(pnlAvatarPics, true);
        scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        pnlAvatars.add(scroller, "w 90%!, pushy, growy, gap 5% 0 0 0");

        final UiCommand cmdHuman = new UiCommand() { @Override
            public void run() { lblAvatarAI.setSelected(false); lblAvatarHuman.requestFocusInWindow(); } };

        final UiCommand cmdAI = new UiCommand() { @Override
            public void run() { lblAvatarHuman.setSelected(false); lblAvatarAI.requestFocusInWindow(); } };

        lblAvatarHuman.setCommand(cmdHuman);
        lblAvatarAI.setCommand(cmdAI);

        lblAvatarHuman.setSelected(true);

        final String[] indexes = FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",");
        int humanIndex = Integer.parseInt(indexes[0]);
        int aiIndex = Integer.parseInt(indexes[1]);

        if (humanIndex >= FSkin.getAvatars().size()) { humanIndex = 0; }
        if (aiIndex >= FSkin.getAvatars().size()) { aiIndex = 0; }

        lblAvatarAI.setIcon(FSkin.getAvatars().get(aiIndex));
        lblAvatarHuman.setIcon(FSkin.getAvatars().get(humanIndex));

        FModel.getPreferences().setPref(FPref.UI_AVATARS, humanIndex + "," + aiIndex);
        FModel.getPreferences().save();
    }

    @SuppressWarnings("serial")
    private FLabel makeAvatarLabel(final SkinImage img0, final int index0) {
        final FLabel lbl = new FLabel.Builder().icon(img0).iconScaleFactor(1.0)
                .iconInBackground(true).hoverable(true).build();

        final Dimension size = new Dimension(100, 100);
        lbl.setPreferredSize(size);
        lbl.setMaximumSize(size);
        lbl.setMinimumSize(size);

        final UiCommand cmd = new UiCommand() {
            @Override
            public void run() {
                String[] indices = FModel.getPreferences()
                        .getPref(FPref.UI_AVATARS).split(",");

                if (lblAvatarAI.isSelected()) {
                    lblAvatarAI.setIcon(FSkin.getAvatars().get(index0));
                    lblAvatarAI.repaintSelf();
                    indices[1] = String.valueOf(index0);
                }
                else {
                    lblAvatarHuman.setIcon(FSkin.getAvatars().get(index0));
                    lblAvatarHuman.repaintSelf();
                    indices[0] = String.valueOf(index0);
                }

                FModel.getPreferences().setPref(FPref.UI_AVATARS, indices[0] + "," + indices[1]);
                FModel.getPreferences().save();
            }
        };

        lbl.setCommand(cmd);
        return lbl;
    }

	public void refreshAvatarFromPrefs(int playerIndex) {
		FLabel avatar = playerIndex == 0 ? lblAvatarHuman : lblAvatarAI;
		String[] currentPrefs = FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",");
		int avatarIndex = Integer.parseInt(currentPrefs[playerIndex]);
		avatar.setIcon(FSkin.getAvatars().get(avatarIndex));
		avatar.repaintSelf();
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
    public CSubmenuAvatars getLayoutControl() {
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
