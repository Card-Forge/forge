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

import forge.card.cardfactory.CardStorageReader;
import forge.control.FControl;
import forge.model.FModel;
import forge.properties.NewConstants;
import forge.view.FView;

/**
 * Provides global/static access to singleton instances.
 */
public final class Singletons {
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
        if(withUi)
            view = FView.SINGLETON_INSTANCE;
        
        // Loads all cards (using progress bar).
        FThreads.assertExecutedByEdt(false);
        final CardStorageReader reader = new CardStorageReader(NewConstants.CARD_DATA_DIR, true, withUi ? view.getSplash().getProgressBar() : null);
        magicDb = new StaticData(reader, "res/editions", "res/blockdata");
        model = FModel.getInstance(withUi);
        
        if(withUi)
            control = FControl.instance;
        
    }
    
    // disallow instantiation
    private Singletons() { }
}
