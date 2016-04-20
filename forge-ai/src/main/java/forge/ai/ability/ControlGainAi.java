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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.ai.AiCardMemory;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.PlayerControllerAi;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


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
public class ControlGainAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(final Player ai, final SpellAbility sa) {
        boolean hasCreature = false;
        boolean hasArtifact = false;
        boolean hasEnchantment = false;
        boolean hasLand = false;
        boolean hasPW = false;

        final List<String> lose = sa.hasParam("LoseControl") ? Arrays.asList(sa.getParam("LoseControl").split(",")) : null;

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        Player opp = ai.getOpponent();

        // if Defined, then don't worry about targeting
        if (tgt == null) {
            if (sa.hasParam("AllValid")) {
                CardCollectionView tgtCards = ai.getOpponent().getCardsIn(ZoneType.Battlefield);
                tgtCards = AbilityUtils.filterListByType(tgtCards, sa.getParam("AllValid"), sa);
                if (tgtCards.isEmpty()) {
                    return false;
                }
            }
            return true;
        } else {
            sa.resetTargets();
            if (sa.hasParam("TargetingPlayer")) {
                Player targetingPlayer = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("TargetingPlayer"), sa).get(0);
                sa.setTargetingPlayer(targetingPlayer);
                return targetingPlayer.getController().chooseTargetsFor(sa);
            }
            if (tgt.canOnlyTgtOpponent()) {
                if (!opp.canBeTargetedBy(sa)) {
                    return false;
                }
                if (tgt.isRandomTarget()) {
                    sa.getTargets().add(Aggregates.random(tgt.getAllCandidates(sa, false)));
                } else {
                    sa.getTargets().add(opp);
                }
            }
        }

        // Don't steal something if I can't Attack without, or prevent it from
        // blocking at least
        if (lose != null && lose.contains("EOT")
                && ai.getGame().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                && !sa.isTrigger()) {
            return false;
        }

        CardCollection list =
                CardLists.getValidCards(opp.getCardsIn(ZoneType.Battlefield), tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getHostCard(), sa);
        
        // AI won't try to grab cards that are filtered out of AI decks on purpose
        list = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                final Map<String, String> vars = c.getSVars();
                if (!c.canBeTargetedBy(sa)) {
                    return false;
                }
                if (sa.isTrigger()) {
                    return true;
                }
                if (c.isCreature() && (!ComputerUtilCombat.canAttackNextTurn(c, ai.getOpponent()) || c.getNetCombatDamage() == 0)) {
                    return false;
                }
                return !vars.containsKey("RemAIDeck");
            }
        });

        if (list.isEmpty()) {
            return false;
        }

        while (sa.getTargets().getNumTargeted() < tgt.getMaxTargets(sa.getHostCard(), sa)) {
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
                if (c.isPlaneswalker()) {
                    hasPW = true;
                }
            }

            if (list.isEmpty()) {
                if ((sa.getTargets().getNumTargeted() < tgt.getMinTargets(sa.getHostCard(), sa)) || (sa.getTargets().getNumTargeted() == 0)) {
                    sa.resetTargets();
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            if (hasPW) {
                t = ComputerUtilCard.getBestPlaneswalkerAI(list);
            } else if (hasCreature) {
                t = ComputerUtilCard.getBestCreatureAI(list);
                if (lose != null && lose.contains("EOT")) {
                    // Remember to always attack with this creature since it'll bounce back to its owner at end of turn anyway
                    ((PlayerControllerAi)ai.getController()).getAi().getCardMemory().rememberCard(t, AiCardMemory.MemorySet.MANDATORY_ATTACKERS);
                }
            } else if (hasArtifact) {
                t = ComputerUtilCard.getBestArtifactAI(list);
            } else if (hasLand) {
                t = ComputerUtilCard.getBestLandAI(list);
            } else if (hasEnchantment) {
                t = ComputerUtilCard.getBestEnchantmentAI(list, sa, true);
            } else {
                t = ComputerUtilCard.getMostExpensivePermanentAI(list, sa, true);
            }

            sa.getTargets().add(t);
            list.remove(t);

            hasCreature = false;
            hasArtifact = false;
            hasLand = false;
            hasEnchantment = false;
            hasPW = false;
        }

        return true;

    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        if (sa.getTargetRestrictions() == null) {
            if (mandatory) {
                return true;
            }
        } else {
            if(sa.hasParam("TargetingPlayer") || (!this.canPlayAI(ai, sa) && mandatory)) {
                List<Card> list = CardLists.getTargetableCards(ai.getCardsIn(ZoneType.Battlefield), sa);
                if (list.isEmpty()) {
                    return false;
                } else {
                    sa.getTargets().add(ComputerUtilCard.getWorstAI(list));
                }
            }
        }

        return true;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, final Player ai) {
        final Game game = ai.getGame();
        if ((sa.getTargetRestrictions() == null) || !sa.getTargetRestrictions().doesTarget()) {
            if (sa.hasParam("AllValid")) {
                CardCollectionView tgtCards = CardLists.filterControlledBy(game.getCardsIn(ZoneType.Battlefield), ai.getOpponent());
                tgtCards = AbilityUtils.filterListByType(tgtCards, sa.getParam("AllValid"), sa);
                if (tgtCards.isEmpty()) {
                    return false;
                }
            }

            final List<String> lose = sa.hasParam("LoseControl") ? Arrays.asList(sa.getParam("LoseControl").split(",")) : null;
            if ((lose != null) && lose.contains("EOT")
                    && game.getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                return false;
            }
        } else {
            return this.canPlayAI(ai, sa);
        }

        return true;
    } // pumpDrawbackAI()

    @Override
    protected Player chooseSinglePlayer(Player ai, SpellAbility sa, Iterable<Player> options) {
        final List<Card> cards = new ArrayList<Card>();
        for (Player p : options) {
            cards.addAll(p.getCreaturesInPlay());
        }
        Card chosen = ComputerUtilCard.getBestCreatureAI(cards);
        return chosen != null ? chosen.getController() : Iterables.getFirst(options, null);
    }
}
