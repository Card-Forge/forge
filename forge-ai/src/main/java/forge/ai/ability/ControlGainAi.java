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

import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.staticability.StaticAbilityMustTarget;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;


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
        final List<String> lose = Lists.newArrayList();

        if (sa.hasParam("LoseControl")) {
            lose.addAll(Lists.newArrayList(sa.getParam("LoseControl").split(",")));
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Game game = ai.getGame();
        final PlayerCollection opponents = ai.getOpponents();

        // if Defined, then don't worry about targeting
        if (tgt == null) {
            if (sa.hasParam("AllValid")) {
                CardCollectionView tgtCards = CardLists.filterControlledBy(game.getCardsIn(ZoneType.Battlefield), opponents);
                tgtCards = AbilityUtils.filterListByType(tgtCards, sa.getParam("AllValid"), sa);
                return !tgtCards.isEmpty();
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
                List<Player> oppList = opponents.filter(PlayerPredicates.isTargetableBy(sa));

                if (oppList.isEmpty()) {
                    return false;
                }

                if (tgt.isRandomTarget()) {
                    sa.getTargets().add(Aggregates.random(oppList));
                } else {
                    sa.getTargets().add(oppList.get(0));
                }
            }
        }

        // Don't steal something if I can't Attack without, or prevent it from blocking at least
        if (lose.contains("EOT")
                && ai.getGame().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                && !sa.isTrigger()) {
            return false;
        }

        if (sa.hasParam("Defined")) {
            // no need to target, we'll pick up the target from Defined
            return true;
        }

        CardCollection list = new CardCollection();
        for (Player pl : opponents) {
            list.addAll(pl.getCardsIn(ZoneType.Battlefield));
        }

        list = CardLists.getValidCards(list, tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getHostCard(), sa);
        
        if (list.isEmpty()) {
            // no valid targets, so we need to bail
            return false;
        }

        // AI won't try to grab cards that are filtered out of AI decks on purpose
        list = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                if (!c.canBeTargetedBy(sa)) {
                    return false;
                }
                if (sa.isTrigger()) {
                    return true;
                }

                // do not take perm control on something that leaves the play end of turn 
                if (!lose.contains("EOT") && c.hasSVar("EndOfTurnLeavePlay")) {
                    return false;
                }

                if (c.isCreature()) {
                    if (c.getNetCombatDamage() <= 0) {
                        return false;
                    }

                    // can not attack any opponent
                    boolean found = false;
                    for (final Player opp : opponents) {
                        if (ComputerUtilCombat.canAttackNextTurn(c, opp)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        return false;
                    }
                }

                // do not take control on something it doesn't know how to use
                return !ComputerUtilCard.isCardRemAIDeck(c);
            }
        });

        if (list.isEmpty()) {
            return false;
        }

        int creatures = 0, artifacts = 0, planeswalkers = 0, lands = 0, enchantments = 0;

        for (final Card c : list) {
            if (c.isCreature()) {
                creatures++;
            }
            if (c.isArtifact()) {
                artifacts++;
            }
            if (c.isLand()) {
                lands++;
            }
            if (c.isEnchantment()) {
                enchantments++;
            }
            if (c.isPlaneswalker()) {
                planeswalkers++;
            }
        }

        while (sa.getTargets().size() < tgt.getMaxTargets(sa.getHostCard(), sa)) {
            Card t = null;

            if (list.isEmpty()) {
                if ((sa.getTargets().size() < tgt.getMinTargets(sa.getHostCard(), sa)) || (sa.getTargets().size() == 0)) {
                    sa.resetTargets();
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            // TODO check life of controller and consider stealing from another opponent so the risk of your army disappearing is spread out
            while (t == null) {
                // filter by MustTarget requirement
                CardCollection originalList = new CardCollection(list);
                boolean mustTargetFiltered = StaticAbilityMustTarget.filterMustTargetCards(ai, list, sa);

                if (planeswalkers > 0) {
                    t = ComputerUtilCard.getBestPlaneswalkerAI(list);
                } else if (creatures > 0) {
                    t = ComputerUtilCard.getBestCreatureAI(list);
                } else if (artifacts > 0) {
                    t = ComputerUtilCard.getBestArtifactAI(list);
                } else if (lands > 0) {
                    t = ComputerUtilCard.getBestLandAI(list);
                } else if (enchantments > 0) {
                    t = ComputerUtilCard.getBestEnchantmentAI(list, sa, true);
                } else {
                    t = ComputerUtilCard.getMostExpensivePermanentAI(list, sa, true);
                }

                if (t != null) {
                    if (t.isCreature())
                        creatures--;
                    if (t.isPlaneswalker())
                        planeswalkers--;
                    if (t.isLand())
                        lands--;
                    if (t.isArtifact())
                        artifacts--;
                    if (t.isEnchantment())
                        enchantments--;
                }

                // Restore original list for next loop if filtered by MustTarget requirement
                if (mustTargetFiltered) {
                    list = originalList;
                }

                if (!sa.canTarget(t)) {
                    list.remove(t);
                    t = null;
                    if (list.isEmpty()) {
                        break;
                    }
                }
            }

            if (t != null) {
                sa.getTargets().add(t);
                list.remove(t);
            }
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
                }
                sa.getTargets().add(ComputerUtilCard.getWorstAI(list));
            }
        }

        return true;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, final Player ai) {
        final Game game = ai.getGame();
        if (!sa.usesTargeting()) {
            if (sa.hasParam("AllValid")) {
                CardCollectionView tgtCards = CardLists.filterControlledBy(game.getCardsIn(ZoneType.Battlefield), ai.getOpponents());
                tgtCards = AbilityUtils.filterListByType(tgtCards, sa.getParam("AllValid"), sa);
                if (tgtCards.isEmpty()) {
                    return false;
                }
            }
            final List<String> lose = Lists.newArrayList();

            if (sa.hasParam("LoseControl")) {
                lose.addAll(Lists.newArrayList(sa.getParam("LoseControl").split(",")));
            }

            return !lose.contains("EOT")
                    || !game.getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS);
        } else {
            return this.canPlayAI(ai, sa);
        }

    } // pumpDrawbackAI()

    @Override
    protected Player chooseSinglePlayer(Player ai, SpellAbility sa, Iterable<Player> options, Map<String, Object> params) {
        final List<Card> cards = Lists.newArrayList();
        for (Player p : options) {
            cards.addAll(p.getCreaturesInPlay());
        }
        Card chosen = ComputerUtilCard.getBestCreatureAI(cards);
        return chosen != null ? chosen.getController() : Iterables.getFirst(options, null);
    }
}
