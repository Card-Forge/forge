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

import javax.swing.SwingUtilities;

import forge.Singletons;
import forge.control.FControl;
import forge.error.ErrorViewer;
import forge.error.ExceptionHandler;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;

/**
 * Main class for Forge's swing application view.
 */
public final class Main {

    /**
     * Do not instantiate.
     */
    private Main() {
        // intentionally blank
    }

    /**
     * Main method for Forge.
     * 
     * @param args
     *            an array of {@link java.lang.String} objects.
     */
    public static void main(final String[] args) {
        ExceptionHandler.registerErrorHandling();
        try {
            final FModel model = new FModel();
            final FView view = new FView();
            final FControl control = FControl.SINGLETON_INSTANCE;

            Singletons.setModel(model);
            Singletons.setView(view);
            Singletons.setControl(control);

            // Instantiate FGameState for TriggerHandler on card objects created in preloader.
            model.resetGameState();

            // Start splash frame.
            view.initialize();

            // Start control on FView.
            control.initialize();

            // Open previous menu on first run, or constructed.
            // Focus is reset when the frame becomes visible,
            // so the call to show the menu must happen here.
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
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
                }
            });
        } catch (final Throwable exn) {
            ErrorViewer.showError(exn);
        }
    }
}
