package forge;


import forge.deck.Deck;
import forge.quest.data.QuestMatchState;

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
        public static final QuestMatchState matchState = new QuestMatchState();
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
        public static final String Triggered = "Triggered";
        public static final String Activated = "Activated";
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
        public static final String Hand = "Hand";
        public static final String Library = "Library";
        public static final String Graveyard = "Graveyard";
        public static final String Battlefield = "Battlefield";
        public static final String Exile = "Exile";
        public static final String Command = "Command";
        public static final String Stack = "Stack";
    }

    public interface Color {
        public static final String Black = "black";
        public static final String Blue = "blue";
        public static final String Green = "green";
        public static final String Red = "red";
        public static final String White = "white";

        public static final String Colorless = "colorless";
        //color order "wubrg"
        public static final String Colors[] = {White, Blue, Black, Red, Green, Colorless};
        public static final String onlyColors[] = {White, Blue, Black, Red, Green};

        public static final String Snow = "snow";
        public static final String ManaColors[] = {White, Blue, Black, Red, Green, Colorless, Snow};
        
        public static final boolean loaded[] = {false};
        //public static final Constant_StringHashMap[] LandColor = new Constant_StringHashMap[1];

        public static final String BasicLands[] = {"Plains", "Island", "Swamp", "Mountain", "Forest"};
    }

    public interface Rarity {
        public static final String Common = "Common";
        public static final String Uncommon = "Uncommon";
        public static final String Rare = "Rare";
        public static final String Mythic = "Mythic";
        public static final String Land = "Land";
    }

    public interface Quest {
        public static final boolean[] fantasyQuest = new boolean[1];

        //public static final Quest_Assignment[] qa = new Quest_Assignment[1];

        public static final CardList[] humanList = new CardList[1];
        public static final CardList[] computerList = new CardList[1];

        public static final int[] humanLife = new int[1];
        public static final int[] computerLife = new int[1];

        public static final String[] oppIconName = new String[1];
    }
    
    public interface CardTypes {
    	public static final boolean loaded[] = {false}; 
    	public static final Constant_StringArrayList cardTypes[] = new Constant_StringArrayList[1];
    	public static final Constant_StringArrayList superTypes[] = new Constant_StringArrayList[1];
    	public static final Constant_StringArrayList basicTypes[] = new Constant_StringArrayList[1];
    	public static final Constant_StringArrayList landTypes[] = new Constant_StringArrayList[1];
    	public static final Constant_StringArrayList creatureTypes[] = new Constant_StringArrayList[1];
    	public static final Constant_StringArrayList instantTypes[] = new Constant_StringArrayList[1];
    	public static final Constant_StringArrayList sorceryTypes[] = new Constant_StringArrayList[1];
    	public static final Constant_StringArrayList enchantmentTypes[] = new Constant_StringArrayList[1];
    	public static final Constant_StringArrayList artifactTypes[] = new Constant_StringArrayList[1];
    	public static final Constant_StringArrayList walkerTypes[] = new Constant_StringArrayList[1];
    }
    
    public interface Keywords {
    	public static final boolean loaded[] = {false};
    	public static final Constant_StringArrayList NonStackingList[] = new Constant_StringArrayList[1];
    }
}//Constant


