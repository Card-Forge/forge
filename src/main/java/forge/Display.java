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

/**
 * <p>
 * Display interface.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public interface Display {
    /**
     * <p>
     * showMessage.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     */
    void showMessage(String s);

    /**
     * <p>
     * getButtonOK.
     * </p>
     * 
     * @return a {@link forge.MyButton} object.
     */
    MyButton getButtonOK();

    /**
     * <p>
     * getButtonCancel.
     * </p>
     * 
     * @return a {@link forge.MyButton} object.
     */
    MyButton getButtonCancel();

    // public void showStatus(String message);
    /**
     * <p>
     * showCombat.
     * </p>
     * 
     * @param message
     *            a {@link java.lang.String} object.
     */
    void showCombat(String message);

    /**
     * <p>
     * setVisible.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    void setVisible(boolean b);

    // assigns combat damage, used by Combat.setAssignedDamage()
    /**
     * <p>
     * assignDamage.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @param blockers
     *            a {@link forge.CardList} object.
     * @param damage
     *            a int.
     */
    void assignDamage(Card attacker, CardList blockers, int damage);

    // public void addAssignDamage(Card attacker, Card blocker, int damage);
    // public void addAssignDamage(Card attacker, int damage);

    /**
     * <p>
     * stopAtPhase.
     * </p>
     * 
     * @param turn
     *            a {@link forge.Player} object.
     * @param phase
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    boolean stopAtPhase(Player turn, String phase);

    /**
     * <p>
     * loadPrefs.
     * </p>
     * 
     * @return a boolean.
     */
    boolean loadPrefs();

    /**
     * <p>
     * savePrefs.
     * </p>
     * 
     * @return a boolean.
     */
    boolean savePrefs();

    /**
     * <p>
     * canLoseByDecking.
     * </p>
     * 
     * @return a boolean.
     */
    boolean canLoseByDecking();

    /**
     * <p>
     * setCard.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    void setCard(Card c);
}
