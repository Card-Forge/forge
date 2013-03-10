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
import forge.control.FControl;
import forge.model.FModel;

/**
 * Main class for Forge's swing application view.
 */
public final class Main {
    /**
     * Main entrypoint for Forge
     */
    public static void main(final String[] args) {
        // HACK - temporary solution to "Comparison method violates it's general contract!" crash
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");

        // Start splash screen first, then data models, then controller.
        Singletons.setView(FView.SINGLETON_INSTANCE);
        Singletons.setModel(FModel.SINGLETON_INSTANCE);
        Singletons.setControl(FControl.SINGLETON_INSTANCE);

        // Controller can now step in and take over.
        Singletons.getControl().initialize();
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            Singletons.getModel().close();
        } finally {
            super.finalize();
        }
    }

    // disallow instantiation
    private Main() { }
}
