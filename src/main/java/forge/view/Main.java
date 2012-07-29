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
import forge.error.ExceptionHandler;
import forge.model.FModel;

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
        //Possible solution to "Comparison method violates it's general contract!" crash
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");

        ExceptionHandler.registerErrorHandling();

        Singletons.setModel(FModel.SINGLETON_INSTANCE);

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    Singletons.setView(FView.SINGLETON_INSTANCE);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        Singletons.setControl(FControl.SINGLETON_INSTANCE);

        // Use splash frame to initialize everything, then transition to core UI.
        Singletons.getControl().initialize();

        SwingUtilities.invokeLater(new Runnable() { @Override
            public void run() { Singletons.getView().initialize(); } });
    }

    /** @throws Throwable  */
    @Override
    protected void finalize() throws Throwable {
        try { } catch (Exception e) { }
        finally {
            super.finalize();
            //more code can be written here as per need of application
            Singletons.getModel().close();
        }
    }
}
