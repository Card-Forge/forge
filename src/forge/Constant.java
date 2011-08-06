
package forge;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.ImageIcon;


public interface Constant {
    public static final String ProgramName = "Forge - 04/2010 - http://mtgrares.blogspot.com";
    
    //used to pass information between the GUI screens
    public interface Runtime {
        public static final Deck[]    HumanDeck    = new Deck[1];
        public static final Deck[]    ComputerDeck = new Deck[1];
        public static final String[]  GameType     = new String[1];
        public static final WinLose   WinLose      = new WinLose();
        public static final boolean[] Smooth       = new boolean[1];
        public static final boolean[] Mill		   = new boolean[1];
        
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
//    public static final String baseDir = "c:\\Tim\\game testing\\";
        
        public static final String    deckFile        = "all-decks2";
        public static final String    boosterDeckFile = "booster-decks";
        
        public static final String    imageBaseDir    = "pics";
        public static final String    cardFile        = "cards.txt";
        
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
        //public static final String Begin_Combat = "Beginning of Combat";
        public static final String Combat_Before_Declare_Attackers_InstantAbility = "Before Attack Phase - Play Instants and Abilities";
        public static final String Combat_Declare_Attackers_InstantAbility        = "Declare Attackers - Play Instants and Abilities";
        public static final String Combat_Declare_Attackers                       = "Declare Attackers";
        public static final String Combat_Declare_Blockers                        = "Declare Blockers";
        public static final String Combat_After_Declare_Blockers				  = "After Declare Blockers Phase";
        public static final String Combat_Declare_Blockers_InstantAbility         = "Declare Blockers - Play Instants and Abilities";
        public static final String Combat_Damage                                  = "Combat Damage";
        public static final String Combat_FirstStrikeDamage                       = "First Strike Damage";
        public static final String End_Of_Combat  								  = "End of Combat";
        public static final String Main2                                          = "Main2";
        public static final String At_End_Of_Turn                                 = "At End of Turn";
        public static final String End_Of_Turn                                    = "End of Turn";
        public static final String Until_End_Of_Turn                              = "Until End of Turn";
        public static final String Cleanup                                        = "Cleanup";
    }
    
    public interface Zone {
        public static final String Hand              = "Hand";
        public static final String Library           = "Library";
        public static final String Graveyard         = "Graveyard";
        public static final String Play              = "Play";
        public static final String Removed_From_Play = "Removed from play";
        //public static final String Stack             = "Stack";
    }
    
    /*
    public interface Player {
        public static final String Human    = "Human";
        public static final String Computer = "Computer";
    }
    */
    
    /*
    public interface CardType
    {
    public static final String Artifact	      = "Artifact";
    public static final String Creature	   = "Creature";
    public static final String Enchantment   = "Enchantment";
    public static final String Aura               = "Aura";
    public static final String Instant            = "Instant";
    public static final String Land               = "Land";
    public static final String Legendary       = "Legendary";
    public static final String Sorcery           = "Sorcery";
    public static final String Basic               = "Basic";
    }
     */
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

/*
  public interface Keyword
  {
    public static final String Swampwalk      = "Swampwalk";
    public static final String Plainswalk     = "Plainswalk";
    public static final String Forestwalk     = "Forestwalk";
    public static final String Islandwalk     = "Islandwalk";
    public static final String Mountainwalk   = "Mountainwalk";

    public static final String Flying         = "Flying";
    public static final String Fear           = "Fear";
    public static final String Vigilance      = "Vigilance";


    public static final String Double_Strike  = "Double Strike";
    public static final String First_Strike   = "First Strike";
    public static final String Haste          = "Haste";
    public static final String Indestructable = "Indestructable";
    public static final String Landwalk       = "Landwalk";
    public static final String Protection     = "Protection";
    public static final String Regeneration   = "Regeneration";
    public static final String Trample        = "Trample";
  }

// mirrodin block abilities
    public static final String Affinity = "affinity";
    public static final String Modular  = "modular";
    public static final String Sunburst = "sunburst";

//onslaught block
    public static final String Cycling      = "cycling";
    public static final String Land_Cycling = "land cycling";
    public static final String Morph        = "morph";


/*
  public static final int Landwalk_Forest = 9;
  public static final int Landwalk_Island = 10;
  public static final int Landwalk_Plain = 11;
  public static final int Landwalk__Swamp = 12;
  public static final int Landwalk_Mountain = 13;

  public static final int Protection_Red = 14;
  public static final int Protection_Black = 15;
  public static final int Protection_White = 16;
  public static final int Protection_Blue = 17;
  public static final int Protection_Green = 18;
  public static final int Protection_Artifact = 19;

//taken from Mirror Golem
  public static final int Protection_Creature = 20;
  public static final int Protection_Enchantment = 21;
  public static final int Protection_Instant = 22;
  public static final int Protection_Land = 23;
  public static final int Protection_Sorcery = 24;


  public static final int Affinity_Artifact = 29;
  public static final int Affinity_Forest = 30;
  public static final int Affinity_Plain = 31;
  public static final int Affinity_Swamp = 32;
  public static final int Affinity_Island = 33;
  public static final int Affinity_Moutain = 34;
  }
 */


