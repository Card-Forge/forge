package forge.game.card;


import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;

import forge.game.CardTraitBase;
import forge.game.GameEntity;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollection;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class CardDamageHistory {

    private int attacksThisGame = 0;
    // amount only needed for Kytheon
    private int creatureAttackedThisCombat = 0;
    private boolean creatureBlockedThisCombat = false;
    private boolean creatureGotBlockedThisCombat = false;

    private List<GameEntity> attackedThisTurn = Lists.newArrayList();
    private boolean attackedBattleThisTurn = false;

    private final List<Player> creatureAttackedLastTurnOf = Lists.newArrayList();
    private final List<Player> NotAttackedSinceLastUpkeepOf = Lists.newArrayList();
    private final List<Player> NotBlockedSinceLastUpkeepOf = Lists.newArrayList();
    private final List<Player> NotBeenBlockedSinceLastUpkeepOf = Lists.newArrayList();

    private List<Pair<Integer, Boolean>> damageDoneThisTurn = Lists.newArrayList();

    // only needed for Glen Elendra (Plane)
    private final List<Player> damagedThisCombat = Lists.newArrayList();
    // only needed for The Fallen
    private final FCollection<GameEntity> damagedThisGame = new FCollection<>();
    boolean hasdealtDamagetoAny = false;

    public final boolean getHasdealtDamagetoAny() {
        return hasdealtDamagetoAny;
    }

    // used to see if an attacking creature with a triggering attack ability
    // triggered this phase:
    /**
     * <p>
     * Setter for the field <code>creatureAttackedThisCombat</code>.
     * </p>
     *
     */
    public final void setCreatureAttackedThisCombat(GameEntity defender, int numOtherAttackers) {
        this.creatureAttackedThisCombat = 1 + numOtherAttackers;

        if (defender != null) {
            this.attacksThisGame++;
            attackedThisTurn.add(defender);
            if (defender instanceof Card) {
                final Card def = (Card) defender;
                if (def.isBattle()) {
                    attackedBattleThisTurn = true;
                }
            }
        }
    }
    /**
     * <p>
     * Getter for the field <code>creatureAttackedThisCombat</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final int getCreatureAttackedThisCombat() {
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
    public final boolean hasAttackedBattleThisTurn() {
        return this.attackedBattleThisTurn;
    }

    public final int getAttacksThisGame() {
        return this.attacksThisGame;
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

    public final List<Player> getThisCombatDamaged() {
        return damagedThisCombat;
    }
    public final FCollection<GameEntity> getThisGameDamaged() {
        return damagedThisGame;
    }

    public void registerDamage(int damage, boolean isCombat, Card sourceLKI, GameEntity target, Map<Integer, Card> lkiCache) {
        if (damage <= 0) {
            return;
        }
        damagedThisGame.add(target);
        hasdealtDamagetoAny = true;
        if (isCombat && target instanceof Player) {
            final Player pTgt = (Player) target;
            damagedThisCombat.add(pTgt);
            if (pTgt.getLastTurnNr() > 0 && !pTgt.getGame().getPhaseHandler().isPlayerTurn(pTgt)) {
                pTgt.setBeenDealtCombatDamageSinceLastTurn(true);
            }
        }
        Pair<Integer, Boolean> dmg = Pair.of(damage, isCombat);
        damageDoneThisTurn.add(dmg);
        target.receiveDamage(dmg);

        sourceLKI.getGame().addGlobalDamageHistory(this, dmg, sourceLKI.isLKI() ? sourceLKI : CardCopyService.getLKICopy(sourceLKI, lkiCache), CardCopyService.getLKICopy(target, lkiCache));
    }

    public int getDamageDoneThisTurn(Boolean isCombat, boolean anyIsEnough, String validSourceCard, String validTargetEntity, Card source, Player sourceController, CardTraitBase ctb) {
        int sum = 0;
        for (Pair<Integer, Boolean> damage : damageDoneThisTurn) {
            Pair<Card, GameEntity> sourceToTarget = sourceController.getGame().getDamageLKI(damage);

            if (isCombat != null && damage.getRight() != isCombat) {
                continue;
            }
            if (sourceToTarget != null) {
                if (validSourceCard != null && !sourceToTarget.getLeft().isValid(validSourceCard.split(","), sourceController, source == null ? sourceToTarget.getLeft() : source, ctb)) {
                    continue;
                }
                if (validTargetEntity != null && !sourceToTarget.getRight().isValid(validTargetEntity.split(","), sourceController, source, ctb)) {
                    continue;
                }
            }
            sum += damage.getLeft();
            if (anyIsEnough) {
                break;
            }
        }
        return sum;
    }

    public void newTurn() {
        attackedThisTurn.clear();
        attackedBattleThisTurn = false;
        damagedThisCombat.clear();
        damageDoneThisTurn.clear();

        // if card already LTB we can safely dereference (allows quite a few objects to be cleaned up earlier for bigger boardstates)
        CardCollection toRemove = new CardCollection();
        for (GameEntity e : damagedThisGame) {
            if (e instanceof Card) {
                if (((Card) e).getZone().getZoneType() != ZoneType.Battlefield) {
                    toRemove.add((Card)e);
                }
            }
        }
        damagedThisGame.removeAll(toRemove);
    }

    public void endCombat() {
        damagedThisCombat.clear();
        setCreatureAttackedThisCombat(null, -1);
        setCreatureBlockedThisCombat(false);
        setCreatureGotBlockedThisCombat(false);
    }
}
