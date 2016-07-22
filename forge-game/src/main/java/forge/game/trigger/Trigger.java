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
package forge.game.trigger;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.TriggerReplacementBase;
import forge.game.card.Card;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.Ability;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.OptionalCost;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.*;

/**
 * <p>
 * Abstract Trigger class. Constructed by reflection only
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class Trigger extends TriggerReplacementBase {

    /** Constant <code>nextID=0</code>. */
    private static int nextID = 0;

    /**
     * <p>
     * resetIDs.
     * </p>
     */
    public static void resetIDs() {
        Trigger.nextID = 50000;
    }

    /** The ID. */
    private int id = Trigger.nextID++;

    /**
     * <p>
     * setID.
     * </p>
     * 
     * @param id
     *            a int.
     */
    public final void setID(final int id) {
        this.id = id;
    }

    /** The run params. */
    private Map<String, Object> runParams;

    private TriggerType mode;

    private HashMap<String, Object> storedTriggeredObjects = null;
    
    private List<Object> triggerRemembered = new ArrayList<Object>();

    // number of times this trigger was activated this this turn
    // used to handle once-per-turn triggers like Crawling Sensation
    private int numberTurnActivations = 0;

    /**
     * <p>
     * Setter for the field <code>storedTriggeredObjects</code>.
     * </p>
     * 
     * @param storedTriggeredObjects
     *            a {@link java.util.HashMap} object.
     * @since 1.0.15
     */
    public final void setStoredTriggeredObjects(final HashMap<String, Object> storedTriggeredObjects) {
        this.storedTriggeredObjects = storedTriggeredObjects;
    }

    /**
     * <p>
     * Getter for the field <code>storedTriggeredObjects</code>.
     * </p>
     * 
     * @return a {@link java.util.HashMap} object.
     * @since 1.0.15
     */
    public final HashMap<String, Object> getStoredTriggeredObjects() {
        return this.storedTriggeredObjects;
    }


    private List<PhaseType> validPhases;

    /**
     * <p>
     * Constructor for Trigger.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public Trigger(final Map<String, String> params, final Card host, final boolean intrinsic) {
        this.intrinsic = intrinsic;

        this.setRunParams(new HashMap<String, Object>());
        this.originalMapParams.putAll(params);
        this.mapParams.putAll(params);
        this.setHostCard(host);
    }

    /**
     * <p>
     * toString.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    @Override
    public final String toString() {
        if (this.mapParams.containsKey("TriggerDescription") && !this.isSuppressed()) {
            StringBuilder sb = new StringBuilder();
            sb.append(this.mapParams.get("TriggerDescription").replace("CARDNAME", this.getHostCard().getName()));
            if (!this.triggerRemembered.isEmpty()) {
                sb.append(" (").append(this.triggerRemembered).append(")");
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    /**
     * <p>
     * phasesCheck.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean phasesCheck(final Game game) {
        PhaseHandler phaseHandler = game.getPhaseHandler();
        if (null != validPhases) {
            if (!validPhases.contains(phaseHandler.getPhase())) {
                return false;
            }
        }

        if (this.mapParams.containsKey("PlayerTurn")) {
            if (!phaseHandler.isPlayerTurn(this.getHostCard().getController())) {
                return false;
            }
        }

        if (this.mapParams.containsKey("NotPlayerTurn")) {
            if (phaseHandler.isPlayerTurn(this.getHostCard().getController())) {
                return false;
            }
        }

        if (this.mapParams.containsKey("OpponentTurn")) {
            if (!this.getHostCard().getController().isOpponentOf(phaseHandler.getPlayerTurn())) {
                return false;
            }
        }

        if (this.mapParams.containsKey("FirstUpkeep")) {
            if (!phaseHandler.isFirstUpkeep()) {
                return false;
            }
        }

        if (this.mapParams.containsKey("FirstUpkeepThisGame")) {
            if (!phaseHandler.isFirstUpkeepThisGame()) {
                return false;
            }
        }

        if (this.mapParams.containsKey("FirstCombat")) {
            if (!phaseHandler.isFirstCombat()) {
                return false;
            }
        }

        return true;
    }
    /**
     * <p>
     * requirementsCheck.
     * </p>
     * @param game 
     *
     * @return a boolean.
     */
    public final boolean requirementsCheck(Game game) {

        if (this.mapParams.containsKey("APlayerHasMoreLifeThanEachOther")) {
            int highestLife = Integer.MIN_VALUE; // Negative base just in case a few Lich's or Platinum Angels are running around
            final List<Player> healthiest = new ArrayList<Player>();
            for (final Player p : game.getPlayers()) {
                if (p.getLife() > highestLife) {
                    healthiest.clear();
                    highestLife = p.getLife();
                    healthiest.add(p);
                } else if (p.getLife() == highestLife) {
                    highestLife = p.getLife();
                    healthiest.add(p);
                }
            }

            if (healthiest.size() != 1) {
                // More than one player tied for most life
                return false;
            }
        }

        if (this.mapParams.containsKey("APlayerHasMostCardsInHand")) {
            int largestHand = 0;
            final List<Player> withLargestHand = new ArrayList<Player>();
            for (final Player p : game.getPlayers()) {
                if (p.getCardsIn(ZoneType.Hand).size() > largestHand) {
                    withLargestHand.clear();
                    largestHand = p.getCardsIn(ZoneType.Hand).size();
                    withLargestHand.add(p);
                } else if (p.getCardsIn(ZoneType.Hand).size() == largestHand) {
                    largestHand = p.getCardsIn(ZoneType.Hand).size();
                    withLargestHand.add(p);
                }
            }

            if (withLargestHand.size() != 1) {
                // More than one player tied for most life
                return false;
            }
        }
        
        if ( !meetsCommonRequirements(this.mapParams))
            return false;

        return true;
    }


    public boolean meetsRequirementsOnTriggeredObjects(Game game,  Map<String, Object> runParams) {
        if ("True".equals(this.mapParams.get("EvolveCondition"))) {
            final Card moved = (Card) runParams.get("Card");
            if (moved == null) {
                return false;
                // final StringBuilder sb = new StringBuilder();
                // sb.append("Trigger::requirementsCheck() - EvolveCondition condition being checked without a moved card. ");
                // sb.append(this.getHostCard().getName());
                // throw new RuntimeException(sb.toString());
            }
            if (moved.getNetPower() <= this.getHostCard().getNetPower()
                    && moved.getNetToughness() <= this.getHostCard().getNetToughness()) {
                return false;
            }
        }

        String condition = this.mapParams.get("Condition");
        if ("AltCost".equals(condition)) {
            final Card moved = (Card) runParams.get("Card");
            if( null != moved && !moved.isOptionalCostPaid(OptionalCost.AltCost))
                return false;
        } else if ("AttackedPlayerWithMostLife".equals(condition)) {
            GameEntity attacked = (GameEntity) runParams.get("Attacked");
            if (attacked == null || !attacked.isValid("Player.withMostLife",
                    this.getHostCard().getController(), this.getHostCard(), null)) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>
     * isSecondary.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isSecondary() {
        if (this.mapParams.containsKey("Secondary")) {
            if (this.mapParams.get("Secondary").equals("True")) {
                return true;
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean equals(final Object o) {
        if (!(o instanceof Trigger)) {
            return false;
        }

        return this.getId() == ((Trigger) o).getId();
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return 41 * (41 + this.getId());
    }

    /**
     * <p>
     * performTest.
     * </p>
     * 
     * @param runParams2
     *            a {@link java.util.HashMap} object.
     * @return a boolean.
     */
    public abstract boolean performTest(java.util.Map<String, Object> runParams2);

    /**
     * <p>
     * setTriggeringObjects.
     * </p>
     * 
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     */
    public abstract void setTriggeringObjects(SpellAbility sa);

    /**
     * Gets the run params.
     * 
     * @return the runParams
     */
    public Map<String, Object> getRunParams() {
        return this.runParams;
    }

    /**
     * Sets the run params.
     * 
     * @param runParams0
     *            the runParams to set
     */
    public void setRunParams(final Map<String, Object> runParams0) {
        this.runParams = runParams0;
    }

    /**
     * Gets the id.
     * 
     * @return the id
     */
    public int getId() {
        return this.id;
    }

    private Ability triggeredSA;

    /**
     * Gets the triggered sa.
     * 
     * @return the triggered sa
     */
    public final Ability getTriggeredSA() {
        return this.triggeredSA;
    }

    /**
     * Sets the triggered sa.
     * 
     * @param sa
     *            the triggered sa to set
     */
    public void setTriggeredSA(final Ability sa) {
        this.triggeredSA = sa;
    }

    public void addRemembered(Object o) {
        this.triggerRemembered.add(o);
    }
    
    public List<Object> getTriggerRemembered() {
        return this.triggerRemembered;
    }

    /**
     * TODO: Write javadoc for this method.
     * @return the mode
     */
    public TriggerType getMode() {
        return mode;
    }

    /**
     * 
     * @param triggerType
     *            the triggerType to set
     * @param triggerType
     */
    void setMode(TriggerType triggerType) {
        mode = triggerType;
    }
    

    public final Trigger getCopyForHostCard(Card newHost) {
        final TriggerType tt = TriggerType.getTypeFor(this);
        final Trigger copy = tt.createTrigger(originalMapParams, newHost, intrinsic); 

        if (this.getOverridingAbility() != null) {
            SpellAbility old = this.getOverridingAbility();
            SpellAbility sa = old;
            // try to copy it if newHost is not the wanted host
            if (!newHost.equals(old.getHostCard())) {
	            if (old instanceof AbilitySub) {
	                sa = ((AbilitySub)old).getCopy();
	                sa.setHostCard(newHost);
	            }
            }
            copy.setOverridingAbility(sa);
        }

        // 2015-03-07 Removing the ID copying which makes copied triggers Identical to each other when removing
        //copy.setID(this.getId());
        copy.setMode(this.getMode());
        copy.setTriggerPhases(this.validPhases);
        copy.setActiveZone(validHostZones);
        copy.setTemporary(isTemporary());
        return copy;
    }

    public boolean isStatic() {
        return this.mapParams.containsKey("Static"); // && params.get("Static").equals("True") [always true if present]
    }

    public void setTriggerPhases(List<PhaseType> phases) {
        validPhases = phases;
    }

    //public String getImportantStackObjects(SpellAbility sa) { return ""; };
    abstract public String getImportantStackObjects(SpellAbility sa);

    public int getActivationsThisTurn() {
        return this.numberTurnActivations;
    }

    public void triggerRun()
    {
        this.numberTurnActivations++;
    }

    // Resets the state stored each turn for per-turn and per-instance restriction
    public void resetTurnState()
    {
        this.numberTurnActivations = 0;
    }
}
