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
package forge.card.abilityfactory.ai;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;

import forge.Card;

import forge.CardLists;
import forge.Singletons;
import forge.card.abilityfactory.AbilityUtils;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.phase.PhaseType;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;


//AB:GainControl|ValidTgts$Creature|TgtPrompt$Select target legendary creature|LoseControl$Untap,LoseControl|SpellDescription$Gain control of target xxxxxxx

//GainControl specific sa:
//  LoseControl - the lose control conditions (as a comma separated list)
//  -Untap - source card becomes untapped
//  -LoseControl - you lose control of source card
//  -LeavesPlay - source card leaves the battlefield
//  -PowerGT - (not implemented yet for Old Man of the Sea)
//  AddKWs - Keywords to add to the controlled card
//            (as a "&"-separated list; like Haste, Sacrifice CARDNAME at EOT, any standard keyword)
//  OppChoice - set to True if opponent chooses creature (for Preacher) - not implemented yet
//  Untap - set to True if target card should untap when control is taken
//  DestroyTgt - actions upon which the tgt should be destroyed.  same list as LoseControl
//  NoRegen - set if destroyed creature can't be regenerated.  used only with DestroyTgt

/**
 * <p>
 * AbilityFactory_GainControl class.
 * </p>
 * 
 * @author Forge
 * @version $Id: AbilityFactoryGainControl.java 17764 2012-10-29 11:04:18Z Sloth $
 */
public class ControlGainAi extends SpellAiLogic {
    @Override
    protected boolean canPlayAI(AIPlayer ai, final SpellAbility sa) {
        boolean hasCreature = false;
        boolean hasArtifact = false;
        boolean hasEnchantment = false;
        boolean hasLand = false;

        final List<String> lose = sa.hasParam("LoseControl") ? Arrays.asList(sa.getParam("LoseControl").split(",")) : null;


        final Target tgt = sa.getTarget();
        Player opp = ai.getOpponent();

        // if Defined, then don't worry about targeting
        if (tgt == null) {
            return true;
        } else {
            tgt.resetTargets();
            if (tgt.canOnlyTgtOpponent()) {
                if (!opp.canBeTargetedBy(sa)) {
                    return false;
                }
                tgt.addTarget(opp);
            }
        }

        List<Card> list =
                CardLists.getValidCards(opp.getCardsIn(ZoneType.Battlefield), tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getSourceCard());
        // AI won't try to grab cards that are filtered out of AI decks on
        // purpose
        list = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                final Map<String, String> vars = c.getSVars();
                return !vars.containsKey("RemAIDeck") && c.canBeTargetedBy(sa);
            }
        });

        if (list.isEmpty()) {
            return false;
        }

        // Don't steal something if I can't Attack without, or prevent it from
        // blocking at least
        if ((lose != null) && lose.contains("EOT")
                && Singletons.getModel().getGame().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            return false;
        }

        while (tgt.getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)) {
            Card t = null;
            for (final Card c : list) {
                if (c.isCreature()) {
                    hasCreature = true;
                }
                if (c.isArtifact()) {
                    hasArtifact = true;
                }
                if (c.isLand()) {
                    hasLand = true;
                }
                if (c.isEnchantment()) {
                    hasEnchantment = true;
                }
            }

            if (list.isEmpty()) {
                if ((tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) || (tgt.getNumTargeted() == 0)) {
                    tgt.resetTargets();
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            if (hasCreature) {
                t = CardFactoryUtil.getBestCreatureAI(list);
            } else if (hasArtifact) {
                t = CardFactoryUtil.getBestArtifactAI(list);
            } else if (hasLand) {
                t = CardFactoryUtil.getBestLandAI(list);
            } else if (hasEnchantment) {
                t = CardFactoryUtil.getBestEnchantmentAI(list, sa, true);
            } else {
                t = CardFactoryUtil.getMostExpensivePermanentAI(list, sa, true);
            }

            tgt.addTarget(t);
            list.remove(t);

            hasCreature = false;
            hasArtifact = false;
            hasLand = false;
            hasEnchantment = false;
        }

        return true;

    }

    @Override
    protected boolean doTriggerAINoCost(AIPlayer ai, SpellAbility sa, boolean mandatory) {
        if (sa.getTarget() == null) {
            if (mandatory) {
                return true;
            }
        } else {
            return this.canPlayAI(ai, sa);
        }

        return true;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, final AIPlayer ai) {
        if ((sa.getTarget() == null) || !sa.getTarget().doesTarget()) {
            if (sa.hasParam("AllValid")) {
                List<Card> tgtCards = CardLists.filterControlledBy(Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield), ai.getOpponent());
                tgtCards = AbilityUtils.filterListByType(tgtCards, sa.getParam("AllValid"), sa);
                if (tgtCards.isEmpty()) {
                    return false;
                }
            }

            final List<String> lose = sa.hasParam("LoseControl") ? Arrays.asList(sa.getParam("LoseControl").split(",")) : null;
            if ((lose != null) && lose.contains("EOT")
                    && Singletons.getModel().getGame().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                return false;
            }
        } else {
            return this.canPlayAI(ai, sa);
        }

        return true;
    } // pumpDrawbackAI()
}
