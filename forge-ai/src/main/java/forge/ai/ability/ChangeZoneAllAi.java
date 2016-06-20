package forge.ai.ability;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.ComputerUtilCost;
import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CounterType;
import forge.game.cost.Cost;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

import java.util.Random;

public class ChangeZoneAllAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        // Change Zone All, can be any type moving from one zone to another
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getHostCard();
        final ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));
        final ZoneType origin = ZoneType.listValueOf(sa.getParam("Origin")).get(0);

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!ComputerUtilCost.checkLifeCost(ai, abCost, source, 4, null)) {
                return false;
            }

            if (!ComputerUtilCost.checkDiscardCost(ai, abCost, source)) {
                return false;
            }
        }

        final Random r = MyRandom.getRandom();
        // prevent run-away activations - first time will always return true
        boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        // TODO targeting with ChangeZoneAll
        // really two types of targeting.
        // Target Player has all their types change zones
        // or target permanent and do something relative to that permanent
        // ex. "Return all Auras attached to target"
        // ex. "Return all blocking/blocked by target creature"

        final Player opp = ai.getOpponent();
        final CardCollectionView humanType = AbilityUtils.filterListByType(opp.getCardsIn(origin), sa.getParam("ChangeType"), sa);
        CardCollectionView computerType = ai.getCardsIn(origin);
        computerType = AbilityUtils.filterListByType(computerType, sa.getParam("ChangeType"), sa);
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        
        // Ugin AI: always try to sweep before considering +1 
        if (source.getName().equals("Ugin, the Spirit Dragon")) {
            final int loyalty = source.getCounters(CounterType.LOYALTY);
            int x = -1, best = 0;
            Card single = null;
            for (int i = 0; i < loyalty; i++) {
                source.setSVar("ChosenX", "Number$" + i);
                final CardCollectionView oppType = AbilityUtils.filterListByType(opp.getCardsIn(origin), sa.getParam("ChangeType"), sa);
                computerType = AbilityUtils.filterListByType(ai.getCardsIn(origin), sa.getParam("ChangeType"), sa);
                int net = ComputerUtilCard.evaluatePermanentList(oppType) - ComputerUtilCard.evaluatePermanentList(computerType) - i;
                if (net > best) {
                    x = i;
                    best = net;
                    if (oppType.size() == 1) {
                        single = oppType.getFirst();
                    } else {
                        single = null;
                    }
                }
            }
            // check if +1 would be sufficient
            if (single != null) {
                SpellAbility ugin_burn = null;
                for (final SpellAbility s : source.getSpellAbilities()) {
                    if (s.getApi() == ApiType.DealDamage) {
                        ugin_burn = s;
                        break;
                    }
                }
                if (ugin_burn != null) {
                    // basic logic copied from DamageDealAi::dealDamageChooseTgtC
                    if (ugin_burn.canTarget(single)) {
                        final boolean can_kill = single.getSVar("Targeting").equals("Dies")
                                || (ComputerUtilCombat.getEnoughDamageToKill(single, 3, source, false, false) <= 3)
                                        && !ComputerUtil.canRegenerate(ai, single)
                                        && !(single.getSVar("SacMe").length() > 0);
                        if (can_kill) {
                            return false;
                        }
                    }
                    // simple check to burn player instead of exiling planeswalker
                    if (single.isPlaneswalker() && single.getCurrentLoyalty() <= 3) {
                        return false;
                    }
                }
            }
             if (x == -1) {
                return false;
            }
            source.setSVar("ChosenX", "Number$" + x);
            return true;
        }

        // TODO improve restrictions on when the AI would want to use this
        // spBounceAll has some AI we can compare to.
        if (origin.equals(ZoneType.Hand) || origin.equals(ZoneType.Library)) {
            if (tgt != null) {
                if (opp.getCardsIn(ZoneType.Hand).isEmpty()
                        || !opp.canBeTargetedBy(sa)) {
                    return false;
                }
                sa.resetTargets();
                sa.getTargets().add(opp);
            }
        } else if (origin.equals(ZoneType.Battlefield)) {
            // this statement is assuming the AI is trying to use this spell
            // offensively
            // if the AI is using it defensively, then something else needs to
            // occur
            // if only creatures are affected evaluate both lists and pass only
            // if human creatures are more valuable
            if (tgt != null) {
                if (opp.getCardsIn(ZoneType.Hand).isEmpty()
                        || !opp.canBeTargetedBy(sa)) {
                    return false;
                }
                sa.resetTargets();
                sa.getTargets().add(opp);
                computerType = new CardCollection();
            }
            if ((CardLists.getNotType(humanType, "Creature").size() == 0) && (CardLists.getNotType(computerType, "Creature").size() == 0)) {
                if ((ComputerUtilCard.evaluateCreatureList(computerType) + 200) >= ComputerUtilCard
                        .evaluateCreatureList(humanType)) {
                    return false;
                }
            } // otherwise evaluate both lists by CMC and pass only if human
              // permanents are more valuable
            else if ((ComputerUtilCard.evaluatePermanentList(computerType) + 3) >= ComputerUtilCard
                    .evaluatePermanentList(humanType)) {
                return false;
            }

            // Don't cast during main1?
            if (ai.getGame().getPhaseHandler().is(PhaseType.MAIN1, ai)) {
                return false;
            }
        } else if (origin.equals(ZoneType.Graveyard)) {
            if (tgt != null) {
                if (opp.getCardsIn(ZoneType.Graveyard).isEmpty()
                        || !opp.canBeTargetedBy(sa)) {
                    return false;
                }
                sa.resetTargets();
                sa.getTargets().add(opp);
            }
        } else if (origin.equals(ZoneType.Exile)) {

        } else if (origin.equals(ZoneType.Stack)) {
            // time stop can do something like this:
            // Origin$ Stack | Destination$ Exile | SubAbility$ DBSkip
            // DBSKipToPhase | DB$SkipToPhase | Phase$ Cleanup
            // otherwise, this situation doesn't exist
            return false;
        }

        if (destination.equals(ZoneType.Battlefield)) {
            if (sa.getParam("GainControl") != null) {
                // Check if the cards are valuable enough
                if ((CardLists.getNotType(humanType, "Creature").size() == 0) && (CardLists.getNotType(computerType, "Creature").size() == 0)) {
                    if ((ComputerUtilCard.evaluateCreatureList(computerType) + ComputerUtilCard
                            .evaluateCreatureList(humanType)) < 400) {
                        return false;
                    }
                } // otherwise evaluate both lists by CMC and pass only if human
                  // permanents are less valuable
                else if ((ComputerUtilCard.evaluatePermanentList(computerType) + ComputerUtilCard
                        .evaluatePermanentList(humanType)) < 6) {
                    return false;
                }
            } else {
                // don't activate if human gets more back than AI does
                if ((CardLists.getNotType(humanType, "Creature").size() == 0) && (CardLists.getNotType(computerType, "Creature").size() == 0)) {
                    if (ComputerUtilCard.evaluateCreatureList(computerType) <= (ComputerUtilCard
                            .evaluateCreatureList(humanType) + 100)) {
                        return false;
                    }
                } // otherwise evaluate both lists by CMC and pass only if human
                  // permanents are less valuable
                else if (ComputerUtilCard.evaluatePermanentList(computerType) <= (ComputerUtilCard
                        .evaluatePermanentList(humanType) + 2)) {
                    return false;
                }
            }
        }

        return (((r.nextFloat() < .8) || sa.isTrigger()) && chance);
    }

    /**
     * <p>
     * changeZoneAllPlayDrawbackAI.
     * </p>
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param af
     *            a {@link forge.game.ability.AbilityFactory} object.
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
    public boolean confirmAction(Player ai, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
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
        	if (ComputerUtilCard.evaluateCreatureList(aiCards) >= ComputerUtilCard
                    .evaluateCreatureList(humanCards)) {
                return true;
            }
        	return false;
        }
        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        // Change Zone All, can be any type moving from one zone to another

        final ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));
        final ZoneType origin = ZoneType.listValueOf(sa.getParam("Origin")).get(0);

        final Player opp = ai.getOpponent();
        final CardCollectionView humanType = AbilityUtils.filterListByType(opp.getCardsIn(origin), sa.getParam("ChangeType"), sa);
        CardCollectionView computerType = ai.getCardsIn(origin);
        computerType = AbilityUtils.filterListByType(computerType, sa.getParam("ChangeType"), sa);

        // TODO improve restrictions on when the AI would want to use this
        // spBounceAll has some AI we can compare to.
        if (origin.equals(ZoneType.Hand) || origin.equals(ZoneType.Library)) {
            final TargetRestrictions tgt = sa.getTargetRestrictions();
            if (tgt != null) {
                if (opp.getCardsIn(ZoneType.Hand).isEmpty()
                        || !opp.canBeTargetedBy(sa)) {
                    return false;
                }
                sa.resetTargets();
                sa.getTargets().add(opp);
            }
        } else if (origin.equals(ZoneType.Battlefield)) {
            // this statement is assuming the AI is trying to use this spell offensively
            // if the AI is using it defensively, then something else needs to occur
            // if only creatures are affected evaluate both lists and pass only
            // if human creatures are more valuable
            if ((CardLists.getNotType(humanType, "Creature").isEmpty()) && (CardLists.getNotType(computerType, "Creature").isEmpty())) {
                if (ComputerUtilCard.evaluateCreatureList(computerType) >= ComputerUtilCard
                        .evaluateCreatureList(humanType)) {
                    return false;
                }
            } // otherwise evaluate both lists by CMC and pass only if human
              // permanents are more valuable
            else if (ComputerUtilCard.evaluatePermanentList(computerType) >= ComputerUtilCard
                    .evaluatePermanentList(humanType)) {
                return false;
            }
        } else if (origin.equals(ZoneType.Graveyard)) {
            final TargetRestrictions tgt = sa.getTargetRestrictions();
            if (tgt != null) {
                if (opp.getCardsIn(ZoneType.Graveyard).isEmpty()
                        || !opp.canBeTargetedBy(sa)) {
                    return false;
                }
                sa.resetTargets();
                sa.getTargets().add(opp);
            }
        } else if (origin.equals(ZoneType.Exile)) {

        } else if (origin.equals(ZoneType.Stack)) {
            // time stop can do something like this:
            // Origin$ Stack | Destination$ Exile | SubAbility$ DBSkip
            // DBSKipToPhase | DB$SkipToPhase | Phase$ Cleanup
            // otherwise, this situation doesn't exist
            return false;
        }

        if (destination.equals(ZoneType.Battlefield)) {
            if (sa.getParam("GainControl") != null) {
                // Check if the cards are valuable enough
                if ((CardLists.getNotType(humanType, "Creature").size() == 0) && (CardLists.getNotType(computerType, "Creature").size() == 0)) {
                    if ((ComputerUtilCard.evaluateCreatureList(computerType) + ComputerUtilCard
                            .evaluateCreatureList(humanType)) < 1) {
                        return false;
                    }
                } // otherwise evaluate both lists by CMC and pass only if human
                  // permanents are less valuable
                else if ((ComputerUtilCard.evaluatePermanentList(computerType) + ComputerUtilCard
                        .evaluatePermanentList(humanType)) < 1) {
                    return false;
                }
            } else {
                // don't activate if human gets more back than AI does
                if ((CardLists.getNotType(humanType, "Creature").isEmpty()) && (CardLists.getNotType(computerType, "Creature").isEmpty())) {
                    if (ComputerUtilCard.evaluateCreatureList(computerType) <= ComputerUtilCard
                            .evaluateCreatureList(humanType)) {
                        return false;
                    }
                } // otherwise evaluate both lists by CMC and pass only if human
                  // permanents are less valuable
                else if (ComputerUtilCard.evaluatePermanentList(computerType) <= ComputerUtilCard
                        .evaluatePermanentList(humanType)) {
                    return false;
                }
            }
        }

        return true;
    }

}
