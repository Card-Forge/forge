package forge;


import forge.deck.Deck;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * <p>Constant interface.</p>
 *
 * @author Forge
 * @version $Id$
 */
public interface Constant {
    /** Constant <code>ProgramName="Forge - http://cardforge.org"</code> */
    public static final String ProgramName = "Forge - http://cardforge.org";

    //used to pass information between the GUI screens
    public interface Runtime {
        public static final Deck[] HumanDeck = new Deck[1];
        public static final Deck[] ComputerDeck = new Deck[1];
        public static final String[] GameType = new String[1];
        
        public static final boolean[] Smooth = new boolean[1];
        public static final boolean[] Mill = new boolean[1];
        public static final boolean[] DevMode = new boolean[1];    // one for normal mode one for quest mode
        
        public static final boolean[] NetConn = new boolean[1];
        public static final boolean[] UpldDrft = new boolean[1];
        
        public static final boolean[] RndCFoil = new boolean[1];

        public static final int[] width = new int[1];
        public static final int[] height = new int[1];

        public static final int[] stackSize = new int[1];
        public static final int[] stackOffset = new int[1];
    }

    public interface GameType {
        public static final String Constructed = "constructed";
        public static final String Sealed = "sealed";
        public static final String Draft = "draft";
        public static final List<String> GameTypes = Collections.unmodifiableList(Arrays.asList(Constructed,
                Sealed, Draft));
    }

    //public interface IO {
        // probably should read this from a file, or set from GUI

        //public static final String deckFile = "all-decks2";
        //public static final String boosterDeckFile = "booster-decks";

        //public static final String imageBaseDir = "pics";

        //public static final ImageIcon upIcon = new ImageIcon("up.gif");
        //public static final ImageIcon downIcon = new ImageIcon("down.gif");
        //public static final ImageIcon leftIcon = new ImageIcon("left.gif");
        //public static final ImageIcon rightIcon = new ImageIcon("right.gif");
    //}

    public interface Ability {
        String Triggered = "Triggered";
        String Activated = "Activated";
    }

    public interface Phase {
        public static final String Untap = "Untap";
        public static final String Upkeep = "Upkeep";
        public static final String Draw = "Draw";

        public static final String Main1 = "Main1";

        public static final String Combat_Begin = "BeginCombat";
        public static final String Combat_Declare_Attackers = "Declare Attackers";
        public static final String Combat_Declare_Attackers_InstantAbility = "Declare Attackers - Play Instants and Abilities";
        public static final String Combat_Declare_Blockers = "Declare Blockers";
        public static final String Combat_Declare_Blockers_InstantAbility = "Declare Blockers - Play Instants and Abilities";
        public static final String Combat_Damage = "Combat Damage";
        public static final String Combat_FirstStrikeDamage = "First Strike Damage";
        public static final String Combat_End = "EndCombat";

        public static final String Main2 = "Main2";

        public static final String End_Of_Turn = "End of Turn";
        public static final String Cleanup = "Cleanup";
    }

    public interface Zone {
        String Hand = "Hand";
        String Library = "Library";
        String Graveyard = "Graveyard";
        String Battlefield = "Battlefield";
        String Exile = "Exile";
        String Command = "Command";
        String Stack = "Stack";
    }

    public interface Color {
        String Black = "black";
        String Blue = "blue";
        String Green = "green";
        String Red = "red";
        String White = "white";

        String Colorless = "colorless";
        //color order "wubrg"
        String[] Colors = {White, Blue, Black, Red, Green, Colorless};
        String[] onlyColors = {White, Blue, Black, Red, Green};

        String Snow = "snow";
        String[] ManaColors = {White, Blue, Black, Red, Green, Colorless, Snow};
        
        boolean[] loaded = {false};
        //public static final Constant_StringHashMap[] LandColor = new Constant_StringHashMap[1];

        String[] BasicLands = {"Plains", "Island", "Swamp", "Mountain", "Forest"};
    }

    public interface Rarity {
        String Common = "Common";
        String Uncommon = "Uncommon";
        String Rare = "Rare";
        String Mythic = "Mythic";
        String Land = "Land";
    }

    public interface Quest {
        boolean[] fantasyQuest = new boolean[1];

        //public static final Quest_Assignment[] qa = new Quest_Assignment[1];

        CardList[] humanList = new CardList[1];
        CardList[] computerList = new CardList[1];

        int[] humanLife = new int[1];
        int[] computerLife = new int[1];

        String[] oppIconName = new String[1];
    }

    public interface CardTypes {
    	boolean[] loaded = {false}; 
    	Constant_StringArrayList[] cardTypes = new Constant_StringArrayList[1];
    	Constant_StringArrayList[] superTypes = new Constant_StringArrayList[1];
    	Constant_StringArrayList[] basicTypes = new Constant_StringArrayList[1];
    	Constant_StringArrayList[] landTypes = new Constant_StringArrayList[1];
    	Constant_StringArrayList[] creatureTypes = new Constant_StringArrayList[1];
    	Constant_StringArrayList[] instantTypes = new Constant_StringArrayList[1];
    	Constant_StringArrayList[] sorceryTypes = new Constant_StringArrayList[1];
    	Constant_StringArrayList[] enchantmentTypes = new Constant_StringArrayList[1];
    	Constant_StringArrayList[] artifactTypes = new Constant_StringArrayList[1];
    	Constant_StringArrayList[] walkerTypes = new Constant_StringArrayList[1];
    }

    public interface Keywords {
    	boolean[] loaded = {false};
    	Constant_StringArrayList[] NonStackingList = new Constant_StringArrayList[1];
    }


} //Constant
