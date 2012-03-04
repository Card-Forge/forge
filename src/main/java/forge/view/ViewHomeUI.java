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
package forge.view;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
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
import forge.AllZone;
import forge.Command;
import forge.Singletons;
import forge.gui.home.EMenuGroup;
import forge.gui.home.EMenuItem;
import forge.gui.home.IVSubmenu;
import forge.gui.home.limited.VSubmenuConstructed;
import forge.gui.home.limited.VSubmenuDraft;
import forge.gui.home.limited.VSubmenuSealed;
import forge.gui.home.quest.VSubmenuChallenges;
import forge.gui.home.quest.VSubmenuDuels;
import forge.gui.home.quest.VSubmenuQuestData;
import forge.gui.home.quest.VSubmenuQuestDecks;
import forge.gui.home.quest.VSubmenuQuestPrefs;
import forge.gui.home.settings.VSubmenuAvatars;
import forge.gui.home.settings.VSubmenuPreferences;
import forge.gui.home.utilities.VSubmenuDeckEditor;
import forge.gui.home.utilities.VSubmenuUtilities;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FPanel;
import forge.gui.toolbox.FScrollPane;
import forge.gui.toolbox.FSkin;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.data.QuestDataIO;
import forge.quest.data.QuestPreferences.QPref;

/** Singleton instance of home screen UI.
 * Use "getPanel()" to work with the main container.
 * <br><br>
 * Generates a card layout with grouped submenus automatically.<br>
 * To add a menu to the home UI:<br>
 * - Register its name in the EMenuItem enum<br>
 * - Build a view implementing IVSubmenu<br>
 * - Build a controller impelementing ICSubmenu<br>
 * - Add its singleton instance to the map storing the views for the card layout.
 */

public enum ViewHomeUI {
    /** */
    SINGLETON_INSTANCE;

    private final CardLayout cards = new CardLayout();
    private final FPanel pnlParent = new FPanel();
    private final FPanel pnlLeft = new FPanel();
    private final FPanel pnlRight = new FPanel(cards);
    private final JPanel pnlMenu = new JPanel();

    private final int insets = 10;
    private final int leftWidthPx = 250;
    private final List<IVSubmenu> allSubmenus = new ArrayList<IVSubmenu>();
    private final Map<EMenuItem, FLabel> allSubmenuLabels = new HashMap<EMenuItem, FLabel>();
    private FLabel lblPreviousSelected;

    /** Mostly, assembles child singletons for the home screen UI.  */
    public void initialize() {
        // There's d a better home for this (model?)
        final File dirQuests = ForgeProps.getFile(NewConstants.Quest.DATA_DIR);
        final String questname = Singletons.getModel()
                .getQuestPreferences().getPreference(QPref.CURRENT_QUEST);
        final File data = new File(dirQuests.getPath() + File.separator + questname);

        if (data.exists()) {
            AllZone.setQuestData(QuestDataIO.loadData(data));
        }        //////////////////////////////


        // Add new menu items here (order doesn't matter).
        allSubmenus.clear();
        allSubmenus.add(VSubmenuConstructed.SINGLETON_INSTANCE);
        allSubmenus.add(VSubmenuDraft.SINGLETON_INSTANCE);
        allSubmenus.add(VSubmenuSealed.SINGLETON_INSTANCE);

        allSubmenus.add(VSubmenuDuels.SINGLETON_INSTANCE);
        allSubmenus.add(VSubmenuChallenges.SINGLETON_INSTANCE);
        allSubmenus.add(VSubmenuQuestDecks.SINGLETON_INSTANCE);
        allSubmenus.add(VSubmenuQuestData.SINGLETON_INSTANCE);
        allSubmenus.add(VSubmenuQuestPrefs.SINGLETON_INSTANCE);

        allSubmenus.add(VSubmenuPreferences.SINGLETON_INSTANCE);
        allSubmenus.add(VSubmenuAvatars.SINGLETON_INSTANCE);

        allSubmenus.add(VSubmenuDeckEditor.SINGLETON_INSTANCE);
        allSubmenus.add(VSubmenuUtilities.SINGLETON_INSTANCE);

        // Parent layout
        pnlParent.setCornerDiameter(0);
        pnlParent.setBorderToggle(false);
        pnlParent.setBackgroundTexture(FSkin.getIcon(FSkin.Backgrounds.BG_TEXTURE));
        pnlParent.setLayout(null);

        pnlParent.add(pnlLeft);
        pnlParent.add(pnlRight);

        // Left pane holds scroller with menu panel.
        final FScrollPane scrMenu = new FScrollPane(pnlMenu,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrMenu.setBorder(null);

        pnlLeft.setLayout(new MigLayout("insets 0, gap 0, align center, wrap"));
        pnlLeft.add(new FLabel.Builder().icon(FSkin.getIcon(FSkin.ForgeIcons.ICO_LOGO))
                .iconScaleFactor(1.0f).build(), "w 150px!, h 150px!, align center");

        pnlLeft.add(scrMenu, "pushy, growy, w 98%!, gap 1% 0 1% 0");

        populateMenu();

        // Select previous
        EMenuItem selected = null;
        try {
            selected = EMenuItem.valueOf(Singletons.getModel()
                .getPreferences().getPref(FPref.SUBMENU_CURRENTMENU));
        } catch (final Exception e) { }

        if (selected != null && allSubmenuLabels.get(selected) != null) {
            itemClick(selected);
        }
        else {
            itemClick(EMenuItem.CONSTRUCTED);
        }

        pnlParent.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent e) {
                final int w = pnlParent.getWidth();
                final int h = pnlParent.getHeight();
                pnlRight.setBounds(new Rectangle(
                        2 * insets + leftWidthPx, insets,
                        w - leftWidthPx - 3 * insets, h - 2 * insets));
                pnlLeft.setBounds(new Rectangle(
                        insets, insets,
                        leftWidthPx, h - 2 * insets
                        ));
                pnlParent.revalidate();
            }
        });
    }

    /** Generates expand/collapse menu using cards in layout, and EMenuGroup enum.
     * No further hardcoding should be required in this method. */
    private void populateMenu() {
        final SortedMap<EMenuGroup, JPanel> allGroupPanels = new TreeMap<EMenuGroup, JPanel>();
        final Map<EMenuGroup, JLabel> allGroupLabels = new HashMap<EMenuGroup, JLabel>();

        final ForgePreferences prefs = Singletons.getModel().getPreferences();
        final String strTitleConstraints = "w 90%!, gap 5% 0 5px 10px";
        final String strGroupConstraints = "w 85%!, gap 10% 0 0 0";
        final String strItemConstraints = "w 100%!, h 26px!, gap 0 0 1px 1px";

        pnlMenu.removeAll();
        pnlMenu.setLayout(new MigLayout("insets 0, gap 0, wrap, hidemode 3"));
        pnlMenu.setOpaque(false);

        // For each group: init its panel
        for (final EMenuGroup e : EMenuGroup.values()) {
            allGroupPanels.put(e, new JPanel());
            allGroupPanels.get(e).setOpaque(false);
            allGroupPanels.get(e).setVisible(false);
            allGroupPanels.get(e).setLayout(new MigLayout("insets 0, gap 0, wrap"));
            allGroupPanels.get(e).setName(e.toString());
        }

        // For each item: Add to its group, and add to the card layout in right panel.
        allSubmenuLabels.clear();
        for (final IVSubmenu item : allSubmenus) {
            allSubmenuLabels.put(EMenuItem.valueOf(item.getItemEnum()), makeItemLabel(item));
            pnlRight.add(item.getItemEnum(), item.getPanel());
            allGroupPanels.get(item.getGroupEnum()).add(
                    allSubmenuLabels.get(EMenuItem.valueOf(item.getItemEnum())), strItemConstraints);
            item.getControl().initialize();
        }

        // For each group: add its title, then its panel, then "click" if necessary.
        for (final EMenuGroup e : allGroupPanels.keySet()) {
            allGroupLabels.put(e, makeTitleLabel(e));
            pnlMenu.add(allGroupLabels.get(e), strTitleConstraints);
            pnlMenu.add(allGroupPanels.get(e), strGroupConstraints);

            // Expand groups expanded from previous session
            if (prefs.getPrefBoolean(FPref.valueOf("SUBMENU_" + e.toString()))) {
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
        final FLabel lbl = new FLabel.Builder().fontScaleAuto(false).fontSize(16)
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

        final FLabel lbl = new FLabel.Builder().fontScaleAuto(false).fontSize(15)
                .hoverable(true).selectable(true).text(item.getMenuTitle())
                .fontAlign(SwingConstants.LEFT).build();

        final Command cmdOnClick = new Command() {
            @Override
            public void execute() {
                if (lblPreviousSelected != null) { lblPreviousSelected.setSelected(false); }

                cards.show(pnlRight, item.getItemEnum());
                lblPreviousSelected = lbl;

                prefs.setPref(FPref.SUBMENU_CURRENTMENU, item.getItemEnum());
                Singletons.getModel().getPreferences().save();

                // Make sure this is called last, so it doesn't interfere
                // with the selection display process.
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (item.getControl().getMenuCommand() != null) {
                            item.getControl().getMenuCommand().execute();
                        }
                    }
                });
            }
        };

        lbl.setCommand(cmdOnClick);
        lbl.setBorder(new EmptyBorder(0, 10, 0, 0));

        return lbl;
    }

    /** Programatically selects a menu item.
     * @param e0 &emsp; {@forge.gui.home.EMenuItem} */
    public void itemClick(final EMenuItem e0) {
        allSubmenuLabels.get(e0).getCommand().execute();
        allSubmenuLabels.get(e0).setSelected(true);
    }

    /** @return {@link javax.swing.JPanel} the parent panel containing the home UI. */
    public JPanel getPanel() {
        return pnlParent;
    }
}
