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
package forge;

import forge.model.FModel;
import forge.view.FView;

/**
 * Provides global/static access to singleton instances.
 */
public final class Singletons {

    private static FModel model = null;

    private static FView view = null;

    /**
     * Do not instantiate.
     */
    private Singletons() {
        // This line intentionally left blank.
    }

    /**
     * Gets the model.
     * 
     * @return the model
     */
    public static FModel getModel() {
        return Singletons.model;
    }

    /**
     * Sets the model.
     * 
     * @param theModel
     *            the model to set
     */
    public static void setModel(final FModel theModel) {
        Singletons.model = theModel;
    }

    /**
     * Gets the view.
     * 
     * @return the view
     */
    public static FView getView() {
        return Singletons.view;
    }

    /**
     * Sets the view.
     * 
     * @param theView
     *            the view to set
     */
    public static void setView(final FView theView) {
        Singletons.view = theView;
    }

}
