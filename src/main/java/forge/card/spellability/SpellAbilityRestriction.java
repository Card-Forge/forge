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
import java.util.List;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;

import forge.CardLists;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.cardfactory.CardFactoryUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;

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
            this.setZone(ZoneType.smartValueOf(params.get("ActivationZone")));
        }

        if (params.containsKey("Flashback")) {
            this.setZone(ZoneType.Graveyard);
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
            this.setLimitToCheck(params.get("ActivationLimit"));
        }

        if (params.containsKey("ActivationNumberSacrifice")) {
            this.setActivationNumberSacrifice(Integer.parseInt(params.get("ActivationNumberSacrifice")));
        }

        if (params.containsKey("ActivationPhases")) {
            this.setPhases(PhaseType.parseRange(params.get("ActivationPhases")));
        }

        if (params.containsKey("ActivationCardsInHand")) {
            this.setActivateCardsInHand(Integer.parseInt(params.get("ActivationCardsInHand")));
        }

        if (params.containsKey("Planeswalker")) {
            this.setPlaneswalker(true);
            this.setSorcerySpeed(true);
        }

        if (params.containsKey("IsPresent")) {
            this.setIsPresent(params.get("IsPresent"));
            if (params.containsKey("PresentCompare")) {
                this.setPresentCompare(params.get("PresentCompare"));
            }
            if (params.containsKey("PresentZone")) {
                this.setPresentZone(ZoneType.smartValueOf(params.get("PresentZone")));
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
     * checkZoneRestrictions.
     * </p>
     * @param c
     *            a {@link forge.Card} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public final boolean checkZoneRestrictions(final Card c, final SpellAbility sa) {
        if (this.getZone() == null) {
            return true;
        }
        PlayerZone cardZone = AllZone.getZoneOf(c);
        Player activator = sa.getActivatingPlayer();
        if (!cardZone.is(this.getZone())) {
            // If Card is not in the default activating zone, do some additional checks
            // Not a Spell, or on Battlefield, return false
            if (!sa.isSpell() || cardZone.is(ZoneType.Battlefield) || !this.getZone().equals(ZoneType.Hand)) {
                return false;
            }
            if (c.hasKeyword("May be played") && activator.isPlayer(c.getController())) {
                return true;
            }
            if (c.hasKeyword("May be played by your opponent") && !activator.isPlayer(c.getController())) {
                return true;
            }
            return false;
        }

        return true;
    }

    /**
     * <p>
     * checkTimingRestrictions.
     * </p>
     * @param c
     *            a {@link forge.Card} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public final boolean checkTimingRestrictions(final Card c, final SpellAbility sa) {
        Player activator = sa.getActivatingPlayer();

        if (this.isPlayerTurn() && !Singletons.getModel().getGameState().getPhaseHandler().isPlayerTurn(activator)) {
            return false;
        }

        if (this.isOpponentTurn() && Singletons.getModel().getGameState().getPhaseHandler().isPlayerTurn(activator)) {
            return false;
        }

        if (this.getPhases().size() > 0) {
            boolean isPhase = false;
            final PhaseType currPhase = Singletons.getModel().getGameState().getPhaseHandler().getPhase();
            for (final PhaseType s : this.getPhases()) {
                if (s == currPhase) {
                    isPhase = true;
                    break;
                }
            }

            if (!isPhase) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>
     * checkActivatorRestrictions.
     * </p>
     * @param c
     *            a {@link forge.Card} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public final boolean checkActivatorRestrictions(final Card c, final SpellAbility sa) {
        Player activator = sa.getActivatingPlayer();
        if (this.isAnyPlayer()) {
            return true;
        }

        if (activator.equals(c.getController()) && !this.isOpponentOnly()) {
            return true;
        }
        if (!activator.equals(c.getController())
                && (this.isOpponentOnly() || c.hasKeyword("May be played by your opponent"))) {
            return true;
        }
        return false;
    }

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

        Player activator = sa.getActivatingPlayer();
        if (activator == null) {
            activator = c.getController();
            sa.setActivatingPlayer(activator);
            System.out.println(c.getName() + " Did not have activator set in SpellAbilityRestriction.canPlay()");
        }

        if (this.isSorcerySpeed() && !PhaseHandler.canCastSorcery(activator)) {
            return false;
        }

        if (!checkTimingRestrictions(c, sa)) {
            return false;
        }

        if (!checkActivatorRestrictions(c, sa)) {
            return false;
        }

        if (!checkZoneRestrictions(c, sa)) {
            return false;
        }

        if (this.getLimitToCheck() != null) {
            String limit = this.getLimitToCheck();
            int activationLimit = limit.matches("[0-9][0-9]?")
              ? Integer.parseInt(limit) : AbilityFactory.calculateAmount(c, limit, sa);
            this.setActivationLimit(activationLimit);

            if ((this.getActivationLimit() != -1) && (this.getNumberTurnActivations() >= this.getActivationLimit())) {
                return false;
            }
        }

        if (this.getCardsInHand() != -1) {
            if (activator.getCardsIn(ZoneType.Hand).size() != this.getCardsInHand()) {
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
        if (this.getProwl() != null && !this.getProwl().isEmpty()) {
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
            List<Card> list = AllZoneUtil.getCardsIn(this.getPresentZone());

            list = CardLists.getValidCards(list, this.getIsPresent().split(","), activator, c);

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
