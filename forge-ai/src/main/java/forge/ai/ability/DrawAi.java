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
package forge.ai.ability;

import forge.ai.*;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.cost.CostDiscard;
import forge.game.cost.CostPart;
import forge.game.cost.PaymentDecision;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

public class DrawAi extends SpellAbilityAi {

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        return targetAI(ai, sa, false);
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Card source = sa.getHostCard();
        final Cost abCost = sa.getPayCosts();
        final Game game = ai.getGame();

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!ComputerUtilCost.checkCreatureSacrificeCost(ai, abCost, source)) {
                return false;
            }

            if (!ComputerUtilCost.checkLifeCost(ai, abCost, source, 4, null)) {
                return false;
            }

            if (!ComputerUtilCost.checkDiscardCost(ai, abCost, source)) {
                AiCostDecision aiDecisions = new AiCostDecision(ai, sa);
                for (final CostPart part : abCost.getCostParts()) {
                    if (part instanceof CostDiscard) {
                        PaymentDecision decision = part.accept(aiDecisions);
                        if ( null == decision )
                            return false;
                        for (Card discard : decision.cards) {
                            if (!ComputerUtil.isWorseThanDraw(ai, discard)) {
                                return false;
                            }
                        }
                    }
                }
            }

            if (!ComputerUtilCost.checkRemoveCounterCost(abCost, source)) {
                return false;
            }

        }

        if (!targetAI(ai, sa, false)) {
            return false;
        }

        if (tgt != null) {
            final Player player = sa.getTargets().getFirstTargetedPlayer();
            if (player != null && player.isOpponentOf(ai)) {
                return true;
            }
        }

        // prevent run-away activations - first time will always return true
        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }

        if (ComputerUtil.playImmediately(ai, sa)) {
            return true;
        }

        if (sa.getConditions() != null && !sa.getConditions().areMet(sa) && sa.getSubAbility() == null) {
        	return false;
        }

        // Don't use draw abilities before main 2 if possible
        if (game.getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)
                && !sa.hasParam("ActivationPhases") && !ComputerUtil.castSpellInMain1(ai, sa)) {
            return false;
        }
        if ((!game.getPhaseHandler().getNextTurn().equals(ai)
                    || game.getPhaseHandler().getPhase().isBefore(PhaseType.END_OF_TURN))
                && !sa.hasParam("PlayerTurn") && !SpellAbilityAi.isSorcerySpeed(sa)
                && ai.getCardsIn(ZoneType.Hand).size() > 1
                && !ComputerUtil.activateForCost(sa, ai)) {
            return false;
        }

        // Don't tap creatures that may be able to block
        if (ComputerUtil.waitForBlocking(sa)) {
            return false;
        }

        return true;
    }

    private boolean targetAI(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Card source = sa.getHostCard();
        final boolean drawback = (sa instanceof AbilitySub);
        final Game game = ai.getGame();
        Player opp = ai.getOpponent();

        int computerHandSize = ai.getCardsIn(ZoneType.Hand).size();
        final int humanLibrarySize = opp.getCardsIn(ZoneType.Library).size();
        final int computerLibrarySize = ai.getCardsIn(ZoneType.Library).size();
        final int computerMaxHandSize = ai.getMaxHandSize();

        //if a spell is used don't count the card
        if (sa.isSpell() && source.isInZone(ZoneType.Hand)) {
            computerHandSize -= 1;
        }

        int numCards = 1;
        if (sa.hasParam("NumCards")) {
            numCards = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumCards"), sa);
        }

        boolean xPaid = false;
        final String num = sa.getParam("NumCards");
        if ((num != null) && num.equals("X") && source.getSVar(num).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            if (sa instanceof AbilitySub && !source.getSVar("PayX").equals("")) {
                numCards = Integer.parseInt(source.getSVar("PayX"));
            } else {
                numCards = ComputerUtilMana.determineLeftoverMana(sa, ai);
                source.setSVar("PayX", Integer.toString(numCards));
            }
            xPaid = true;
        }
        //if (n)

        // TODO: if xPaid and one of the below reasons would fail, instead of
        // bailing reduce toPay amount to acceptable level

        if (tgt != null) {
            // ability is targeted
            sa.resetTargets();

            final boolean canTgtHuman = sa.canTarget(opp);
            final boolean canTgtComp = sa.canTarget(ai);
            boolean tgtHuman = false;

            if (!canTgtHuman && !canTgtComp) {
                return false;
            }

            if (canTgtHuman && !opp.cantLose() && numCards >= humanLibrarySize) {
                // Deck the Human? DO IT!
                sa.getTargets().add(opp);
                return true;
            }

            if (numCards >= computerLibrarySize) {
                if (xPaid) {
                    numCards = computerLibrarySize - 1;
                    source.setSVar("PayX", Integer.toString(numCards));
                } else {
                    // Don't deck your self
                    if (!mandatory) {
                        return false;
                    }
                    tgtHuman = true;
                }
            }

            if (computerHandSize + numCards > computerMaxHandSize && game.getPhaseHandler().isPlayerTurn(ai)) {
                if (xPaid) {
                    numCards = computerMaxHandSize - computerHandSize;
                    source.setSVar("PayX", Integer.toString(numCards));
                } else {
                    // Don't draw too many cards and then risk discarding cards
                    // at EOT
                    // TODO: "NextUpkeep" is deprecated
                    if (!(sa.hasParam("NextUpkeep") || (sa instanceof AbilitySub)) && !mandatory) {
                        return false;
                    }
                }
            }

            if (numCards == 0 && !mandatory && !drawback) {
                return false;
            }

            if ((!tgtHuman || !canTgtHuman) && canTgtComp) {
                sa.getTargets().add(ai);
            } else if (mandatory && canTgtHuman) {
                sa.getTargets().add(opp);
            } else {
                return false;
            }
        } else if (!mandatory) {
            // TODO: consider if human is the defined player

            // ability is not targeted
            if (numCards >= computerLibrarySize) {
                if (ai.isCardInPlay("Laboratory Maniac")) {
                    return true;
                }
                // Don't deck yourself
                return false;
            }

            if (numCards == 0  && !drawback) {
                return false;
            }

            if ((computerHandSize + numCards > computerMaxHandSize)
                    && game.getPhaseHandler().isPlayerTurn(ai)
                    && !sa.isTrigger()) {
                // Don't draw too many cards and then risk discarding cards at
                // EOT
                if (!sa.hasParam("NextUpkeep") && !drawback) {
                    return false;
                }
            }
        }
        return true;
    } // drawTargetAI()


    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        return targetAI(ai, sa, mandatory);
    }

    
    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#confirmAction(forge.game.player.Player, forge.card.spellability.SpellAbility, forge.game.player.PlayerActionConfirmMode, java.lang.String)
     */
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        int numCards = sa.hasParam("NumCards") ? AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumCards"), sa) : 1;
        // AI shouldn't mill itself
        return numCards < player.getZone(ZoneType.Library).size();
    }
}
