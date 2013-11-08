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
    private static FModel   model   = null;
    private static FView    view    = null;
    private static FControl control = null;

    /**
     * IMPORTANT - does not return view frame!  Must call
     * getFrame() from FView for that.
     */
    public static FView    getView()    { return view;    }
    public static FControl getControl() { return control; }
    public static FModel   getModel()   { return model;   }

    public static void setModel  (FModel   model0)   { model   = model0;   }
    public static void setView   (FView    view0)    { view    = view0;    }
    public static void setControl(FControl control0) { control = control0; }

    // disallow instantiation
    private Singletons() { }
}
