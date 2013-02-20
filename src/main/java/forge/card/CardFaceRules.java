package forge.card;

import java.util.ArrayList;
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
 * TODO: Write javadoc for this type.
 *
 */
final class CardFaceRules implements ICardCharacteristics {

    private final String name;
    private CardType type = null;
    private ManaCost manaCost = ManaCost.NO_COST;
    private ColorSet color = null;

    private String oracleText = null;
    private int iPower = -1;
    private int iToughness = -1;
    private String power = null;
    private String toughness = null;
    private int initialLoyalty = 0;

    private final List<String> keywords = new ArrayList<String>();

    // these implement ICardCharacteristics 
    @Override public final String getOracleText()         { return oracleText; }
    @Override public final int getIntPower()              { return iPower; }
    @Override public final int getIntToughness()          { return iToughness; }
    @Override public final String getPower()              { return power; }
    @Override public final String getToughness()          { return toughness; }
    @Override public int getInitialLoyalty()        { return initialLoyalty; }
    @Override public final String getName()         { return this.name; }
    @Override public final CardType getType()       { return this.type; }
    @Override public final ManaCost getManaCost()   { return this.manaCost; }
    @Override public final ColorSet getColor()      { return this.color; }
    @Override public final Iterable<String> getKeywords() { return keywords; }


    // Here come setters to allow parser supply values
    public CardFaceRules(String name0) { this.name = name0; if ( StringUtils.isBlank(name0) ) throw new RuntimeException("Card name is empty"); }

    public final void setType(CardType type0)             { this.type = type0; }
    public final void setManaCost(ManaCost manaCost0)     { this.manaCost = manaCost0; }
    public final void setColor(ColorSet color0)           { this.color = color0; }
    public final void setOracleText(String text)          { this.oracleText = text; }
    public final void addKeyword(String value)            { this.keywords.add(value); }
    public final void setInitialLoaylty(int value)        { this.initialLoyalty = value; }
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

    public void calculateColor() { // Most scripts do not specify color explicitly
        if ( oracleText == null ) System.err.println(name + " has no Oracle text");
        if ( manaCost == null && color == null ) System.err.println(name + " has neither ManaCost nor Color");
        if ( color == null ) color = ColorSet.fromManaCost(manaCost);
    }


    // This should not be here
    private final Map<String, CardInSet> setsPrinted = new TreeMap<String, CardInSet>(String.CASE_INSENSITIVE_ORDER);
    @Override public Set<Entry<String, CardInSet>> getSetsPrinted() { return this.setsPrinted.entrySet(); }
    @Override public CardInSet getEditionInfo(final String setCode) {
        final CardInSet result = this.setsPrinted.get(setCode);
        if (result != null) {
            return result;
        }
        throw new RuntimeException(String.format("Card '%s' was never printed in set '%s'", this.getName(), setCode));
    }

}
