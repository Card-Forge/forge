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
package forge.card.trigger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.Card;

import forge.CardLists;
import forge.CardUtil;
import forge.Singletons;
import forge.card.TriggerReplacementBase;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.Ability;
import forge.card.spellability.SpellAbility;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.Expressions;

/**
 * <p>
 * Abstract Trigger class.
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

    /** The name. */
    private String name;

    /**
     * <p>
     * Getter for the field <code>name</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getName() {
        return this.name;
    }

    /**
     * <p>
     * Setter for the field <code>name</code>.
     * </p>
     * 
     * @param n
     *            a {@link java.lang.String} object.
     */
    public final void setName(final String n) {
        this.name = n;
    }

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

    /** The map params. */
    private HashMap<String, String> mapParams = new HashMap<String, String>();

    /**
     * <p>
     * Getter for the field <code>mapParams</code>.
     * </p>
     * 
     * @return a {@link java.util.HashMap} object.
     */
    public final HashMap<String, String> getMapParams() {
        return this.mapParams;
    }

    /**
     * Sets the map params.
     * 
     * @param mapParams0
     *            the mapParams to set
     */
    public final void setMapParams(final HashMap<String, String> mapParams0) {
        this.mapParams = mapParams0;
    }

    /** The run params. */
    private Map<String, Object> runParams;

    private TriggerType mode;

    private HashMap<String, Object> storedTriggeredObjects = null;

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

    /** The is intrinsic. */
    private boolean isIntrinsic;

    private List<PhaseType> validPhases;

    /**
     * <p>
     * Constructor for Trigger.
     * </p>
     * 
     * @param n
     *            a {@link java.lang.String} object.
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public Trigger(final String n, final Map<String, String> params, final Card host, final boolean intrinsic) {
        this.name = n;
        this.setRunParams(new HashMap<String, Object>());
        this.getMapParams().putAll(params);
        this.setHostCard(host);

        this.setIntrinsic(intrinsic);
    }

    /**
     * <p>
     * Constructor for Trigger.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public Trigger(final Map<String, String> params, final Card host, final boolean intrinsic) {
        this.setRunParams(new HashMap<String, Object>());
        this.getMapParams().putAll(params);
        this.setHostCard(host);

        this.setIntrinsic(intrinsic);
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
        if (this.getMapParams().containsKey("TriggerDescription") && !this.isSuppressed()) {
            return this.getMapParams().get("TriggerDescription").replace("CARDNAME", this.getHostCard().getName());
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
    public final boolean phasesCheck() {
        PhaseHandler phaseHandler = Singletons.getModel().getGame().getPhaseHandler();
        if (null != validPhases) {
            if (!validPhases.contains(phaseHandler.getPhase())) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("PlayerTurn")) {
            if (!phaseHandler.isPlayerTurn(this.getHostCard().getController())) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("OpponentTurn")) {
            if (phaseHandler.isPlayerTurn(this.getHostCard().getController())) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("FirstCombat")) {
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
     * 
     * @return a boolean.
     */
    public final boolean requirementsCheck() {
        return this.requirementsCheck(this.getRunParams());
    }

    /**
     * <p>
     * requirementsCheck.
     * </p>
     * 
     * @param runParams2
     *            a {@link java.util.HashMap} object.
     * @return a boolean.
     */
    public final boolean requirementsCheck(final java.util.Map<String, Object> runParams2) {
        if (this.getMapParams().containsKey("FatefulHour")) {
            if (this.getMapParams().get("FatefulHour").equals("True")
                    && !(this.getHostCard().getController().getLife() <= 5)) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("Metalcraft")) {
            if (this.getMapParams().get("Metalcraft").equals("True")
                    && !this.getHostCard().getController().hasMetalcraft()) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("Threshold")) {
            if (this.getMapParams().get("Threshold").equals("True")
                    && !this.getHostCard().getController().hasThreshold()) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("Hellbent")) {
            if (this.getMapParams().get("Hellbent").equals("True") && !this.getHostCard().getController().hasHellbent()) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("PlayersPoisoned")) {
            if (this.getMapParams().get("PlayersPoisoned").equals("You")
                    && (this.getHostCard().getController().getPoisonCounters() == 0)) {
                return false;
            } else if (this.getMapParams().get("PlayersPoisoned").equals("Opponent")
                    && (this.getHostCard().getController().getOpponent().getPoisonCounters() == 0)) {
                return false;
            } else if (this.getMapParams().get("PlayersPoisoned").equals("Each")
                    && !((this.getHostCard().getController().getPoisonCounters() != 0) && (this.getHostCard()
                            .getController().getPoisonCounters() != 0))) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("LifeTotal")) {
            final String player = this.getMapParams().get("LifeTotal");
            String lifeCompare = "GE1";
            int life = 1;

            if (player.equals("You")) {
                life = this.getHostCard().getController().getLife();
            }
            if (player.equals("Opponent")) {
                life = this.getHostCard().getController().getOpponent().getLife();
            }

            if (this.getMapParams().containsKey("LifeAmount")) {
                lifeCompare = this.getMapParams().get("LifeAmount");
            }

            int right = 1;
            final String rightString = lifeCompare.substring(2);
            try {
                right = Integer.parseInt(rightString);
            } catch (final NumberFormatException nfe) {
                right = CardFactoryUtil.xCount(this.getHostCard(), this.getHostCard().getSVar(rightString));
            }

            if (!Expressions.compare(life, lifeCompare, right)) {
                return false;
            }

        }

        if (this.getMapParams().containsKey("APlayerHasMoreLifeThanEachOther")) {
            int highestLife = -50; // Negative base just in case a few Lich's or Platinum Angels are running around
            final List<Player> healthiest = new ArrayList<Player>();
            for (final Player p : Singletons.getModel().getGame().getPlayers()) {
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

        if (this.getMapParams().containsKey("IsPresent")) {
            final String sIsPresent = this.getMapParams().get("IsPresent");
            String presentCompare = "GE1";
            ZoneType presentZone = ZoneType.Battlefield;
            String presentPlayer = "Any";
            if (this.getMapParams().containsKey("PresentCompare")) {
                presentCompare = this.getMapParams().get("PresentCompare");
            }
            if (this.getMapParams().containsKey("PresentZone")) {
                presentZone = ZoneType.smartValueOf(this.getMapParams().get("PresentZone"));
            }
            if (this.getMapParams().containsKey("PresentPlayer")) {
                presentPlayer = this.getMapParams().get("PresentPlayer");
            }
            List<Card> list = new ArrayList<Card>();
            if (presentPlayer.equals("You") || presentPlayer.equals("Any")) {
                list.addAll(this.getHostCard().getController().getCardsIn(presentZone));
            }
            if (presentPlayer.equals("Opponent") || presentPlayer.equals("Any")) {
                list.addAll(this.getHostCard().getController().getOpponent().getCardsIn(presentZone));
            }

            list = CardLists.getValidCards(list, sIsPresent.split(","), this.getHostCard().getController(), this.getHostCard());

            int right = 1;
            final String rightString = presentCompare.substring(2);
            if (rightString.equals("X")) {
                right = CardFactoryUtil.xCount(this.getHostCard(), this.getHostCard().getSVar("X"));
            } else {
                right = Integer.parseInt(presentCompare.substring(2));
            }
            final int left = list.size();

            if (!Expressions.compare(left, presentCompare, right)) {
                return false;
            }

        }

        if (this.getMapParams().containsKey("IsPresent2")) {
            final String sIsPresent = this.getMapParams().get("IsPresent2");
            String presentCompare = "GE1";
            ZoneType presentZone = ZoneType.Battlefield;
            String presentPlayer = "Any";
            if (this.getMapParams().containsKey("PresentCompare2")) {
                presentCompare = this.getMapParams().get("PresentCompare2");
            }
            if (this.getMapParams().containsKey("PresentZone2")) {
                presentZone = ZoneType.smartValueOf(this.getMapParams().get("PresentZone2"));
            }
            if (this.getMapParams().containsKey("PresentPlayer2")) {
                presentPlayer = this.getMapParams().get("PresentPlayer2");
            }
            List<Card> list = new ArrayList<Card>();
            if (presentPlayer.equals("You") || presentPlayer.equals("Any")) {
                list.addAll(this.getHostCard().getController().getCardsIn(presentZone));
            }
            if (presentPlayer.equals("Opponent") || presentPlayer.equals("Any")) {
                list.addAll(this.getHostCard().getController().getOpponent().getCardsIn(presentZone));
            }

            list = CardLists.getValidCards(list, sIsPresent.split(","), this.getHostCard().getController(), this.getHostCard());

            int right = 1;
            final String rightString = presentCompare.substring(2);
            if (rightString.equals("X")) {
                right = CardFactoryUtil.xCount(this.getHostCard(), this.getHostCard().getSVar("X"));
            } else {
                right = Integer.parseInt(presentCompare.substring(2));
            }
            final int left = list.size();

            if (!Expressions.compare(left, presentCompare, right)) {
                return false;
            }

        }

        if (this.getMapParams().containsKey("CheckSVar")) {
            final int sVar = AbilityFactory.calculateAmount(Singletons.getModel().getGame().getCardState(this.getHostCard()), this
                    .getMapParams().get("CheckSVar"), null);
            String comparator = "GE1";
            if (this.getMapParams().containsKey("SVarCompare")) {
                comparator = this.getMapParams().get("SVarCompare");
            }
            final String svarOperator = comparator.substring(0, 2);
            final String svarOperand = comparator.substring(2);
            final int operandValue = AbilityFactory.calculateAmount(Singletons.getModel().getGame().getCardState(this.getHostCard()),
                    svarOperand, null);
            if (!Expressions.compare(sVar, svarOperator, operandValue)) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("ManaSpent")) {
            if (!this.getHostCard().getColorsPaid().contains(this.getMapParams().get("ManaSpent"))) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("ManaNotSpent")) {
            if (this.getHostCard().getColorsPaid().contains(this.getMapParams().get("ManaNotSpent"))) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("WerewolfTransformCondition")) {
            if (CardUtil.getLastTurnCast("Card", this.getHostCard()).size() > 0) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("WerewolfUntransformCondition")) {
            final List<Card> you = CardUtil.getLastTurnCast("Card.YouCtrl", this.getHostCard());
            final List<Card> opp = CardUtil.getLastTurnCast("Card.YouDontCtrl", this.getHostCard());
            if (!((you.size() > 1) || (opp.size() > 1))) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("EvolveCondition")) {
            if (this.getMapParams().get("EvolveCondition").equals("True")) {
                final Card moved = (Card) runParams2.get("Card");
                if (moved == null) {
                    final StringBuilder sb = new StringBuilder();
                    sb.append("Trigger::requirementsCheck() - EvolveCondition condition being checked without a moved card. ");
                    sb.append(this.getHostCard().getName());
                    throw new RuntimeException(sb.toString());
                }
                if (moved.getNetAttack() <= this.getHostCard().getNetAttack()
                        && moved.getNetDefense() <= this.getHostCard().getNetDefense()) {
                    return false;
                }
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
        if (this.getMapParams().containsKey("Secondary")) {
            if (this.getMapParams().get("Secondary").equals("True")) {
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
     * getCopy.
     * </p>
     * 
     * @return a {@link forge.card.trigger.Trigger} object.
     */
    public abstract Trigger getCopy();

    /**
     * <p>
     * setTriggeringObjects.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public abstract void setTriggeringObjects(SpellAbility sa);

    /** The temporary. */
    private boolean temporary = false;

    /**
     * Sets the temporary.
     * 
     * @param temp
     *            the new temporary
     */
    public final void setTemporary(final boolean temp) {
        this.temporary = temp;
    }

    /**
     * Checks if is temporary.
     * 
     * @return true, if is temporary
     */
    public final boolean isTemporary() {
        return this.temporary;
    }

    /**
     * Checks if is intrinsic.
     * 
     * @return the isIntrinsic
     */
    public boolean isIntrinsic() {
        return this.isIntrinsic;
    }

    /**
     * Sets the intrinsic.
     * 
     * @param isIntrinsic0
     *            the isIntrinsic to set
     */
    public void setIntrinsic(final boolean isIntrinsic0) {
        this.isIntrinsic = isIntrinsic0;
    }

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

    /**
     * Sets the id.
     * 
     * @param id0
     *            the id to set
     */
    public void setId(final int id0) {
        this.id = id0;
    }

    private Ability triggeredSA;

    /**
     * Gets the triggered sa.
     * 
     * @return the triggered sa
     */
    public final Ability getTriggeredSA() {
        System.out.println("TriggeredSA = " + this.triggeredSA);
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

    /**
     * TODO: Write javadoc for this method.
     * @param triggerAlways
     * @return
     */
    public void copyFieldsTo(Trigger copy) {
        copy.setName(this.getName());
        copy.setID(this.getId());
        copy.setMode(this.getMode());
        copy.setTriggerPhases(this.validPhases);
        copy.setActiveZone(validHostZones);
    }

    public boolean isStatic() {
        return getMapParams().containsKey("Static"); // && params.get("Static").equals("True") [always true if present]
    }

    public void setTriggerPhases(List<PhaseType> phases) {
        validPhases = phases;
    }
}
