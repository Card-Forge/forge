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
import forge.gui.CardPreferences;
import forge.gui.toolbox.FProgressBar;
import forge.gui.workshop.CardScriptInfo;
import forge.model.FModel;
import forge.properties.NewConstants;
import forge.view.FView;

/**
 * Provides global/static access to singleton instances.
 */
public final class Singletons {
    private static boolean initialized = false;
    private static FModel   model   = null;
    private static FView    view    = null;
    private static FControl control = null;
    private static StaticData magicDb = null;

    /**
     * IMPORTANT - does not return view frame!  Must call
     * getFrame() from FView for that.
     */
    public static FView    getView()    { return view;    }
    public static FControl getControl() { return control; }
    public static FModel   getModel()   { return model;   }
    public static StaticData getMagicDb() { return magicDb; }

    public static void initializeOnce(boolean withUi) {
        FThreads.assertExecutedByEdt(false);
        
        synchronized (Singletons.class) {
            if(initialized) 
                throw new IllegalStateException("Singletons.initializeOnce really cannot be called again");
            initialized = true;
        }

        if (withUi) {
            view = FView.SINGLETON_INSTANCE;
        }

        CardStorageReader.ProgressObserver progressBarBridge = view == null 
                ? CardStorageReader.ProgressObserver.emptyObserver : new CardStorageReader.ProgressObserver() {
            FProgressBar bar = view.getSplash().getProgressBar();
            @Override
            public void setOperationName(final String name, final boolean usePercents) { 
                FThreads.invokeInEdtLater(new Runnable() { @Override public void run() {
                    bar.setDescription(name); 
                    bar.setPercentMode(usePercents); 
                } });
            }
            
            @Override
            public void report(int current, int total) {
                if ( total != bar.getMaximum())
                    bar.setMaximum(total);
                bar.setValueThreadSafe(current);
            }
        };
        
        // Loads all cards (using progress bar).
        final CardStorageReader reader = new CardStorageReader(NewConstants.CARD_DATA_DIR, progressBarBridge, CardScriptInfo.readerObserver);
        magicDb = new StaticData(reader, "res/editions", "res/blockdata");
        model = FModel.getInstance(withUi);
        
        if (withUi) {
            control = FControl.instance;
            
            CardPreferences.load(NewConstants.CARD_PREFS_FILE);
        }
    }
    
    // disallow instantiation
    private Singletons() { }
}
