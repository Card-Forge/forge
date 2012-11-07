package forge;

import java.util.ArrayList;
import java.util.List;

import forge.game.player.Player;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class CardDamageHistory {

    private boolean creatureAttackedThisTurn = false;
    private boolean creatureAttackedThisCombat = false;
    private boolean creatureBlockedThisCombat = false;
    private boolean creatureBlockedThisTurn = false;
    private boolean creatureGotBlockedThisCombat = false;
    private boolean creatureGotBlockedThisTurn = false;

    private final List<Player> creatureAttackedLastTurnOf = new ArrayList<Player>(2);
    private final List<Player> damagedThisTurn = new ArrayList<Player>(2);
    private final List<Player> damagedThisTurnInCombat = new ArrayList<Player>(2);
    private final List<Player> damagedThisGame = new ArrayList<Player>(2);
    // used to see if an attacking creature with a triggering attack ability
    // triggered this phase:
    /**
     * <p>
     * Setter for the field <code>creatureAttackedThisCombat</code>.
     * </p>
     * 
     * @param hasAttacked
     *            a boolean.
     */
    public final void setCreatureAttackedThisCombat(final boolean hasAttacked) {
        this.creatureAttackedThisCombat = hasAttacked;

        if (hasAttacked) {
            this.setCreatureAttackedThisTurn(true);
        }
    }
    /**
     * <p>
     * Getter for the field <code>creatureAttackedThisCombat</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getCreatureAttackedThisCombat() {
        return this.creatureAttackedThisCombat;
    }
    /**
     * <p>
     * Setter for the field <code>creatureAttackedThisTurn</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setCreatureAttackedThisTurn(final boolean b) {
        this.creatureAttackedThisTurn = b;
    }
    /**
     * <p>
     * Getter for the field <code>creatureAttackedThisTurn</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getCreatureAttackedThisTurn() {
        return this.creatureAttackedThisTurn;
    }
    /**
     * <p>
     * Setter for the field <code>creatureAttackedLastTurn</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setCreatureAttackedLastTurnOf(final Player p, boolean value) {
        if (value && !creatureAttackedLastTurnOf.contains(p)) {
            creatureAttackedLastTurnOf.add(p);
        }
        while (!value && creatureAttackedLastTurnOf.remove(p)) { } // remove should return false once no player is found in collection
    }
    /**
     * <p>
     * Getter for the field <code>creatureAttackedLastTurn</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getCreatureAttackedLastTurnOf(final Player p) {
        return creatureAttackedLastTurnOf.contains(p);
    }
    /**
     * <p>
     * Setter for the field <code>creatureBlockedThisCombat</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setCreatureBlockedThisCombat(final boolean b) {
        this.creatureBlockedThisCombat = b;
        if (b) {
            this.setCreatureBlockedThisTurn(true);
        }
    }
    /**
     * <p>
     * Getter for the field <code>creatureBlockedThisCombat</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getCreatureBlockedThisCombat() {
        return this.creatureBlockedThisCombat;
    }
    /**
     * <p>
     * Setter for the field <code>creatureBlockedThisTurn</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setCreatureBlockedThisTurn(final boolean b) {
        this.creatureBlockedThisTurn = b;
    }
    /**
     * <p>
     * Getter for the field <code>creatureBlockedThisTurn</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getCreatureBlockedThisTurn() {
        return this.creatureBlockedThisTurn;
    }
    /**
     * <p>
     * Setter for the field <code>creatureGotBlockedThisCombat</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setCreatureGotBlockedThisCombat(final boolean b) {
        this.creatureGotBlockedThisCombat = b;
        if (b) {
            this.setCreatureGotBlockedThisTurn(true);
        }
    }
    /**
     * <p>
     * Getter for the field <code>creatureGotBlockedThisCombat</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getCreatureGotBlockedThisCombat() {
        return this.creatureGotBlockedThisCombat;
    }
    /**
     * <p>
     * Setter for the field <code>creatureGotBlockedThisTurn</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setCreatureGotBlockedThisTurn(final boolean b) {
        this.creatureGotBlockedThisTurn = b;
    }
    /**
     * <p>
     * Getter for the field <code>creatureGotBlockedThisTurn</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getCreatureGotBlockedThisTurn() {
        return this.creatureGotBlockedThisTurn;
    }
    public final List<Player> getThisTurnDamaged() {
        return damagedThisTurn;
    }
    public final List<Player> getThisTurnCombatDamaged() {
        return damagedThisTurnInCombat;
    }
    public final List<Player> getThisGameDamaged() {
        return damagedThisGame;
    }
    /**
     * TODO: Write javadoc for this method.
     * @param player
     */
    public void registerCombatDamage(Player player) {
        if (!damagedThisTurnInCombat.contains(player)) {
            damagedThisTurnInCombat.add(player);
        }
    }
    /**
     * TODO: Write javadoc for this method.
     */
    public void newTurn() {
        damagedThisTurnInCombat.clear();
        damagedThisTurn.clear();
    }
    /**
     * TODO: Write javadoc for this method.
     * @param player
     */
    public void registerDamage(Player player) {
        if (!damagedThisTurn.contains(player)) {
            damagedThisTurn.add(player);
        }
    }

}
