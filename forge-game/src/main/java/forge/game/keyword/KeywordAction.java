package forge.game.keyword;

import forge.util.Localizer;

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
 *
 * <p>Display names and reminder text are stored in {@code en-US.properties} under
 * keys derived from the enum name (e.g. {@code lblKwActionActivate},
 * {@code lblKwActionActivateReminder}).</p>
 */
public enum KeywordAction {
    // 701.2 – 701.13: Basic game actions
    ACTIVATE(true),
    ATTACH(true),
    BEHOLD(false),
    CAST(true),
    COUNTER(true),
    CREATE(true),
    DESTROY(true),
    DISCARD(true),
    DOUBLE(true),
    TRIPLE(true),
    EXCHANGE(true),
    EXILE(true),

    // 701.14 – 701.17: Combat & graveyard actions
    FIGHT(false),
    GOAD(false),
    INVESTIGATE(false),
    MILL(false),

    // 701.18 – 701.21: More basic actions
    PLAY(true),
    REGENERATE(false),
    REVEAL(true),
    SACRIFICE(true),

    // 701.22 – 701.28: Library & transform actions
    SCRY(false),
    SEARCH(true),
    SHUFFLE(true),
    SURVEIL(false),
    TAP_UNTAP(true),
    TRANSFORM(false),
    CONVERT(false),

    // 701.29 – 701.30: Opponent manipulation
    FATESEAL(false),
    CLASH(false),

    // 701.31 – 701.33: Supplemental format actions
    PLANESWALK(false),
    SET_IN_MOTION(false),
    ABANDON(false),

    // 701.34 – 701.36: Counter & token actions
    PROLIFERATE(false),
    DETAIN(false),
    POPULATE(false),

    // 701.37 – 701.39: Creature enhancement
    MONSTROSITY(false),
    VOTE(false),
    BOLSTER(false),

    // 701.40 – 701.44: Face-down & explore
    MANIFEST(false),
    SUPPORT(false),
    MELD(false),
    EXERT(false),
    EXPLORE(false),

    // 701.45 – 701.48: Un-set & learning
    ASSEMBLE(false),
    ADAPT(false),
    AMASS(false),
    LEARN(false),

    // 701.49 – 701.50: Dungeon & connive
    VENTURE(false),
    CONNIVE(false),

    // 701.51 – 701.52: Attraction actions
    OPEN_AN_ATTRACTION(false),
    ROLL_TO_VISIT(false),

    // 701.53 – 701.54: Incubate & ring
    INCUBATE(false),
    THE_RING_TEMPTS_YOU(false),

    // 701.55 – 701.56: Villainous choice & time travel
    FACE_A_VILLAINOUS_CHOICE(false),
    TIME_TRAVEL(false),

    // 701.57 – 701.60: Discover, cloak, evidence, suspect
    DISCOVER(false),
    CLOAK(false),
    COLLECT_EVIDENCE(false),
    SUSPECT(false),

    // 701.61 – 701.64: Bloomburrow & beyond
    FORAGE(false),
    MANIFEST_DREAD(false),
    ENDURE(false),
    HARNESS(false),

    // 701.65 – 701.68: Avatar & Lorwyn Eclipsed
    AIRBEND(false),
    EARTHBEND(false),
    WATERBEND(false),
    BLIGHT(false);

    /** True for fundamental game actions (destroy, exile, sacrifice, etc.) that don't need tooltip explanations. */
    public final boolean basic;
    /** Translation key prefix derived from enum name, e.g. "lblKwActionActivate". */
    private final String translationKey;

    KeywordAction(boolean basic) {
        this.basic = basic;
        this.translationKey = "lblKwAction" + toCamelCase(name());
    }

    private static String toCamelCase(String enumName) {
        StringBuilder sb = new StringBuilder();
        boolean capitalize = true;
        for (char c : enumName.toCharArray()) {
            if (c == '_') {
                capitalize = true;
            } else {
                sb.append(capitalize ? c : Character.toLowerCase(c));
                capitalize = false;
            }
        }
        return sb.toString();
    }

    /** Returns the localized display name. */
    public String getDisplayName() {
        return Localizer.getInstance().getMessage(translationKey);
    }

    /** Returns the localized reminder text. */
    public String getReminderText() {
        return Localizer.getInstance().getMessage(translationKey + "Reminder");
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
