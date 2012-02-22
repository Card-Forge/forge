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

import java.awt.Dimension;
import java.awt.Frame;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

import forge.Singletons;
import forge.control.FControl;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.view.toolbox.FOverlay;
import forge.view.toolbox.FSkin;

/**
 * The main view for Forge: a java swing application. All view class instances
 * should be accessible from here.
 */
@SuppressWarnings("serial")
public final class FView extends JFrame {
    private final JLayeredPane lpnContent = new JLayeredPane();
    private final FOverlay overlay = new FOverlay();

    private SplashFrame splash;
    private ViewHomeUI home = null;
    private ViewMatchUI match = null;
    private ViewEditorUI editor = null;
    private ViewBazaarUI bazaar = null;

  //private static final JLayeredPane lpnContent;
    //private static final FControl control;

    /** The splash frame is guaranteed to exist when this constructor exits. */
    public FView() {
        super();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try { splash = new SplashFrame(); }
                catch (Exception e) { e.printStackTrace(); }
            }
        });
    }

    /** Transitions between splash and main UI.  Called after everything is initialized. */
    public void initialize() {
        SplashFrame.PROGRESS_BAR.setDescription("Creating display components.");

        // After events and shortcuts are assembled, instantiate all different state screens
        Singletons.getView().instantiateCachedUIStates();

        // Open previous menu on first run, or constructed.
        // Focus is reset when the frame becomes visible,
        // so the call to show the menu must happen here.
        final ForgePreferences.HomeMenus lastMenu =
                ForgePreferences.HomeMenus.valueOf(Singletons.getModel().getPreferences().getPref(FPref.UI_HOMEMENU));

        switch(lastMenu) {
            case draft:
                Singletons.getView().getViewHome().getBtnDraft().grabFocus();
                Singletons.getView().getViewHome().showDraftMenu();
                break;
            case sealed:
                Singletons.getView().getViewHome().getBtnSealed().grabFocus();
                Singletons.getView().getViewHome().showSealedMenu();
                break;
            case quest:
                Singletons.getView().getViewHome().getBtnQuest().grabFocus();
                Singletons.getView().getViewHome().showQuestMenu();
                break;
            case settings:
                Singletons.getView().getViewHome().getBtnSettings().grabFocus();
                Singletons.getView().getViewHome().showSettingsMenu();
                break;
            case utilities:
                Singletons.getView().getViewHome().getBtnUtilities().grabFocus();
                Singletons.getView().getViewHome().showUtilitiesMenu();
                break;
            default:
                Singletons.getView().getViewHome().getBtnConstructed().grabFocus();
                Singletons.getView().getViewHome().showConstructedMenu();
        }

        // Frame styling
        FView.this.setMinimumSize(new Dimension(800, 600));
        FView.this.setLocationRelativeTo(null);
        FView.this.setExtendedState(FView.this.getExtendedState() | Frame.MAXIMIZED_BOTH);
        FView.this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        FView.this.setIconImage(FSkin.getIcon(FSkin.ForgeIcons.ICO_FAVICON).getImage());
        FView.this.setTitle("Forge: " + Singletons.getModel().getBuildInfo().getVersion());

        // Content pane
        FView.this.lpnContent.setOpaque(true);
        FView.this.setContentPane(FView.this.lpnContent);

        // Overlay
        overlay.setBounds(0, 0, FView.this.getWidth(), FView.this.getHeight());
        overlay.setBackground(FSkin.getColor(FSkin.Colors.CLR_OVERLAY));
        FView.this.lpnContent.add(overlay, JLayeredPane.MODAL_LAYER);

        // All is ready to go - fire up home screen and discard splash frame.
        Singletons.getControl().changeState(FControl.HOME_SCREEN);

        FView.this.splash.dispose();
        FView.this.splash = null;

        FView.this.setVisible(true);
    }

    /** @return {@link javax.swing.JLayeredPane} */
    public JLayeredPane getLayeredContentPane() {
        return FView.this.lpnContent;
    }

    /** @return {@link forge.view.toolbox.FOverlay} */
    public FOverlay getOverlay() {
        return FView.this.overlay;
    }

    /** @return {@link forge.view.ViewHomeUI} */
    public ViewHomeUI getViewHome() {
        if (Singletons.getControl().getState() != FControl.HOME_SCREEN) {
            throw new IllegalArgumentException("FView$getViewHome\n"
                    + "may only be called while the home UI is showing.");
        }
        return FView.this.home;
    }

    /** @return {@link forge.view.ViewMatchUI} */
    public ViewMatchUI getViewMatch() {
        if (Singletons.getControl().getState() != FControl.MATCH_SCREEN) {
            throw new IllegalArgumentException("FView$getVIewMatch\n"
                    + "may only be called while the match UI is showing.");
        }
        return FView.this.match;
    }

    /** @return {@link forge.view.ViewEditorUI} */
    public ViewEditorUI getViewEditor() {
        if (Singletons.getControl().getState() != FControl.DEFAULT_EDITOR) {
            throw new IllegalArgumentException("FView$getViewEditor\n"
                    + "may only be called while the editor UI is showing.");
        }
        return FView.this.editor;
    }

    /** @return {@link forge.view.ViewBazaarUI} */
    public ViewBazaarUI getViewBazaar() {
        if (Singletons.getControl().getState() != FControl.QUEST_BAZAAR) {
            throw new IllegalArgumentException("FView$getViewBazaar\n"
                    + "may only be called while the bazaar UI is showing.");
        }
        return FView.this.bazaar;
    }

    /** Like it says. */
    public void instantiateCachedUIStates() {
        FView.this.home = new ViewHomeUI();
        FView.this.match = new ViewMatchUI();
        FView.this.editor = new ViewEditorUI();
        FView.this.bazaar = new ViewBazaarUI();
    }
}
