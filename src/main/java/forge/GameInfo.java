package forge;

import forge.card.mana.ManaCost;

import java.util.ArrayList;

/**
 * <p>GameInfo class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class GameInfo {
    private boolean computerStartedThisGame = false;

    private int humanNumberOfTimesMulliganed;
    private boolean humanMulliganedToZero;

    private boolean preventCombatDamageThisTurn;
    private boolean assignedFirstStrikeDamageThisCombat;
    private boolean resolvedFirstStrikeDamageThisCombat;

    private ArrayList<Card_Color> globalColorChanges = new ArrayList<Card_Color>();

    /**
     * <p>Getter for the field <code>humanNumberOfTimesMulliganed</code>.</p>
     *
     * @return a int.
     */
    public int getHumanNumberOfTimesMulliganed() {
        return humanNumberOfTimesMulliganed;
    }

    /**
     * <p>addHumanNumberOfTimesMulliganed.</p>
     *
     * @param n a int.
     */
    public void addHumanNumberOfTimesMulliganed(int n) {
        humanNumberOfTimesMulliganed += n;
    }

    /**
     * <p>Setter for the field <code>humanNumberOfTimesMulliganed</code>.</p>
     *
     * @param n a int.
     */
    public void setHumanNumberOfTimesMulliganed(int n) {
        humanNumberOfTimesMulliganed = n;
    }

    /**
     * <p>Getter for the field <code>humanMulliganedToZero</code>.</p>
     *
     * @return a boolean.
     */
    public boolean getHumanMulliganedToZero() {
        return humanMulliganedToZero;
    }

    /**
     * <p>Setter for the field <code>humanMulliganedToZero</code>.</p>
     *
     * @param b a boolean.
     */
    public void setHumanMulliganedToZero(boolean b) {
        humanMulliganedToZero = b;
    }

    /**
     * <p>Setter for the field <code>preventCombatDamageThisTurn</code>.</p>
     *
     * @param b a boolean.
     */
    public void setPreventCombatDamageThisTurn(boolean b) {
        preventCombatDamageThisTurn = b;
    }

    /**
     * <p>isPreventCombatDamageThisTurn.</p>
     *
     * @return a boolean.
     */
    public boolean isPreventCombatDamageThisTurn() {
        return preventCombatDamageThisTurn;
    }

    /**
     * <p>Setter for the field <code>assignedFirstStrikeDamageThisCombat</code>.</p>
     *
     * @param b a boolean.
     */
    public void setAssignedFirstStrikeDamageThisCombat(boolean b) {
        assignedFirstStrikeDamageThisCombat = b;
    }

    /**
     * <p>Getter for the field <code>assignedFirstStrikeDamageThisCombat</code>.</p>
     *
     * @return a boolean.
     */
    public boolean getAssignedFirstStrikeDamageThisCombat() {
        return assignedFirstStrikeDamageThisCombat;
    }

    /**
     * <p>Setter for the field <code>resolvedFirstStrikeDamageThisCombat</code>.</p>
     *
     * @param b a boolean.
     */
    public void setResolvedFirstStrikeDamageThisCombat(boolean b) {
        resolvedFirstStrikeDamageThisCombat = b;
    }

    /**
     * <p>Getter for the field <code>resolvedFirstStrikeDamageThisCombat</code>.</p>
     *
     * @return a boolean.
     */
    public boolean getResolvedFirstStrikeDamageThisCombat() {
        return resolvedFirstStrikeDamageThisCombat;
    }

    /**
     * <p>Setter for the field <code>computerStartedThisGame</code>.</p>
     *
     * @param computerStartedThisGame a boolean.
     */
    public void setComputerStartedThisGame(boolean computerStartedThisGame) {
        this.computerStartedThisGame = computerStartedThisGame;
    }

    /**
     * <p>isComputerStartedThisGame.</p>
     *
     * @return a boolean.
     */
    public boolean isComputerStartedThisGame() {
        return computerStartedThisGame;
    }

    /**
     * <p>addColorChanges.</p>
     *
     * @param s a {@link java.lang.String} object.
     * @param c a {@link forge.Card} object.
     * @param addToColors a boolean.
     * @param bIncrease a boolean.
     * @return a long.
     */
    public long addColorChanges(String s, Card c, boolean addToColors, boolean bIncrease) {
        if (bIncrease)
            Card_Color.increaseTimestamp();
        globalColorChanges.add(new Card_Color(new ManaCost(s), c, addToColors, false));
        return Card_Color.getTimestamp();
    }

    /**
     * <p>removeColorChanges.</p>
     *
     * @param s a {@link java.lang.String} object.
     * @param c a {@link forge.Card} object.
     * @param addTo a boolean.
     * @param timestamp a long.
     */
    public void removeColorChanges(String s, Card c, boolean addTo, long timestamp) {
        Card_Color removeCol = null;
        for (Card_Color cc : globalColorChanges)
            if (cc.equals(s, c, addTo, timestamp))
                removeCol = cc;

        if (removeCol != null)
            globalColorChanges.remove(removeCol);
    }

    /**
     * <p>clearColorChanges.</p>
     */
    public void clearColorChanges() {
        // clear the global color changes at end of each game
        globalColorChanges.clear();
    }

    /**
     * <p>getColorChanges.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<Card_Color> getColorChanges() {
        return globalColorChanges;
    }


}
