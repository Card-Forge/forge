package forge.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import forge.card.mana.ManaCost;

//
// DO NOT AUTOFORMAT / CHECKSTYLE THIS FILE
// 

/** 
 * Represents a single side or part of a magic card with its original characteristics. 
 * <br><br>
 * <i>Do not use reference to class except for card parsing.<br>Always use reference to interface type outside of package.</i>
 */
final class CardFace implements ICardFace {

    public enum FaceSelectionMethod { // 
        USE_ACTIVE_FACE,
        USE_PRIMARY_FACE,
        COMBINE
    }

    
    private final static List<String> emptyList = Collections.unmodifiableList(new ArrayList<>());
    private final static Map<String, String> emptyMap = Collections.unmodifiableMap(new TreeMap<>());

    private final String name;
    private String altName = null;
    private CardType type = null;
    private ManaCost manaCost = ManaCost.NO_COST;
    private ColorSet color = null;

    private String oracleText = null;
    private int iPower = Integer.MAX_VALUE;
    private int iToughness = Integer.MAX_VALUE;
    private String power = null;
    private String toughness = null;
    private String initialLoyalty = "";

    private String nonAbilityText = null;
    private List<String> keywords = null;
    private List<String> abilities = null;
    private List<String> staticAbilities = null;
    private List<String> triggers = null;
    private List<String> replacements = null;
    private Map<String, String> variables = null;



    // these implement ICardCharacteristics 
    @Override public String getOracleText()         { return oracleText; }
    @Override public int getIntPower()              { return iPower; }
    @Override public int getIntToughness()          { return iToughness; }
    @Override public String getPower()              { return power; }
    @Override public String getToughness()          { return toughness; }
    @Override public String getInitialLoyalty()              { return initialLoyalty; }
    @Override public String getName()               { return this.name; }
    @Override public CardType getType()             { return this.type; }
    @Override public ManaCost getManaCost()         { return this.manaCost; }
    @Override public ColorSet getColor()            { return this.color; }
        
    // these are raw and unparsed used for Card creation
    @Override public Iterable<String> getKeywords()   { return keywords; }
    @Override public Iterable<String> getAbilities()  { return abilities; }
    @Override public Iterable<String> getStaticAbilities() { return staticAbilities; }
    @Override public Iterable<String> getTriggers()   { return triggers; }
    @Override public Iterable<String> getReplacements() { return replacements; }
    @Override public String getNonAbilityText()       { return nonAbilityText; }
    @Override public Iterable<Entry<String, String>> getVariables() { return variables.entrySet(); }

    @Override public String getAltName()              { return this.altName; }
    
    public CardFace(String name0) { 
        this.name = name0; 
        if ( StringUtils.isBlank(name0) )
            throw new RuntimeException("Card name is empty");
    }
    // Here come setters to allow parser supply values
    void setAltName(String name)             { this.altName = name; }
    void setType(CardType type0)             { this.type = type0; }
    void setManaCost(ManaCost manaCost0)     { this.manaCost = manaCost0; }
    void setColor(ColorSet color0)           { this.color = color0; }
    void setOracleText(String text)          { this.oracleText = text; }
    void setInitialLoyalty(String value)        { this.initialLoyalty = value; }

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
    void addStaticAbility(String value)      { if (null == this.staticAbilities) { this.staticAbilities = new ArrayList<>(); } this.staticAbilities.add(value);}
    void addReplacementEffect(String value)  { if (null == this.replacements) { this.replacements = new ArrayList<>(); } this.replacements.add(value);}
    void addSVar(String key, String value)   { if (null == this.variables) { this.variables = new TreeMap<>(String.CASE_INSENSITIVE_ORDER); } this.variables.put(key, value); }

    
    void assignMissingFields() { // Most scripts do not specify color explicitly
        if ( null == oracleText ) { System.err.println(name + " has no Oracle text."); oracleText = ""; }
        if ( manaCost == null && color == null ) System.err.println(name + " has neither ManaCost nor Color");
        if ( color == null ) color = ColorSet.fromManaCost(manaCost);

        if ( keywords == null ) keywords = emptyList;
        if ( abilities == null ) abilities = emptyList;
        if ( staticAbilities == null ) staticAbilities = emptyList;
        if ( triggers == null ) triggers = emptyList;
        if ( replacements == null ) replacements = emptyList;
        if ( variables == null ) variables = emptyMap;
        if ( null == nonAbilityText ) nonAbilityText = "";
    }


    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int compareTo(ICardFace o) {
        return getName().compareTo(o.getName());
    }
}
