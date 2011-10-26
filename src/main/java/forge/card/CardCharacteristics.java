package forge.card;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import forge.Card_Color;
import forge.Counters;
import forge.SetInfo;
import forge.card.spellability.Ability_Mana;
import forge.card.spellability.SpellAbility;
import forge.card.staticAbility.StaticAbility;
import forge.card.trigger.Trigger;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class CardCharacteristics {
    private String name = "";
    private ArrayList<String> type = new ArrayList<String>();
    private String manaCost = "";
    private ArrayList<Card_Color> cardColor = new ArrayList<Card_Color>();
    private boolean cardColorsOverridden = false;
    private int baseAttack = 0;
    private int baseDefense = 0;   
    private ArrayList<String> intrinsicKeyword = new ArrayList<String>();
    private ArrayList<SpellAbility> spellAbility = new ArrayList<SpellAbility>();
    private ArrayList<String> intrinsicAbility = new ArrayList<String>();
    private ArrayList<Ability_Mana> manaAbility = new ArrayList<Ability_Mana>();
    private ArrayList<Trigger> triggers = new ArrayList<Trigger>();
    private ArrayList<StaticAbility> staticAbilities = new ArrayList<StaticAbility>();
    private ArrayList<String> staticAbilityStrings = new ArrayList<String>();
    private String ImageFilename = "";
    private Map<String, String> sVars = new TreeMap<String, String>();
    private ArrayList<SetInfo> Sets = new ArrayList<SetInfo>();
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
    /**
     * @param name0 the name to set
     */
    public void setName(String name0) {
        this.name = name0; // TODO: Add 0 to parameter's name.
    }
    /**
     * @return the type
     */
    public ArrayList<String> getType() {
        return type;
    }
    /**
     * @param type0 the type to set
     */
    public void setType(ArrayList<String> type0) {
        this.type = type0; // TODO: Add 0 to parameter's name.
    }
    /**
     * @return the manaCost
     */
    public String getManaCost() {
        return manaCost;
    }
    /**
     * @param manaCost0 the manaCost to set
     */
    public void setManaCost(String manaCost0) {
        this.manaCost = manaCost0; // TODO: Add 0 to parameter's name.
    }
    /**
     * @return the cardColor
     */
    public ArrayList<Card_Color> getCardColor() {
        return cardColor;
    }
    /**
     * @param cardColor0 the cardColor to set
     */
    public void setCardColor(ArrayList<Card_Color> cardColor0) {
        this.cardColor = new ArrayList<Card_Color>(cardColor0); // TODO: Add 0 to parameter's name.
    }
    /**
     * @return the cardColorsOverridden
     */
    public boolean isCardColorsOverridden() {
        return cardColorsOverridden;
    }
    /**
     * @param cardColorsOverridden0 the cardColorsOverridden to set
     */
    public void setCardColorsOverridden(boolean cardColorsOverridden0) {
        this.cardColorsOverridden = cardColorsOverridden0; // TODO: Add 0 to parameter's name.
    }
    /**
     * @return the baseAttack
     */
    public int getBaseAttack() {
        return baseAttack;
    }
    /**
     * @param baseAttack0 the baseAttack to set
     */
    public void setBaseAttack(int baseAttack0) {
        this.baseAttack = baseAttack0; // TODO: Add 0 to parameter's name.
    }
    /**
     * @return the baseDefense
     */
    public int getBaseDefense() {
        return baseDefense;
    }
    /**
     * @param baseDefense0 the baseDefense to set
     */
    public void setBaseDefense(int baseDefense0) {
        this.baseDefense = baseDefense0; // TODO: Add 0 to parameter's name.
    }
    /**
     * @return the intrinsicKeyword
     */
    public ArrayList<String> getIntrinsicKeyword() {
        return intrinsicKeyword;
    }
    /**
     * @param intrinsicKeyword0 the intrinsicKeyword to set
     */
    public void setIntrinsicKeyword(ArrayList<String> intrinsicKeyword0) {
        this.intrinsicKeyword = intrinsicKeyword0; // TODO: Add 0 to parameter's name.
    }
    /**
     * @return the spellAbility
     */
    public ArrayList<SpellAbility> getSpellAbility() {
        return spellAbility;
    }
    /**
     * @param spellAbility0 the spellAbility to set
     */
    public void setSpellAbility(ArrayList<SpellAbility> spellAbility0) {
        this.spellAbility = spellAbility0; // TODO: Add 0 to parameter's name.
    }
    /**
     * @return the intrinsicAbility
     */
    public ArrayList<String> getIntrinsicAbility() {
        return intrinsicAbility;
    }
    /**
     * @param intrinsicAbility0 the intrinsicAbility to set
     */
    public void setIntrinsicAbility(ArrayList<String> intrinsicAbility0) {
        this.intrinsicAbility = intrinsicAbility0; // TODO: Add 0 to parameter's name.
    }
    /**
     * @return the manaAbility
     */
    public ArrayList<Ability_Mana> getManaAbility() {
        return manaAbility;
    }
    /**
     * @param manaAbility0 the manaAbility to set
     */
    public void setManaAbility(ArrayList<Ability_Mana> manaAbility0) {
        this.manaAbility = manaAbility0; // TODO: Add 0 to parameter's name.
    }
    /**
     * @return the triggers
     */
    public ArrayList<Trigger> getTriggers() {
        return triggers;
    }
    /**
     * @param triggers0 the triggers to set
     */
    public void setTriggers(ArrayList<Trigger> triggers0) {
        this.triggers = triggers0; // TODO: Add 0 to parameter's name.
    }
    /**
     * @return the staticAbilities
     */
    public ArrayList<StaticAbility> getStaticAbilities() {
        return staticAbilities;
    }
    /**
     * @param staticAbilities0 the staticAbilities to set
     */
    public void setStaticAbilities(ArrayList<StaticAbility> staticAbilities0) {
        this.staticAbilities = new ArrayList<StaticAbility>(staticAbilities0); // TODO: Add 0 to parameter's name.
    }
    /**
     * @return the imageFilename
     */
    public String getImageFilename() {
        return ImageFilename;
    }
    /**
     * @param imageFilename0 the imageFilename to set
     */
    public void setImageFilename(String imageFilename0) {
        ImageFilename = imageFilename0; // TODO: Add 0 to parameter's name.
    }
    /**
     * @return the sVars
     */
    public Map<String, String> getsVars() {
        return sVars;
    }
    /**
     * @param sVars0 the sVars to set
     */
    public void setsVars(Map<String, String> sVars0) {
        this.sVars = new HashMap<String, String>(sVars0); // TODO: Add 0 to parameter's name.
    }
    /**
     * @return the sets
     */
    public ArrayList<SetInfo> getSets() {
        return Sets;
    }
    /**
     * @param sets0 the sets to set
     */
    public void setSets(ArrayList<SetInfo> sets0) {
        Sets = new ArrayList<SetInfo>(sets0); // TODO: Add 0 to parameter's name.
    }
    /**
     * @return the staticAbilityStrings
     */
    public ArrayList<String> getStaticAbilityStrings() {
        return staticAbilityStrings;
    }
    /**
     * @param staticAbilityStrings0 the staticAbilityStrings to set
     */
    public void setStaticAbilityStrings(ArrayList<String> staticAbilityStrings0) {
        this.staticAbilityStrings = staticAbilityStrings0; // TODO: Add 0 to parameter's name.
    }
}
