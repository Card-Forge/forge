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

import forge.control.FControl;
import forge.model.FModel;
import forge.view.FView;

/**
 * Provides global/static access to singleton instances.
 */
public final class Singletons {

    private static FModel model = null;

    private static FView view = null;

    private static FControl control = null;

    /**
     * Do not instantiate.
     */
    private Singletons() {
        // This line intentionally left blank.
    }

    /** @return {@link forge.model.FModel} */
    public static FModel getModel() {
        return Singletons.model;
    }

    /** @param model0 &emsp; {@link forge.model.FModel} */
    public static void setModel(final FModel model0) {
        Singletons.model = model0;
    }

    /**
     * IMPORTANT - does not return view frame!  Must call
     * getFrame() from FView for that.
     * @return {@link forge.model.JFrame} */
    public static FView getView() {
        return Singletons.view;
    }

    /** @param view0 &emsp; {@link forge.model.FView} */
    public static void setView(final FView view0) {
        Singletons.view = view0;
    }

    /** @return {@link forge.control.FControl} */
    public static FControl getControl() {
        return Singletons.control;
    }

    /** @param control0 &emsp; {@link forge.control.FControl} */
    public static void setControl(final FControl control0) {
        Singletons.control = control0;
    }
}
