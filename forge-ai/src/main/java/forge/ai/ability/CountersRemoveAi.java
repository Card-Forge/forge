package forge.ai.ability;

import com.google.common.collect.Iterables;
import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCost;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.card.*;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class CountersRemoveAi extends SpellAbilityAi {

    /*
     * (non-Javadoc)
     *
     * @see
     * forge.ai.SpellAbilityAi#checkPhaseRestrictions(forge.game.player.Player,
     * forge.game.spellability.SpellAbility, forge.game.phase.PhaseHandler)
     */
    @Override
    protected boolean checkPhaseRestrictions(Player ai, SpellAbility sa, PhaseHandler ph) {
        final String type = sa.getParam("CounterType");

        if (ph.getPhase().isBefore(PhaseType.MAIN2) && !sa.hasParam("ActivationPhases") && !type.equals("M1M1")) {
            return false;
        }
        return super.checkPhaseRestrictions(ai, sa, ph);
    }

    /*
     * (non-Javadoc)
     *
     * @see forge.ai.SpellAbilityAi#checkApiLogic(forge.game.player.Player,
     * forge.game.spellability.SpellAbility)
     */
    @Override
    protected AiAbilityDecision checkApiLogic(Player ai, SpellAbility sa) {
        final String type = sa.getParam("CounterType");

        if (sa.usesTargeting()) {
            return doTgt(ai, sa, false);
        }

        if (!type.matches("Any") && !type.matches("All")) {
            final int currCounters = sa.getHostCard().getCounters(CounterType.getType(type));
            if (currCounters < 1) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }

        return super.checkApiLogic(ai, sa);
    }

    private AiAbilityDecision doTgt(Player ai, SpellAbility sa, boolean mandatory) {
        final Card source = sa.getHostCard();
        final Game game = ai.getGame();

        final String type = sa.getParam("CounterType");
        final String amountStr = sa.getParamOrDefault("CounterNum", "1");

        // remove counter with Time might use Exile Zone too
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        // need to targetable
        CardCollection list = CardLists.getTargetableCards(game.getCardsIn(tgt.getZone()), sa);

        if (list.isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
        }

        // Filter AI-specific targets if provided
        list = ComputerUtil.filterAITgts(sa, ai, list, false);

        CardCollectionView marit = ai.getCardsIn(ZoneType.Battlefield, "Marit Lage");
        boolean maritEmpty = marit.isEmpty() || Iterables.contains(marit, (Predicate<Card>) Card::ignoreLegendRule);

        if (type.matches("All")) {
            // Logic Part for Vampire Hexmage
            // Break Dark Depths
            if (maritEmpty) {
                CardCollectionView depthsList = ai.getCardsIn(ZoneType.Battlefield, "Dark Depths");
                depthsList = CardLists.filter(depthsList, CardPredicates.isTargetableBy(sa),
                        CardPredicates.hasCounter(CounterEnumType.ICE, 3));
                if (!depthsList.isEmpty()) {
                    sa.getTargets().add(depthsList.getFirst());
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            }

            // Get rid of Planeswalkers:
            list = ai.getOpponents().getCardsIn(ZoneType.Battlefield);
            list = CardLists.filter(list, CardPredicates.isTargetableBy(sa));

            CardCollection planeswalkerList = CardLists.filter(list, CardPredicates.PLANESWALKERS,
                    CardPredicates.hasCounter(CounterEnumType.LOYALTY, 5));

            if (!planeswalkerList.isEmpty()) {
                sa.getTargets().add(ComputerUtilCard.getBestPlaneswalkerAI(planeswalkerList));
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
        } else if (type.matches("Any")) {
            // variable amount for Hex Parasite
            int amount;
            boolean xPay = false;
            if (amountStr.equals("X") && sa.getSVar("X").equals("Count$xPaid")) {
                final int manaLeft = ComputerUtilCost.getMaxXValue(sa, ai, sa.isTrigger());

                if (manaLeft == 0) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantAffordX);
                }
                amount = manaLeft;
                xPay = true;
            } else {
                amount = AbilityUtils.calculateAmount(source, amountStr, sa);
            }
            // try to remove them from Dark Depths and Planeswalkers too

            if (maritEmpty) {
                CardCollectionView depthsList = ai.getCardsIn(ZoneType.Battlefield, "Dark Depths");
                depthsList = CardLists.filter(depthsList, CardPredicates.isTargetableBy(sa),
                        CardPredicates.hasCounter(CounterEnumType.ICE));

                if (!depthsList.isEmpty()) {
                    Card depth = depthsList.getFirst();
                    int ice = depth.getCounters(CounterEnumType.ICE);
                    if (amount >= ice) {
                        sa.getTargets().add(depth);
                        if (xPay) {
                            sa.setXManaCostPaid(ice);
                        }
                        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                    }
                }
            }

            // Get rid of Planeswalkers:
            list = game.getPlayers().getCardsIn(ZoneType.Battlefield);
            list = CardLists.filter(list, CardPredicates.isTargetableBy(sa));

            CardCollection planeswalkerList = CardLists.filter(list,
                    CardPredicates.PLANESWALKERS.and(CardPredicates.isControlledByAnyOf(ai.getOpponents())),
                    CardPredicates.hasLessCounter(CounterEnumType.LOYALTY, amount));

            if (!planeswalkerList.isEmpty()) {
                Card best = ComputerUtilCard.getBestPlaneswalkerAI(planeswalkerList);
                sa.getTargets().add(best);
                if (xPay) {
                    sa.setXManaCostPaid(best.getCurrentLoyalty());
                }
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }

            // some rules only for amount = 1
            if (!xPay) {
                // do as M1M1 part
                CardCollection aiList = CardLists.filterControlledBy(list, ai);

                CardCollection aiM1M1List = CardLists.filter(aiList, CardPredicates.hasCounter(CounterEnumType.M1M1));

                CardCollection aiPersistList = CardLists.getKeyword(aiM1M1List, Keyword.PERSIST);
                if (!aiPersistList.isEmpty()) {
                    aiM1M1List = aiPersistList;
                }

                if (!aiM1M1List.isEmpty()) {
                    sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(aiM1M1List));
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }

                // do as P1P1 part
                CardCollection aiP1P1List = CardLists.filter(aiList, CardPredicates.hasLessCounter(CounterEnumType.P1P1, amount));
                CardCollection aiUndyingList = CardLists.getKeyword(aiP1P1List, Keyword.UNDYING);

                if (!aiUndyingList.isEmpty()) {
                    sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(aiUndyingList));
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
  
                // TODO stun counters with canRemoveCounters check

                // remove P1P1 counters from opposing creatures
                CardCollection oppP1P1List = CardLists.filter(list,
                        CardPredicates.CREATURES.and(CardPredicates.isControlledByAnyOf(ai.getOpponents())),
                        CardPredicates.hasCounter(CounterEnumType.P1P1));
                if (!oppP1P1List.isEmpty()) {
                    sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(oppP1P1List));
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }

                // fallback to remove any counter from opponent
                CardCollection oppList = CardLists.filterControlledBy(list, ai.getOpponents());
                oppList = CardLists.filter(oppList, CardPredicates.hasCounters());
                if (!oppList.isEmpty()) {
                    final Card best = ComputerUtilCard.getBestAI(oppList);

                    for (final CounterType aType : best.getCounters().keySet()) {
                        if (!ComputerUtil.isNegativeCounter(aType, best)) {
                            sa.getTargets().add(best);
                            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                        }
                    }
                }
            }
        } else if (type.equals("M1M1")) {
            // no special amount for that one yet
            int amount = AbilityUtils.calculateAmount(source, amountStr, sa);
            CardCollection aiList = CardLists.filterControlledBy(list, ai);
            aiList = CardLists.filter(aiList, CardPredicates.hasCounter(CounterEnumType.M1M1, amount));

            CardCollection aiPersist = CardLists.getKeyword(aiList, Keyword.PERSIST);
            if (!aiPersist.isEmpty()) {
                aiList = aiPersist;
            }

            // TODO do not remove -1/-1 counters from cards which does need
            // them for abilities

            if (!aiList.isEmpty()) {
                sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(aiList));
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
        } else if (type.equals("P1P1")) {
            // no special amount for that one yet
            int amount = AbilityUtils.calculateAmount(source, amountStr, sa);

            list = CardLists.filter(list, CardPredicates.hasCounter(CounterEnumType.P1P1, amount));

            // currently only logic for Bloodcrazed Hoplite, but add logic for
            // targeting ai creatures too
            CardCollection aiList = CardLists.filterControlledBy(list, ai);
            if (!aiList.isEmpty()) {
                CardCollection aiListUndying = CardLists.getKeyword(aiList, Keyword.UNDYING);
                if (!aiListUndying.isEmpty()) {
                    aiList = aiListUndying;
                }
                if (!aiList.isEmpty()) {
                    sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(aiList));
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            }

            // need to target opponent creatures
            CardCollection oppList = CardLists.filterControlledBy(list, ai.getOpponents());
            if (!oppList.isEmpty()) {
                CardCollection oppListNotUndying = CardLists.getNotKeyword(oppList, Keyword.UNDYING);
                if (!oppListNotUndying.isEmpty()) {
                    oppList = oppListNotUndying;
                }

                if (!oppList.isEmpty()) {
                    sa.getTargets().add(ComputerUtilCard.getWorstCreatureAI(oppList));
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            }
        } else if (type.equals("TIME")) {
            int amount;
            boolean xPay = false;
            // Timecrafting has X R
            if (amountStr.equals("X") && sa.getSVar("X").equals("Count$xPaid")) {
                final int manaLeft = ComputerUtilCost.getMaxXValue(sa, ai, sa.isTrigger());

                if (manaLeft == 0) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantAffordX);
                }
                amount = manaLeft;
                xPay = true;
            } else {
                amount = AbilityUtils.calculateAmount(source, amountStr, sa);
            }

            CardCollection timeList = CardLists.filter(list, CardPredicates.hasLessCounter(CounterEnumType.TIME, amount));

            if (!timeList.isEmpty()) {
                Card best = ComputerUtilCard.getBestAI(timeList);

                int timeCount = best.getCounters(CounterEnumType.TIME);
                sa.getTargets().add(best);
                if (xPay) {
                    sa.setXManaCostPaid(timeCount);
                }
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
        }
        if (mandatory) {
            if (type.equals("P1P1")) {
                // Try to target creatures with Adapt or similar
                CardCollection adaptCreats = CardLists.filter(list, c -> c.getNonManaAbilities().anyMatch(ab -> ab.hasParam("Adapt")));
                if (!adaptCreats.isEmpty()) {
                    sa.getTargets().add(ComputerUtilCard.getWorstAI(adaptCreats));
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }

                // Outlast nice target
                CardCollection outlastCreats = CardLists.filter(list, CardPredicates.hasKeyword(Keyword.OUTLAST));
                if (!outlastCreats.isEmpty()) {
                    // outlast cards often benefit from having +1/+1 counters, try not to remove last one
                    CardCollection betterTargets = CardLists.filter(outlastCreats, CardPredicates.hasCounter(CounterEnumType.P1P1, 2));

                    if (!betterTargets.isEmpty()) {
                        sa.getTargets().add(ComputerUtilCard.getWorstAI(betterTargets));
                        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                    }

                    sa.getTargets().add(ComputerUtilCard.getWorstAI(outlastCreats));
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            }

            sa.getTargets().add(ComputerUtilCard.getWorstAI(list));
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        if (sa.usesTargeting()) {
            return doTgt(aiPlayer, sa, mandatory);
        }
        return mandatory ? new AiAbilityDecision(100, AiPlayDecision.MandatoryPlay)
                         : new AiAbilityDecision(0, AiPlayDecision.CantPlaySa);
    }

    /*
     * (non-Javadoc)
     *
     * @see forge.ai.SpellAbilityAi#chooseNumber(forge.game.player.Player,
     * forge.game.spellability.SpellAbility, int, int, java.util.Map)
     */
    @Override
    public int chooseNumber(Player player, SpellAbility sa, int min, int max, Map<String, Object> params) {
        GameEntity target = (GameEntity) params.get("Target");
        CounterType type = (CounterType) params.get("CounterType");

        if (target instanceof Card targetCard) {
            if (targetCard.getController().isOpponentOf(player)) {
                return !ComputerUtil.isNegativeCounter(type, targetCard) ? max : min;
            } else {
                if (targetCard.hasKeyword(Keyword.UNDYING) && type.is(CounterEnumType.P1P1)
                        && targetCard.getCounters(CounterEnumType.P1P1) >= max) {
                    return max;
                }

                return ComputerUtil.isNegativeCounter(type, targetCard) ? max : min;
            }
        } else if (target instanceof Player targetPlayer) {
            if (targetPlayer.isOpponentOf(player)) {
                return !type.is(CounterEnumType.POISON) ? max : min;
            } else {
                return type.is(CounterEnumType.POISON) ? max : min;
            }
        }

        return super.chooseNumber(player, sa, min, max, params);
    }

    /*
     * (non-Javadoc)
     *
     * @see forge.ai.SpellAbilityAi#chooseCounterType(java.util.List,
     * forge.game.spellability.SpellAbility, java.util.Map)
     */
    @Override
    public CounterType chooseCounterType(List<CounterType> options, SpellAbility sa, Map<String, Object> params) {
        Player ai = sa.getActivatingPlayer();
        GameEntity target = (GameEntity) params.get("Target");

        if (target instanceof Card) {
            Card targetCard = (Card) target;
            if (targetCard.getController().isOpponentOf(ai)) {
                // if its a Planeswalker try to remove Loyality first
                if (targetCard.isPlaneswalker()) {
                    return CounterEnumType.LOYALTY;
                }
                for (CounterType type : options) {
                    if (!ComputerUtil.isNegativeCounter(type, targetCard)) {
                        return type;
                    }
                }
            } else {
                if (options.contains(CounterEnumType.M1M1) && targetCard.hasKeyword(Keyword.PERSIST)) {
                    return CounterEnumType.M1M1;
                } else if (options.contains(CounterEnumType.P1P1) && targetCard.hasKeyword(Keyword.UNDYING)) {
                    return CounterEnumType.P1P1;
                }
                for (CounterType type : options) {
                    if (ComputerUtil.isNegativeCounter(type, targetCard)) {
                        return type;
                    }
                }
            }
        } else if (target instanceof Player) {
            Player targetPlayer = (Player) target;
            if (targetPlayer.isOpponentOf(ai)) {
                for (CounterType type : options) {
                    if (!type.is(CounterEnumType.POISON)) {
                        return type;
                    }
                }
            } else {
                for (CounterType type : options) {
                    if (type.is(CounterEnumType.POISON)) {
                        return type;
                    }
                }
            }
        }

        return super.chooseCounterType(options, sa, params);
    }
}
