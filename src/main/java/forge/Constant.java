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

import forge.properties.ForgeProps;
import forge.properties.NewConstants;

/**
 * <p>
 * Constant interface.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class Constant {
    /** Constant <code>ProgramName="Forge - http://cardforge.org"</code>. */
    public static final String PROGRAM_NAME = "Forge - " + ForgeProps.getProperty(NewConstants.CARDFORGE_URL);

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
     * The Interface Color.
     */
    public static class Color {

        /** The Black. */
        public static final String BLACK = "black";

        /** The Blue. */
        public static final String BLUE = "blue";

        /** The Green. */
        public static final String GREEN = "green";

        /** The Red. */
        public static final String RED = "red";

        /** The White. */
        public static final String WHITE = "white";

        /** The Colorless. */
        public static final String COLORLESS = "colorless";
        // color order "wubrg"
        /** The Colors. */
        public static final String[] COLORS = { Color.WHITE, Color.BLUE, Color.BLACK, Color.RED, Color.GREEN, Color.COLORLESS };

        /** The only colors. */
        public static final String[] ONLY_COLORS = { Color.WHITE, Color.BLUE, Color.BLACK, Color.RED, Color.GREEN };

        /** The Snow. */
        public static final String SNOW = "snow";

        /** The Mana colors. */
        public static final String[] MANA_COLORS = { Color.WHITE, Color.BLUE, Color.BLACK, Color.RED, Color.GREEN,
                Color.COLORLESS, Color.SNOW };

        /** The Basic lands. */
        public static final String[] BASIC_LANDS = { "Plains", "Island", "Swamp", "Mountain", "Forest" };
    }

    /**
     * The Interface CardTypes.
     */
    public static class CardTypes {

        /** The loaded. */
        public static final boolean[] LOADED = { false };

        /** The card types. */
        public static final List<String> CARD_TYPES = new ArrayList<String>();

        /** The super types. */
        public static final List<String> SUPER_TYPES = new ArrayList<String>();

        /** The basic types. */
        public static final List<String> BASIC_TYPES = new ArrayList<String>();

        /** The land types. */
        public static final List<String> LAND_TYPES = new ArrayList<String>();

        /** The creature types. */
        public static final List<String> CREATURE_TYPES = new ArrayList<String>();

        /** The instant types. */
        public static final List<String> INSTANT_TYPES = new ArrayList<String>();

        /** The sorcery types. */
        public static final List<String> SORCERY_TYPES = new ArrayList<String>();

        /** The enchantment types. */
        public static final List<String> ENCHANTMENT_TYPES = new ArrayList<String>();

        /** The artifact types. */
        public static final List<String> ARTIFACT_TYPES = new ArrayList<String>();

        /** The walker types. */
        public static final List<String> WALKER_TYPES = new ArrayList<String>();
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


