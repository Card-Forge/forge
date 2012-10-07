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

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import net.miginfocom.swing.MigLayout;
import forge.Command;
import forge.Singletons;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.framework.IVDoc;
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
import forge.gui.home.settings.VSubmenuPreferences;
import forge.gui.home.utilities.VSubmenuDeckEditor;
import forge.gui.home.utilities.VSubmenuExit;
import forge.gui.home.utilities.VSubmenuUtilities;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FScrollPane;
import forge.gui.toolbox.FSkin;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;

/**
 * Assembles Swing components of main menu panel in home screen.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */

public enum VMainMenu implements IVDoc {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Welcome");

    private final JPanel pnlMenu = new JPanel();
    private final FScrollPane scrMenu = new FScrollPane(pnlMenu,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    private final List<IVSubmenu> allSubmenus = new ArrayList<IVSubmenu>();
    private final Map<EDocID, FLabel> allSubmenuLabels = new HashMap<EDocID, FLabel>();
    private FLabel lblPreviousSelected;
    private JLabel lblLogo = new FLabel.Builder()
        .icon(FSkin.getIcon(FSkin.InterfaceIcons.ICO_LOGO))
        .iconAlignX(SwingConstants.CENTER)
        .iconInBackground(true).iconScaleFactor(1.0).build();

    private VMainMenu() {
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

        allSubmenus.add(VSubmenuDeckEditor.SINGLETON_INSTANCE);
        allSubmenus.add(VSubmenuUtilities.SINGLETON_INSTANCE);
        allSubmenus.add(VSubmenuExit.SINGLETON_INSTANCE);

        // Left pane holds scroller with menu panel.
        pnlMenu.setLayout(new MigLayout("insets 0, gap 0, wrap, hidemode 3"));
        pnlMenu.setOpaque(false);
        scrMenu.setBorder(null);

        final Map<EMenuGroup, JLabel> allGroupLabels = new HashMap<EMenuGroup, JLabel>();

        final String strTitleConstraints = "w 90%!, gap 5% 0 5px 10px";
        final String strGroupConstraints = "w 85%!, gap 10% 0 0 0";
        final String strItemConstraints = "w 100%!, h 26px!, gap 0 0 1px 1px";

        // For each group: init its panel
        final SortedMap<EMenuGroup, JPanel> allGroupPanels = new TreeMap<EMenuGroup, JPanel>();
        for (final EMenuGroup e : EMenuGroup.values()) {
            allGroupPanels.put(e, new JPanel());
            allGroupPanels.get(e).setOpaque(false);
            allGroupPanels.get(e).setVisible(false);
            allGroupPanels.get(e).setLayout(new MigLayout("insets 0, gap 0, wrap"));
            allGroupPanels.get(e).setName(e.toString());
        }

        // For each item: Add to its group, and add to the card layout in right panel.
        for (final IVSubmenu item : allSubmenus) {
            allSubmenuLabels.put(item.getItemEnum(), makeItemLabel(item));
            allGroupPanels.get(item.getGroupEnum()).add(
                    allSubmenuLabels.get(item.getItemEnum()), strItemConstraints);
        }

        // For each group: add its title, then its panel, then "click" if necessary.
        for (final EMenuGroup e : allGroupPanels.keySet()) {
            allGroupLabels.put(e, makeTitleLabel(e));
            pnlMenu.add(allGroupLabels.get(e), strTitleConstraints);
            pnlMenu.add(allGroupPanels.get(e), strGroupConstraints);

            // Expand groups expanded from previous session
            if (Singletons.getModel().getPreferences().getPrefBoolean(FPref.valueOf("SUBMENU_" + e.toString()))) {
                groupClick(e, allGroupLabels.get(e));
            }
        }
    }

    private void groupClick(final EMenuGroup e0, final JLabel lbl0) {
        final Component[] menuObjects = pnlMenu.getComponents();
        for (final Component c : menuObjects) {
            if (c.getName() != null && c.getName().equals(e0.toString())) {
                if (c.isVisible()) {
                    lbl0.setText("+ " + e0.getTitle());
                    c.setVisible(false);
                    Singletons.getModel().getPreferences().setPref(
                            FPref.valueOf("SUBMENU_" + e0.toString()), "false");
                }
                else {
                    lbl0.setText("- " + e0.getTitle());
                    c.setVisible(true);
                    Singletons.getModel().getPreferences().setPref(
                            FPref.valueOf("SUBMENU_" + e0.toString()), "true");
                }

                Singletons.getModel().getPreferences().save();
                break;
            }
        }
    }

    /** Custom title label styling. */
    @SuppressWarnings("serial")
    private JLabel makeTitleLabel(final EMenuGroup e0) {
        final FLabel lbl = new FLabel.Builder().fontSize(16)
                .hoverable(true).fontAlign(SwingConstants.LEFT).build();

        lbl.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, FSkin.getColor(FSkin.Colors.CLR_BORDERS)),
                new EmptyBorder(2, 2, 2, 2)));

        lbl.setCommand(new Command() { @Override
            public void execute() { groupClick(e0, lbl); } });

        lbl.setText("+ " + e0.getTitle());
        return lbl;
    }

    /** Custom subsection label styling. */
    @SuppressWarnings("serial")
    private FLabel makeItemLabel(final IVSubmenu item) {
        final ForgePreferences prefs = Singletons.getModel().getPreferences();

        final FLabel lbl = new FLabel.Builder().fontSize(15)
                .hoverable(true).selectable(true).text(item.getMenuTitle())
                .fontAlign(SwingConstants.LEFT).build();

        final Command cmdOnClick = new Command() {
            @Override
            public void execute() {
                if (lblPreviousSelected != null) { lblPreviousSelected.setSelected(false); }

                if (!item.getItemEnum().equals(EDocID.HOME_EXIT)) {
                    item.getItemEnum().getDoc().getParentCell().setSelected(item.getItemEnum().getDoc());
                    lblPreviousSelected = lbl;

                    prefs.setPref(FPref.SUBMENU_CURRENTMENU, item.getItemEnum().toString());
                    Singletons.getModel().getPreferences().save();
                }

                // Make sure this is called last, so it doesn't interfere
                // with the selection display process.
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (item.getLayoutControl().getCommandOnSelect() != null) {
                            item.getLayoutControl().getCommandOnSelect().execute();
                        }
                    }
                });
            }
        };

        lbl.setCommand(cmdOnClick);
        lbl.setBorder(new EmptyBorder(0, 10, 0, 0));

        return lbl;
    }

    /**
     * 
     * @return Map<EMenuItem, FLabel>
     */
    public Map<EDocID, FLabel> getAllSubmenuLabels() {
        return allSubmenuLabels;
    }

    //========== Overridden from IVDoc

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_MAINMENU;
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
        return CMainMenu.SINGLETON_INSTANCE;
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

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#populate()
     */
    @Override
    public void populate() {
        parentCell.getBody().setLayout(new MigLayout("insets 0, gap 0, align center, wrap"));
        parentCell.getBody().add(lblLogo, "w 200px!, h 20%:20%:200px, gap 0 0 5px 10px, ax center");
        parentCell.getBody().add(scrMenu, "w 98%!, pushy, growy, ax center, gap 0 0 0 10px");
    }
}
