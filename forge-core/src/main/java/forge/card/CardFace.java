package forge.card;

import forge.card.mana.ManaCost;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.Map.Entry;

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
        COMBINE;
    }

    
    private final static List<String> emptyList = Collections.unmodifiableList(new ArrayList<String>());
    private final static Map<String, String> emptyMap = Collections.unmodifiableMap(new TreeMap<String, String>());

    private final String name;
    private CardType type = null;
    private ManaCost manaCost = ManaCost.NO_COST;
    private ColorSet color = null;

    private String oracleText = null;
    private int iPower = -1;
    private int iToughness = -1;
    private String power = null;
    private String toughness = null;
    private int initialLoyalty = -1;

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
    @Override public int getInitialLoyalty()              { return initialLoyalty; }
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

    public CardFace(String name0) { 
        this.name = name0; 
        if ( StringUtils.isBlank(name0) )
            throw new RuntimeException("Card name is empty");
    }
    // Here come setters to allow parser supply values
    void setType(CardType type0)             { this.type = type0; }
    void setManaCost(ManaCost manaCost0)     { this.manaCost = manaCost0; }
    void setColor(ColorSet color0)           { this.color = color0; }
    void setOracleText(String text)          { this.oracleText = text; }
    void setInitialLoyalty(int value)        { this.initialLoyalty = value; }

    void setPtText(String value) {
        final int slashPos = value.indexOf('/');
        if (slashPos == -1) {
            throw new RuntimeException(String.format("Creature '%s' has bad p/t stats", this.getName()));
        }
        this.power = value.substring(0, slashPos);
        this.toughness = value.substring(slashPos + 1);
        this.iPower = StringUtils.isNumeric(this.power) ? Integer.parseInt(this.power) : 0;
        this.iToughness = StringUtils.isNumeric(this.toughness) ? Integer.parseInt(this.toughness) : 0;
    }

    // Raw fields used for Card creation
    void setNonAbilityText(String value)     { this.nonAbilityText = value; }
    void addKeyword(String value)            { if (null == this.keywords) { this.keywords = new ArrayList<String>(); } this.keywords.add(value); }
    void addAbility(String value)            { if (null == this.abilities) { this.abilities = new ArrayList<String>(); } this.abilities.add(value);}
    void addTrigger(String value)            { if (null == this.triggers) { this.triggers = new ArrayList<String>(); } this.triggers.add(value);}
    void addStaticAbility(String value)      { if (null == this.staticAbilities) { this.staticAbilities = new ArrayList<String>(); } this.staticAbilities.add(value);}
    void addReplacementEffect(String value)  { if (null == this.replacements) { this.replacements = new ArrayList<String>(); } this.replacements.add(value);}
    void addSVar(String key, String value)   { if (null == this.variables) { this.variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER); } this.variables.put(key, value); }

    
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
}
