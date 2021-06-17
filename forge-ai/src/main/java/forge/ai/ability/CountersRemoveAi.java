package forge.ai.ability;

import java.util.List;
import java.util.Map;

import com.google.common.base.Predicates;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCost;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GlobalRuleChange;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

public class CountersRemoveAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayWithoutRestrict(final Player ai, final SpellAbility sa) {
        if ("Always".equals(sa.getParam("AILogic"))) {
            return true;
        }
        return super.canPlayWithoutRestrict(ai, sa);
    }

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
     * @see
     * forge.ai.SpellAbilityAi#checkPhaseRestrictions(forge.game.player.Player,
     * forge.game.spellability.SpellAbility, forge.game.phase.PhaseHandler,
     * java.lang.String)
     */
    @Override
    protected boolean checkPhaseRestrictions(Player ai, SpellAbility sa, PhaseHandler ph, String logic) {
        if ("EndOfOpponentsTurn".equals(logic)) {
            if (!ph.is(PhaseType.END_OF_TURN) || !ph.getNextTurn().equals(ai)) {
                return false;
            }
        }
        return super.checkPhaseRestrictions(ai, sa, ph, logic);
    }

    /*
     * (non-Javadoc)
     *
     * @see forge.ai.SpellAbilityAi#checkApiLogic(forge.game.player.Player,
     * forge.game.spellability.SpellAbility)
     */
    @Override
    protected boolean checkApiLogic(Player ai, SpellAbility sa) {
        final String type = sa.getParam("CounterType");

        if (sa.usesTargeting()) {
            return doTgt(ai, sa, false);
        }

        if (!type.matches("Any") && !type.matches("All")) {
            final int currCounters = sa.getHostCard().getCounters(CounterType.getType(type));
            if (currCounters < 1) {
                return false;
            }
        }

        return super.checkApiLogic(ai, sa);
    }

    private boolean doTgt(Player ai, SpellAbility sa, boolean mandatory) {
        final Card source = sa.getHostCard();
        final Game game = ai.getGame();

        final String type = sa.getParam("CounterType");
        final String amountStr = sa.getParam("CounterNum");

        // remove counter with Time might use Exile Zone too
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        CardCollection list = new CardCollection(game.getCardsIn(tgt.getZone()));
        // need to targetable
        list = CardLists.getTargetableCards(list, sa);

        if (list.isEmpty()) {
            return false;
        }

        // Filter AI-specific targets if provided
        list = ComputerUtil.filterAITgts(sa, ai, list, false);

        boolean noLegendary = game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noLegendRule);

        if (type.matches("All")) {
            // Logic Part for Vampire Hexmage
            // Break Dark Depths
            if (!ai.isCardInPlay("Marit Lage") || noLegendary) {
                CardCollectionView depthsList = ai.getCardsIn(ZoneType.Battlefield, "Dark Depths");
                depthsList = CardLists.filter(depthsList, CardPredicates.isTargetableBy(sa),
                        CardPredicates.hasCounter(CounterEnumType.ICE, 3));

                if (!depthsList.isEmpty()) {
                    sa.getTargets().add(depthsList.getFirst());
                    return true;
                }
            }

            // Get rid of Planeswalkers:
            list = ai.getOpponents().getCardsIn(ZoneType.Battlefield);
            list = CardLists.filter(list, CardPredicates.isTargetableBy(sa));

            CardCollection planeswalkerList = CardLists.filter(list, CardPredicates.Presets.PLANESWALKERS,
                    CardPredicates.hasCounter(CounterEnumType.LOYALTY, 5));

            if (!planeswalkerList.isEmpty()) {
                sa.getTargets().add(ComputerUtilCard.getBestPlaneswalkerAI(planeswalkerList));
                return true;
            }

        } else if (type.matches("Any")) {
            // variable amount for Hex Parasite
            int amount;
            boolean xPay = false;
            if (amountStr.equals("X") && sa.getSVar("X").equals("Count$xPaid")) {
                final int manaLeft = ComputerUtilCost.getMaxXValue(sa, ai);

                if (manaLeft == 0) {
                    return false;
                }
                amount = manaLeft;
                xPay = true;
            } else {
                amount = AbilityUtils.calculateAmount(source, amountStr, sa);
            }
            // try to remove them from Dark Depths and Planeswalkers too

            if (!ai.isCardInPlay("Marit Lage") || noLegendary) {
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
                        return true;
                    }
                }
            }

            // Get rid of Planeswalkers:
            list = game.getPlayers().getCardsIn(ZoneType.Battlefield);
            list = CardLists.filter(list, CardPredicates.isTargetableBy(sa));

            CardCollection planeswalkerList = CardLists.filter(list,
                    Predicates.and(CardPredicates.Presets.PLANESWALKERS, CardPredicates.isControlledByAnyOf(ai.getOpponents())),
                    CardPredicates.hasLessCounter(CounterEnumType.LOYALTY, amount));

            if (!planeswalkerList.isEmpty()) {
                Card best = ComputerUtilCard.getBestPlaneswalkerAI(planeswalkerList);
                sa.getTargets().add(best);
                if (xPay) {
                    sa.setXManaCostPaid(best.getCurrentLoyalty());
                }
                return true;
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
                    return true;
                }

                // do as P1P1 part
                CardCollection aiP1P1List = CardLists.filter(aiList, CardPredicates.hasLessCounter(CounterEnumType.P1P1, amount));
                CardCollection aiUndyingList = CardLists.getKeyword(aiP1P1List, Keyword.UNDYING);

                if (!aiUndyingList.isEmpty()) {
                    sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(aiUndyingList));
                    return true;
                }

                // remove P1P1 counters from opposing creatures
                CardCollection oppP1P1List = CardLists.filter(list,
                        Predicates.and(CardPredicates.Presets.CREATURES, CardPredicates.isControlledByAnyOf(ai.getOpponents())),
                        CardPredicates.hasCounter(CounterEnumType.P1P1));
                if (!oppP1P1List.isEmpty()) {
                    sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(oppP1P1List));
                    return true;
                }

                // fallback to remove any counter from opponent
                CardCollection oppList = CardLists.filterControlledBy(list, ai.getOpponents());
                oppList = CardLists.filter(oppList, CardPredicates.hasCounters());
                if (!oppList.isEmpty()) {
                    final Card best = ComputerUtilCard.getBestAI(oppList);

                    for (final CounterType aType : best.getCounters().keySet()) {
                        if (!ComputerUtil.isNegativeCounter(aType, best)) {
                            sa.getTargets().add(best);
                            return true;
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
                return true;
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
                    return true;
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
                    return true;
                }
            }

        } else if (type.equals("TIME")) {
            int amount;
            boolean xPay = false;
            // Timecrafting has X R
            if (amountStr.equals("X") && sa.getSVar("X").equals("Count$xPaid")) {
                final int manaLeft = ComputerUtilCost.getMaxXValue(sa, ai);

                if (manaLeft == 0) {
                    return false;
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
                return true;
            }
        }
        if (mandatory) {
            if (type.equals("P1P1")) {
                // Try to target creatures with Adapt or similar
                CardCollection adaptCreats = CardLists.filter(list, CardPredicates.hasKeyword(Keyword.ADAPT));
                if (!adaptCreats.isEmpty()) {
                    sa.getTargets().add(ComputerUtilCard.getWorstAI(adaptCreats));
                    return true;
                }

                // Outlast nice target
                CardCollection outlastCreats = CardLists.filter(list, CardPredicates.hasKeyword(Keyword.OUTLAST));
                if (!outlastCreats.isEmpty()) {
                    // outlast cards often benefit from having +1/+1 counters, try not to remove last one
                    CardCollection betterTargets = CardLists.filter(outlastCreats, CardPredicates.hasCounter(CounterEnumType.P1P1, 2));

                    if (!betterTargets.isEmpty()) {
                        sa.getTargets().add(ComputerUtilCard.getWorstAI(betterTargets));
                        return true;
                    }

                    sa.getTargets().add(ComputerUtilCard.getWorstAI(outlastCreats));
                    return true;
                }
            }

            sa.getTargets().add(ComputerUtilCard.getWorstAI(list));
            return true;
        }
        return false;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        if (sa.usesTargeting()) {
            return doTgt(aiPlayer, sa, mandatory);
        }
        return mandatory;
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

        if (target instanceof Card) {
            Card targetCard = (Card) target;
            if (targetCard.getController().isOpponentOf(player)) {
                return !ComputerUtil.isNegativeCounter(type, targetCard) ? max : min;
            } else {
                if (targetCard.hasKeyword(Keyword.UNDYING) && type.is(CounterEnumType.P1P1)
                        && targetCard.getCounters(CounterEnumType.P1P1) >= max) {
                    return max;
                }

                return ComputerUtil.isNegativeCounter(type, targetCard) ? max : min;
            }
        } else if (target instanceof Player) {
            Player targetPlayer = (Player) target;
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
        if (options.size() <= 1) {
            return super.chooseCounterType(options, sa, params);
        }
        Player ai = sa.getActivatingPlayer();
        GameEntity target = (GameEntity) params.get("Target");

        if (target instanceof Card) {
            Card targetCard = (Card) target;
            if (targetCard.getController().isOpponentOf(ai)) {
                // if its a Planeswalker try to remove Loyality first
                if (targetCard.isPlaneswalker()) {
                    return CounterType.get(CounterEnumType.LOYALTY);
                }
                for (CounterType type : options) {
                    if (!ComputerUtil.isNegativeCounter(type, targetCard)) {
                        return type;
                    }
                }
            } else {
                if (options.contains(CounterType.get(CounterEnumType.M1M1)) && targetCard.hasKeyword(Keyword.PERSIST)) {
                    return CounterType.get(CounterEnumType.M1M1);
                } else if (options.contains(CounterType.get(CounterEnumType.P1P1)) && targetCard.hasKeyword(Keyword.UNDYING)) {
                    return CounterType.get(CounterEnumType.P1P1);
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
