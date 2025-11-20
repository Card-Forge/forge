package forge.ai.ability;

import forge.ai.*;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.*;
import forge.game.cost.Cost;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

import java.util.Collections;
import java.util.Map;

public class ChangeZoneAllAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision canPlay(Player ai, SpellAbility sa) {
        // Change Zone All, can be any type moving from one zone to another
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getHostCard();
        final String sourceName = ComputerUtilAbility.getAbilitySourceName(sa);
        final Game game = ai.getGame();
        final ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));
        final ZoneType origin = ZoneType.listValueOf(sa.getParam("Origin")).get(0);
        final String aiLogic = sa.getParamOrDefault("AILogic" ,"");

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!ComputerUtilCost.checkLifeCost(ai, abCost, source, 4, sa)) {
                return new AiAbilityDecision(0, AiPlayDecision.CostNotAcceptable);
            }

            if (!ComputerUtilCost.checkDiscardCost(ai, abCost, source, sa)) {
                boolean aiLogicAllowsDiscard = aiLogic.startsWith("DiscardAll");

                if (!aiLogicAllowsDiscard) {
                    return new AiAbilityDecision(0, AiPlayDecision.CostNotAcceptable);
                }
            }
        }

        // prevent run-away activations - first time will always return true
        boolean chance = MyRandom.getRandom().nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        // TODO targeting with ChangeZoneAll
        // really two types of targeting.
        // Target Player has all their types change zones
        // or target permanent and do something relative to that permanent
        // ex. "Return all Auras attached to target"
        // ex. "Return all blocking/blocked by target creature"

        CardCollectionView oppType = ai.getOpponents().getCardsIn(origin);
        CardCollectionView computerType = ai.getCardsIn(origin);

        // Ugin AI: always try to sweep before considering +1
        if (sourceName.equals("Ugin, the Spirit Dragon")) {
            boolean result = SpecialCardAi.UginTheSpiritDragon.considerPWAbilityPriority(ai, sa, origin, oppType, computerType);
            return result ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        oppType = AbilityUtils.filterListByType(oppType, sa.getParam("ChangeType"), sa);
        computerType = AbilityUtils.filterListByType(computerType, sa.getParam("ChangeType"), sa);
        
        if ("LivingDeath".equals(aiLogic)) {
            return SpecialCardAi.LivingDeath.consider(ai, sa);
        } else if ("Timetwister".equals(aiLogic)) {
            return SpecialCardAi.Timetwister.consider(ai, sa);
        } else if ("RetDiscardedThisTurn".equals(aiLogic)) {
            boolean result = !ai.getDiscardedThisTurn().isEmpty() && ai.getGame().getPhaseHandler().is(PhaseType.END_OF_TURN);
            return result ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        } else if ("ExileGraveyards".equals(aiLogic)) {
            for (Player opp : ai.getOpponents()) {
                CardCollectionView cardsGY = opp.getCardsIn(ZoneType.Graveyard);
                CardCollection creats = CardLists.filter(cardsGY, CardPredicates.CREATURES);
                if (opp.hasDelirium() || opp.hasThreshold() || creats.size() >= 5) {
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            }
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        } else if ("ManifestCreatsFromGraveyard".equals(aiLogic)) {
            PlayerCollection players = ai.getOpponents();
            players.add(ai);
            int maxSize = 1;
            for (Player player : players) {
                Player bestTgt = null;
                if (player.canBeTargetedBy(sa)) {
                    int numGY = CardLists.count(player.getCardsIn(ZoneType.Graveyard),
                            CardPredicates.CREATURES);
                    if (numGY > maxSize) {
                        maxSize = numGY;
                        bestTgt = player;
                    }
                }
                if (bestTgt != null) {
                    sa.resetTargets();
                    sa.getTargets().add(bestTgt);
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            }
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        // TODO improve restrictions on when the AI would want to use this
        // spBounceAll has some AI we can compare to.
        if (origin.equals(ZoneType.Hand) || origin.equals(ZoneType.Library)) {
            if (!sa.usesTargeting()) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            } else {
                final PlayerCollection oppList = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
                if (oppList.isEmpty()) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlaySa);
                }
                Player oppTarget = oppList.max(PlayerPredicates.compareByZoneSize(origin));
                if (!oppTarget.getCardsIn(ZoneType.Hand).isEmpty()) {
                    sa.resetTargets();
                    sa.getTargets().add(oppTarget);
                } else {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlaySa);
                }
            }
        } else if (origin.equals(ZoneType.Battlefield)) {
            if (sa.usesTargeting()) {
                final PlayerCollection oppList = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
                if (oppList.isEmpty()) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlaySa);
                }
                Player oppTarget = oppList.max(PlayerPredicates.compareByZoneSize(origin));
                if (oppTarget.getCardsIn(ZoneType.Graveyard).isEmpty()) {
                    sa.resetTargets();
                    sa.getTargets().add(oppTarget);
                } else {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlaySa);
                }
                computerType = new CardCollection();
            }

            int creatureEvalThreshold; // value difference (in evaluateCreatureList units)
            int nonCreatureEvalThreshold; // CMC difference
            if (destination == ZoneType.Hand) {
                creatureEvalThreshold = AiProfileUtil.getIntProperty(ai, AiProps.BOUNCE_ALL_TO_HAND_CREAT_EVAL_DIFF);
                nonCreatureEvalThreshold = AiProfileUtil.getIntProperty(ai, AiProps.BOUNCE_ALL_TO_HAND_NONCREAT_EVAL_DIFF);
            } else {
                creatureEvalThreshold = AiProfileUtil.getIntProperty(ai, AiProps.BOUNCE_ALL_ELSEWHERE_CREAT_EVAL_DIFF);
                nonCreatureEvalThreshold = AiProfileUtil.getIntProperty(ai, AiProps.BOUNCE_ALL_ELSEWHERE_NONCREAT_EVAL_DIFF);
            }

            // mass zone change for creatures: if in dire danger, do it; otherwise, only do it if the opponent's
            // creatures are better in value
            if (CardLists.getNotType(oppType, "Creature").isEmpty() && CardLists.getNotType(computerType, "Creature").isEmpty()) {
                if (game.getCombat() != null && ComputerUtilCombat.lifeInSeriousDanger(ai, game.getCombat())) {
                    if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)
                            && game.getPhaseHandler().getPlayerTurn().isOpponentOf(ai)) {
                        // Life is in serious danger, return all creatures from the battlefield to wherever
                        // so they don't deal lethal damage
                        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                    }
                }
                if ((ComputerUtilCard.evaluateCreatureList(computerType) + creatureEvalThreshold) >= ComputerUtilCard
                        .evaluateCreatureList(oppType)) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            } else if ((ComputerUtilCard.evaluatePermanentList(computerType) + nonCreatureEvalThreshold) >= ComputerUtilCard
                    .evaluatePermanentList(oppType)) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            if (game.getPhaseHandler().is(PhaseType.MAIN1, ai) && !aiLogic.equals("Main1")) {
                return new AiAbilityDecision(0, AiPlayDecision.TimingRestrictions);
            }
        } else if (origin.equals(ZoneType.Graveyard)) {
            if (sa.usesTargeting()) {
                final PlayerCollection oppList = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
                if (oppList.isEmpty()) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlaySa);
                }
                Player oppTarget = Collections.max(oppList, AiPlayerPredicates.compareByZoneValue(sa.getParam("ChangeType"), origin, sa));
                if (!oppTarget.getCardsIn(ZoneType.Graveyard).isEmpty()) {
                    sa.resetTargets();
                    sa.getTargets().add(oppTarget);
                } else {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlaySa);
                }
            } else if (destination.equals(ZoneType.Library) && "Card.YouOwn".equals(sa.getParam("ChangeType"))) {
                boolean result = (ai.getCardsIn(ZoneType.Graveyard).size() > ai.getCardsIn(ZoneType.Library).size())
                        && !ComputerUtil.isPlayingReanimator(ai);
                return result ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        } else if (origin.equals(ZoneType.Exile)) {
            if (aiLogic.startsWith("DiscardAllAndRetExiled")) {
                int numExiledWithSrc = CardLists.filter(ai.getCardsIn(ZoneType.Exile), CardPredicates.isExiledWith(source)).size();
                int curHandSize = ai.getCardsIn(ZoneType.Hand).size();
                int minAdv = aiLogic.contains(".minAdv") ? Integer.parseInt(aiLogic.substring(aiLogic.indexOf(".minAdv") + 7)) : 0;
                boolean noDiscard = aiLogic.contains(".noDiscard");
                if (numExiledWithSrc > curHandSize || (noDiscard && numExiledWithSrc > 0)) {
                    if (ComputerUtil.predictThreatenedObjects(ai, sa, true).contains(source)) {
                        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                    }
                }
                boolean result = (curHandSize + minAdv - 1 < numExiledWithSrc) || (!noDiscard && numExiledWithSrc >= ai.getMaxHandSize());
                return result ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        } else if (origin.equals(ZoneType.Stack)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        if (destination.equals(ZoneType.Battlefield)) {
            if (sa.hasParam("GainControl")) {
                if (CardLists.getNotType(oppType, "Creature").isEmpty() && CardLists.getNotType(computerType, "Creature").isEmpty()) {
                    if ((ComputerUtilCard.evaluateCreatureList(computerType) + ComputerUtilCard
                            .evaluateCreatureList(oppType)) < 400) {
                        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                    }
                } else if ((ComputerUtilCard.evaluatePermanentList(computerType) + ComputerUtilCard
                        .evaluatePermanentList(oppType)) < 6) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            } else {
                if (CardLists.getNotType(oppType, "Creature").isEmpty() && CardLists.getNotType(computerType, "Creature").isEmpty()) {
                    if (ComputerUtilCard.evaluateCreatureList(computerType) <= (ComputerUtilCard
                            .evaluateCreatureList(oppType) + 100)) {
                        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                    }
                } else if (ComputerUtilCard.evaluatePermanentList(computerType) <= (ComputerUtilCard
                        .evaluatePermanentList(oppType) + 2)) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            }
        }
        boolean result = ((MyRandom.getRandom().nextFloat() < .8) || sa.isTrigger()) && chance;
        return result ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    /**
     * <p>
     * changeZoneAllPlayDrawbackAI.
     * </p>
     *
     * @param aiPlayer a {@link Player} object.
     * @param sa       a {@link SpellAbility} object.
     * @return a boolean.
     */
    @Override
    public AiAbilityDecision chkDrawback(Player aiPlayer, SpellAbility sa) {
        // if putting cards from hand to library and parent is drawing cards
        // make sure this will actually do something:

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#confirmAction(forge.game.player.Player, forge.card.spellability.SpellAbility, forge.game.player.PlayerActionConfirmMode, java.lang.String)
     */
    @Override
    public boolean confirmAction(Player ai, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        final Card source = sa.getHostCard();
        final String hostName = source.getName();
        final ZoneType origin = ZoneType.listValueOf(sa.getParam("Origin")).get(0);

        if (hostName.equals("Dawnbreak Reclaimer")) {
        	final CardCollectionView cards = AbilityUtils.filterListByType(ai.getGame().getCardsIn(origin), sa.getParam("ChangeType"), sa);

        	// AI gets nothing
        	final CardCollection aiCards = CardLists.filterControlledBy(cards, ai);        	
        	if (aiCards.isEmpty())
        		return false;

        	// Human gets nothing
        	final CardCollection humanCards = CardLists.filterControlledBy(cards, ai.getOpponents());
        	if (humanCards.isEmpty())
        		return true;

        	// if AI creature is better than Human Creature
            return ComputerUtilCard.evaluateCreatureList(aiCards) >= ComputerUtilCard.evaluateCreatureList(humanCards);
        }
        return true;
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, final SpellAbility sa, boolean mandatory) {
        final ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));
        final ZoneType origin = ZoneType.listValueOf(sa.getParam("Origin")).get(0);

        if (ComputerUtilAbility.getAbilitySourceName(sa).equals("Profaner of the Dead")) {
            boolean result = ai.getOpponents().getCardsIn(origin).anyMatch(CardPredicates.CREATURES);
            return result ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        CardCollectionView humanType = ai.getOpponents().getCardsIn(origin);
        humanType = AbilityUtils.filterListByType(humanType, sa.getParam("ChangeType"), sa);
        CardCollectionView computerType = ai.getCardsIn(origin);
        computerType = AbilityUtils.filterListByType(computerType, sa.getParam("ChangeType"), sa);
        if (origin.equals(ZoneType.Hand) || origin.equals(ZoneType.Library)) {
            if (sa.usesTargeting()) {
                final PlayerCollection oppList = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
                if (oppList.isEmpty()) {
                    if (mandatory && !sa.isTargetNumberValid() && sa.canTarget(ai)) {
                        sa.resetTargets();
                        sa.getTargets().add(ai);
                        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                    }
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlaySa);
                }
                Player oppTarget = oppList.max(PlayerPredicates.compareByZoneSize(origin));
                if (!oppTarget.getCardsIn(ZoneType.Hand).isEmpty() || mandatory) {
                    sa.resetTargets();
                    sa.getTargets().add(oppTarget);
                } else {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlaySa);
                }
            }
        } else if (origin.equals(ZoneType.Battlefield)) {
            if (mandatory) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
            if (CardLists.getNotType(humanType, "Creature").isEmpty() && CardLists.getNotType(computerType, "Creature").isEmpty()) {
                if (ComputerUtilCard.evaluateCreatureList(computerType) >= ComputerUtilCard.evaluateCreatureList(humanType)) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            } else if (ComputerUtilCard.evaluatePermanentList(computerType) >= ComputerUtilCard.evaluatePermanentList(humanType)) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        } else if (origin.equals(ZoneType.Graveyard)) {
            if (sa.usesTargeting()) {
                final PlayerCollection oppList = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
                if (oppList.isEmpty()) {
                    if (mandatory && !sa.isTargetNumberValid() && sa.canTarget(ai)) {
                        sa.resetTargets();
                        sa.getTargets().add(ai);
                        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                    }
                    return sa.isTargetNumberValid() ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlaySa);
                }
                Player oppTarget = oppList.max(
                        AiPlayerPredicates.compareByZoneValue(sa.getParam("ChangeType"), origin, sa));
                if (!oppTarget.getCardsIn(ZoneType.Graveyard).isEmpty() || mandatory) {
                    sa.resetTargets();
                    sa.getTargets().add(oppTarget);
                } else {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlaySa);
                }
            }
        }
        if (destination.equals(ZoneType.Battlefield)) {
            if (mandatory) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
            if (sa.hasParam("GainControl")) {
                if (CardLists.getNotType(humanType, "Creature").isEmpty() && CardLists.getNotType(computerType, "Creature").isEmpty()) {
                    boolean result = (ComputerUtilCard.evaluateCreatureList(computerType) + ComputerUtilCard.evaluateCreatureList(humanType)) >= 1;
                    return result ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
                boolean result = (ComputerUtilCard.evaluatePermanentList(computerType) + ComputerUtilCard
                        .evaluatePermanentList(humanType)) >= 1;
                return result ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            if (CardLists.getNotType(humanType, "Creature").isEmpty() && CardLists.getNotType(computerType, "Creature").isEmpty()) {
                boolean result = ComputerUtilCard.evaluateCreatureList(computerType) > ComputerUtilCard.evaluateCreatureList(humanType);
                return result ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            boolean result = ComputerUtilCard.evaluatePermanentList(computerType) > ComputerUtilCard.evaluatePermanentList(humanType);
            return result ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

}
