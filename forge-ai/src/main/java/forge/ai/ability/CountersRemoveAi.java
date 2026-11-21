package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCost;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.*;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

import java.util.List;
import java.util.Map;

public class CountersRemoveAi extends SpellAbilityAi {
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        return MayDecisions.removeCounter(player, sa);
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

        // "remove all counters" (Vampire Hexmage) is also an answer to something on the stack, in any phase
        final boolean respondsToStack = type.equals("All") && !ai.getGame().getStack().isEmpty();
        if (ph.getPhase().isBefore(PhaseType.MAIN2) && !sa.hasParam("ActivationPhases") && !type.equals("M1M1") && !respondsToStack) {
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

    /** True only while a card was actually cast for Impending and still has time counters. */
    public static boolean isImpendingPermanent(final Card card) {
        if (card == null || card.getCounters(CounterEnumType.TIME) <= 0 || card.isCreature()
                || !card.hasStartOfKeyword("Impending") || card.getRules() == null
                || !card.getRules().getType().isCreature()) {
            return false;
        }
        // The keyword is printed on the card even when it was cast normally. Check the actual cast so
        // that an ordinary Overlord with unrelated time counters is not mistaken for an impending one.
        final SpellAbility cast = card.getCastSA();
        return cast != null && cast.isImpending();
    }

    /**
     * "Remove all counters from target permanent" paid for with the source itself (Vampire Hexmage): the own permanent
     * that is better off without its counters, or null when there is none or this is not the moment to give up the source.
     * Impending permanents (Overlord of the Balemurk: not a creature until the last time counter is gone) become what they
     * are at once, creatures held down by negative counters are freed. It is worth the source at the end of the opponent's
     * turn (the counters would otherwise keep ticking down only on our own end step), when the source is about to be
     * removed anyway, and when the target is about to be removed while it is not a creature.
     */
    public static Card worthwhileOwnTarget(final Player ai, final SpellAbility sa) {
        if (sa.getApi() != ApiType.RemoveCounter || !"All".equals(sa.getParam("CounterType")) || !sa.usesTargeting()) {
            return null;
        }
        final Card source = sa.getHostCard();
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        final boolean endOfOpponentTurn = ph.is(PhaseType.END_OF_TURN) && !ph.isPlayerTurn(ai);
        // with this ability as the "saviour" the prediction only knows the answers it was written for (regenerate, bounce,
        // a pump): ask what the opponent's spells threaten at all, giving up the source is the answer worked out below
        final List<GameObject> threatened = ComputerUtil.predictThreatenedObjects(ai, null);
        final boolean sourceThreatened = threatened.contains(source);
        final int sourceValue = source.isCreature() ? ComputerUtilCard.evaluateCreature(source) : 0;

        Card best = null;
        int bestValue = Integer.MIN_VALUE;
        final CardCollection ownTargets = new CardCollection();
        ownTargets.addAll(CardLists.getTargetableCards(ai.getCardsIn(ZoneType.Battlefield), sa));
        // canTarget() rejects a target already selected by this ability (unique-target check). Include it
        // explicitly because this method is also called again while the AI is checking the sacrifice cost.
        for (final Card selected : sa.getTargets().getTargetCards()) {
            if (selected.isInZone(ZoneType.Battlefield) && selected.getController() == ai && !ownTargets.contains(selected)) {
                ownTargets.add(selected);
            }
        }
        for (final Card c : ownTargets) {
            if (c == source) {
                continue;
            }
            int value;
            if (isImpendingPermanent(c)) {
                value = 10000 + ComputerUtilCard.evaluateCreature(c); // it turns into the creature it is
            } else if (c.isCreature() && hasNegativeCounter(c) && ComputerUtilCard.evaluateCreature(c) > sourceValue) {
                value = ComputerUtilCard.evaluateCreature(c);
            } else {
                continue;
            }
            if (!(endOfOpponentTurn || sourceThreatened || threatened.contains(c))) {
                continue;
            }
            if (value > bestValue) {
                best = c;
                bestValue = value;
            }
        }
        return best;
    }

    private static boolean hasNegativeCounter(final Card c) {
        for (final CounterType t : c.getCounters().elementSet()) {
            if (ComputerUtil.isNegativeCounter(t, c)) {
                return true;
            }
        }
        return false;
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
        boolean maritEmpty = marit.isEmpty() || marit.get(0).ignoreLegendRule();

        CounterType iceType = CounterType.getType("ICE");

        if (type.matches("All")) {
            // Logic Part for Vampire Hexmage
            // Break Dark Depths
            if (maritEmpty) {
                CardCollectionView depthsList = ai.getCardsIn(ZoneType.Battlefield, "Dark Depths");
                depthsList = CardLists.filter(depthsList, CardPredicates.isTargetableBy(sa),
                        CardPredicates.hasCounter(iceType, 3));
                if (!depthsList.isEmpty()) {
                    sa.getTargets().add(depthsList.getFirst());
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            }

            // An impending permanent is about to become a creature (or is being saved from removal).
            // Prefer freeing it over stripping loyalty from an opposing planeswalker.
            final Card own = worthwhileOwnTarget(ai, sa);
            if (isImpendingPermanent(own)) {
                sa.getTargets().add(own);
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
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

            if (own != null) {
                sa.getTargets().add(own);
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
        } else if (type.matches("Any")) {
            // variable amount for Hex Parasite
            int amount;
            boolean xPay = false;
            if (amountStr.equals("X") && sa.getSVar("X").equals("Count$xPaid")) {
                final int manaLeft = ComputerUtilCost.setMaxXValue(sa, ai, sa.isTrigger());

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
                CardCollectionView depthsList = CardLists.filter(
                    ai.getCardsIn(ZoneType.Battlefield, "Dark Depths"),
                    CardPredicates.isTargetableBy(sa), CardPredicates.hasCounter(iceType));

                if (!depthsList.isEmpty()) {
                    Card depth = depthsList.getFirst();
                    int ice = depth.getCounters(iceType);
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

                    for (final CounterType aType : best.getCounters().elementSet()) {
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
                final int manaLeft = ComputerUtilCost.setMaxXValue(sa, ai, sa.isTrigger());

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
            }
            return type.is(CounterEnumType.POISON) ? max : min;
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

        if (target instanceof Card targetCard) {
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
                }
                if (options.contains(CounterEnumType.P1P1) && targetCard.hasKeyword(Keyword.UNDYING)) {
                    return CounterEnumType.P1P1;
                }
                for (CounterType type : options) {
                    if (ComputerUtil.isNegativeCounter(type, targetCard)) {
                        return type;
                    }
                }
            }
        } else if (target instanceof Player targetPlayer) {
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
