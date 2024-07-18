package forge.ai.ability;

import java.util.Collections;
import java.util.Map;

import com.google.common.collect.Iterables;

import forge.ai.AiController;
import forge.ai.AiPlayerPredicates;
import forge.ai.AiProps;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilAbility;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.ComputerUtilCost;
import forge.ai.PlayerControllerAi;
import forge.ai.SpecialCardAi;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.cost.Cost;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class ChangeZoneAllAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
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
                return false;
            }

            if (!ComputerUtilCost.checkDiscardCost(ai, abCost, source, sa)) {
                boolean aiLogicAllowsDiscard = aiLogic.startsWith("DiscardAll");

                if (!aiLogicAllowsDiscard) {
                    return false;
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
            return SpecialCardAi.UginTheSpiritDragon.considerPWAbilityPriority(ai, sa, origin, oppType, computerType);
        }

        oppType = AbilityUtils.filterListByType(oppType, sa.getParam("ChangeType"), sa);
        computerType = AbilityUtils.filterListByType(computerType, sa.getParam("ChangeType"), sa);
        
        if ("LivingDeath".equals(aiLogic)) {
            // Living Death AI
            return SpecialCardAi.LivingDeath.consider(ai, sa);
        } else if ("Timetwister".equals(aiLogic)) {
            // Timetwister AI
            return SpecialCardAi.Timetwister.consider(ai, sa);
        } else if ("RetDiscardedThisTurn".equals(aiLogic)) {
            // e.g. Shadow of the Grave
            return ai.getDiscardedThisTurn().size() > 0 && ai.getGame().getPhaseHandler().is(PhaseType.END_OF_TURN);
        } else if ("ExileGraveyards".equals(aiLogic)) {
            for (Player opp : ai.getOpponents()) {
                CardCollectionView cardsGY = opp.getCardsIn(ZoneType.Graveyard);
                CardCollection creats = CardLists.filter(cardsGY, CardPredicates.Presets.CREATURES);

                if (opp.hasDelirium() || opp.hasThreshold() || creats.size() >= 5) {
                    return true;
                }
            }
            return false;
        } else if ("ManifestCreatsFromGraveyard".equals(aiLogic)) {
            PlayerCollection players = ai.getOpponents();
            players.add(ai);
            int maxSize = 1;
            for (Player player : players) {
                Player bestTgt = null;
                if (player.canBeTargetedBy(sa)) {
                    int numGY = CardLists.count(player.getCardsIn(ZoneType.Graveyard),
                            CardPredicates.Presets.CREATURES);
                    if (numGY > maxSize) {
                        maxSize = numGY;
                        bestTgt = player;
                    }
                }

                if (bestTgt != null) {
                    sa.resetTargets();
                    sa.getTargets().add(bestTgt);
                    return true;
                }
            }
            return false;
        }

        // TODO improve restrictions on when the AI would want to use this
        // spBounceAll has some AI we can compare to.
        if (origin.equals(ZoneType.Hand) || origin.equals(ZoneType.Library)) {
            if (!sa.usesTargeting()) {
                // TODO: improve logic for non-targeted SAs of this type (most are currently AI:RemoveDeck:All, e.g. Memory Jar)
                return true;
            } else {
                // search targetable Opponents
                final PlayerCollection oppList = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));

                if (oppList.isEmpty()) {
                    return false;
                }

                // get the one with the most handsize
                Player oppTarget = oppList.max(PlayerPredicates.compareByZoneSize(origin));

                // set the target
                if (!oppTarget.getCardsIn(ZoneType.Hand).isEmpty()) {
                    sa.resetTargets();
                    sa.getTargets().add(oppTarget);
                } else {
                    return false;
                }
            }
        } else if (origin.equals(ZoneType.Battlefield)) {
            // this statement is assuming the AI is trying to use this spell offensively
            // if the AI is using it defensively, then something else needs to occur
            // if only creatures are affected evaluate both lists and pass only
            // if human creatures are more valuable
            if (sa.usesTargeting()) {
                // search targetable Opponents
                final PlayerCollection oppList = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));

                if (oppList.isEmpty()) {
                    return false;
                }

                // get the one with the most in graveyard
                // zone is visible so evaluate which would be hurt the most
                Player oppTarget = oppList.max(PlayerPredicates.compareByZoneSize(origin));

                // set the target
                if (oppTarget.getCardsIn(ZoneType.Graveyard).isEmpty()) {
                    sa.resetTargets();
                    sa.getTargets().add(oppTarget);
                } else {
                    return false;
                }
                computerType = new CardCollection();
            }

            int creatureEvalThreshold = 200; // value difference (in evaluateCreatureList units)
            int nonCreatureEvalThreshold = 3; // CMC difference
            if (ai.getController().isAI()) {
                AiController aic = ((PlayerControllerAi)ai.getController()).getAi();
                if (destination == ZoneType.Hand) {
                    creatureEvalThreshold = aic.getIntProperty(AiProps.BOUNCE_ALL_TO_HAND_CREAT_EVAL_DIFF);
                    nonCreatureEvalThreshold = aic.getIntProperty(AiProps.BOUNCE_ALL_TO_HAND_NONCREAT_EVAL_DIFF);
                } else {
                    creatureEvalThreshold = aic.getIntProperty(AiProps.BOUNCE_ALL_ELSEWHERE_CREAT_EVAL_DIFF);
                    nonCreatureEvalThreshold = aic.getIntProperty(AiProps.BOUNCE_ALL_ELSEWHERE_NONCREAT_EVAL_DIFF);
                }
            }

            // mass zone change for creatures: if in dire danger, do it; otherwise, only do it if the opponent's
            // creatures are better in value
            if (CardLists.getNotType(oppType, "Creature").isEmpty() && CardLists.getNotType(computerType, "Creature").isEmpty()) {
                if (game.getCombat() != null && ComputerUtilCombat.lifeInSeriousDanger(ai, game.getCombat())) {
                    if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)
                            && game.getPhaseHandler().getPlayerTurn().isOpponentOf(ai)) {
                        // Life is in serious danger, return all creatures from the battlefield to wherever
                        // so they don't deal lethal damage
                        return true;
                    }
                }
                if ((ComputerUtilCard.evaluateCreatureList(computerType) + creatureEvalThreshold) >= ComputerUtilCard
                        .evaluateCreatureList(oppType)) {
                    return false;
                }
            } // mass zone change for non-creatures: evaluate both lists by CMC and pass only if human
              // permanents are more valuable
            else if ((ComputerUtilCard.evaluatePermanentList(computerType) + nonCreatureEvalThreshold) >= ComputerUtilCard
                    .evaluatePermanentList(oppType)) {
                return false;
            }

            // Don't cast during main1?
            if (game.getPhaseHandler().is(PhaseType.MAIN1, ai) && !aiLogic.equals("Main1")) {
                return false;
            }
        } else if (origin.equals(ZoneType.Graveyard)) {
            if (sa.usesTargeting()) {
                // search targetable Opponents
                final PlayerCollection oppList = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));

                if (oppList.isEmpty()) {
                    return false;
                }

                // get the one with the most in graveyard
                // zone is visible so evaluate which would be hurt the most
                Player oppTarget = Collections.max(oppList, AiPlayerPredicates.compareByZoneValue(sa.getParam("ChangeType"), origin, sa));

                // set the target
                if (!oppTarget.getCardsIn(ZoneType.Graveyard).isEmpty()) {
                    sa.resetTargets();
                    sa.getTargets().add(oppTarget);
                } else {
                    return false;
                }
            } else if (destination.equals(ZoneType.Library) && "Card.YouOwn".equals(sa.getParam("ChangeType"))) {
                return (ai.getCardsIn(ZoneType.Graveyard).size() > ai.getCardsIn(ZoneType.Library).size())
                        && !ComputerUtil.isPlayingReanimator(ai);
            }
        } else if (origin.equals(ZoneType.Exile)) {
            if (aiLogic.startsWith("DiscardAllAndRetExiled")) {
                int numExiledWithSrc = CardLists.filter(ai.getCardsIn(ZoneType.Exile), CardPredicates.isExiledWith(source)).size();
                int curHandSize = ai.getCardsIn(ZoneType.Hand).size();
            
                // minimum card advantage unless the hand will be fully reloaded
                int minAdv = aiLogic.contains(".minAdv") ? Integer.parseInt(aiLogic.substring(aiLogic.indexOf(".minAdv") + 7)) : 0;
                boolean noDiscard = aiLogic.contains(".noDiscard");

                if (numExiledWithSrc > curHandSize || (noDiscard && numExiledWithSrc > 0)) {
                    if (ComputerUtil.predictThreatenedObjects(ai, sa, true).contains(source)) {
                        // Try to gain some card advantage if the card will die anyway
                        // TODO: ideally, should evaluate the hand value and not discard good hands to it
                        return true;
                    }
                }

                return (curHandSize + minAdv - 1 < numExiledWithSrc) || (!noDiscard && numExiledWithSrc >= ai.getMaxHandSize());
            }
        } else if (origin.equals(ZoneType.Stack)) {
            // TODO
            return false;
        }

        if (destination.equals(ZoneType.Battlefield)) {
            if (sa.hasParam("GainControl")) {
                // Check if the cards are valuable enough
                if (CardLists.getNotType(oppType, "Creature").isEmpty() && CardLists.getNotType(computerType, "Creature").isEmpty()) {
                    if ((ComputerUtilCard.evaluateCreatureList(computerType) + ComputerUtilCard
                            .evaluateCreatureList(oppType)) < 400) {
                        return false;
                    }
                } // otherwise evaluate both lists by CMC and pass only if human
                  // permanents are less valuable
                else if ((ComputerUtilCard.evaluatePermanentList(computerType) + ComputerUtilCard
                        .evaluatePermanentList(oppType)) < 6) {
                    return false;
                }
            } else {
                // don't activate if human gets more back than AI does
                if (CardLists.getNotType(oppType, "Creature").isEmpty() && CardLists.getNotType(computerType, "Creature").isEmpty()) {
                    if (ComputerUtilCard.evaluateCreatureList(computerType) <= (ComputerUtilCard
                            .evaluateCreatureList(oppType) + 100)) {
                        return false;
                    }
                } // otherwise evaluate both lists by CMC and pass only if human
                  // permanents are less valuable
                else if (ComputerUtilCard.evaluatePermanentList(computerType) <= (ComputerUtilCard
                        .evaluatePermanentList(oppType) + 2)) {
                    return false;
                }
            }
        }

        return (((MyRandom.getRandom().nextFloat() < .8) || sa.isTrigger()) && chance);
    }

    /**
     * <p>
     * changeZoneAllPlayDrawbackAI.
     * </p>
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param aiPlayer
     *            a {@link forge.game.player.Player} object.
     * 
     * @return a boolean.
     */
    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        // if putting cards from hand to library and parent is drawing cards
        // make sure this will actually do something:

        return true;
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
    protected boolean doTriggerAINoCost(Player ai, final SpellAbility sa, boolean mandatory) {
        // Change Zone All, can be any type moving from one zone to another

        final ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));
        final ZoneType origin = ZoneType.listValueOf(sa.getParam("Origin")).get(0);

        if (ComputerUtilAbility.getAbilitySourceName(sa).equals("Profaner of the Dead")) {
            // TODO: this is a stub to prevent the AI from crashing the game when, for instance, playing the opponent's
            // Profaner from exile without paying its mana cost. Otherwise the card is marked AI:RemoveDeck:All and
            // there is no specific AI to support playing it in a smarter way. Feel free to expand.
            return Iterables.any(ai.getOpponents().getCardsIn(origin), CardPredicates.Presets.CREATURES);
        }

        CardCollectionView humanType = ai.getOpponents().getCardsIn(origin);
        humanType = AbilityUtils.filterListByType(humanType, sa.getParam("ChangeType"), sa);

        CardCollectionView computerType = ai.getCardsIn(origin);
        computerType = AbilityUtils.filterListByType(computerType, sa.getParam("ChangeType"), sa);

        // TODO improve restrictions on when the AI would want to use this
        // spBounceAll has some AI we can compare to.
        if (origin.equals(ZoneType.Hand) || origin.equals(ZoneType.Library)) {
            if (sa.usesTargeting()) {
                // search targetable Opponents
                final PlayerCollection oppList = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));

                if (oppList.isEmpty()) {
                    if (mandatory && !sa.isTargetNumberValid() && sa.canTarget(ai)) {
                        sa.resetTargets();
                        sa.getTargets().add(ai);
                        return true;
                    }
                    return false;
                }

                // get the one with the most handsize
                Player oppTarget = oppList.max(PlayerPredicates.compareByZoneSize(origin));

                // set the target
                if (!oppTarget.getCardsIn(ZoneType.Hand).isEmpty() || mandatory) {
                    sa.resetTargets();
                    sa.getTargets().add(oppTarget);
                } else {
                    return false;
                }
            }
        } else if (origin.equals(ZoneType.Battlefield)) {
            // if mandatory, no need to evaluate
            if (mandatory) {
                return true;
            }
            // this statement is assuming the AI is trying to use this spell offensively
            // if the AI is using it defensively, then something else needs to occur
            // if only creatures are affected evaluate both lists and pass only
            // if human creatures are more valuable
            if (CardLists.getNotType(humanType, "Creature").isEmpty() && CardLists.getNotType(computerType, "Creature").isEmpty()) {
                if (ComputerUtilCard.evaluateCreatureList(computerType) >= ComputerUtilCard.evaluateCreatureList(humanType)) {
                    return false;
                }
            } // otherwise evaluate both lists by CMC and pass only if human
              // permanents are more valuable
            else if (ComputerUtilCard.evaluatePermanentList(computerType) >= ComputerUtilCard.evaluatePermanentList(humanType)) {
                return false;
            }
        } else if (origin.equals(ZoneType.Graveyard)) {
            if (sa.usesTargeting()) {
                // search targetable Opponents
                final PlayerCollection oppList = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));

                if (oppList.isEmpty()) {
                    if (mandatory && !sa.isTargetNumberValid() && sa.canTarget(ai)) {
                        sa.resetTargets();
                        sa.getTargets().add(ai);
                        return true;
                    }
                    return sa.isTargetNumberValid();
                }

                // get the one with the most in graveyard
                // zone is visible so evaluate which would be hurt the most
                Player oppTarget = oppList.max(
                        AiPlayerPredicates.compareByZoneValue(sa.getParam("ChangeType"), origin, sa));

                // set the target
                if (!oppTarget.getCardsIn(ZoneType.Graveyard).isEmpty() || mandatory) {
                    sa.resetTargets();
                    sa.getTargets().add(oppTarget);
                } else {
                    return false;
                }
            }
        } else if (origin.equals(ZoneType.Exile)) {

        } else if (origin.equals(ZoneType.Stack)) {
            // currently only exists indirectly (e.g. Summary Dismissal via PlayAi)
        }

        if (destination.equals(ZoneType.Battlefield)) {
            // if mandatory, no need to evaluate
            if (mandatory) {
                return true;
            }
            if (sa.hasParam("GainControl")) {
                // Check if the cards are valuable enough
                if (CardLists.getNotType(humanType, "Creature").isEmpty() && CardLists.getNotType(computerType, "Creature").isEmpty()) {
                    return (ComputerUtilCard.evaluateCreatureList(computerType) + ComputerUtilCard.evaluateCreatureList(humanType)) >= 1;
                } // otherwise evaluate both lists by CMC and pass only if human
                  // permanents are less valuable
                return (ComputerUtilCard.evaluatePermanentList(computerType) + ComputerUtilCard
                        .evaluatePermanentList(humanType)) >= 1;
            }

            // don't activate if human gets more back than AI does
            if (CardLists.getNotType(humanType, "Creature").isEmpty() && CardLists.getNotType(computerType, "Creature").isEmpty()) {
                return ComputerUtilCard.evaluateCreatureList(computerType) > ComputerUtilCard.evaluateCreatureList(humanType);
            } // otherwise evaluate both lists by CMC and pass only if human
            // permanents are less valuable
            return ComputerUtilCard.evaluatePermanentList(computerType) > ComputerUtilCard.evaluatePermanentList(humanType);
        }

        return true;
    }

}
