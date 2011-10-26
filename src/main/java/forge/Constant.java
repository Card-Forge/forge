package forge;

import java.util.ArrayList;
import java.util.List;

import forge.deck.Deck;
import forge.game.GameType;

/**
 * <p>
 * Constant interface.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public interface Constant {
    /** Constant <code>ProgramName="Forge - http://cardforge.org"</code> */
    String ProgramName = "Forge - http://cardforge.org";

    // used to pass information between the GUI screens
    /**
     * The Class Runtime.
     */
    public abstract class Runtime {

        /** The Constant HumanDeck. */
        public static final Deck[] HumanDeck = new Deck[1];

        /** The Constant ComputerDeck. */
        public static final Deck[] ComputerDeck = new Deck[1];

        /** The game type. */
        public static GameType gameType = GameType.Constructed;

        /** The Constant Smooth. */
        public static final boolean[] Smooth = new boolean[1];

        /** The Constant Mill. */
        public static final boolean[] Mill = new boolean[1];

        /** The Constant DevMode. */
        public static final boolean[] DevMode = new boolean[1]; // one for
                                                                // normal mode
                                                                // one for quest
                                                                // mode

        /** The Constant NetConn. */
        public static final boolean[] NetConn = new boolean[1];

        /** The Constant UpldDrft. */
        public static final boolean[] UpldDrft = new boolean[1];

        /** The Constant RndCFoil. */
        public static final boolean[] RndCFoil = new boolean[1];

        /** The Constant width. */
        public static final int[] width = { 300 };

        /** The Constant height. */
        public static final int[] height = new int[1];

        /** The Constant stackSize. */
        public static final int[] stackSize = new int[1];

        /** The Constant stackOffset. */
        public static final int[] stackOffset = new int[1];
    }

    // public interface IO {
    // probably should read this from a file, or set from GUI

    // public static final String deckFile = "all-decks2";
    // public static final String boosterDeckFile = "booster-decks";

    // public static final String imageBaseDir = "pics";

    // public static final ImageIcon upIcon = new ImageIcon("up.gif");
    // public static final ImageIcon downIcon = new ImageIcon("down.gif");
    // public static final ImageIcon leftIcon = new ImageIcon("left.gif");
    // public static final ImageIcon rightIcon = new ImageIcon("right.gif");
    // }

    /**
     * The Interface Ability.
     */
    public interface Ability {

        /** The Triggered. */
        String Triggered = "Triggered";

        /** The Activated. */
        String Activated = "Activated";
    }

    /**
     * The Interface Phase.
     */
    public interface Phase {

        /** The Constant Untap. */
        String Untap = "Untap";

        /** The Constant Upkeep. */
        String Upkeep = "Upkeep";

        /** The Constant Draw. */
        String Draw = "Draw";

        /** The Constant Main1. */
        String Main1 = "Main1";

        /** The Constant Combat_Begin. */
        String Combat_Begin = "BeginCombat";

        /** The Constant Combat_Declare_Attackers. */
        String Combat_Declare_Attackers = "Declare Attackers";

        /** The Constant Combat_Declare_Attackers_InstantAbility. */
        String Combat_Declare_Attackers_InstantAbility = "Declare Attackers - Play Instants and Abilities";

        /** The Constant Combat_Declare_Blockers. */
        String Combat_Declare_Blockers = "Declare Blockers";

        /** The Constant Combat_Declare_Blockers_InstantAbility. */
        String Combat_Declare_Blockers_InstantAbility = "Declare Blockers - Play Instants and Abilities";

        /** The Constant Combat_Damage. */
        String Combat_Damage = "Combat Damage";

        /** The Constant Combat_FirstStrikeDamage. */
        String Combat_FirstStrikeDamage = "First Strike Damage";

        /** The Constant Combat_End. */
        String Combat_End = "EndCombat";

        /** The Constant Main2. */
        String Main2 = "Main2";

        /** The Constant End_Of_Turn. */
        String End_Of_Turn = "End of Turn";

        /** The Constant Cleanup. */
        String Cleanup = "Cleanup";
    }

    /**
     * The Enum Zone.
     */
    public enum Zone {

        /** The Hand. */
        Hand,

        /** The Library. */
        Library,

        /** The Graveyard. */
        Graveyard,

        /** The Battlefield. */
        Battlefield,

        /** The Exile. */
        Exile,

        /** The Command. */
        Command,

        /** The Stack. */
        Stack;

        /**
         * Smart value of.
         * 
         * @param value
         *            the value
         * @return the zone
         */
        public static Zone smartValueOf(final String value) {
            if (value == null) {
                return null;
            }
            if ("All".equals(value)) {
                return null;
            }
            String valToCompate = value.trim();
            for (Zone v : Zone.values()) {
                if (v.name().compareToIgnoreCase(valToCompate) == 0) {
                    return v;
                }
            }
            throw new IllegalArgumentException("No element named " + value + " in enum Zone");
        }

        /**
         * List value of.
         * 
         * @param values
         *            the values
         * @return the list
         */
        public static List<Zone> listValueOf(final String values) {
            List<Zone> result = new ArrayList<Constant.Zone>();
            for (String s : values.split("[, ]+")) {
                result.add(smartValueOf(s));
            }
            return result;
        }
    }

    /**
     * The Interface Color.
     */
    public interface Color {

        /** The Black. */
        String Black = "black";

        /** The Blue. */
        String Blue = "blue";

        /** The Green. */
        String Green = "green";

        /** The Red. */
        String Red = "red";

        /** The White. */
        String White = "white";

        /** The Colorless. */
        String Colorless = "colorless";
        // color order "wubrg"
        /** The Colors. */
        String[] Colors = { White, Blue, Black, Red, Green, Colorless };

        /** The only colors. */
        String[] onlyColors = { White, Blue, Black, Red, Green };

        /** The Snow. */
        String Snow = "snow";

        /** The Mana colors. */
        String[] ManaColors = { White, Blue, Black, Red, Green, Colorless, Snow };

        /** The loaded. */
        boolean[] loaded = { false };
        // public static final Constant_StringHashMap[] LandColor = new
        // Constant_StringHashMap[1];

        /** The Basic lands. */
        String[] BasicLands = { "Plains", "Island", "Swamp", "Mountain", "Forest" };
    }

    /**
     * The Interface Quest.
     */
    public interface Quest {

        /** The fantasy quest. */
        boolean[] fantasyQuest = new boolean[1];

        // public static final Quest_Assignment[] qa = new Quest_Assignment[1];

        /** The human list. */
        CardList[] humanList = new CardList[1];

        /** The computer list. */
        CardList[] computerList = new CardList[1];

        /** The human life. */
        int[] humanLife = new int[1];

        /** The computer life. */
        int[] computerLife = new int[1];

        /** The opp icon name. */
        String[] oppIconName = new String[1];
    }

    /**
     * The Interface CardTypes.
     */
    public interface CardTypes {

        /** The loaded. */
        boolean[] loaded = { false };

        /** The card types. */
        Constant_StringArrayList[] cardTypes = new Constant_StringArrayList[1];

        /** The super types. */
        Constant_StringArrayList[] superTypes = new Constant_StringArrayList[1];

        /** The basic types. */
        Constant_StringArrayList[] basicTypes = new Constant_StringArrayList[1];

        /** The land types. */
        Constant_StringArrayList[] landTypes = new Constant_StringArrayList[1];

        /** The creature types. */
        Constant_StringArrayList[] creatureTypes = new Constant_StringArrayList[1];

        /** The instant types. */
        Constant_StringArrayList[] instantTypes = new Constant_StringArrayList[1];

        /** The sorcery types. */
        Constant_StringArrayList[] sorceryTypes = new Constant_StringArrayList[1];

        /** The enchantment types. */
        Constant_StringArrayList[] enchantmentTypes = new Constant_StringArrayList[1];

        /** The artifact types. */
        Constant_StringArrayList[] artifactTypes = new Constant_StringArrayList[1];

        /** The walker types. */
        Constant_StringArrayList[] walkerTypes = new Constant_StringArrayList[1];
    }

    /**
     * The Interface Keywords.
     */
    public interface Keywords {

        /** The loaded. */
        boolean[] loaded = { false };

        /** The Non stacking list. */
        Constant_StringArrayList[] NonStackingList = new Constant_StringArrayList[1];
    }

} // Constant
