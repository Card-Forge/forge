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
    /** Constant <code>ProgramName="Forge - http://cardforge.org"</code>. */
    String PROGRAM_NAME = "Forge - http://cardforge.org";

    // used to pass information between the GUI screens
    /**
     * The Class Runtime.
     */
    public abstract class Runtime {

        /** The Constant HumanDeck. */
        public static final Deck[] HUMAN_DECK = new Deck[1];

        /** The Constant ComputerDeck. */
        public static final Deck[] COMPUTER_DECK = new Deck[1];

        /** The game type. */
        private static GameType gameType = GameType.Constructed;

        /** The Constant Smooth. */
        public static final boolean[] SMOOTH = new boolean[1];

        /** The Constant Mill. */
        public static final boolean[] MILL = new boolean[1];

        /** The Constant DevMode. */
        public static final boolean[] DEV_MODE = new boolean[1]; // one for
                                                                // normal mode
                                                                // one for quest
                                                                // mode

        /** The Constant NetConn. */
        public static final boolean[] NET_CONN = new boolean[1];

        /** The Constant UpldDrft. */
        public static final boolean[] UPLOAD_DRAFT = new boolean[1];

        /** The Constant RndCFoil. */
        public static final boolean[] RANDOM_FOIL = new boolean[1];

        /** The Constant width. */
        public static final int[] WIDTH = { 300 };

        /** The Constant height. */
        public static final int[] HEIGHT = new int[1];

        /** The Constant stackSize. */
        public static final int[] STACK_SIZE = new int[1];

        /** The Constant stackOffset. */
        public static final int[] STACK_OFFSET = new int[1];

        /**
         * @return the gameType
         */
        public static GameType getGameType() {
            return gameType;
        }

        /**
         * @param gameType the gameType to set
         */
        public static void setGameType(GameType gameType) {
            Runtime.gameType = gameType; // TODO: Add 0 to parameter's name.
        }
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
        String TRIGGERED = "Triggered";

        /** The Activated. */
        String ACTIVATED = "Activated";
    }

    /**
     * The Interface Phase.
     */
    public interface Phase {

        /** The Constant Untap. */
        String UNTAP = "Untap";

        /** The Constant Upkeep. */
        String UPKEEP = "Upkeep";

        /** The Constant Draw. */
        String DRAW = "Draw";

        /** The Constant Main1. */
        String MAIN1 = "Main1";

        /** The Constant Combat_Begin. */
        String COMBAT_BEGIN = "BeginCombat";

        /** The Constant Combat_Declare_Attackers. */
        String COMBAT_DECLARE_ATTACKERS = "Declare Attackers";

        /** The Constant Combat_Declare_Attackers_InstantAbility. */
        String COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY = "Declare Attackers - Play Instants and Abilities";

        /** The Constant Combat_Declare_Blockers. */
        String COMBAT_DECLARE_BLOCKERS = "Declare Blockers";

        /** The Constant Combat_Declare_Blockers_InstantAbility. */
        String COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY = "Declare Blockers - Play Instants and Abilities";

        /** The Constant Combat_Damage. */
        String COMBAT_DAMAGE = "Combat Damage";

        /** The Constant Combat_FirstStrikeDamage. */
        String COMBAT_FIRST_STRIKE_DAMAGE = "First Strike Damage";

        /** The Constant Combat_End. */
        String COMBAT_END = "EndCombat";

        /** The Constant Main2. */
        String MAIN2 = "Main2";

        /** The Constant End_Of_Turn. */
        String END_OF_TURN = "End of Turn";

        /** The Constant Cleanup. */
        String CLEANUP = "Cleanup";
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
        String BLACK = "black";

        /** The Blue. */
        String BLUE = "blue";

        /** The Green. */
        String GREEN = "green";

        /** The Red. */
        String RED = "red";

        /** The White. */
        String WHITE = "white";

        /** The Colorless. */
        String COLORLESS = "colorless";
        // color order "wubrg"
        /** The Colors. */
        String[] COLORS = { WHITE, BLUE, BLACK, RED, GREEN, COLORLESS };

        /** The only colors. */
        String[] ONLY_COLORS = { WHITE, BLUE, BLACK, RED, GREEN };

        /** The Snow. */
        String SNOW = "snow";

        /** The Mana colors. */
        String[] MANA_COLORS = { WHITE, BLUE, BLACK, RED, GREEN, COLORLESS, SNOW };

        /** The loaded. */
        boolean[] LOADED = { false };
        // public static final Constant_StringHashMap[] LandColor = new
        // Constant_StringHashMap[1];

        /** The Basic lands. */
        String[] BASIC_LANDS = { "Plains", "Island", "Swamp", "Mountain", "Forest" };
    }

    /**
     * The Interface Quest.
     */
    public interface Quest {

        /** The fantasy quest. */
        boolean[] FANTASY_QUEST = new boolean[1];

        // public static final Quest_Assignment[] qa = new Quest_Assignment[1];

        /** The human list. */
        CardList[] HUMAN_LIST = new CardList[1];

        /** The computer list. */
        CardList[] COMPUTER_LIST = new CardList[1];

        /** The human life. */
        int[] HUMAN_LIFE = new int[1];

        /** The computer life. */
        int[] COMPUTER_LIFE = new int[1];

        /** The opp icon name. */
        String[] OPP_ICON_NAME = new String[1];
    }

    /**
     * The Interface CardTypes.
     */
    public interface CardTypes {

        /** The loaded. */
        boolean[] LOADED = { false };

        /** The card types. */
        ConstantStringArrayList[] CARD_TYPES = new ConstantStringArrayList[1];

        /** The super types. */
        ConstantStringArrayList[] SUPER_TYPES = new ConstantStringArrayList[1];

        /** The basic types. */
        ConstantStringArrayList[] BASIC_TYPES = new ConstantStringArrayList[1];

        /** The land types. */
        ConstantStringArrayList[] LAND_TYPES = new ConstantStringArrayList[1];

        /** The creature types. */
        ConstantStringArrayList[] CREATURE_TYPES = new ConstantStringArrayList[1];

        /** The instant types. */
        ConstantStringArrayList[] INSTANT_TYPES = new ConstantStringArrayList[1];

        /** The sorcery types. */
        ConstantStringArrayList[] SORCERY_TYPES = new ConstantStringArrayList[1];

        /** The enchantment types. */
        ConstantStringArrayList[] ENCHANTMENT_TYPES = new ConstantStringArrayList[1];

        /** The artifact types. */
        ConstantStringArrayList[] ARTIFACT_TYPES = new ConstantStringArrayList[1];

        /** The walker types. */
        ConstantStringArrayList[] WALKER_TYPES = new ConstantStringArrayList[1];
    }

    /**
     * The Interface Keywords.
     */
    public interface Keywords {

        /** The loaded. */
        boolean[] LOADED = { false };

        /** The Non stacking list. */
        ConstantStringArrayList[] NON_STACKING_LIST = new ConstantStringArrayList[1];
    }

} // Constant
