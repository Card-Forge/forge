package forge.game.card;


import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.game.GameEntity;
import forge.game.player.Player;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class CardDamageHistory {

    private boolean creatureAttackedThisCombat = false;
    private boolean creatureBlockedThisCombat = false;
    private boolean creatureGotBlockedThisCombat = false;
    private boolean receivedNonCombatDamageThisTurn = false;
    private List<GameEntity> attackedThisTurn = Lists.newArrayList();

    private final List<Player> creatureAttackedLastTurnOf = Lists.newArrayList();
    private final List<Player> NotAttackedSinceLastUpkeepOf = Lists.newArrayList();
    private final List<Player> NotBlockedSinceLastUpkeepOf = Lists.newArrayList();
    private final List<Player> NotBeenBlockedSinceLastUpkeepOf = Lists.newArrayList();

    private final Map<GameEntity, Integer> damagedThisCombat = Maps.newHashMap();
    private final Map<GameEntity, Integer> damagedThisTurn = Maps.newHashMap();
    private final Map<GameEntity, Integer> damagedThisTurnInCombat = Maps.newHashMap();
    private final Map<GameEntity, Integer> damagedThisGame = Maps.newHashMap();

    public final boolean getHasdealtDamagetoAny() {
        return !damagedThisGame.isEmpty();
    }

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
    public final void setCreatureAttackedThisCombat(GameEntity defender) {
        this.creatureAttackedThisCombat = defender != null;

        if (defender != null) {
            attackedThisTurn.add(defender);
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
     * Getter for the field <code>attacksThisTurn</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getCreatureAttacksThisTurn() {
        return this.attackedThisTurn.size();
    }
    public final boolean hasAttackedThisTurn(GameEntity e) {
        return this.attackedThisTurn.contains(e);
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
     * Setter for the field <code>creatureGotBlockedThisCombat</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setCreatureGotBlockedThisCombat(final boolean b) {
        this.creatureGotBlockedThisCombat = b;
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
     * Getter for the field <code>receivedNonCombatDamageThisTurn</code>.
     * </p>
     *
     * @return a boolean.
     */
    public boolean hasBeenDealtNonCombatDamageThisTurn() {
        return this.receivedNonCombatDamageThisTurn;
    }

    /**
     * <p>
     * Setter for the field <code>receivedNonCombatDamageThisTurn</code>.
     * </p>
     *
     * @param b
     *            a boolean.
     */
    public void setHasBeenDealtNonCombatDamageThisTurn(boolean b) {
        this.receivedNonCombatDamageThisTurn = b;
    }

    public final Map<GameEntity, Integer> getThisCombatDamaged() {
        return damagedThisCombat;
    }
    public final Map<GameEntity, Integer> getThisTurnDamaged() {
        return damagedThisTurn;
    }
    public final Map<GameEntity, Integer> getThisTurnCombatDamaged() {
        return damagedThisTurnInCombat;
    }
    public final Map<GameEntity, Integer> getThisGameDamaged() {
        return damagedThisGame;
    }
    /**
     * TODO: Write javadoc for this method.
     * @param player
     */
    public void registerCombatDamage(GameEntity entity, int amount) {
        int old = 0;
        if (damagedThisCombat.containsKey(entity)) {
            old = damagedThisCombat.get(entity);
        }
        damagedThisCombat.put(entity, old + amount);
        old = 0;
        if (damagedThisTurnInCombat.containsKey(entity)) {
            old = damagedThisTurnInCombat.get(entity);
        }
        damagedThisTurnInCombat.put(entity, old + amount);
    }
    /**
     * TODO: Write javadoc for this method.
     */
    public void newTurn() {
        damagedThisCombat.clear();
        damagedThisTurnInCombat.clear();
        damagedThisTurn.clear();
        attackedThisTurn.clear();
        setHasBeenDealtNonCombatDamageThisTurn(false);
    }

    public void endCombat() {
        damagedThisCombat.clear();
    }

    /**
     * TODO: Write javadoc for this method.
     * @param player
     */
    public void registerDamage(GameEntity entity, int amount) {
        int old = 0;
        if (damagedThisTurn.containsKey(entity)) {
            old = damagedThisTurn.get(entity);
        }
        damagedThisTurn.put(entity, old + amount);
        old = 0;
        if (damagedThisGame.containsKey(entity)) {
            old = damagedThisGame.get(entity);
        }
        damagedThisGame.put(entity, old + amount);
    }

}
