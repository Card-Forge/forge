package forge.card;

import forge.card.mana.ManaCost;
import forge.util.Lang;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

//
// DO NOT AUTOFORMAT / CHECKSTYLE THIS FILE
// 

/** 
 * Represents a single side or part of a magic card with its original characteristics. 
 * <br><br>
 * <i>Do not use reference to class except for card parsing.<br>Always use reference to interface type outside of package.</i>
 */
final class CardFace implements ICardFace, Cloneable {
    private final static List<String> emptyList = Collections.unmodifiableList(new ArrayList<>());
    private final static Map<String, String> emptyMap = Collections.unmodifiableMap(new TreeMap<>());
    private final static Set<Integer> emptySet = Collections.unmodifiableSet(new HashSet<>());

    private String name;
    private String flavorName = null;
    private CardType type = null;
    private ManaCost manaCost = null;
    private ColorSet color = null;

    private String oracleText = null;
    private int iPower = Integer.MAX_VALUE;
    private int iToughness = Integer.MAX_VALUE;
    private String power = null;
    private String toughness = null;
    private String initialLoyalty = "";
    private String defense = "";
    private Set<Integer> attractionLights = null;

    private String nonAbilityText = null;
    private List<String> keywords = null;
    private List<String> abilities = null;
    private List<String> staticAbilities = null;
    private List<String> triggers = null;
    private List<String> draftActions = null;
    private List<String> replacements = null;
    private Map<String, String> variables = null;

    private Map<String, CardFace> functionalVariants = null;



    // these implement ICardCharacteristics 
    @Override public String getOracleText()         { return oracleText; }
    @Override public int getIntPower()              { return iPower; }
    @Override public int getIntToughness()          { return iToughness; }
    @Override public String getPower()              { return power; }
    @Override public String getToughness()          { return toughness; }
    @Override public String getInitialLoyalty()              { return initialLoyalty; }
    @Override public String getDefense()              { return defense; }
    @Override public Set<Integer> getAttractionLights()   { return attractionLights; }
    @Override public String getName()               { return this.name; }
    @Override public CardType getType()             { return this.type; }
    @Override public ManaCost getManaCost()         { return this.manaCost; }
    @Override public ColorSet getColor()            { return this.color; }
        
    // these are raw and unparsed used for Card creation
    @Override public Iterable<String> getKeywords()   { return keywords; }
    @Override public Iterable<String> getAbilities()  { return abilities; }
    @Override public Iterable<String> getStaticAbilities() { return staticAbilities; }
    @Override public Iterable<String> getTriggers()   { return triggers; }
    @Override public Iterable<String> getDraftActions()   { return draftActions; }
    @Override public Iterable<String> getReplacements() { return replacements; }
    @Override public String getNonAbilityText()       { return nonAbilityText; }
    @Override public Iterable<Entry<String, String>> getVariables() {
        if (variables == null)
            return null;
        return variables.entrySet();
    }

    @Override public String getFlavorName()              { return this.flavorName; }

    public CardFace(String name0) {
        this.name = name0; 
        if ( StringUtils.isBlank(name0) )
            throw new RuntimeException("Card name is empty");
    }
    // Here come setters to allow parser supply values
    void setName(String name)                { this.name = name; }
    void setFlavorName(String name)          { this.flavorName = name; }
    void setType(CardType type0)             { this.type = type0; }
    void setManaCost(ManaCost manaCost0)     { this.manaCost = manaCost0; }
    void setColor(ColorSet color0)           { this.color = color0; }
    void setOracleText(String text)          { this.oracleText = text; }
    void setInitialLoyalty(String value)     { this.initialLoyalty = value; }
    void setDefense(String value)            { this.defense = value; }
    void setAttractionLights(String value) {
        if (value == null) {
            this.attractionLights = null;
            return;
        }
        this.attractionLights = Arrays.stream(value.split(" ")).map(Integer::parseInt).collect(Collectors.toSet());
    }

    void setPtText(String value) {
        final String[] k = value.split("/");

        if (k.length != 2) {
            throw new RuntimeException("Creature '" + this.getName() + "' has bad p/t stats");
        }

        this.power = k[0];
        this.toughness = k[1];

        this.iPower = parsePT(k[0]);
        this.iToughness = parsePT(k[1]);
    }

    static int parsePT(String val) {
        // normalize PT value
        if (val.contains("*")) {
            val = val.replace("+*", "");
            val = val.replace("-*", "");
            val = val.replace("*+", "");
            val = val.replace("*", "0");
        }
        return Integer.parseInt(val);
    }

    // Raw fields used for Card creation
    void setNonAbilityText(String value)     { this.nonAbilityText = value; }
    void addKeyword(String value)            { if (null == this.keywords) { this.keywords = new ArrayList<>(); } this.keywords.add(value); }
    void addAbility(String value)            { if (null == this.abilities) { this.abilities = new ArrayList<>(); } this.abilities.add(value);}
    void addTrigger(String value)            { if (null == this.triggers) { this.triggers = new ArrayList<>(); } this.triggers.add(value);}
    void addDraftAction(String value)        { if (null == this.draftActions) { this.draftActions = new ArrayList<>(); } this.draftActions.add(value);}
    void addStaticAbility(String value)      { if (null == this.staticAbilities) { this.staticAbilities = new ArrayList<>(); } this.staticAbilities.add(value);}
    void addReplacementEffect(String value)  { if (null == this.replacements) { this.replacements = new ArrayList<>(); } this.replacements.add(value);}
    void addSVar(String key, String value)   { if (null == this.variables) { this.variables = new TreeMap<>(String.CASE_INSENSITIVE_ORDER); } this.variables.put(key, value); }


    //Functional variant methods. Used for Attractions and some Un-cards,
    //when cards with the same name can have different logic.
    @Override public boolean hasFunctionalVariants() {
        return this.functionalVariants != null;
    }
    @Override public ICardFace getFunctionalVariant(String variant) {
        if(this.functionalVariants == null)
            return null;
        return this.functionalVariants.get(variant);
    }
    @Override public Map<String, ? extends ICardFace> getFunctionalVariants() {
        return this.functionalVariants;
    }
    CardFace getOrCreateFunctionalVariant(String variant) {
        if (this.functionalVariants == null) {
            this.functionalVariants = new HashMap<>();
        }
        if (!this.functionalVariants.containsKey(variant)) {
            this.functionalVariants.put(variant, new CardFace(this.name));
        }
        return this.functionalVariants.get(variant);
    }

    
    void assignMissingFields() { // Most scripts do not specify color explicitly
        if ( null == oracleText ) { System.err.println(name + " has no Oracle text."); oracleText = ""; }
        if ( manaCost == null && color == null ) System.err.println(name + " has neither ManaCost nor Color");
        if ( manaCost == null ) manaCost = ManaCost.NO_COST;
        if ( color == null ) color = ColorSet.fromManaCost(manaCost);

        if ( keywords == null ) keywords = emptyList;
        if ( abilities == null ) abilities = emptyList;
        if ( staticAbilities == null ) staticAbilities = emptyList;
        if ( triggers == null ) triggers = emptyList;
        if ( replacements == null ) replacements = emptyList;
        if ( variables == null ) variables = emptyMap;
        if ( null == nonAbilityText ) nonAbilityText = "";
        if ( attractionLights == null) attractionLights = emptySet;

        if(this.functionalVariants != null) {
            //Copy fields to undefined ones in functional variants
            for (CardFace variant : this.functionalVariants.values()) {
                assignMissingFieldsToVariant(variant);
            }
        }
    }

    void assignMissingFieldsToVariant(CardFace variant) {
        if(variant.oracleText == null) {
            if(variant.flavorName != null && this.oracleText != null) {
                try {
                    Lang lang = Lang.getInstance();
                    //Rudimentary name replacement. Can't do pronouns, ability words, or flavored keywords. Need to define variant text manually for that.
                    //Regex here checks for the name following either a word boundary or a literal "\n" string, since those haven't yet been converted to line breaks.
                    String flavoredText = this.oracleText.replaceAll("(?<=\\b|\\\\n)" + this.name + "\\b", variant.flavorName);
                    flavoredText = flavoredText.replaceAll("(?<=\\b|\\\\n)" + lang.getNickName(this.name) + "\\b", lang.getNickName(variant.flavorName));
                    variant.oracleText = flavoredText;
                }
                catch (PatternSyntaxException ignored) {
                    // Old versions of Android are weird about patterns sometimes. I don't *think* this is such a case but
                    // the documentation is unreliable. May be worth removing this once we're sure it's not a problem.
                    variant.oracleText = this.oracleText;
                }
            }
            else
                variant.oracleText = this.oracleText;
        }
        if(variant.manaCost == null) variant.manaCost = this.manaCost;
        if(variant.color == null) variant.color = ColorSet.fromManaCost(variant.manaCost);

        if(variant.type == null) variant.type = this.type;

        if(variant.power == null) {
            variant.power = this.power;
            variant.iPower = this.iPower;
        }
        if(variant.toughness == null) {
            variant.toughness = this.toughness;
            variant.iToughness = this.iToughness;
        }

        if("".equals(variant.initialLoyalty)) variant.initialLoyalty = this.initialLoyalty;
        if("".equals(variant.defense)) variant.defense = this.defense;

        if(variant.keywords == null) variant.keywords = this.keywords;
        else variant.keywords.addAll(0, this.keywords);

        if(variant.abilities == null) variant.abilities = this.abilities;
        else variant.abilities.addAll(0, this.abilities);

        if(variant.staticAbilities == null) variant.staticAbilities = this.staticAbilities;
        else variant.staticAbilities.addAll(0, this.staticAbilities);

        if(variant.triggers == null) variant.triggers = this.triggers;
        else variant.triggers.addAll(0, this.triggers);

        if(variant.replacements == null) variant.replacements = this.replacements;
        else variant.replacements.addAll(0, this.replacements);

        if(variant.variables == null) variant.variables = this.variables;
        else this.variables.forEach((k, v) -> variant.variables.putIfAbsent(k, v));

        if(variant.nonAbilityText == null) variant.nonAbilityText = this.nonAbilityText;
        if(variant.draftActions == null) variant.draftActions = this.draftActions;
        if(variant.attractionLights == null) variant.attractionLights = this.attractionLights;
        //if(variant.flavorName == null) variant.flavorName = this.flavorName; //Probably shouldn't be setting this on the main variant to begin with?
    }


    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int compareTo(ICardFace o) {
        return getName().compareTo(o.getName());
    }

    /** {@inheritDoc} */
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (final Exception ex) {
            throw new RuntimeException("CardFace : clone() error, " + ex);
        }
    }
}
