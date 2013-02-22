package forge.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    @Override public final String getOracleText()         { return oracleText; }
    @Override public final int getIntPower()              { return iPower; }
    @Override public final int getIntToughness()          { return iToughness; }
    @Override public final String getPower()              { return power; }
    @Override public final String getToughness()          { return toughness; }
    @Override public int getInitialLoyalty()              { return initialLoyalty; }
    @Override public final String getName()               { return this.name; }
    @Override public final CardType getType()             { return this.type; }
    @Override public final ManaCost getManaCost()         { return this.manaCost; }
    @Override public final ColorSet getColor()            { return this.color; }
    
    // these are raw and unparsed used for Card creation
    @Override public final Iterable<String> getKeywords()   { return keywords; }
    @Override public final Iterable<String> getAbilities()  { return abilities; }
    @Override public final Iterable<String> getStaticAbilities() { return staticAbilities; }
    @Override public final Iterable<String> getTriggers()   { return triggers; }
    @Override public final Iterable<String> getReplacements() { return replacements; }
    @Override public final String getNonAbilityText()       { return nonAbilityText; }
    @Override public final Iterable<Entry<String, String>> getVariables() { return variables.entrySet(); }

    public CardFace(String name0) { 
        this.name = name0; 
        if ( StringUtils.isBlank(name0) )
            throw new RuntimeException("Card name is empty");
    }
    // Here come setters to allow parser supply values
    public final void setType(CardType type0)             { this.type = type0; }
    public final void setManaCost(ManaCost manaCost0)     { this.manaCost = manaCost0; }
    public final void setColor(ColorSet color0)           { this.color = color0; }
    public final void setOracleText(String text)          { this.oracleText = text; }
    public final void setInitialLoyalty(int value)        { this.initialLoyalty = value; }
    public final Map<String, CardInSet> getSetsData()     { return this.setsPrinted; } // reader will add sets here

    public void setPtText(String value) {
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
    public final void setNonAbilityText(String value)     { this.nonAbilityText = value; }
    public final void addKeyword(String value)            { if (null == this.keywords) { this.keywords = new ArrayList<String>(); } this.keywords.add(value); }
    public final void addAbility(String value)            { if (null == this.abilities) { this.abilities = new ArrayList<String>(); } this.abilities.add(value);}
    public final void addTrigger(String value)            { if (null == this.triggers) { this.triggers = new ArrayList<String>(); } this.triggers.add(value);}
    public final void addStaticAbility(String value)      { if (null == this.staticAbilities) { this.staticAbilities = new ArrayList<String>(); } this.staticAbilities.add(value);}
    public final void addReplacementEffect(String value)  { if (null == this.replacements) { this.replacements = new ArrayList<String>(); } this.replacements.add(value);}
    public final void addSVar(String key, String value)   { if (null == this.variables) { this.variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER); } this.variables.put(key, value); }

    
    public void assignMissingFields() { // Most scripts do not specify color explicitly
        if ( null == oracleText ) { System.err.println(name + " has no Oracle text."); oracleText = ""; }
        if ( setsPrinted.isEmpty() ) { System.err.println(name + " was not assigned any set."); setsPrinted.put(CardEdition.UNKNOWN.getCode(), new CardInSet(CardRarity.Common, 1, null) ) ; }
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


    // This should not be here, but I don't know a better place yet 
    private final Map<String, CardInSet> setsPrinted = new TreeMap<String, CardInSet>(String.CASE_INSENSITIVE_ORDER);
    @Override public Set<String> getSets() { return this.setsPrinted.keySet(); }
    @Override public CardInSet getEditionInfo(final String setCode) {
        final CardInSet result = this.setsPrinted.get(setCode);
        return result; // if returns null, String.format("Card '%s' was never printed in set '%s'", this.getName(), setCode);
    }



}
