package forge.game.keyword;

import forge.card.CardSplitType;
import forge.item.PaperCard;
import forge.util.Localizer;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public enum Keyword {
    UNDEFINED("", SimpleKeyword.class, false),
    ABSORB("Absorb", KeywordWithAmount.class, false),
    AFFINITY("Affinity", Affinity.class, false),
    AFFLICT("Afflict", KeywordWithAmount.class, false),
    AFTERLIFE("Afterlife", KeywordWithAmount.class, false),
    AFTERMATH("Aftermath", SimpleKeyword.class, false),
    AMPLIFY("Amplify", Amplify.class, false),
    ANNIHILATOR("Annihilator", KeywordWithAmount.class, false),
    ASCEND("Ascend", SimpleKeyword.class, true),
    ASSIST("Assist", SimpleKeyword.class, true),
    AURA_SWAP("Aura swap", KeywordWithCost.class, false),
    AWAKEN("Awaken", KeywordWithCostAndAmount.class, false),
    BACKUP("Backup", KeywordWithAmount.class, false),
    BANDING("Banding", SimpleKeyword.class, true),
    BANDSWITH("Bands with other", KeywordWithType.class, false),
    BARGAIN("Bargain", SimpleKeyword.class, false),
    BATTLE_CRY("Battle cry", SimpleKeyword.class, false),
    BESTOW("Bestow", KeywordWithCost.class, false),
    BLITZ("Blitz", KeywordWithCost.class, false),
    BLOODTHIRST("Bloodthirst", KeywordWithAmount.class, false),
    BUSHIDO("Bushido", KeywordWithAmount.class, false),
    BUYBACK("Buyback", KeywordWithCost.class, false),
    CASCADE("Cascade", SimpleKeyword.class, false),
    CASUALTY("Casualty", KeywordWithAmount.class, false),
    CHAMPION("Champion", KeywordWithType.class, false),
    CHANGELING("Changeling", SimpleKeyword.class, true),
    CHOOSE_A_BACKGROUND("Choose a Background", Partner.class, true),
    CIPHER("Cipher", SimpleKeyword.class, true),
    COMPANION("Companion", Companion.class, true),
    COMPLEATED("Compleated", Compleated.class, true),
    CONSPIRE("Conspire", SimpleKeyword.class, false),
    CONVOKE("Convoke", SimpleKeyword.class, true),
    CRAFT("Craft", Craft.class, false),
    CREW("Crew", KeywordWithAmount.class, false),
    CUMULATIVE_UPKEEP("Cumulative upkeep", KeywordWithCost.class, false),
    CYCLING("Cycling", KeywordWithCost.class, false), //Typecycling reminder text handled by Cycling class
    DASH("Dash", KeywordWithCost.class, false),
    DAYBOUND("Daybound", SimpleKeyword.class, true),
    DEATHTOUCH("Deathtouch", SimpleKeyword.class, true),
    DECAYED("Decayed", SimpleKeyword.class, true),
    DEFENDER("Defender", SimpleKeyword.class, true),
    DELVE("Delve", SimpleKeyword.class, true),
    DEMONSTRATE("Demonstrate", SimpleKeyword.class, false),
    DETHRONE("Dethrone", SimpleKeyword.class, false),
    DEVOUR("Devour", Devour.class, false),
    DEVOID("Devoid", SimpleKeyword.class, true),
    DISGUISE("Disguise", KeywordWithCost.class, false),
    DISTURB("Disturb", KeywordWithCost.class, false),
    DOCTORS_COMPANION("Doctor's companion", Partner.class, true),
    DOUBLE_AGENDA("Double agenda", SimpleKeyword.class, false),
    DOUBLE_STRIKE("Double Strike", SimpleKeyword.class, true),
    DOUBLE_TEAM("Double team", SimpleKeyword.class, false),
    DREDGE("Dredge", KeywordWithAmount.class, false),
    ECHO("Echo", KeywordWithCost.class, false),
    EMBALM("Embalm", KeywordWithCost.class, false),
    EMERGE("Emerge", Emerge.class, false),
    ENCHANT("Enchant", KeywordWithType.class, false),
    ENCORE("Encore", KeywordWithCost.class, false),
    ENLIST("Enlist", SimpleKeyword.class, false),
    ENTWINE("Entwine", KeywordWithCost.class, true),
    EPIC("Epic", SimpleKeyword.class, true),
    EQUIP("Equip", Equip.class, false),
    ESCAPE("Escape", KeywordWithCost.class, false),
    ESCALATE("Escalate", KeywordWithCost.class, true),
    ETERNALIZE("Eternalize", KeywordWithCost.class, false),
    EVOKE("Evoke", KeywordWithCost.class, false),
    EVOLVE("Evolve", SimpleKeyword.class, false),
    EXALTED("Exalted", SimpleKeyword.class, false),
    EXPLOIT("Exploit", SimpleKeyword.class, false),
    EXTORT("Extort", SimpleKeyword.class, false),
    FABRICATE("Fabricate", KeywordWithAmount.class, false),
    FADING("Fading", KeywordWithAmount.class, false),
    FEAR("Fear", SimpleKeyword.class, true),
    FIREBENDING("Firebending", Firebending.class, false),
    FIRST_STRIKE("First Strike", SimpleKeyword.class, true),
    FLANKING("Flanking", SimpleKeyword.class, false),
    FLASH("Flash", SimpleKeyword.class, true),
    FLASHBACK("Flashback", KeywordWithCost.class, false),
    FLYING("Flying", SimpleKeyword.class, true),
    FOR_MIRRODIN("For Mirrodin", SimpleKeyword.class, false),
    FORETELL("Foretell", KeywordWithCost.class, false),
    FORTIFY("Fortify", KeywordWithCost.class, false),
    FREERUNNING("Freerunning", KeywordWithCost.class, false),
    FRENZY("Frenzy", KeywordWithAmount.class, false),
    FUSE("Fuse", SimpleKeyword.class, true),
    GIFT("Gift", SimpleKeyword.class, true),
    GRAFT("Graft", KeywordWithAmount.class, false),
    GRAVESTORM("Gravestorm", SimpleKeyword.class, false),
    HARMONIZE("Harmonize", KeywordWithCost.class, false),
    HASTE("Haste", SimpleKeyword.class, true),
    HAUNT("Haunt", SimpleKeyword.class, false),
    HEXPROOF("Hexproof", Hexproof.class, true),
    HIDEAWAY("Hideaway", KeywordWithAmount.class, false),
    HIDDEN_AGENDA("Hidden agenda", SimpleKeyword.class, false),
    HORSEMANSHIP("Horsemanship", SimpleKeyword.class, true),
    IMPENDING("Impending", KeywordWithCostAndAmount.class, false),
    IMPROVISE("Improvise", SimpleKeyword.class, true),
    INCREMENT("Increment", SimpleKeyword.class, false),
    INDESTRUCTIBLE("Indestructible", SimpleKeyword.class, true),
    INFECT("Infect", SimpleKeyword.class, true),
    INGEST("Ingest", SimpleKeyword.class, false),
    INTIMIDATE("Intimidate", SimpleKeyword.class, true),
    KICKER("Kicker", Kicker.class, false),
    JOB_SELECT("Job select", SimpleKeyword.class, false),
    JUMP_START("Jump-start", SimpleKeyword.class, false),
    LANDWALK("Landwalk", Landwalk.class, true),
    LEVEL_UP("Level up", KeywordWithCost.class, false),
    LIFELINK("Lifelink", SimpleKeyword.class, true),
    LIVING_METAL("Living metal", SimpleKeyword.class, true),
    LIVING_WEAPON("Living Weapon", SimpleKeyword.class, true),
    MADNESS("Madness", KeywordWithCost.class, false),
    MAYHEM("Mayhem", Mayhem.class, false),
    MELEE("Melee", SimpleKeyword.class, false),
    MENTOR("Mentor", SimpleKeyword.class, false),
    MENACE("Menace", SimpleKeyword.class, true),
    MEGAMORPH("Megamorph", KeywordWithCost.class, false),
    MIRACLE("Miracle", KeywordWithCost.class, false),
    MOBILIZE("Mobilize", KeywordWithAmount.class, false),
    MODULAR("Modular", Modular.class, false),
    MORE_THAN_MEETS_THE_EYE("More Than Meets the Eye", KeywordWithCost.class, false),
    MORPH("Morph", KeywordWithCost.class, false),
    MULTIKICKER("Multikicker", KeywordWithCost.class, false),
    MUTATE("Mutate", KeywordWithCost.class, true),
    MYRIAD("Myriad", SimpleKeyword.class, false),
    NIGHTBOUND("Nightbound", SimpleKeyword.class, true),
    NINJUTSU("Ninjutsu", Ninjutsu.class, false),
    OUTLAST("Outlast", KeywordWithCost.class, false),
    OFFERING("Offering", KeywordWithType.class, false),
    OFFSPRING("Offspring", KeywordWithCost.class, false),
    OVERLOAD("Overload", KeywordWithCost.class, false),
    PARADIGM("Paradigm", SimpleKeyword.class, false),
    PARTNER("Partner", Partner.class, true),
    PARTNER_WITH("Partner with", KeywordWithType.class, false),
    PERSIST("Persist", SimpleKeyword.class, false),
    PHASING("Phasing", SimpleKeyword.class, true),
    PLOT("Plot", KeywordWithCost.class, false),
    POISONOUS("Poisonous", KeywordWithAmount.class, false),
    PROTECTION("Protection", Protection.class, true),
    PROTOTYPE("Prototype", KeywordWithCost.class, false),
    PROVOKE("Provoke", SimpleKeyword.class, false),
    PROWESS("Prowess", SimpleKeyword.class, false),
    PROWL("Prowl", KeywordWithCost.class, false),
    RAMPAGE("Rampage", KeywordWithAmount.class, false),
    RAVENOUS("Ravenous", SimpleKeyword.class, false),
    REACH("Reach", SimpleKeyword.class, true),
    READ_AHEAD("Read ahead", SimpleKeyword.class, true),
    REBOUND("Rebound", SimpleKeyword.class, true),
    RECOVER("Recover", KeywordWithCost.class, false),
    RECONFIGURE("Reconfigure", KeywordWithCost.class, false),
    REFLECT("Reflect", KeywordWithCost.class, false),
    REINFORCE("Reinforce", KeywordWithCostAndAmount.class, false),
    RENOWN("Renown", KeywordWithAmount.class, false),
    REPLICATE("Replicate", KeywordWithCost.class, false),
    RETRACE("Retrace", SimpleKeyword.class, false),
    RIOT("Riot", SimpleKeyword.class, false),
    RIPPLE("Ripple", KeywordWithAmount.class, false),
    SADDLE("Saddle", KeywordWithAmount.class, false),
    SCAVENGE("Scavenge", KeywordWithCost.class, false),
    SHADOW("Shadow", SimpleKeyword.class, true),
    SHROUD("Shroud", SimpleKeyword.class, true),
    SKULK("Skulk", SimpleKeyword.class, true),
    SNEAK("Sneak", KeywordWithCost.class, false),
    SOULBOND("Soulbond", SimpleKeyword.class, true),
    SOULSHIFT("Soulshift", KeywordWithAmount.class, false),
    SPACE_SCULPTOR("Space sculptor", SimpleKeyword.class, true),
    SPECIALIZE("Specialize", KeywordWithCost.class, false),
    SPECTACLE("Spectacle", KeywordWithCost.class, false),
    SPLICE("Splice", KeywordWithCostAndType.class, false),
    SPLIT_SECOND("Split second", SimpleKeyword.class, true),
    SPREE("Spree", SimpleKeyword.class, true),
    SQUAD("Squad", KeywordWithCost.class, false),
    START_YOUR_ENGINES("Start your engines", SimpleKeyword.class, true),
    STARTING_INTENSITY("Starting intensity", KeywordWithAmount.class, true),
    STATION("Station", KeywordWithAmount.class, false),
    STORM("Storm", SimpleKeyword.class, false),
    STRIVE("Strive", KeywordWithCost.class, false),
    SUNBURST("Sunburst", SimpleKeyword.class, false),
    SURGE("Surge", KeywordWithCost.class, false),
    SUSPEND("Suspend", Suspend.class, false),
    TIERED("Tiered", SimpleKeyword.class, true),
    TOXIC("Toxic", KeywordWithAmount.class, false),
    TRAINING("Training", SimpleKeyword.class, false),
    TRAMPLE("Trample", Trample.class, true),
    TRANSFIGURE("Transfigure", KeywordWithCost.class, false),
    TRANSMUTE("Transmute", KeywordWithCost.class, false),
    TRIBUTE("Tribute", KeywordWithAmount.class, false),
    TYPECYCLING("TypeCycling", KeywordWithCostAndType.class, false),
    UMBRA_ARMOR("Umbra armor", SimpleKeyword.class, true),
    UNDAUNTED("Undaunted", SimpleKeyword.class, false),
    UNDYING("Undying", SimpleKeyword.class, false),
    UNEARTH("Unearth", KeywordWithCost.class, false),
    UNLEASH("Unleash", SimpleKeyword.class, false),
    VANISHING("Vanishing", Vanishing.class, false),
    VIGILANCE("Vigilance", SimpleKeyword.class, true),
    WARD("Ward", KeywordWithCost.class, false),
    WARP("Warp", KeywordWithCost.class, false),
    WEB_SLINGING("Web-slinging", KeywordWithCost.class, false),
    WITHER("Wither", SimpleKeyword.class, true),

    // mayflash additional cast
    MAYFLASHCOST("MayFlashCost", KeywordWithCost.class, false),
    MAYFLASHSAC("MayFlashSac", SimpleKeyword.class, false),

    ;

    protected final Class<? extends KeywordInstance<?>> type;
    protected final boolean isMultipleRedundant;
    protected final String displayName;
    private final String translationKey;

    Keyword(String displayName0, Class<? extends KeywordInstance<?>> type0, boolean isMultipleRedundant0) {
        type = type0;
        isMultipleRedundant = isMultipleRedundant0;
        displayName = displayName0;
        this.translationKey = getTranslationKey(name());
    }

    private static String getTranslationKey(String enumName) {
        StringBuilder sb = new StringBuilder("lblKwAbility");
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

    private static Pair<Keyword, String> getKeywordDetails(String k) {
        Keyword keyword = Keyword.UNDEFINED;
        String details = k;
        // try to get real part
        if (k.contains(":")) {
            final String[] x = k.split(":", 2);
            keyword = smartValueOf(x[0]);
            details = x[1];
            // Flavor keyword titles should be last in the card script K: line
            if (details.contains(":Flavor ")) details = details.substring(0, details.indexOf(":Flavor "));
            // Simply remove flavor here so it doesn't goof up parsing details
        } else if (k.contains(" ")) {
            // First strike
            keyword = smartValueOf(k);
            details = "";

            // other keywords that contains other stuff like Enchant
            if (keyword == Keyword.UNDEFINED) {
                final String[] x = k.split(" ", 2);

                final Keyword k2 = smartValueOf(x[0]);
                // Keywords that needs to be undefined
                if (k2 != Keyword.UNDEFINED) {
                    keyword = k2;
                    details = x[1];
                }
            }
        } else {
            // Simple Keyword
            keyword = smartValueOf(k);
            details = "";
        }
        return Pair.of(keyword, details);
    }

    public static KeywordInterface getInstance(String k) {
        Pair<Keyword, String> p = getKeywordDetails(k);

        KeywordInstance<?> inst;
        try {
            inst = p.getKey().type.getConstructor().newInstance();
        } catch (Exception e) {
            inst = new SimpleKeyword();
        }
        inst.initialize(k, p.getKey(), p.getValue());
        return inst;
    }

    public String getDisplayName() {
        return Localizer.getInstance().getMessage(translationKey);
    }

    @Override
    public String toString() {
        // English-only: getDisplayName() can NPE before Localizer is initialized.
        return displayName;
    }

    public static List<Keyword> getAllKeywords() {
        Keyword[] values = values();
        List<Keyword> keywords = new ArrayList<>(Arrays.asList(values).subList(1, values.length)); //skip UNDEFINED
        return keywords;
    }

    private static Keyword get(String k) {
        if (k == null || k.isEmpty())
            return Keyword.UNDEFINED;

        return getKeywordDetails(k).getKey();
    }

    private static final Map<String, Set<Keyword>> cardKeywordSetLookup = new HashMap<>();

    public static Set<Keyword> getKeywordSet(PaperCard card) {
        String name = card.getName();
        Set<Keyword> keywordSet = cardKeywordSetLookup.get(name);
        if (keywordSet == null) {
            CardSplitType cardSplitType = card.getRules().getSplitType();
            keywordSet = EnumSet.noneOf(Keyword.class);
            if (cardSplitType != CardSplitType.None && cardSplitType != CardSplitType.Split) {
                if (card.getRules().getOtherPart() != null) {
                    if (card.getRules().getOtherPart().getKeywords() != null) {
                        for (String key : card.getRules().getOtherPart().getKeywords()) {
                            Keyword keyword = get(key);
                            if (!Keyword.UNDEFINED.equals(keyword))
                                keywordSet.add(keyword);
                        }
                    }
                }
            }
            if (card.getRules().getMainPart().getKeywords() != null) {
                for (String key : card.getRules().getMainPart().getKeywords()) {
                    Keyword keyword = get(key);
                    if (!Keyword.UNDEFINED.equals(keyword))
                        keywordSet.add(keyword);
                }
            }
            cardKeywordSetLookup.put(name, keywordSet);
        }
        return keywordSet;
    }

    public static Keyword smartValueOf(String value) {
        for (final Keyword v : Keyword.values()) {
            if (v.displayName.equalsIgnoreCase(value)) {
                return v;
            }
        }

        return UNDEFINED;
    }

    public static Set<Keyword> setValueOf(String value) {
        Set<Keyword> result = EnumSet.noneOf(Keyword.class);
        for (String s : value.split(" & ")) {
            Keyword k = smartValueOf(s);
            if (!UNDEFINED.equals(k)) {
                result.add(k);
            }
        }
        return result;
    }

    public String getReminderText() {
        return Localizer.getInstance().getMessage(translationKey + "Reminder");
    }

    public boolean isMultipleRedundant() {
        return isMultipleRedundant;
    }
}
