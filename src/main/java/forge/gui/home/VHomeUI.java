/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.gui.home;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import net.miginfocom.swing.MigLayout;
import forge.Singletons;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.framework.ILocalRepaint;
import forge.gui.framework.IVTopLevelUI;
import forge.gui.home.gauntlet.VSubmenuGauntletBuild;
import forge.gui.home.gauntlet.VSubmenuGauntletContests;
import forge.gui.home.gauntlet.VSubmenuGauntletLoad;
import forge.gui.home.gauntlet.VSubmenuGauntletQuick;
import forge.gui.home.quest.VSubmenuChallenges;
import forge.gui.home.quest.VSubmenuDuels;
import forge.gui.home.quest.VSubmenuQuestData;
import forge.gui.home.quest.VSubmenuQuestDecks;
import forge.gui.home.quest.VSubmenuQuestPrefs;
import forge.gui.home.sanctioned.VSubmenuConstructed;
import forge.gui.home.sanctioned.VSubmenuDraft;
import forge.gui.home.sanctioned.VSubmenuSealed;
import forge.gui.home.settings.VSubmenuAvatars;
import forge.gui.home.settings.VSubmenuDownloaders;
import forge.gui.home.settings.VSubmenuPreferences;
import forge.gui.home.variant.VSubmenuArchenemy;
import forge.gui.home.variant.VSubmenuPlanechase;
import forge.gui.home.variant.VSubmenuVanguard;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FScrollPanel;
import forge.gui.toolbox.FSkin;
import forge.properties.NewConstants;
import forge.properties.ForgePreferences.FPref;
import forge.view.FView;

/**
 * Top level view class for home UI drag layout.<br>
 * Uses singleton pattern.<br>
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */

@SuppressWarnings("serial")
public enum VHomeUI implements IVTopLevelUI {
    /** */
    SINGLETON_INSTANCE;

    private final Color clrTheme = FSkin.getColor(FSkin.Colors.CLR_THEME);
    private final Color l00 = FSkin.stepColor(clrTheme, 0);

    private final List<IVSubmenu<? extends ICDoc>> allSubmenus = new ArrayList<IVSubmenu<? extends ICDoc>>();
    private final Map<EDocID, LblMenuItem> allSubmenuLabels = new HashMap<EDocID, LblMenuItem>();
    private final Map<EMenuGroup, LblGroup> allGroupLabels = new HashMap<EMenuGroup, LblGroup>();

    private final PnlMenu pnlMenu = new PnlMenu();
    private final PnlDisplay pnlDisplay = new PnlDisplay();
    private final FScrollPanel pnlSubmenus;

    private JLabel lblLogo = new FLabel.Builder()
        .icon(FSkin.getIcon(FSkin.InterfaceIcons.ICO_LOGO))
        .iconAlignX(SwingConstants.CENTER)
        .iconInBackground(true).iconScaleFactor(1.0).build();

    private final FLabel lblEditor = new FLabel.ButtonBuilder().text("Deck Editor").fontSize(14).build();
    private final FLabel lblStartServer = new FLabel.ButtonBuilder().text("Start Server").fontSize(14).build();
    private final FLabel lblStopServer = new FLabel.ButtonBuilder().text("Stop Server").fontSize(14).build();
    private final FLabel lblExit = new FLabel.ButtonBuilder().text("Exit Forge").fontSize(14).build();

    private VHomeUI() {
        // Add main menu containing logo and menu buttons
        final JPanel pnlMainMenu = new JPanel(new MigLayout("w 200px!, ax center, insets 0, gap 0, wrap"));
        pnlMainMenu.setOpaque(false);

        final List<FLabel> mainMenuButtons = new ArrayList<FLabel>();
        mainMenuButtons.add(lblEditor);
        if (NewConstants.SERVER_PORT_NUMBER >= 80) {
            mainMenuButtons.add(lblStartServer);
            mainMenuButtons.add(lblStopServer);
            lblStopServer.setEnabled(false);
        }
        mainMenuButtons.add(lblExit);

        final int logoSize = 170;
        final int logoBottomGap = 4;
        final int buttonHeight = 30;
        final int buttonBottomGap = 8;
        final int pnlMainMenuHeight = logoSize + logoBottomGap +
                mainMenuButtons.size() * (buttonHeight + buttonBottomGap);

        pnlMainMenu.add(lblLogo, "w " + logoSize + "px!, h " + logoSize +
                "px!, gap 0 4px 0 " + logoBottomGap + "px");
        String buttonLayout = "w 170px!, h " + buttonHeight +
                "px!, gap 0 0 0 " + buttonBottomGap + "px";
        for (FLabel button : mainMenuButtons) {
            pnlMainMenu.add(button, buttonLayout);
        }
        pnlMenu.add(pnlMainMenu);
        
        pnlSubmenus = new FScrollPanel(new MigLayout("insets 0, gap 0, wrap, hidemode 3"), true,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Add new menu items here (order doesn't matter).
        allSubmenus.add(VSubmenuConstructed.SINGLETON_INSTANCE);
        allSubmenus.add(VSubmenuDraft.SINGLETON_INSTANCE);
        allSubmenus.add(VSubmenuSealed.SINGLETON_INSTANCE);

        allSubmenus.add(VSubmenuDuels.SINGLETON_INSTANCE);
        allSubmenus.add(VSubmenuChallenges.SINGLETON_INSTANCE);
        allSubmenus.add(VSubmenuQuestDecks.SINGLETON_INSTANCE);
        allSubmenus.add(VSubmenuQuestData.SINGLETON_INSTANCE);
        allSubmenus.add(VSubmenuQuestPrefs.SINGLETON_INSTANCE);

        allSubmenus.add(VSubmenuGauntletQuick.SINGLETON_INSTANCE);
        allSubmenus.add(VSubmenuGauntletBuild.SINGLETON_INSTANCE);
        allSubmenus.add(VSubmenuGauntletLoad.SINGLETON_INSTANCE);
        allSubmenus.add(VSubmenuGauntletContests.SINGLETON_INSTANCE);

        allSubmenus.add(VSubmenuPreferences.SINGLETON_INSTANCE);
        allSubmenus.add(VSubmenuAvatars.SINGLETON_INSTANCE);
        allSubmenus.add(VSubmenuDownloaders.SINGLETON_INSTANCE);

        //allSubmenus.add(VSubmenuDeckEditor.SINGLETON_INSTANCE);
        //allSubmenus.add(VSubmenuExit.SINGLETON_INSTANCE);

        allSubmenus.add(VSubmenuArchenemy.SINGLETON_INSTANCE);
        allSubmenus.add(VSubmenuVanguard.SINGLETON_INSTANCE);        
        allSubmenus.add(VSubmenuPlanechase.SINGLETON_INSTANCE);

        // For each group: init its panel
        final SortedMap<EMenuGroup, JPanel> allGroupPanels = new TreeMap<EMenuGroup, JPanel>();
        for (final EMenuGroup e : EMenuGroup.values()) {
            allGroupPanels.put(e, new PnlGroup());
            allGroupPanels.get(e).setVisible(false);
            allGroupPanels.get(e).setLayout(new MigLayout("insets 0, gap 0, wrap"));
            allGroupPanels.get(e).setName(e.toString());
        }

        // For each item: Add to its group, and add to the card layout in right panel.
        for (final IVSubmenu<? extends ICDoc> item : allSubmenus) {
            allSubmenuLabels.put(item.getItemEnum(), new LblMenuItem(item));
            allGroupPanels.get(item.getGroupEnum()).add(
                    allSubmenuLabels.get(item.getItemEnum()), "w 100%!, h 30px!, gap 0 0 1px 1px");
        }

        // For each group: add its title, then its panel, then "click" if necessary.
        for (final EMenuGroup e : allGroupPanels.keySet()) {
            allGroupLabels.put(e, new LblGroup(e));
            pnlSubmenus.add(allGroupLabels.get(e), "w 100%!, h 30px!, gap 0 0 3px 3px");
            pnlSubmenus.add(allGroupPanels.get(e), "w 100%!, gap 0 0 0 0");

            // Expand groups expanded from previous session
            if (Singletons.getModel().getPreferences().getPrefBoolean(FPref.valueOf("SUBMENU_" + e.toString()))) {
                allGroupLabels.get(e).groupClick(e);
            }
        }

        pnlMenu.add(pnlSubmenus, "w 100%!, h 100% - " + pnlMainMenuHeight + "px!");
        pnlDisplay.setBackground(FSkin.alphaColor(l00, 100));
    }

    /** @return {@link forge.gui.toolbox.ExperimentalLabel} */
    public FLabel getLblExit() {
        return this.lblExit;
    }

    /** @return {@link forge.gui.toolbox.ExperimentalLabel} */
    public FLabel getLblEditor() {
        return this.lblEditor;
    }

    public final FLabel getLblStartServer() {
        return lblStartServer;
    }
    
    public final FLabel getLblStopServer() {
        return lblStopServer;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getPnlMenu() {
        return pnlMenu;
    }

    /** @return {@link javax.swing.JPanel} */
    public PnlDisplay getPnlDisplay() {
        return pnlDisplay;
    }

    /** @return {@link forge.gui.toolbox.FScrollPanel} */
    public FScrollPanel getPnlSubmenus() {
        return pnlSubmenus;
    }

    /**
     * 
     * @return Map<EMenuItem, FLabel>
     */
    public Map<EDocID, LblMenuItem> getAllSubmenuLabels() {
        return allSubmenuLabels;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVTopLevelUI#instantiate()
     */
    @Override
    public void instantiate() {

    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVTopLevelUI#populate()
     */
    @Override
    public void populate() {
        JPanel pnl = FView.SINGLETON_INSTANCE.getPnlInsets();
        pnl.setBorder(null);
        pnl.setLayout(new MigLayout("insets 0, gap 0"));

        pnl.add(pnlMenu, "w 205px!, h 100%!");
        pnl.add(pnlDisplay, "w 100% - 205px!, h 100%!");
    }

    /** */
    public class PnlDisplay extends JPanel implements ILocalRepaint {
        /** Constructor. */
        public PnlDisplay() {
            this.setOpaque(false);
        }

        @Override
        public void repaintSelf() {
            final Dimension d = this.getSize();
            repaint(0, 0, d.width, d.height);
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            final Graphics2D g2d = (Graphics2D) g.create();

            g2d.setColor(this.getBackground());
            g2d.fillRect(0, 0, getWidth(), getHeight());

            g2d.dispose();
        }
    }

    private class PnlMenu extends JPanel {
        private final Color d80 = FSkin.stepColor(clrTheme, -80);

        public PnlMenu() {
            this.setLayout(new MigLayout("insets 0, gap 0, wrap, hidemode 3"));
            this.setOpaque(false);
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            final LblMenuItem lblSelected = CHomeUI.SINGLETON_INSTANCE.getLblSelected();
            final FScrollPanel scrollPanel = VHomeUI.SINGLETON_INSTANCE.getPnlSubmenus();
            final Graphics2D g2d = (Graphics2D) g.create();
            final int w = getWidth();
            int y1 = 0;
            int y2 = 0;
            int h1 = getHeight();
            int h2 = 0;
            
            if (lblSelected.isShowing()) {
                int scrollPanelTop = scrollPanel.getY();
                int labelTop = lblSelected.getY() + lblSelected.getParent().getY() + scrollPanelTop - scrollPanel.getVerticalScrollBar().getValue();
                y2 = labelTop + lblSelected.getHeight();

                //ensure clipped to scroll panel
                if (y2 > scrollPanelTop) {
                    if (labelTop < scrollPanelTop) {
                        labelTop = scrollPanelTop;
                    }
                    h2 = h1 - y2;
                    h1 = labelTop - y1;
                }
            }

            g2d.setColor(l00);
            g2d.fillRect(0, y1, w, h1);
            if (h2 > 0) {
                g2d.fillRect(0, y2, w, h2);
            }
            
            int x = w - 8;
            GradientPaint edge = new GradientPaint(x, 0, l00, w, 0, d80, false);
            g2d.setPaint(edge);
            g2d.fillRect(x, y1, w, h1);
            if (h2 > 0) {
                g2d.fillRect(x, y2, w, h2);
            }

            g2d.dispose();
        }
    }
}
