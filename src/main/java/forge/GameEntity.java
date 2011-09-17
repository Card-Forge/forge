package forge;

import java.util.ArrayList;

import forge.card.spellability.SpellAbility;


/**
 * <p>Abstract Player class.</p>
 *
 * @author Forge
 * @version $Id: Player.java 10091 2011-08-30 16:11:21Z Sloth $
 */
public abstract class GameEntity extends MyObservable {
    private String name = "";
    private int preventNextDamage = 0;
    protected ArrayList<Card> enchantedBy = new ArrayList<Card>();

    /**
     * <p>Getter for the field <code>name</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return name;
    }
    

    /**
     * <p>Setter for the field <code>name</code>.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public void setName(String s) {
        name = s;
    }

    //////////////////////////
    //
    // methods for handling damage
    //
    //////////////////////////

    /**
     * <p>addDamage.</p>
     *
     * @param damage a int.
     * @param source a {@link forge.Card} object.
     */
    public void addDamage(final int damage, final Card source) {
        int damageToDo = damage;

        damageToDo = replaceDamage(damageToDo, source, false);
        damageToDo = preventDamage(damageToDo, source, false);

        addDamageAfterPrevention(damageToDo, source, false);
    }

    /**
     * <p>addDamageWithoutPrevention.</p>
     *
     * @param damage a int.
     * @param source a {@link forge.Card} object.
     */
    public void addDamageWithoutPrevention(final int damage, final Card source) {
        int damageToDo = damage;

        damageToDo = replaceDamage(damageToDo, source, false);

        addDamageAfterPrevention(damageToDo, source, false);
    }

    //This function handles damage after replacement and prevention effects are applied
    /**
     * <p>addDamageAfterPrevention.</p>
     *
     * @param damage a int.
     * @param source a {@link forge.Card} object.
     * @param isCombat a boolean.
     */
    public void addDamageAfterPrevention(final int damage, final Card source, final boolean isCombat) {
        
    }

    /**
     * <p>predictDamage.</p>
     *
     * @param damage a int.
     * @param source a {@link forge.Card} object.
     * @param isCombat a boolean.
     * @return a int.
     */
    //This function helps the AI calculate the actual amount of damage an effect would deal
    public int predictDamage(final int damage, final Card source, final boolean isCombat) {

        int restDamage = damage;

        restDamage = staticReplaceDamage(restDamage, source, isCombat);
        restDamage = staticDamagePrevention(restDamage, source, isCombat);

        return restDamage;
    }

    //This should be also usable by the AI to forecast an effect (so it must not change the game state)
    /**
     * <p>staticDamagePrevention.</p>
     *
     * @param damage a int.
     * @param source a {@link forge.Card} object.
     * @param isCombat a boolean.
     * @return a int.
     */
    public int staticDamagePrevention(final int damage, final Card source, final boolean isCombat) {
        return 0;
    }

    //This should be also usable by the AI to forecast an effect (so it must not change the game state)
    /**
     * <p>staticReplaceDamage.</p>
     *
     * @param damage a int.
     * @param source a {@link forge.Card} object.
     * @param isCombat a boolean.
     * @return a int.
     */
    public int staticReplaceDamage(final int damage, Card source, boolean isCombat) {
        return 0;
    }

    /**
     * <p>replaceDamage.</p>
     *
     * @param damage a int.
     * @param source a {@link forge.Card} object.
     * @param isCombat a boolean.
     * @return a int.
     */
    public int replaceDamage(final int damage, Card source, boolean isCombat) {
        return 0;
    }

    /**
     * <p>preventDamage.</p>
     *
     * @param damage a int.
     * @param source a {@link forge.Card} object.
     * @param isCombat a boolean.
     * @return a int.
     */
    public int preventDamage(final int damage, Card source, boolean isCombat) {
        return 0;
    }

    //////////////////////////
    //
    // methods for handling Damage Prevention
    //
    //////////////////////////

    //PreventNextDamage
    /**
     * <p>Setter for the field <code>preventNextDamage</code>.</p>
     *
     * @param n a int.
     */
    public void setPreventNextDamage(int n) {
        preventNextDamage = n;
    }

    /**
     * <p>Getter for the field <code>preventNextDamage</code>.</p>
     *
     * @return a int.
     */
    public int getPreventNextDamage() {
        return preventNextDamage;
    }

    /**
     * <p>addPreventNextDamage.</p>
     *
     * @param n a int.
     */
    public void addPreventNextDamage(int n) {
        preventNextDamage += n;
    }

    /**
     * <p>subtractPreventNextDamage.</p>
     *
     * @param n a int.
     */
    public void subtractPreventNextDamage(int n) {
        preventNextDamage -= n;
    }

    /**
     * <p>resetPreventNextDamage.</p>
     */
    public void resetPreventNextDamage() {
        preventNextDamage = 0;
    }
    
	
	public boolean hasKeyword(String keyword){
		return false;
	}

    /**
     *
     * @param sa
     * @return  a boolean
     */
    public boolean canTarget(SpellAbility sa) {    		
        return false;
    }
    
    public boolean isValid(final String Restriction, final Player sourceController, final Card source) {
        
        String incR[] = Restriction.split("\\.");
        
        if (!incR[0].equals("Player") && 
                !(incR[0].equals("Opponent") && !this.equals(sourceController)) &&
                !(incR[0].equals("You") && this.equals(sourceController)))
            return false;
        
        if (incR.length > 1) {
            final String excR = incR[1];
            String exR[] = excR.split("\\+"); // Exclusive Restrictions are ...
            for (int j = 0; j < exR.length; j++)
                if (hasProperty(exR[j], sourceController, source) == false) return false;
        }
        
        return true;
    }
    
    public boolean hasProperty(String Property, final Player sourceController, final Card source) {
        
        if (Property.equals("You")) {
            if (!this.equals(sourceController)) {
                return false;
            }
        }        
        else if (Property.equals("Opponent")) {
            if (this.equals(sourceController)) {
                return false;
            }
        }
        
        return true;
    }
    
    // GameEntities can now be Enchanted
    /**
     * <p>Getter for the field <code>enchantedBy</code>.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<Card> getEnchantedBy() {
        return enchantedBy;
    }

    /**
     * <p>Setter for the field <code>enchantedBy</code>.</p>
     *
     * @param list a {@link java.util.ArrayList} object.
     */
    public final void setEnchantedBy(final ArrayList<Card> list) {
        enchantedBy = list;
    }
    
    /**
     * <p>isEnchanted.</p>
     *
     * @return a boolean.
     */
    public final boolean isEnchanted() {
        return enchantedBy.size() != 0;
    }
    

    /**
     * <p>addEnchantedBy.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public final void addEnchantedBy(final Card c) {
        enchantedBy.add(c);
        this.updateObservers();
    }

    /**
     * <p>removeEnchantedBy.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public final void removeEnchantedBy(final Card c) {
        enchantedBy.remove(c);
        this.updateObservers();
    }
    
    /**
     * <p>unEnchantAllCards.</p>
     */
    public final void unEnchantAllCards() {
        for (int i = 0; i < enchantedBy.size(); i++) {
            enchantedBy.get(i).unEnchantEntity(this);
        }
    }


    ////////////////////////////////
    //
    // generic Object overrides
    //
    /////////////////////////////////

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return name;
    }
}
