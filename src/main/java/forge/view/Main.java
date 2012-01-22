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

import forge.Singletons;
import forge.error.ErrorViewer;
import forge.error.ExceptionHandler;
import forge.model.FModel;
import forge.view.toolbox.FSkin;

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
            final FSkin skin = new FSkin(model.getPreferences().getSkin());
            final FView view = new FView(skin);

            Singletons.setModel(model);
            Singletons.setView(view);

            // Instantiate FGameState for TriggerHandler on card objects created in preloader.
            model.resetGameState();

            // Start splash frame.
            view.initialize();
        } catch (final Throwable exn) {
            ErrorViewer.showError(exn);
        }
    }
}
