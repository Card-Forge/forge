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
package forge.view.swing;

import forge.AllZone;
import forge.Singletons;
import forge.error.ErrorViewer;
import forge.error.ExceptionHandler;
import forge.model.FModel;
import forge.view.FView;
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
     * main method for Forge's swing application view.
     * 
     * @param args
     *            an array of {@link java.lang.String} objects.
     */
    public static void main(final String[] args) {
        ExceptionHandler.registerErrorHandling();
        try {
            final FModel model = new FModel(null);
            Singletons.setModel(model);

            final FSkin skin = new FSkin(model.getPreferences().getSkin());
            final FView view = new ApplicationView(skin);
            Singletons.setView(view);
            AllZone.setSkin(skin);

            // Need this soon after card factory is loaded
            OldGuiNewGame.loadDynamicGamedata();

            // TODO this code should go elsewhere, like wherever we start a new
            // game.
            // It is only here to maintain semantic equality with the current
            // code base.

            model.resetGameState();

            view.setModel(model);

        } catch (final Throwable exn) { // NOPMD by Braids on 8/7/11 1:07 PM:
                                        // must
            // catch all throwables here.
            ErrorViewer.showError(exn);
        }
    }
}
