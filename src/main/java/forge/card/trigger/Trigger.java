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

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardUtil;
import forge.Constant.Zone;
import forge.Player;
import forge.PlayerZone;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;

/**
 * <p>
 * Abstract Trigger class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class Trigger {

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
     * @param mapParams
     *            the mapParams to set
     */
    public final void setMapParams(final HashMap<String, String> mapParams) {
        this.mapParams = mapParams; // TODO: Add 0 to parameter's name.
    }

    /** The run params. */
    private Map<String, Object> runParams;

    /** The overriding ability. */
    private SpellAbility overridingAbility = null;

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

    /** The host card. */
    private Card hostCard;

    /**
     * <p>
     * Getter for the field <code>hostCard</code>.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getHostCard() {
        return this.hostCard;
    }

    /**
     * <p>
     * Setter for the field <code>hostCard</code>.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void setHostCard(final Card c) {
        this.hostCard = c;
    }

    /** The is intrinsic. */
    private boolean isIntrinsic;

    /**
     * Gets the checks if is intrinsic.
     * 
     * @return the checks if is intrinsic
     */
    public final boolean getIsIntrinsic() {
        return this.isIntrinsic();
    }

    /**
     * Sets the checks if is intrinsic.
     * 
     * @param b
     *            the new checks if is intrinsic
     */
    public final void setIsIntrinsic(final boolean b) {
        this.setIntrinsic(b);
    }

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
    public Trigger(final String n, final HashMap<String, String> params, final Card host, final boolean intrinsic) {
        this.name = n;
        this.setRunParams(new HashMap<String, Object>());
        for (final Map.Entry<String, String> entry : params.entrySet()) {
            this.getMapParams().put(entry.getKey(), entry.getValue());
        }
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
    public Trigger(final HashMap<String, String> params, final Card host, final boolean intrinsic) {
        this.setRunParams(new HashMap<String, Object>());
        for (final Map.Entry<String, String> entry : params.entrySet()) {
            this.getMapParams().put(entry.getKey(), entry.getValue());
        }
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
     * zonesCheck.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean zonesCheck() {
        if (this.getMapParams().containsKey("TriggerZones")) {
            final List<Zone> triggerZones = new ArrayList<Zone>();
            final PlayerZone zone = AllZone.getZoneOf(this.getHostCard());
            for (final String s : this.getMapParams().get("TriggerZones").split(",")) {
                triggerZones.add(Zone.smartValueOf(s));
            }
            if (zone == null) {
                return false;
            }
            if (!triggerZones.contains(zone.getZoneType()) || this.getHostCard().isPhasedOut()) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>
     * phasesCheck.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean phasesCheck() {
        if (this.getMapParams().containsKey("TriggerPhases")) {
            String phases = this.getMapParams().get("TriggerPhases");

            if (phases.contains("->")) {
                // If phases lists a Range, split and Build Activate String
                // Combat_Begin->Combat_End (During Combat)
                // Draw-> (After Upkeep)
                // Upkeep->Combat_Begin (Before Declare Attackers)

                final String[] split = phases.split("->", 2);
                phases = AllZone.getPhase().buildActivateString(split[0], split[1]);
            }
            final ArrayList<String> triggerPhases = new ArrayList<String>();
            for (final String s : phases.split(",")) {
                triggerPhases.add(s);
            }
            if (!triggerPhases.contains(AllZone.getPhase().getPhase())) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("PlayerTurn")) {
            if (!AllZone.getPhase().isPlayerTurn(this.getHostCard().getController())) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("OpponentTurn")) {
            if (AllZone.getPhase().isPlayerTurn(this.getHostCard().getController())) {
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

            if (!AllZoneUtil.compare(life, lifeCompare, right)) {
                return false;
            }

        }

        if (this.getMapParams().containsKey("IsPresent")) {
            final String sIsPresent = this.getMapParams().get("IsPresent");
            String presentCompare = "GE1";
            Zone presentZone = Zone.Battlefield;
            String presentPlayer = "Any";
            if (this.getMapParams().containsKey("PresentCompare")) {
                presentCompare = this.getMapParams().get("PresentCompare");
            }
            if (this.getMapParams().containsKey("PresentZone")) {
                presentZone = Zone.smartValueOf(this.getMapParams().get("PresentZone"));
            }
            if (this.getMapParams().containsKey("PresentPlayer")) {
                presentPlayer = this.getMapParams().get("PresentPlayer");
            }
            CardList list = new CardList();
            if (presentPlayer.equals("You") || presentPlayer.equals("Any")) {
                list.addAll(this.getHostCard().getController().getCardsIn(presentZone));
            }
            if (presentPlayer.equals("Opponent") || presentPlayer.equals("Any")) {
                list.addAll(this.getHostCard().getController().getOpponent().getCardsIn(presentZone));
            }

            list = list.getValidCards(sIsPresent.split(","), this.getHostCard().getController(), this.getHostCard());

            int right = 1;
            final String rightString = presentCompare.substring(2);
            if (rightString.equals("X")) {
                right = CardFactoryUtil.xCount(this.getHostCard(), this.getHostCard().getSVar("X"));
            } else {
                right = Integer.parseInt(presentCompare.substring(2));
            }
            final int left = list.size();

            if (!AllZoneUtil.compare(left, presentCompare, right)) {
                return false;
            }

        }

        if (this.getMapParams().containsKey("IsPresent2")) {
            final String sIsPresent = this.getMapParams().get("IsPresent2");
            String presentCompare = "GE1";
            Zone presentZone = Zone.Battlefield;
            String presentPlayer = "Any";
            if (this.getMapParams().containsKey("PresentCompare2")) {
                presentCompare = this.getMapParams().get("PresentCompare2");
            }
            if (this.getMapParams().containsKey("PresentZone2")) {
                presentZone = Zone.smartValueOf(this.getMapParams().get("PresentZone2"));
            }
            if (this.getMapParams().containsKey("PresentPlayer2")) {
                presentPlayer = this.getMapParams().get("PresentPlayer2");
            }
            CardList list = new CardList();
            if (presentPlayer.equals("You") || presentPlayer.equals("Any")) {
                list.addAll(this.getHostCard().getController().getCardsIn(presentZone));
            }
            if (presentPlayer.equals("Opponent") || presentPlayer.equals("Any")) {
                list.addAll(this.getHostCard().getController().getOpponent().getCardsIn(presentZone));
            }

            list = list.getValidCards(sIsPresent.split(","), this.getHostCard().getController(), this.getHostCard());

            int right = 1;
            final String rightString = presentCompare.substring(2);
            if (rightString.equals("X")) {
                right = CardFactoryUtil.xCount(this.getHostCard(), this.getHostCard().getSVar("X"));
            } else {
                right = Integer.parseInt(presentCompare.substring(2));
            }
            final int left = list.size();

            if (!AllZoneUtil.compare(left, presentCompare, right)) {
                return false;
            }

        }

        if (this.getMapParams().containsKey("CheckSVar")) {
            final int sVar = AbilityFactory.calculateAmount(AllZoneUtil.getCardState(this.getHostCard()), this
                    .getMapParams().get("CheckSVar"), null);
            String comparator = "GE1";
            if (this.getMapParams().containsKey("SVarCompare")) {
                comparator = this.getMapParams().get("SVarCompare");
            }
            final String svarOperator = comparator.substring(0, 2);
            final String svarOperand = comparator.substring(2);
            final int operandValue = AbilityFactory.calculateAmount(AllZoneUtil.getCardState(this.getHostCard()),
                    svarOperand, null);
            if (!AllZoneUtil.compare(sVar, svarOperator, operandValue)) {
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
            final CardList you = CardUtil.getLastTurnCast("Card.YouCtrl", this.getHostCard());
            final CardList opp = CardUtil.getLastTurnCast("Card.YouDontCtrl", this.getHostCard());
            if (!((you.size() > 1) || (opp.size() > 1))) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>
     * matchesValid.
     * </p>
     * 
     * @param o
     *            a {@link java.lang.Object} object.
     * @param valids
     *            an array of {@link java.lang.String} objects.
     * @param srcCard
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean matchesValid(final Object o, final String[] valids, final Card srcCard) {
        if (o instanceof Card) {
            final Card c = (Card) o;
            return c.isValid(valids, srcCard.getController(), srcCard);
        }

        if (o instanceof Player) {
            for (final String v : valids) {
                if (v.equalsIgnoreCase("Player") || v.equalsIgnoreCase("Each")) {
                    return true;
                }
                if (v.equalsIgnoreCase("Opponent")) {
                    if (o.equals(srcCard.getController().getOpponent())) {
                        return true;
                    }
                }
                if (v.equalsIgnoreCase("You")) {
                    return o.equals(srcCard.getController());
                }
                if (v.equalsIgnoreCase("EnchantedController")) {
                    return ((Player) o).isPlayer(srcCard.getEnchantingCard().getController());
                }
                if (v.equalsIgnoreCase("EnchantedPlayer")) {
                    return o.equals(srcCard.getEnchanting());
                }
            }
        }

        return false;
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

    /** The suppressed. */
    private boolean suppressed = false;

    /** The temporarily suppressed. */
    private boolean temporarilySuppressed = false;

    /**
     * Sets the suppressed.
     * 
     * @param supp
     *            the new suppressed
     */
    public final void setSuppressed(final boolean supp) {
        this.suppressed = supp;
    }

    /**
     * Sets the temporarily suppressed.
     * 
     * @param supp
     *            the new temporarily suppressed
     */
    public final void setTemporarilySuppressed(final boolean supp) {
        this.temporarilySuppressed = supp;
    }

    /**
     * Checks if is suppressed.
     * 
     * @return true, if is suppressed
     */
    public final boolean isSuppressed() {
        return (this.suppressed || this.temporarilySuppressed);
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
     * @param isIntrinsic
     *            the isIntrinsic to set
     */
    public void setIntrinsic(final boolean isIntrinsic) {
        this.isIntrinsic = isIntrinsic; // TODO: Add 0 to parameter's name.
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
     * @param runParams
     *            the runParams to set
     */
    public void setRunParams(final Map<String, Object> runParams) {
        this.runParams = runParams; // TODO: Add 0 to parameter's name.
    }

    /**
     * Gets the overriding ability.
     * 
     * @return the overridingAbility
     */
    public SpellAbility getOverridingAbility() {
        return this.overridingAbility;
    }

    /**
     * Sets the overriding ability.
     * 
     * @param overridingAbility
     *            the overridingAbility to set
     */
    public void setOverridingAbility(final SpellAbility overridingAbility) {
        this.overridingAbility = overridingAbility; // TODO: Add 0 to
                                                    // parameter's name.
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
     * @param id
     *            the id to set
     */
    public void setId(final int id) {
        this.id = id; // TODO: Add 0 to parameter's name.
    }
}
