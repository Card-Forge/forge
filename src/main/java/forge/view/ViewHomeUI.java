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

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import forge.Singletons;
import forge.control.ControlHomeUI;
import forge.control.home.ControlConstructed;
import forge.control.home.ControlDraft;
import forge.control.home.ControlSealed;
import forge.control.home.ControlUtilities;
import forge.game.GameType;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.view.home.ViewConstructed;
import forge.view.home.ViewDraft;
import forge.view.home.ViewQuest;
import forge.view.home.ViewSealed;
import forge.view.home.ViewSettings;
import forge.view.home.ViewUtilities;
import forge.view.toolbox.FButton;
import forge.view.toolbox.FPanel;
import forge.view.toolbox.FSkin;

/**
 * - Lays out containers and borders for main menu and submenus<br>
 * - Instantiates top-level controller for submenus.<br>
 * - Has access methods for all child controllers<br>
 * 
 */

@SuppressWarnings("serial")
public class ViewHomeUI extends FPanel {
    private JPanel pnlMenu, pnlContent;
    private FButton btnDraft, btnConstructed, btnSealed, btnQuest, btnSettings, btnUtilities, btnExit, btnDeckEditor;
    private String constraints;
    private ControlHomeUI control;

    private ViewConstructed constructed;
    private ViewSealed sealed;
    private ViewDraft draft;
    private ViewQuest quest;
    private ViewSettings settings;
    private ViewUtilities utilities;

    private final int insets = 10;
    private final int menuWidthPx = 350;

    /**
     * Instantiates a new home top level.
     */
    public ViewHomeUI() {
        super();

        constructed = new ViewConstructed(this);
        sealed = new ViewSealed(this);
        draft = new ViewDraft(this);
        quest = new ViewQuest(this);
        settings = new ViewSettings(this);
        utilities = new ViewUtilities(this);

        this.setCornerDiameter(0);
        this.setBorderToggle(false);
        this.setBackgroundTexture(FSkin.getIcon(FSkin.Backgrounds.BG_TEXTURE));
        this.setLayout(null);
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = getWidth();
                int h = getHeight();
                pnlContent.setBounds(new Rectangle(
                        2 * insets + menuWidthPx, insets,
                        w - menuWidthPx - 3 * insets, h - 2 * insets));
                pnlMenu.setBounds(new Rectangle(
                        insets, insets,
                        menuWidthPx, h - 2 * insets
                        ));
                revalidate();
            }
        });

        pnlMenu = new FPanel();
        pnlMenu.setLayout(new MigLayout("insets 0, gap 0, wrap"));

        pnlContent = new FPanel();
        pnlContent.setLayout(new MigLayout("insets 0, gap 0"));

        btnConstructed = new FButton();
        btnConstructed.setAction(new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) { showConstructedMenu(); }
        });
        btnConstructed.setText("Constructed");

        btnSealed = new FButton();
        btnSealed.setAction(new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) { showSealedMenu(); }
        });
        btnSealed.setText("Sealed");

        btnDraft = new FButton();
        btnDraft.setAction(new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) { showDraftMenu(); }
        });
        btnDraft.setText("Draft");

        btnQuest = new FButton();
        btnQuest.setAction(new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) { showQuestMenu(); }
        });
        btnQuest.setText("Quest");

        btnDeckEditor = new FButton();
        btnDeckEditor.setAction(new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) {
                ViewHomeUI.this.getUtilitiesController().showDeckEditor(GameType.Constructed, null);
            }
        });
        btnDeckEditor.setText("Deck Editor");

        btnSettings = new FButton();
        btnSettings.setAction(new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) { showSettingsMenu(); }
        });
        btnSettings.setText("Settings");

        btnUtilities = new FButton();
        btnUtilities.setAction(new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) { showUtilitiesMenu(); }
        });
        btnUtilities.setText("Utilities");

        btnExit = new FButton();
        btnExit.setAction(new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) { control.exit(); }
        });
        btnExit.setText("Exit");

        add(pnlMenu, "w 36%!, h 96%!, gap 2% 2% 2% 2%");
        add(pnlContent, "w 58%!, h 96%!, gap 0% 2% 2% 2%");

        JLabel lblIcon = new JLabel(FSkin.getIcon(FSkin.ForgeIcons.ICO_LOGO));
        pnlMenu.add(lblIcon, "gapleft 10%, ax center");

        constraints = "w 80%!, gapleft 10%, gaptop 1%, gapbottom 1%, h 40px!";
        pnlMenu.add(btnConstructed, constraints);
        pnlMenu.add(btnSealed, constraints);
        pnlMenu.add(btnDraft, constraints);
        pnlMenu.add(btnQuest, constraints);
        //pnlMenu.add(btnDeckEditor, constraints);
        pnlMenu.add(btnSettings, constraints);
        pnlMenu.add(btnUtilities, constraints);
        pnlMenu.add(btnExit, constraints);

        control = new ControlHomeUI(this);
        final ForgePreferences.HomeMenus lastMenu =
                ForgePreferences.HomeMenus.valueOf(Singletons.getModel().getPreferences().getPref(FPref.UI_HOMEMENU));

        switch(lastMenu) {
            case draft: showDraftMenu(); break;
            case sealed: showSealedMenu(); break;
            case quest: showQuestMenu(); break;
            case settings: showSettingsMenu(); break;
            case utilities: showUtilitiesMenu(); break;
            default: showConstructedMenu();
        }
    }

    /** Opens menu for constructed mode. */
    public void showConstructedMenu() {
        clearToggles();
        btnConstructed.setToggled(true);
        btnConstructed.grabFocus();
        pnlContent.removeAll();
        pnlContent.add(constructed, "w 100%!, h 100%!");
        pnlContent.revalidate();
        pnlContent.repaint();

        this.getConstructedController().updateDeckSelectionCheckboxes();

        Singletons.getModel().getPreferences().setPref(FPref.UI_HOMEMENU,
                ForgePreferences.HomeMenus.constructed.toString());
        Singletons.getModel().getPreferences().save();
    }

    /** Opens menu for draft mode. */
    public void showDraftMenu() {
        clearToggles();
        btnDraft.setToggled(true);
        btnDraft.grabFocus();

        pnlContent.removeAll();
        pnlContent.add(draft, "w 100%!, h 100%!");
        pnlContent.revalidate();
        pnlContent.repaint();

        Singletons.getModel().getPreferences().setPref(FPref.UI_HOMEMENU,
                ForgePreferences.HomeMenus.draft.toString());
        Singletons.getModel().getPreferences().save();
    }

    /** Opens menu for sealed mode. */
    public void showSealedMenu() {
        clearToggles();
        btnSealed.setToggled(true);
        btnSealed.grabFocus();

        pnlContent.removeAll();
        pnlContent.add(sealed, "w 100%!, h 100%!");
        pnlContent.revalidate();
        pnlContent.repaint();

        Singletons.getModel().getPreferences().setPref(FPref.UI_HOMEMENU,
                ForgePreferences.HomeMenus.sealed.toString());
        Singletons.getModel().getPreferences().save();
    }

    /** Opens menu for quest mode. */
    public void showQuestMenu() {
        clearToggles();
        pnlContent.removeAll();
        pnlContent.add(quest, "w 99%!, h 95%!, gaptop 2.5%, gapleft 0.5%");
        pnlContent.revalidate();
        pnlContent.repaint();

        Singletons.getModel().getPreferences().setPref(FPref.UI_HOMEMENU,
                ForgePreferences.HomeMenus.quest.toString());
        Singletons.getModel().getPreferences().save();

        SwingUtilities.invokeLater(new Runnable() { @Override
            public void run() {
                btnQuest.setToggled(true);
                btnQuest.grabFocus();
            }
        });
    }

    /** Opens menu for settings. */
    public void showSettingsMenu() {
        clearToggles();

        pnlContent.removeAll();
        pnlContent.add(settings, "w 99%!, h 95%!, gaptop 2.5%, gapleft 0.5%");
        pnlContent.revalidate();
        pnlContent.repaint();

        Singletons.getModel().getPreferences().setPref(FPref.UI_HOMEMENU,
                ForgePreferences.HomeMenus.settings.toString());
        Singletons.getModel().getPreferences().save();

        SwingUtilities.invokeLater(new Runnable() { @Override
            public void run() {
                btnSettings.setToggled(true);
                btnSettings.grabFocus();
            }
        });
    }

    /** Opens menu for utilities. */
    public void showUtilitiesMenu() {
        clearToggles();
        btnUtilities.setToggled(true);
        btnUtilities.grabFocus();

        pnlContent.removeAll();
        pnlContent.add(utilities, "w 100%!, h 100%!");
        pnlContent.revalidate();
        pnlContent.repaint();

        Singletons.getModel().getPreferences().setPref(FPref.UI_HOMEMENU,
                ForgePreferences.HomeMenus.utilities.toString());
        Singletons.getModel().getPreferences().save();
    }

    /** @return ControlConstructed */
    public ControlConstructed getConstructedController() {
        return constructed.getControl();
    }

    /** @return ControlDraft */
    public ControlDraft getDraftController() {
        return draft.getController();
    }

    /** @return ControlSealed */
    public ControlSealed getSealedController() {
        return sealed.getController();
    }

    /** @return ControlUtilities */
    public ControlUtilities getUtilitiesController() {
        return utilities.getController();
    }

    private void clearToggles() {
        btnConstructed.setToggled(false);
        btnSealed.setToggled(false);
        btnDraft.setToggled(false);
        btnQuest.setToggled(false);
        btnDeckEditor.setToggled(false);
        btnSettings.setToggled(false);
        btnUtilities.setToggled(false);
    }

    /** */
    public void resetQuest() {
        quest = new ViewQuest(this);
        showQuestMenu();
    }

    /** */
    public void resetSettings() {
        settings = new ViewSettings(this);
        showSettingsMenu();
    }

    /** @return ControlHomeUI */
    public ControlHomeUI getControl() {
        return control;
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnConstructed() {
        return this.btnConstructed;
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnSealed() {
        return this.btnSealed;
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnDraft() {
        return this.btnDraft;
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnQuest() {
        return this.btnQuest;
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnSettings() {
        return this.btnSettings;
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnUtilities() {
        return this.btnUtilities;
    }
}
