/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge;

import java.util.ArrayList;

import forge.card.spellability.SpellAbility;
import forge.game.player.Player;
import forge.util.MyObservable;

/**
 * <p>
 * Abstract Player class.
 * </p>
 * 
 * @author Forge
 * @version $Id: Player.java 10091 2011-08-30 16:11:21Z Sloth $
 */
public abstract class GameEntity extends MyObservable {
    private String name = "";
    private int preventNextDamage = 0;

    /** The enchanted by. */
    private ArrayList<Card> enchantedBy = new ArrayList<Card>();

    /**
     * <p>
     * Getter for the field <code>name</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return this.name;
    }

    /**
     * <p>
     * Setter for the field <code>name</code>.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     */
    public void setName(final String s) {
        this.name = s;
    }

    // ////////////////////////
    //
    // methods for handling damage
    //
    // ////////////////////////

    /**
     * <p>
     * addDamage.
     * </p>
     * 
     * @param damage
     *            a int.
     * @param source
     *            a {@link forge.Card} object.
     * @return whether or not damage was dealt
     */
    public boolean addDamage(final int damage, final Card source) {
        int damageToDo = damage;

        damageToDo = this.replaceDamage(damageToDo, source, false);
        damageToDo = this.preventDamage(damageToDo, source, false);

        return this.addDamageAfterPrevention(damageToDo, source, false);
    }

    /**
     * <p>
     * addDamageWithoutPrevention.
     * </p>
     * 
     * @param damage
     *            a int.
     * @param source
     *            a {@link forge.Card} object.
     * @return whether or not damage was dealt
     */
    public boolean addDamageWithoutPrevention(final int damage, final Card source) {
        int damageToDo = damage;

        damageToDo = this.replaceDamage(damageToDo, source, false);

        return this.addDamageAfterPrevention(damageToDo, source, false);
    }

    // This function handles damage after replacement and prevention effects are
    // applied
    /**
     * <p>
     * addDamageAfterPrevention.
     * </p>
     * 
     * @param damage
     *            a int.
     * @param source
     *            a {@link forge.Card} object.
     * @param isCombat
     *            a boolean.
     * @return whether or not damage was dealt
     */
    public abstract boolean addDamageAfterPrevention(final int damage, final Card source, final boolean isCombat);


    // This should be also usable by the AI to forecast an effect (so it must
    // not change the game state)
    /**
     * <p>
     * staticDamagePrevention.
     * </p>
     * 
     * @param damage
     *            a int.
     * @param source
     *            a {@link forge.Card} object.
     * @param isCombat
     *            a boolean.
     * @return a int.
     */
    public int staticDamagePrevention(final int damage, final Card source, final boolean isCombat) {
        return 0;
    }

    // This should be also usable by the AI to forecast an effect (so it must
    // not change the game state)
    /**
     * <p>
     * staticReplaceDamage.
     * </p>
     * 
     * @param damage
     *            a int.
     * @param source
     *            a {@link forge.Card} object.
     * @param isCombat
     *            a boolean.
     * @return a int.
     */
    public int staticReplaceDamage(final int damage, final Card source, final boolean isCombat) {
        return 0;
    }

    /**
     * <p>
     * replaceDamage.
     * </p>
     * 
     * @param damage
     *            a int.
     * @param source
     *            a {@link forge.Card} object.
     * @param isCombat
     *            a boolean.
     * @return a int.
     */
    public abstract int replaceDamage(final int damage, final Card source, final boolean isCombat);

    /**
     * <p>
     * preventDamage.
     * </p>
     * 
     * @param damage
     *            a int.
     * @param source
     *            a {@link forge.Card} object.
     * @param isCombat
     *            a boolean.
     * @return a int.
     */
    public abstract int preventDamage(final int damage, final Card source, final boolean isCombat);

    // ////////////////////////
    //
    // methods for handling Damage Prevention
    //
    // ////////////////////////

    // PreventNextDamage
    /**
     * <p>
     * Setter for the field <code>preventNextDamage</code>.
     * </p>
     * 
     * @param n
     *            a int.
     */
    public void setPreventNextDamage(final int n) {
        this.preventNextDamage = n;
    }

    /**
     * <p>
     * Getter for the field <code>preventNextDamage</code>.
     * </p>
     * 
     * @return a int.
     */
    public int getPreventNextDamage() {
        return this.preventNextDamage;
    }

    /**
     * <p>
     * addPreventNextDamage.
     * </p>
     * 
     * @param n
     *            a int.
     */
    public void addPreventNextDamage(final int n) {
        this.preventNextDamage += n;
    }

    /**
     * <p>
     * subtractPreventNextDamage.
     * </p>
     * 
     * @param n
     *            a int.
     */
    public void subtractPreventNextDamage(final int n) {
        this.preventNextDamage -= n;
    }

    /**
     * <p>
     * resetPreventNextDamage.
     * </p>
     */
    public void resetPreventNextDamage() {
        this.preventNextDamage = 0;
    }

    /**
     * Checks for keyword.
     * 
     * @param keyword
     *            the keyword
     * @return true, if successful
     */
    public boolean hasKeyword(final String keyword) {
        return false;
    }

    /**
     * Can target.
     * 
     * @param sa
     *            the sa
     * @return a boolean
     */
    public boolean canBeTargetedBy(final SpellAbility sa) {
        return false;
    }

    /**
     * Checks if is valid.
     * 
     * @param restrictions
     *            the restrictions
     * @param sourceController
     *            the source controller
     * @param source
     *            the source
     * @return true, if is valid
     */
    public boolean isValid(final String[] restrictions, final Player sourceController, final Card source) {

        for (final String restriction : restrictions) {
            if (this.isValid(restriction, sourceController, source)) {
                return true;
            }
        }
        return false;

    } // isValid

    /**
     * Checks if is valid.
     * 
     * @param restriction
     *            the restriction
     * @param sourceController
     *            the source controller
     * @param source
     *            the source
     * @return true, if is valid
     */
    public boolean isValid(final String restriction, final Player sourceController, final Card source) {
        return false;
    }

    /**
     * Checks for property.
     * 
     * @param property
     *            the property
     * @param sourceController
     *            the source controller
     * @param source
     *            the source
     * @return true, if successful
     */
    public boolean hasProperty(final String property, final Player sourceController, final Card source) {
        return false;
    }

    // GameEntities can now be Enchanted
    /**
     * <p>
     * Getter for the field <code>enchantedBy</code>.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<Card> getEnchantedBy() {
        return this.enchantedBy;
    }

    /**
     * <p>
     * Setter for the field <code>enchantedBy</code>.
     * </p>
     * 
     * @param list
     *            a {@link java.util.ArrayList} object.
     */
    public final void setEnchantedBy(final ArrayList<Card> list) {
        this.enchantedBy = list;
    }

    /**
     * <p>
     * isEnchanted.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isEnchanted() {
        return this.enchantedBy.size() != 0;
    }

    /**
     * <p>
     * addEnchantedBy.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void addEnchantedBy(final Card c) {
        this.enchantedBy.add(c);
        this.updateObservers();
    }

    /**
     * <p>
     * removeEnchantedBy.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void removeEnchantedBy(final Card c) {
        this.enchantedBy.remove(c);
        this.updateObservers();
    }

    /**
     * <p>
     * unEnchantAllCards.
     * </p>
     */
    public final void unEnchantAllCards() {
        for (int i = 0; i < this.enchantedBy.size(); i++) {
            this.enchantedBy.get(i).unEnchantEntity(this);
        }
    }

    /**
     * 
     * hasProtectionFrom.
     * 
     * @param source
     *            Card
     * @return boolean
     */
    public boolean hasProtectionFrom(final Card source) {
        return false;
    }

    // //////////////////////////////
    //
    // generic Object overrides
    //
    // ///////////////////////////////

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.name;
    }
}
