package forge.card;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import forge.CardColor;
import forge.SetInfo;
import forge.card.spellability.AbilityMana;
import forge.card.spellability.SpellAbility;
import forge.card.staticability.StaticAbility;
import forge.card.trigger.Trigger;

/**
 * TODO: Write javadoc for this type.
 * 
 */
public class CardCharacteristics {
    private String name = "";
    private ArrayList<String> type = new ArrayList<String>();
    private String manaCost = "";
    private ArrayList<CardColor> cardColor = new ArrayList<CardColor>();
    private boolean cardColorsOverridden = false;
    private int baseAttack = 0;
    private int baseDefense = 0;
    private ArrayList<String> intrinsicKeyword = new ArrayList<String>();
    private ArrayList<SpellAbility> spellAbility = new ArrayList<SpellAbility>();
    private ArrayList<String> intrinsicAbility = new ArrayList<String>();
    private ArrayList<AbilityMana> manaAbility = new ArrayList<AbilityMana>();
    private ArrayList<Trigger> triggers = new ArrayList<Trigger>();
    private ArrayList<StaticAbility> staticAbilities = new ArrayList<StaticAbility>();
    private ArrayList<String> staticAbilityStrings = new ArrayList<String>();
    private String imageFilename = "";
    private String imageName = "";
    private ArrayList<SetInfo> sets = new ArrayList<SetInfo>();

    /**
     * Gets the name.
     * 
     * @return the name
     */
    public final String getName() {
        return name;
    }

    /**
     * Sets the name.
     * 
     * @param name0
     *            the name to set
     */
    public final void setName(final String name0) {
        this.name = name0; // TODO: Add 0 to parameter's name.
    }

    /**
     * Gets the type.
     * 
     * @return the type
     */
    public final ArrayList<String> getType() {
        return type;
    }

    /**
     * Sets the type.
     * 
     * @param type0
     *            the type to set
     */
    public final void setType(final ArrayList<String> type0) {
        this.type = type0; // TODO: Add 0 to parameter's name.
    }

    /**
     * Gets the mana cost.
     * 
     * @return the manaCost
     */
    public final String getManaCost() {
        return manaCost;
    }

    /**
     * Sets the mana cost.
     * 
     * @param manaCost0
     *            the manaCost to set
     */
    public final void setManaCost(final String manaCost0) {
        this.manaCost = manaCost0; // TODO: Add 0 to parameter's name.
    }

    /**
     * Gets the card color.
     * 
     * @return the cardColor
     */
    public final ArrayList<CardColor> getCardColor() {
        return cardColor;
    }

    /**
     * Sets the card color.
     * 
     * @param cardColor0
     *            the cardColor to set
     */
    public final void setCardColor(final ArrayList<CardColor> cardColor0) {
        this.cardColor = new ArrayList<CardColor>(cardColor0); // TODO: Add 0
                                                                // to
                                                                // parameter's
                                                                // name.
    }

    /**
     * Checks if is card colors overridden.
     * 
     * @return the cardColorsOverridden
     */
    public final boolean isCardColorsOverridden() {
        return cardColorsOverridden;
    }

    /**
     * Sets the card colors overridden.
     * 
     * @param cardColorsOverridden0
     *            the cardColorsOverridden to set
     */
    public final void setCardColorsOverridden(final boolean cardColorsOverridden0) {
        this.cardColorsOverridden = cardColorsOverridden0; // TODO: Add 0 to
                                                           // parameter's name.
    }

    /**
     * Gets the base attack.
     * 
     * @return the baseAttack
     */
    public final int getBaseAttack() {
        return baseAttack;
    }

    /**
     * Sets the base attack.
     * 
     * @param baseAttack0
     *            the baseAttack to set
     */
    public final void setBaseAttack(final int baseAttack0) {
        this.baseAttack = baseAttack0; // TODO: Add 0 to parameter's name.
    }

    /**
     * Gets the base defense.
     * 
     * @return the baseDefense
     */
    public final int getBaseDefense() {
        return baseDefense;
    }

    /**
     * Sets the base defense.
     * 
     * @param baseDefense0
     *            the baseDefense to set
     */
    public final void setBaseDefense(final int baseDefense0) {
        this.baseDefense = baseDefense0; // TODO: Add 0 to parameter's name.
    }

    /**
     * Gets the intrinsic keyword.
     * 
     * @return the intrinsicKeyword
     */
    public final ArrayList<String> getIntrinsicKeyword() {
        return intrinsicKeyword;
    }

    /**
     * Sets the intrinsic keyword.
     * 
     * @param intrinsicKeyword0
     *            the intrinsicKeyword to set
     */
    public final void setIntrinsicKeyword(final ArrayList<String> intrinsicKeyword0) {
        this.intrinsicKeyword = intrinsicKeyword0; // TODO: Add 0 to parameter's
                                                   // name.
    }

    /**
     * Gets the spell ability.
     * 
     * @return the spellAbility
     */
    public final ArrayList<SpellAbility> getSpellAbility() {
        return spellAbility;
    }

    /**
     * Sets the spell ability.
     * 
     * @param spellAbility0
     *            the spellAbility to set
     */
    public final void setSpellAbility(final ArrayList<SpellAbility> spellAbility0) {
        this.spellAbility = spellAbility0; // TODO: Add 0 to parameter's name.
    }

    /**
     * Gets the intrinsic ability.
     * 
     * @return the intrinsicAbility
     */
    public final ArrayList<String> getIntrinsicAbility() {
        return intrinsicAbility;
    }

    /**
     * Sets the intrinsic ability.
     * 
     * @param intrinsicAbility0
     *            the intrinsicAbility to set
     */
    public final void setIntrinsicAbility(final ArrayList<String> intrinsicAbility0) {
        this.intrinsicAbility = intrinsicAbility0; // TODO: Add 0 to parameter's
                                                   // name.
    }

    /**
     * Gets the mana ability.
     * 
     * @return the manaAbility
     */
    public final ArrayList<AbilityMana> getManaAbility() {
        return manaAbility;
    }

    /**
     * Sets the mana ability.
     * 
     * @param manaAbility0
     *            the manaAbility to set
     */
    public final void setManaAbility(final ArrayList<AbilityMana> manaAbility0) {
        this.manaAbility = manaAbility0; // TODO: Add 0 to parameter's name.
    }

    /**
     * Gets the triggers.
     * 
     * @return the triggers
     */
    public final ArrayList<Trigger> getTriggers() {
        return triggers;
    }

    /**
     * Sets the triggers.
     * 
     * @param triggers0
     *            the triggers to set
     */
    public final void setTriggers(final ArrayList<Trigger> triggers0) {
        this.triggers = triggers0; // TODO: Add 0 to parameter's name.
    }

    /**
     * Gets the static abilities.
     * 
     * @return the staticAbilities
     */
    public final ArrayList<StaticAbility> getStaticAbilities() {
        return staticAbilities;
    }

    /**
     * Sets the static abilities.
     * 
     * @param staticAbilities0
     *            the staticAbilities to set
     */
    public final void setStaticAbilities(final ArrayList<StaticAbility> staticAbilities0) {
        this.staticAbilities = new ArrayList<StaticAbility>(staticAbilities0); // TODO:
                                                                               // Add
                                                                               // 0
                                                                               // to
                                                                               // parameter's
                                                                               // name.
    }

    /**
     * Gets the image filename.
     * 
     * @return the imageFilename
     */
    public final String getImageFilename() {
        return imageFilename;
    }

    /**
     * Sets the image filename.
     * 
     * @param imageFilename0
     *            the imageFilename to set
     */
    public final void setImageFilename(final String imageFilename0) {
        imageFilename = imageFilename0; // TODO: Add 0 to parameter's name.
    }

    /**
     * Gets the sets.
     * 
     * @return the sets
     */
    public final ArrayList<SetInfo> getSets() {
        return sets;
    }

    /**
     * Sets the sets.
     * 
     * @param sets0
     *            the sets to set
     */
    public final void setSets(final ArrayList<SetInfo> sets0) {
        sets = new ArrayList<SetInfo>(sets0); // TODO: Add 0 to parameter's
                                              // name.
    }

    /**
     * Gets the static ability strings.
     * 
     * @return the staticAbilityStrings
     */
    public final ArrayList<String> getStaticAbilityStrings() {
        return staticAbilityStrings;
    }

    /**
     * Sets the static ability strings.
     * 
     * @param staticAbilityStrings0
     *            the staticAbilityStrings to set
     */
    public final void setStaticAbilityStrings(final ArrayList<String> staticAbilityStrings0) {
        this.staticAbilityStrings = staticAbilityStrings0; // TODO: Add 0 to
                                                           // parameter's name.
    }

    /**
     * @return the imageName
     */
    public String getImageName() {
        return imageName;
    }

    /**
     * @param imageName0 the imageName to set
     */
    public void setImageName(String imageName0) {
        this.imageName = imageName0; // TODO: Add 0 to parameter's name.
    }
}
