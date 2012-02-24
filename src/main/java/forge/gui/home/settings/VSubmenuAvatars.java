package forge.gui.home.settings;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;
import forge.PlayerType;
import forge.Singletons;
import forge.gui.home.EMenuGroup;
import forge.gui.home.IVSubmenu;
import forge.properties.ForgePreferences.FPref;
import forge.view.toolbox.FScrollPane;
import forge.view.toolbox.FSkin;
import forge.view.toolbox.WrapLayout;

/** 
 * Singleton instance of "Draft" submenu in "Constructed" group.
 */
public enum VSubmenuAvatars implements IVSubmenu {
    /** */
    SINGLETON_INSTANCE;

    /** */
    private final JPanel pnl = new JPanel();
    private final JPanel pnlAvatars = new JPanel();

    private AvatarLabel avatarHuman, avatarAI;
    private List<AvatarLabel> lstAvatars;

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        populateAvatars();

        pnl.removeAll();
        pnl.setOpaque(false);
        pnl.setLayout(new MigLayout("insets 0, gap 0"));
        pnl.add(new FScrollPane(pnlAvatars,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), " w 100%!, h 100%!");
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getGroup()
     */
    @Override
    public EMenuGroup getGroup() {
        return EMenuGroup.SETTINGS;
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getPanel()
     */
    @Override
    public JPanel getPanel() {
        return pnl;
    }

    private void populateAvatars() {
        pnlAvatars.removeAll();
        pnlAvatars.setLayout(new WrapLayout());
        pnlAvatars.setOpaque(false);

        lstAvatars = new ArrayList<AvatarLabel>();
        int counter = 0;
        for (final Image i : FSkin.getAvatars().values()) {
            lstAvatars.add(new AvatarLabel(i, counter++));
            pnlAvatars.add(lstAvatars.get(lstAvatars.size() - 1));
        }

        final String[] indexes = Singletons.getModel().getPreferences().getPref(FPref.UI_AVATARS).split(",");
        int humanIndex = Integer.parseInt(indexes[0]);
        int aiIndex = Integer.parseInt(indexes[1]);

        // Set human avatar from preferences
        if (humanIndex >= lstAvatars.size()) {
            humanIndex = (int) (Math.random() * (lstAvatars.size() - 1));
        }

        avatarHuman = lstAvatars.get(humanIndex);
        avatarHuman.setOwner(PlayerType.HUMAN);
        avatarHuman.repaintOnlyThisLabel();

        if (humanIndex == aiIndex || aiIndex >= lstAvatars.size()) {
            aiIndex = humanIndex;
            while (aiIndex == humanIndex) {
                aiIndex = (int) (Math.random() * (lstAvatars.size() - 1));
            }
        }

        avatarAI = lstAvatars.get(aiIndex);
        avatarAI.setOwner(PlayerType.COMPUTER);
        avatarAI.repaintOnlyThisLabel();
    }

    @SuppressWarnings("serial")
    private class AvatarLabel extends JLabel {
        private final Image img;
        private final int index;
        private PlayerType owner;
        private boolean hovered = false;

        public AvatarLabel(final Image img0, final int index0) {
            super();
            img = img0;
            index = index0;
            setMaximumSize(new Dimension(100, 120));
            setMinimumSize(new Dimension(100, 120));
            setPreferredSize(new Dimension(100, 120));

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent evt) { hovered = true; repaintOnlyThisLabel(); }

                @Override
                public void mouseExited(final MouseEvent evt) { hovered = false; repaintOnlyThisLabel(); }

                @Override
                public void mouseClicked(final MouseEvent evt) { cycleOwner(AvatarLabel.this); repaintOnlyThisLabel(); }
            });
        }

        public void setOwner(final PlayerType player0) {
            this.owner = player0;
        }

        public PlayerType getOwner() {
            return this.owner;
        }

        public int getIndex() {
            return this.index;
        }

        public void repaintOnlyThisLabel() {
            final Dimension d = AvatarLabel.this.getSize();
            repaint(0, 0, d.width, d.height);
        }

        @Override
        protected void paintComponent(final Graphics graphics0) {
            if (hovered) {
                graphics0.setColor(FSkin.getColor(FSkin.Colors.CLR_HOVER));
                graphics0.fillRect(0, 0, 100, 120);
            }

            graphics0.drawImage(img, 0, 20, null);
            if (owner == null) { return; }

            graphics0.setColor(FSkin.getColor(FSkin.Colors.CLR_TEXT));
            graphics0.drawRect(0, 0, 99, 119);
            graphics0.setFont(FSkin.getBoldFont(14));
            graphics0.drawString(owner.toString(), 5, 15);
        }
    }

    /** Surprisingly complicated - be careful when modifying! */
    private void cycleOwner(final AvatarLabel lbl0) {
        if (lbl0.getOwner() == null) {
            lbl0.setOwner(PlayerType.HUMAN);
            lbl0.repaintOnlyThisLabel();

            if (avatarHuman != null) {
                avatarHuman.setOwner(null);
                avatarHuman.repaintOnlyThisLabel();
            }

            avatarHuman = lbl0;
        }
        else if (lbl0.getOwner() == PlayerType.HUMAN) {
            // Re-assign avatar to human
            avatarHuman.setOwner(null);
            avatarHuman.repaintOnlyThisLabel();

            for (int i = 0; i < lstAvatars.size(); i++) {
                if (lstAvatars.get(i) != lbl0) {
                    avatarHuman = lstAvatars.get(i);
                    avatarHuman.setOwner(PlayerType.HUMAN);
                    avatarHuman.repaintOnlyThisLabel();
                    break;
                }
            }

            // Assign computer
            lbl0.setOwner(PlayerType.COMPUTER);
            lbl0.repaintOnlyThisLabel();

            if (avatarAI != null) {
                avatarAI.setOwner(null);
                avatarAI.repaintOnlyThisLabel();
            }

            avatarAI = lbl0;
        }
        else {
            lbl0.setOwner(null);
            lbl0.repaintOnlyThisLabel();

            // Re-assign avatar to computer
            avatarAI.setOwner(null);
            avatarAI.repaintOnlyThisLabel();

            for (int i = 0; i < lstAvatars.size(); i++) {
                if (lstAvatars.get(i) != avatarHuman) {
                    avatarAI = lstAvatars.get(i);
                    avatarAI.setOwner(PlayerType.COMPUTER);
                    avatarAI.repaintOnlyThisLabel();
                    break;
                }
            }
        }

        Singletons.getModel().getPreferences().setPref(
                FPref.UI_AVATARS, avatarHuman.getIndex() + "," + avatarAI.getIndex());
        Singletons.getModel().getPreferences().save();
    }
}
