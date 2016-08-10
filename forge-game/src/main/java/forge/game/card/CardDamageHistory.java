package forge.game.card;


import forge.game.GameEntity;
import forge.game.player.Player;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

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
    private int attacksThisTurn = 0;

    private final List<Player> creatureAttackedLastTurnOf = Lists.newArrayList();
    private final List<Player> NotAttackedSinceLastUpkeepOf = Lists.newArrayList();
    private final List<Player> NotBlockedSinceLastUpkeepOf = Lists.newArrayList();
    private final List<Player> NotBeenBlockedSinceLastUpkeepOf = Lists.newArrayList();

    private final List<GameEntity> damagedThisCombat = Lists.newArrayList();
    private final List<GameEntity> damagedThisTurn = Lists.newArrayList();
    private final List<GameEntity> damagedThisTurnInCombat = Lists.newArrayList();
    private final List<GameEntity> damagedThisGame = Lists.newArrayList();
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
            this.attacksThisTurn++;
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
     * Setter for the field <code>attacksThisTurn</code>.
     * </p>
     * 
     * @param num
     *            a integer.
     */
    public final void setCreatureAttacksThisTurn(final int num) {
        this.attacksThisTurn = num;
    }
    /**
     * <p>
     * Getter for the field <code>attacksThisTurn</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getCreatureAttacksThisTurn() {
        return this.attacksThisTurn;
    }
    /**
     * <p>
     * Setter for the field <code>creatureAttackedLastTurn</code>.
     * </p>
     * 
     * @param value
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
     * Setter for the field <code>NotAttackedSinceLastUpkeepOf</code>.
     * </p>
     * 
     * @param value
     *            a boolean.
     */
    public final void setNotAttackedSinceLastUpkeepOf(final Player p) {
        NotAttackedSinceLastUpkeepOf.add(p);
    }

    public final void clearNotAttackedSinceLastUpkeepOf() {
        NotAttackedSinceLastUpkeepOf.clear();
    }
    /**
     * <p>
     * Getter for the field <code>NotAttackedSinceLastUpkeepOf</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean hasAttackedSinceLastUpkeepOf(final Player p) {
        return !NotAttackedSinceLastUpkeepOf.contains(p);
    }
    /**
     * <p>
     * Setter for the field <code>NotAttackedSinceLastUpkeepOf</code>.
     * </p>
     * 
     * @param value
     *            a boolean.
     */
    public final void setNotBlockedSinceLastUpkeepOf(final Player p) {
        NotBlockedSinceLastUpkeepOf.add(p);
    }

    public final void clearNotBlockedSinceLastUpkeepOf() {
        NotBlockedSinceLastUpkeepOf.clear();
    }
    /**
     * <p>
     * Getter for the field <code>NotAttackedSinceLastUpkeepOf</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean hasBlockedSinceLastUpkeepOf(final Player p) {
        return !NotBlockedSinceLastUpkeepOf.contains(p);
    }
    /**
     * <p>
     * Setter for the field <code>NotAttackedSinceLastUpkeepOf</code>.
     * </p>
     * 
     * @param value
     *            a boolean.
     */
    public final void setNotBeenBlockedSinceLastUpkeepOf(final Player p) {
        NotBeenBlockedSinceLastUpkeepOf.add(p);
    }

    public final void clearNotBeenBlockedSinceLastUpkeepOf() {
        NotBeenBlockedSinceLastUpkeepOf.clear();
    }
    /**
     * <p>
     * Getter for the field <code>NotAttackedSinceLastUpkeepOf</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean hasBeenBlockedSinceLastUpkeepOf(final Player p) {
        return !NotBeenBlockedSinceLastUpkeepOf.contains(p);
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
    public final List<GameEntity> getThisTurnDamaged() {
        return damagedThisTurn;
    }
    public final List<GameEntity> getThisTurnCombatDamaged() {
        return damagedThisTurnInCombat;
    }
    public final List<GameEntity> getThisGameDamaged() {
        return damagedThisGame;
    }
    /**
     * TODO: Write javadoc for this method.
     * @param player
     */
    public void registerCombatDamage(GameEntity entity) {
        if (!damagedThisCombat.contains(entity)) {
            damagedThisCombat.add(entity);
        }
        if (!damagedThisTurnInCombat.contains(entity)) {
            damagedThisTurnInCombat.add(entity);
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
    public void registerDamage(GameEntity entity) {
        if (!damagedThisTurn.contains(entity)) {
            damagedThisTurn.add(entity);
        }
        if (!damagedThisGame.contains(entity)) {
            damagedThisGame.add(entity);
        }
    }

}
