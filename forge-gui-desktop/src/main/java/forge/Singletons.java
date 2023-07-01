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
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.model.FModel;
import forge.view.FView;

/**
 * Provides global/static access to singleton instances.
 */
public final class Singletons {
    private static boolean initialized = false;
    private static FView    view    = null;
    private static FControl control = null;

    /**
     * IMPORTANT - does not return view frame!  Must call
     * getFrame() from FView for that.
     */
    public static FView    getView()    { return view;    }
    public static FControl getControl() { return control; }

    public static void initializeOnce(boolean withUi) {
        FThreads.assertExecutedByEdt(false);

        synchronized (Singletons.class) {
            if (initialized) {
                throw new IllegalStateException("Singletons.initializeOnce really cannot be called again");
            }
            initialized = true;
        }

        if (withUi) {
            view = FView.SINGLETON_INSTANCE;
        }

        ImageKeys.setIsLibGDXPort(GuiBase.getInterface().isLibgdxPort());
        FModel.initialize(view == null ? null : view.getSplash().getProgressBar(), null);

        if (withUi) {
            control = FControl.instance;
        }
    }

    // disallow instantiation
    private Singletons() { }
}
