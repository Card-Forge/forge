package forge.game.keyword;

/**
 * Keyword actions — verbs that appear in rules text but aren't keyword abilities.
 * Descriptions are based on the MTG comprehensive rules (section 701).
 *
 * <p>Unlike {@link Keyword} (keyword abilities that grant continuous effects or
 * triggered/static abilities), keyword actions are one-shot game actions performed
 * when instructed by a spell or ability.</p>
 *
 * <p>Actions marked {@code basic=true} are fundamental game actions (destroy, exile,
 * sacrifice, etc.) that every player knows — UI code may choose to omit these from
 * tooltips to avoid clutter.</p>
 */
public enum KeywordAction {
    // 701.2 – 701.13: Basic game actions
    ACTIVATE("Activate", "Put an activated ability on the stack.", true),
    ATTACH("Attach", "Move an Aura, Equipment, or Fortification onto another object or player.", true),
    BEHOLD("Behold", "Reveal a card with the required quality from your hand, or choose a permanent you control with that quality.", false),
    CAST("Cast", "Put a spell on the stack.", true),
    COUNTER("Counter", "Remove a spell or ability from the stack. It doesn't resolve and none of its effects happen.", true),
    CREATE("Create", "Put a token onto the battlefield.", true),
    DESTROY("Destroy", "Move a permanent from the battlefield to its owner's graveyard.", true),
    DISCARD("Discard", "Put a card from your hand into its owner's graveyard.", true),
    DOUBLE("Double", "Double a creature's power and toughness, double the number of counters, or double the number of tokens.", true),
    TRIPLE("Triple", "Triple a value — for example, triple a creature's power and toughness.", true),
    EXCHANGE("Exchange", "Swap values or control of objects between two players or permanents.", true),
    EXILE("Exile", "Move an object to the exile zone.", true),

    // 701.14 – 701.17: Combat & graveyard actions
    FIGHT("Fight", "Each creature deals damage equal to its power to the other.", false),
    GOAD("Goad", "A goaded creature attacks each combat if able and attacks a player other than you if able.", false),
    INVESTIGATE("Investigate", "Create a Clue token. It's an artifact with \"{2}, Sacrifice this: Draw a card.\"", false),
    MILL("Mill", "Put the top N cards of your library into your graveyard.", false),

    // 701.18 – 701.21: More basic actions
    PLAY("Play", "Play a land or cast a spell.", true),
    REGENERATE("Regenerate", "Instead of being destroyed, tap this permanent, remove all damage from it, and remove it from combat.", false),
    REVEAL("Reveal", "Show a card to all players.", true),
    SACRIFICE("Sacrifice", "Move a permanent you control from the battlefield to its owner's graveyard.", true),

    // 701.22 – 701.28: Library & transform actions
    SCRY("Scry", "Look at the top N cards of your library, then put any number on the bottom and the rest on top in any order.", false),
    SEARCH("Search", "Look through a zone for a card meeting certain criteria.", true),
    SHUFFLE("Shuffle", "Randomize the order of cards in a library.", true),
    SURVEIL("Surveil", "Look at the top N cards of your library, then put any number into your graveyard and the rest on top in any order.", false),
    TAP_UNTAP("Tap/Untap", "Rotate a permanent sideways to show it's been used, or straighten it to show it's ready.", true),
    TRANSFORM("Transform", "Turn this double-faced card over to its other face.", false),
    CONVERT("Convert", "Turn this double-faced card over to its other face. Unlike transform, convert can change a card from front to back or back to front.", false),

    // 701.29 – 701.30: Opponent manipulation
    FATESEAL("Fateseal", "Look at the top N cards of an opponent's library, then put any number on the bottom and the rest on top in any order.", false),
    CLASH("Clash", "Each clashing player reveals the top card of their library, then puts it on the top or bottom. You win if your card's mana value is higher.", false),

    // 701.31 – 701.33: Supplemental format actions
    PLANESWALK("Planeswalk", "Move to a new plane by turning over the next card in the planar deck.", false),
    SET_IN_MOTION("Set in motion", "Turn a scheme face up and follow its instructions.", false),
    ABANDON("Abandon", "Turn a face-up ongoing scheme face down and put it on the bottom of its owner's scheme deck.", false),

    // 701.34 – 701.36: Counter & token actions
    PROLIFERATE("Proliferate", "Choose any number of permanents and/or players, then give each another counter of each kind already there.", false),
    DETAIN("Detain", "Until your next turn, that permanent can't attack or block and its activated abilities can't be activated.", false),
    POPULATE("Populate", "Create a token that's a copy of a creature token you control.", false),

    // 701.37 – 701.39: Creature enhancement
    MONSTROSITY("Monstrosity", "If this creature isn't monstrous, put N +1/+1 counters on it and it becomes monstrous.", false),
    VOTE("Vote", "Each player votes for one of the given options. The outcome depends on which option gets more votes.", false),
    BOLSTER("Bolster", "Choose a creature you control with the least toughness and put N +1/+1 counters on it.", false),

    // 701.40 – 701.44: Face-down & explore
    MANIFEST("Manifest", "Put the top card of your library onto the battlefield face down as a 2/2 creature. Turn it face up any time for its mana cost if it's a creature card.", false),
    SUPPORT("Support", "Put a +1/+1 counter on each of up to N target creatures.", false),
    MELD("Meld", "Exile two specific cards and combine them into one oversized card on the battlefield.", false),
    EXERT("Exert", "An exerted creature won't untap during your next untap step.", false),
    EXPLORE("Explore", "Reveal the top card of your library. Put it into your hand if it's a land. Otherwise, put a +1/+1 counter on this creature, then you may put the card back or into your graveyard.", false),

    // 701.45 – 701.48: Un-set & learning
    ASSEMBLE("Assemble", "Put a Contraption you own from outside the game onto the battlefield on one of your sprockets.", false),
    ADAPT("Adapt", "If this creature has no +1/+1 counters on it, put N +1/+1 counters on it.", false),
    AMASS("Amass", "Put N +1/+1 counters on an Army you control. If you don't control one, create a 0/0 black Zombie Army creature token first.", false),
    LEARN("Learn", "You may reveal a Lesson card from outside the game and put it into your hand, or discard a card to draw a card.", false),

    // 701.49 – 701.50: Dungeon & connive
    VENTURE("Venture", "Move to the next room of a dungeon. If you're not in one, enter the first room of a dungeon of your choice.", false),
    CONNIVE("Connive", "Draw a card, then discard a card. If you discarded a nonland card, put a +1/+1 counter on this creature.", false),

    // 701.51 – 701.52: Attraction actions
    OPEN_AN_ATTRACTION("Open an Attraction", "Put the top card of your Attraction deck onto the battlefield face up.", false),
    ROLL_TO_VISIT("Roll to visit your Attractions", "Roll a six-sided die. Each Attraction you control whose lit-up numbers include the result is visited.", false),

    // 701.53 – 701.54: Incubate & ring
    INCUBATE("Incubate", "Create an Incubator token with N +1/+1 counters on it. It has \"{2}: Transform this artifact.\" It transforms into a 0/0 Phyrexian artifact creature.", false),
    THE_RING_TEMPTS_YOU("The Ring tempts you", "Choose a creature you control as your Ring-bearer. Your Ring gains its next ability.", false),

    // 701.55 – 701.56: Villainous choice & time travel
    FACE_A_VILLAINOUS_CHOICE("Face a villainous choice", "Choose one of two options presented by an opponent. The chosen option's effects happen.", false),
    TIME_TRAVEL("Time travel", "For each suspended card you own and each permanent you control with a time counter, you may add or remove a time counter.", false),

    // 701.57 – 701.60: Discover, cloak, evidence, suspect
    DISCOVER("Discover", "Exile cards from the top of your library until you exile a nonland card with lower mana value. Cast it without paying its mana cost or put it into your hand.", false),
    CLOAK("Cloak", "Put a card onto the battlefield face down as a 2/2 creature with ward {2}. Turn it face up any time for its mana cost if it's a creature card.", false),
    COLLECT_EVIDENCE("Collect evidence", "Exile cards from your graveyard with total mana value N or greater.", false),
    SUSPECT("Suspect", "A suspected creature has menace and can't block.", false),

    // 701.61 – 701.64: Bloomburrow & beyond
    FORAGE("Forage", "Exile three cards from your graveyard or sacrifice a Food.", false),
    MANIFEST_DREAD("Manifest dread", "Look at the top two cards of your library. Manifest one and put the other into your graveyard.", false),
    ENDURE("Endure", "Choose to either put N +1/+1 counters on this creature or create an N/N white Spirit creature token.", false),
    HARNESS("Harness", "This permanent becomes harnessed. It stays harnessed until it leaves the battlefield.", false),

    // 701.65 – 701.68: Avatar & Lorwyn Eclipsed
    AIRBEND("Airbend", "Exile a permanent. Its owner may cast it for {2} as long as it remains exiled.", false),
    EARTHBEND("Earthbend", "Target land you control becomes a 0/0 creature with haste. Put N +1/+1 counters on it. When it dies or is exiled, return it to the battlefield tapped.", false),
    WATERBEND("Waterbend", "Pay a mana cost, but for each mana symbol you may tap an untapped artifact or creature you control instead of paying that mana.", false),
    BLIGHT("Blight", "Put N -1/-1 counters on a creature you control.", false);

    /** Display name as it appears in rules text. */
    public final String displayName;
    /** Reminder text describing what the action does. */
    public final String reminderText;
    /** True for fundamental game actions (destroy, exile, sacrifice, etc.) that don't need tooltip explanations. */
    public final boolean basic;

    KeywordAction(String displayName, String reminderText, boolean basic) {
        this.displayName = displayName;
        this.reminderText = reminderText;
        this.basic = basic;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
