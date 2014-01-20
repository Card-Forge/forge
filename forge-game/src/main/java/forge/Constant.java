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

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Constant interface.
 * </p>
 * 
 * @author Forge
 * @version $Id: Constant.java 23713 2013-11-20 08:34:37Z Max mtg $
 */
public final class Constant {
    // used to pass information between the GUI screens
    /**
     * The Class Runtime.
     */
    public static class Preferences  {
        /** The Constant DevMode. */
        // one for normal mode, one for quest mode
        public static boolean DEV_MODE;
        /** The Constant UpldDrft. */
        public static boolean UPLOAD_DRAFT;

    }

    public static class Runtime {
        /** The Constant NetConn. */
        public static volatile boolean NET_CONN = false;

        /** The Constant width. */
        public static final int WIDTH = 300;

        /** The Constant height. */
        public static final int HEIGHT = 0;
    }


    /**
     * The Interface Keywords.
     */
    public static class Keywords {

        /** The loaded. */
        public static final boolean[] LOADED = { false };

        /** The Non stacking list. */
        public static final List<String> NON_STACKING_LIST = new ArrayList<String>();
    }

} // Constant


