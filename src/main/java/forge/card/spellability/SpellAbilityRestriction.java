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
package forge.card.spellability;

import java.util.ArrayList;
import java.util.HashMap;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.Constant.Zone;
import forge.Phase;
import forge.Player;
import forge.PlayerZone;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.cardfactory.CardFactoryUtil;

/**
 * <p>
 * SpellAbilityRestriction class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class SpellAbilityRestriction extends SpellAbilityVariables {
    // A class for handling SpellAbility Restrictions. These restrictions
    // include:
    // Zone, Phase, OwnTurn, Speed (instant/sorcery), Amount per Turn, Player,
    // Threshold, Metalcraft, LevelRange, etc
    // Each value will have a default, that can be overridden (mostly by
    // AbilityFactory)
    // The canPlay function will use these values to determine if the current
    // game state is ok with these restrictions

    /**
     * <p>
     * Constructor for SpellAbilityRestriction.
     * </p>
     */
    public SpellAbilityRestriction() {
    }

    /**
     * <p>
     * setRestrictions.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @since 1.0.15
     */
    public final void setRestrictions(final HashMap<String, String> params) {
        if (params.containsKey("Activation")) {
            final String value = params.get("Activation");
            if (value.equals("Threshold")) {
                this.setThreshold(true);
            }
            if (value.equals("Metalcraft")) {
                this.setMetalcraft(true);
            }
            if (value.equals("Hellbent")) {
                this.setHellbent(true);
            }
            if (value.startsWith("Prowl")) {
                final ArrayList<String> prowlTypes = new ArrayList<String>();
                prowlTypes.add("Rogue");
                if (value.split("Prowl").length > 1) {
                    prowlTypes.add(value.split("Prowl")[1]);
                }
                this.setProwl(prowlTypes);
            }
        }

        if (params.containsKey("ActivationZone")) {
            this.setZone(Zone.smartValueOf(params.get("ActivationZone")));
        }

        if (params.containsKey("Flashback")) {
            this.setZone(Zone.Graveyard);
        }

        if (params.containsKey("SorcerySpeed")) {
            this.setSorcerySpeed(true);
        }

        if (params.containsKey("InstantSpeed")) {
            this.setInstantSpeed(true);
        }

        if (params.containsKey("PlayerTurn")) {
            this.setPlayerTurn(true);
        }

        if (params.containsKey("OpponentTurn")) {
            this.setOpponentTurn(true);
        }

        if (params.containsKey("AnyPlayer")) {
            this.setAnyPlayer(true);
        }

        if (params.containsKey("ActivationLimit")) {
            this.setActivationLimit(Integer.parseInt(params.get("ActivationLimit")));
        }

        if (params.containsKey("ActivationNumberSacrifice")) {
            this.setActivationNumberSacrifice(Integer.parseInt(params.get("ActivationNumberSacrifice")));
        }

        if (params.containsKey("ActivationPhases")) {
            String phases = params.get("ActivationPhases");

            if (phases.contains("->")) {
                // If phases lists a Range, split and Build Activate String
                // Combat_Begin->Combat_End (During Combat)
                // Draw-> (After Upkeep)
                // Upkeep->Combat_Begin (Before Declare Attackers)

                final String[] split = phases.split("->", 2);
                phases = AllZone.getPhase().buildActivateString(split[0], split[1]);
            }

            this.setPhases(phases);
        }

        if (params.containsKey("ActivationCardsInHand")) {
            this.setActivateCardsInHand(Integer.parseInt(params.get("ActivationCardsInHand")));
        }

        if (params.containsKey("Planeswalker")) {
            this.setPlaneswalker(true);
        }

        if (params.containsKey("IsPresent")) {
            this.setIsPresent(params.get("IsPresent"));
            if (params.containsKey("PresentCompare")) {
                this.setPresentCompare(params.get("PresentCompare"));
            }
            if (params.containsKey("PresentZone")) {
                this.setPresentZone(Zone.smartValueOf(params.get("PresentZone")));
            }
        }

        if (params.containsKey("IsNotPresent")) {
            this.setIsPresent(params.get("IsNotPresent"));
            this.setPresentCompare("EQ0");
        }

        // basically PresentCompare for life totals:
        if (params.containsKey("ActivationLifeTotal")) {
            this.setLifeTotal(params.get("ActivationLifeTotal"));
            if (params.containsKey("ActivationLifeAmount")) {
                this.setLifeAmount(params.get("ActivationLifeAmount"));
            }
        }

        if (params.containsKey("CheckSVar")) {
            this.setSvarToCheck(params.get("CheckSVar"));
        }
        if (params.containsKey("SVarCompare")) {
            this.setSvarOperator(params.get("SVarCompare").substring(0, 2));
            this.setSvarOperand(params.get("SVarCompare").substring(2));
        }
    } // end setRestrictions()

    /**
     * <p>
     * canPlay.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public final boolean canPlay(final Card c, final SpellAbility sa) {
        if (c.isPhasedOut()) {
            return false;
        }

        final PlayerZone cardZone = AllZone.getZoneOf(c);
        if (!cardZone.is(this.getZone())) {
            // If Card is not in the default activating zone, do some additional
            // checks
            // Not a Spell, or on Battlefield, return false
            if (!sa.isSpell() || cardZone.is(Zone.Battlefield) || !this.getZone().equals(Zone.Hand)) {
                return false;
            } else if (!c.hasStartOfKeyword("May be played")
                    && !(c.hasStartOfKeyword("Flashback") && cardZone.is(Zone.Graveyard))) {
                return false;
            }
        }

        Player activator = sa.getActivatingPlayer();
        if (activator == null) {
            activator = c.getController();
            System.out.println(c.getName() + " Did not have activator set in SpellAbilityRestriction.canPlay()");
        }

        if (this.isSorcerySpeed() && !Phase.canCastSorcery(activator)) {
            return false;
        }

        if (this.isPlayerTurn() && !AllZone.getPhase().isPlayerTurn(activator)) {
            return false;
        }

        if (this.isOpponentTurn() && AllZone.getPhase().isPlayerTurn(activator)) {
            return false;
        }

        if (!this.isAnyPlayer() && !activator.equals(c.getController())) {
            return false;
        }

        if ((this.getActivationLimit() != -1) && (this.getNumberTurnActivations() >= this.getActivationLimit())) {
            return false;
        }

        if (this.getPhases().size() > 0) {
            boolean isPhase = false;
            final String currPhase = AllZone.getPhase().getPhase();
            for (final String s : this.getPhases()) {
                if (s.equals(currPhase)) {
                    isPhase = true;
                    break;
                }
            }

            if (!isPhase) {
                return false;
            }
        }

        if (this.getCardsInHand() != -1) {
            if (activator.getCardsIn(Zone.Hand).size() != this.getCardsInHand()) {
                return false;
            }
        }
        if (this.isHellbent()) {
            if (!activator.hasHellbent()) {
                return false;
            }
        }
        if (this.isThreshold()) {
            if (!activator.hasThreshold()) {
                return false;
            }
        }
        if (this.isMetalcraft()) {
            if (!activator.hasMetalcraft()) {
                return false;
            }
        }
        if (this.getProwl() != null) {
            // only true if the activating player has damaged the opponent with
            // one of the specified types
            boolean prowlFlag = false;
            for (final String type : this.getProwl()) {
                if (activator.hasProwl(type)) {
                    prowlFlag = true;
                }
            }
            if (!prowlFlag) {
                return false;
            }
        }
        if (this.getIsPresent() != null) {
            CardList list = AllZoneUtil.getCardsIn(this.getPresentZone());

            list = list.getValidCards(this.getIsPresent().split(","), activator, c);

            int right = 1;
            final String rightString = this.getPresentCompare().substring(2);
            if (rightString.equals("X")) {
                right = CardFactoryUtil.xCount(c, c.getSVar("X"));
            } else {
                right = Integer.parseInt(this.getPresentCompare().substring(2));
            }
            final int left = list.size();

            if (!AllZoneUtil.compare(left, this.getPresentCompare(), right)) {
                return false;
            }
        }

        if (this.getLifeTotal() != null) {
            int life = 1;
            if (this.getLifeTotal().equals("You")) {
                life = activator.getLife();
            }
            if (this.getLifeTotal().equals("Opponent")) {
                life = activator.getOpponent().getLife();
            }

            int right = 1;
            final String rightString = this.getLifeAmount().substring(2);
            if (rightString.equals("X")) {
                right = CardFactoryUtil.xCount(sa.getSourceCard(), sa.getSourceCard().getSVar("X"));
            } else {
                right = Integer.parseInt(this.getLifeAmount().substring(2));
            }

            if (!AllZoneUtil.compare(life, this.getLifeAmount(), right)) {
                return false;
            }
        }

        if (this.isPwAbility()) {
            // Planeswalker abilities can only be activated as Sorceries
            if (!Phase.canCastSorcery(activator)) {
                return false;
            }

            for (final SpellAbility pwAbs : c.getAllSpellAbilities()) {
                // check all abilities on card that have their planeswalker
                // restriction set to confirm they haven't been activated
                final SpellAbilityRestriction restrict = pwAbs.getRestrictions();
                if (restrict.getPlaneswalker() && (restrict.getNumberTurnActivations() > 0)) {
                    return false;
                }
            }
        }

        if (this.getsVarToCheck() != null) {
            final int svarValue = AbilityFactory.calculateAmount(c, this.getsVarToCheck(), sa);
            final int operandValue = AbilityFactory.calculateAmount(c, this.getsVarOperand(), sa);

            if (!AllZoneUtil.compare(svarValue, this.getsVarOperator(), operandValue)) {
                return false;
            }

        }

        return true;
    } // canPlay()

} // end class SpellAbilityRestriction
