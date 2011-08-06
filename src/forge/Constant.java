
package forge;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.ImageIcon;


public interface Constant {
    public static final String ProgramName = "Forge - http://cardforge.org";
    
    //used to pass information between the GUI screens
    public interface Runtime {
        public static final Deck[]    HumanDeck    = new Deck[1];
        public static final Deck[]    ComputerDeck = new Deck[1];
        public static final String[]  GameType     = new String[1];
        public static final WinLose   WinLose      = new WinLose();
        public static final boolean[] Smooth       = new boolean[1];
        public static final boolean[] Mill		   = new boolean[1];
        public static final boolean[] DevMode	   = new boolean[1];	// one for normal mode one for quest mode
        
        public static final int[]     width        = new int[1];
        public static final int[]     height       = new int[1];
        
        public static final int[]	  stackSize    = new int[1];
        public static final int[]	  stackOffset  = new int[1];
    }
    
    public interface GameType {
        public static final String       Constructed = "constructed";
        public static final String       Sealed      = "sealed";
        public static final String       Draft       = "draft";
        public static final List<String> GameTypes   = Collections.unmodifiableList(Arrays.asList(Constructed,
                                                             Sealed, Draft));
    }
    
    public interface IO {
        //TODO: probably should read this from a file, or set from GUI
        
        public static final String    deckFile        = "all-decks2";
        public static final String    boosterDeckFile = "booster-decks";
        
        public static final String    imageBaseDir    = "pics";
        
        public static final ImageIcon upIcon          = new ImageIcon("up.gif");
        public static final ImageIcon downIcon        = new ImageIcon("down.gif");
        public static final ImageIcon leftIcon        = new ImageIcon("left.gif");
        public static final ImageIcon rightIcon       = new ImageIcon("right.gif");
    }
    
    public interface Ability {
        public static final String Triggered = "Triggered";
        public static final String Activated = "Activated";
    }
    
    public interface Phase {
        public static final String Untap                                          = "Untap";
        public static final String Upkeep                                         = "Upkeep";
        public static final String Draw                                           = "Draw";
        
        public static final String Main1                                          = "Main1";

        public static final String Combat_Begin		 					  		  = "BeginCombat";
        public static final String Combat_Declare_Attackers                       = "Declare Attackers";
        public static final String Combat_Declare_Attackers_InstantAbility        = "Declare Attackers - Play Instants and Abilities";
        public static final String Combat_Declare_Blockers                        = "Declare Blockers";
        public static final String Combat_Declare_Blockers_InstantAbility         = "Declare Blockers - Play Instants and Abilities";
        public static final String Combat_Damage                                  = "Combat Damage";
        public static final String Combat_FirstStrikeDamage                       = "First Strike Damage";
        public static final String Combat_End  								 	  = "EndCombat";
        
        public static final String Main2                                          = "Main2";

        public static final String End_Of_Turn                                    = "End of Turn";
        public static final String Cleanup                                        = "Cleanup";
    }
    
    public interface Zone {
        public static final String Hand              = "Hand";
        public static final String Library           = "Library";
        public static final String Graveyard         = "Graveyard";
        public static final String Battlefield       = "Battlefield";
        public static final String Exile 			 = "Exile";
        public static final String Command           = "Command";
        public static final String Stack             = "Stack";
    }

    public interface Color {
        public static final String Black        = "black";
        public static final String Blue         = "blue";
        public static final String Green        = "green";
        public static final String Red          = "red";
        public static final String White        = "white";
        
        public static final String Colorless    = "colorless";
        //color order "wubrg"
        public static final String Colors[]     = {White, Blue, Black, Red, Green, Colorless};
        public static final String ColorsOnly[] = {White, Blue, Black, Red, Green};
        
        
        public static final String onlyColors[] = {White, Blue, Black, Red, Green};
        
        public static final String Snow         = "snow";
        public static final String ManaColors[] = {White, Blue, Black, Red, Green, Colorless, Snow};
    }
    
    public interface Quest {
    	public static final boolean[] fantasyQuest = new boolean[1];
    	
    	//public static final Quest_Assignment[] qa = new Quest_Assignment[1];
    	
    	public static final CardList[] humanList = new CardList[1];
    	public static final CardList[] computerList = new CardList[1];
        
    	public static final int[] humanLife = new int[1];
    	public static final int[] computerLife = new int[1];
    	
    	public static final String[]  oppIconName     = new String[1];
    }
    
    
}//Constant



