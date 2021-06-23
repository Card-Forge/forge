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

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.GlobalRuleChange;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.player.PlayerController.BinaryChoiceType;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;

/**
 * <p>
 * AbilityFactory_PutOrRemoveCountersAi class.
 * </p>
 *
 * @author Forge
 * @version $Id$
 */
public class CountersPutOrRemoveAi extends SpellAbilityAi {

    /*
     * (non-Javadoc)
     *
     * @see forge.ai.SpellAbilityAi#checkApiLogic(forge.game.player.Player,
     * forge.game.spellability.SpellAbility)
     */
    @Override
    protected boolean checkApiLogic(Player ai, SpellAbility sa) {
        if (sa.usesTargeting()) {
            return doTgt(ai, sa, false);
        }
        return super.checkApiLogic(ai, sa);
    }

    private boolean doTgt(Player ai, SpellAbility sa, boolean mandatory) {
        final Game game = ai.getGame();

        final int amount = Integer.valueOf(sa.getParam("CounterNum"));

        // remove counter with Time might use Exile Zone too
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        // need to targetable
        CardCollection list = CardLists.getTargetableCards(game.getCardsIn(tgt.getZone()), sa);

        if (list.isEmpty()) {
            return false;
        }

        // Filter AI-specific targets if provided
        list = ComputerUtil.filterAITgts(sa, ai, list, false);

        if (sa.hasParam("CounterType")) {
            // currently only Jhoira's Timebug
            final CounterType type = CounterType.getType(sa.getParam("CounterType"));

            CardCollection countersList = CardLists.filter(list, CardPredicates.hasCounter(type, amount));

            if (countersList.isEmpty()) {
                return false;
            }

            // currently can only target cards you control or you own
            final Card best = ComputerUtilCard.getBestAI(countersList);

            // currently both cards only has one target
            sa.getTargets().add(best);
            return true;
        } else {
            // currently only Clockspinning
            boolean noLegendary = game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noLegendRule);

            // logic to remove some counter
            CardCollection countersList = CardLists.filter(list, CardPredicates.hasCounters());

            if (!countersList.isEmpty()) {

                if (!ai.isCardInPlay("Marit Lage") || noLegendary) {
                    CardCollectionView depthsList = CardLists.filter(countersList,
                            CardPredicates.nameEquals("Dark Depths"), CardPredicates.hasCounter(CounterEnumType.ICE));

                    if (!depthsList.isEmpty()) {
                        sa.getTargets().add(depthsList.getFirst());
                        return true;
                    }
                }

                // Get rid of Planeswalkers, currently only if it can kill them
                // with one touch
                CardCollection planeswalkerList = CardLists.filter(
                        CardLists.filterControlledBy(countersList, ai.getOpponents()),
                        CardPredicates.Presets.PLANESWALKERS,
                        CardPredicates.hasLessCounter(CounterEnumType.LOYALTY, amount));

                if (!planeswalkerList.isEmpty()) {
                    sa.getTargets().add(ComputerUtilCard.getBestPlaneswalkerAI(planeswalkerList));
                    return true;
                }

                // do as M1M1 part
                CardCollection aiList = CardLists.filterControlledBy(countersList, ai);

                CardCollection aiM1M1List = CardLists.filter(aiList, CardPredicates.hasCounter(CounterEnumType.M1M1));

                CardCollection aiPersistList = CardLists.getKeyword(aiM1M1List, Keyword.PERSIST);
                if (!aiPersistList.isEmpty()) {
                    aiM1M1List = aiPersistList;
                }

                if (!aiM1M1List.isEmpty()) {
                    sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(aiM1M1List));
                    return true;
                }

                // do as P1P1 part
                CardCollection aiP1P1List = CardLists.filter(aiList, CardPredicates.hasCounter(CounterEnumType.P1P1));
                CardCollection aiUndyingList = CardLists.getKeyword(aiM1M1List, Keyword.UNDYING);

                if (!aiUndyingList.isEmpty()) {
                    aiP1P1List = aiUndyingList;
                }
                if (!aiP1P1List.isEmpty()) {
                    sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(aiP1P1List));
                    return true;
                }

                // fallback to remove any counter from opponent
                CardCollection oppList = CardLists.filterControlledBy(countersList, ai.getOpponents());
                oppList = CardLists.filter(oppList, CardPredicates.hasCounters());
                if (!oppList.isEmpty()) {
                    final Card best = ComputerUtilCard.getBestAI(oppList);

                    for (final CounterType aType : best.getCounters().keySet()) {
                        if (!ComputerUtil.isNegativeCounter(aType, best)) {
                            sa.getTargets().add(best);
                            return true;
                        } else if (!ComputerUtil.isUselessCounter(aType, best)) {
                            // whould remove positive counter
                            if (best.getCounters(aType) <= amount) {
                                sa.getTargets().add(best);
                                return true;
                            }
                        }
                    }
                }
            }
        }

        if (mandatory) {
            sa.getTargets().add(ComputerUtilCard.getWorstAI(list));
            return true;
        }
        return false;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        return doTgt(ai, sa, true);
    }

    /*
     * (non-Javadoc)
     *
     * @see forge.ai.SpellAbilityAi#chooseCounterType(java.util.List,
     * forge.game.spellability.SpellAbility, java.util.Map)
     */
    @Override
    public CounterType chooseCounterType(List<CounterType> options, SpellAbility sa, Map<String, Object> params) {
        if (options.size() > 1) {
            final Player ai = sa.getActivatingPlayer();
            final Game game = ai.getGame();

            boolean noLegendary = game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noLegendRule);

            Card tgt = (Card) params.get("Target");

            // planeswalker has high priority for loyalty counters
            if (tgt.isPlaneswalker() && options.contains(CounterType.get(CounterEnumType.LOYALTY))) {
                return CounterType.get(CounterEnumType.LOYALTY);
            }

            if (tgt.getController().isOpponentOf(ai)) {
                // creatures with BaseToughness below or equal zero might be
                // killed if their counters are removed
                if (tgt.isCreature() && tgt.getBaseToughness() <= 0) {
                    if (options.contains(CounterType.get(CounterEnumType.P1P1))) {
                        return CounterType.get(CounterEnumType.P1P1);
                    } else if (options.contains(CounterType.get(CounterEnumType.M1M1))) {
                        return CounterType.get(CounterEnumType.M1M1);
                    }
                }

                // fallback logic, select positive counter to remove it
                for (final CounterType type : options) {
                    if (!ComputerUtil.isNegativeCounter(type, tgt)) {
                        return type;
                    }
                }
            } else {
                // this counters are treat first to be removed
                if ("Dark Depths".equals(tgt.getName()) && options.contains(CounterType.get(CounterEnumType.ICE))) {
                    if (!ai.isCardInPlay("Marit Lage") || noLegendary) {
                        return CounterType.get(CounterEnumType.ICE);
                    }
                } else if (tgt.hasKeyword(Keyword.UNDYING) && options.contains(CounterType.get(CounterEnumType.P1P1))) {
                    return CounterType.get(CounterEnumType.P1P1);
                } else if (tgt.hasKeyword(Keyword.PERSIST) && options.contains(CounterType.get(CounterEnumType.M1M1))) {
                    return CounterType.get(CounterEnumType.M1M1);
                }

                // fallback logic, select positive counter to add more
                for (final CounterType type : options) {
                    if (!ComputerUtil.isNegativeCounter(type, tgt)) {
                        return type;
                    }
                }
            }
        }

        return super.chooseCounterType(options, sa, params);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * forge.ai.SpellAbilityAi#chooseBinary(forge.game.player.PlayerController.
     * BinaryChoiceType, forge.game.spellability.SpellAbility, java.util.Map)
     */
    @Override
    public boolean chooseBinary(BinaryChoiceType kindOfChoice, SpellAbility sa, Map<String, Object> params) {
        if (kindOfChoice.equals(BinaryChoiceType.AddOrRemove)) {
            final Player ai = sa.getActivatingPlayer();
            final Game game = ai.getGame();
            Card tgt = (Card) params.get("Target");
            CounterType type = (CounterType) params.get("CounterType");

            boolean noLegendary = game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noLegendRule);

            if (tgt.getController().isOpponentOf(ai)) {
                if (type.is(CounterEnumType.LOYALTY) && tgt.isPlaneswalker()) {
                    return false;
                }

                return ComputerUtil.isNegativeCounter(type, tgt);
            } else {
                if (type.is(CounterEnumType.ICE) && "Dark Depths".equals(tgt.getName())) {
                    if (!ai.isCardInPlay("Marit Lage") || noLegendary) {
                        return false;
                    }
                } else if (type.is(CounterEnumType.M1M1) && tgt.hasKeyword(Keyword.PERSIST)) {
                    return false;
                } else if (type.is(CounterEnumType.P1P1) && tgt.hasKeyword(Keyword.UNDYING)) {
                    return false;
                }

                return !ComputerUtil.isNegativeCounter(type, tgt);
            }
        }
        return super.chooseBinary(kindOfChoice, sa, params);
    }

}
