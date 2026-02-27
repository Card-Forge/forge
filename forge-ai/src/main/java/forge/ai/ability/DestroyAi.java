package forge.ai.ability;

import java.util.function.Predicate;

import forge.ai.*;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.*;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostSacrifice;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityMustTarget;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;

public class DestroyAi extends SpellAbilityAi {
    @Override
    public AiAbilityDecision chkDrawback(Player ai, SpellAbility sa) {
        return checkApiLogic(ai, sa);
    }

    @Override
    protected boolean checkAiLogic(final Player ai, final SpellAbility sa, final String aiLogic) {
        final Card source = sa.getHostCard();
        if (sa.usesTargeting()) {
            sa.resetTargets();
            if (aiLogic.startsWith("MinLoyalty.")) {
                int minLoyalty = Integer.parseInt(aiLogic.substring(aiLogic.indexOf(".") + 1));
                if (source.getCounters(CounterEnumType.LOYALTY) < minLoyalty) {
                    return false;
                }
            } else if ("Polymorph".equals(aiLogic)) {
                CardCollection list = CardLists.getTargetableCards(ai.getCardsIn(ZoneType.Battlefield), sa);
                if (list.isEmpty()) {
                    return false;
                }
                for (Card c : list) {
                    if (c.hasKeyword(Keyword.INDESTRUCTIBLE)) {
                        sa.getTargets().add(c);
                        return true;
                    }
                }
                Card worst = ComputerUtilCard.getWorstAI(list);
                if (worst.isCreature() && ComputerUtilCard.evaluateCreature(worst) >= 200) {
                    return false;
                }
                if (!worst.isCreature() && worst.getCMC() > 1) {
                    return false;
                }
                sa.getTargets().add(worst);
                return true;
            } else if ("Pongify".equals(aiLogic)) {
                return SpecialAiLogic.doPongifyLogic(ai, sa);
            }
        }
        return super.checkAiLogic(ai, sa, aiLogic);
    }

    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph, final String logic) {
        if ("AtOpponentsCombatOrAfter".equals(logic)) {
            if (ph.isPlayerTurn(ai) || ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                return false;
            }
        } else if ("AtEOT".equals(logic)) {
            if (!ph.is(PhaseType.END_OF_TURN)) {
                return false;
            }
        } else if ("AtEOTIfNotAttacking".equals(logic)) {
            if (!ph.is(PhaseType.END_OF_TURN) || !ai.getCreaturesAttackedThisTurn().isEmpty()) {
                return false;
            }
        } else if ("Pactivator".equals(logic)) {
            // Ability that's intended to destroy own useless token to trigger Grave Pacts
            // should be fired at end of turn or when under attack after blocking to make opponent sac something
            boolean havepact = false;

            // TODO replace it with look for a dies -> sacrifice trigger check
            havepact |= ai.isCardInPlay("Grave Pact");
            havepact |= ai.isCardInPlay("Butcher of Malakir");
            havepact |= ai.isCardInPlay("Dictate of Erebos");
            if (havepact) {
                if ((!ph.isPlayerTurn(ai))
                        && ((ph.is(PhaseType.END_OF_TURN)) || (ph.is(PhaseType.COMBAT_DECLARE_BLOCKERS)))
                        && (ai.getOpponents().getCreaturesInPlay().size() > 0)) {
                    CardCollection list = CardLists.getTargetableCards(ai.getCardsIn(ZoneType.Battlefield), sa);
                    Card worst = ComputerUtilCard.getWorstAI(list);
                    if (worst != null) {
                        sa.getTargets().add(worst);
                        return true;
                    }
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        final Card source = sa.getHostCard();
        final boolean noRegen = sa.hasParam("NoRegen");
        final String logic = sa.getParam("AILogic");

        CardCollection list;

        if (sa.usesTargeting()) {
            // Assume there where already enough targets chosen by AI Logic Above
            if (sa.hasParam("AILogic") && !sa.canAddMoreTarget() && sa.isTargetNumberValid()) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }

            // reset targets before AI Logic part
            sa.resetTargets();
            int maxTargets;

            // If there's X in payment costs and it's tied to targeting, make sure we set the XManaCostPaid first
            // (e.g. Heliod's Intervention)
            if (sa.getRootAbility().costHasManaX() ||
                    ("X".equals(sa.getTargetRestrictions().getMinTargets()) && sa.getSVar("X").equals("Count$xPaid"))) {
                // TODO: currently the AI will maximize mana spent on X, trying to maximize damage. This may need improvement.
                maxTargets = ComputerUtilCost.getMaxXValue(sa, ai, sa.isTrigger());
                // need to set XPaid to get the right number for
                sa.getRootAbility().setXManaCostPaid(maxTargets);
                // need to check for maxTargets
                maxTargets = Math.min(maxTargets, sa.getMaxTargets());
            } else {
                maxTargets = sa.getMaxTargets();
            }

            if (maxTargets == 0) {
                // can't afford X or otherwise target anything
                return new AiAbilityDecision(0, AiPlayDecision.CantAffordX);
            }

            if (sa.hasParam("TargetingPlayer")) {
                Player targetingPlayer = AbilityUtils.getDefinedPlayers(source, sa.getParam("TargetingPlayer"), sa).get(0);
                sa.setTargetingPlayer(targetingPlayer);
                if (CardLists.getTargetableCards(ai.getGame().getCardsIn(sa.getTargetRestrictions().getZone()), sa).isEmpty()) {
                    return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
                }
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }

            // AI doesn't destroy own cards if it isn't defined in AI logic
            list = CardLists.getTargetableCards(ai.getOpponents().getCardsIn(ZoneType.Battlefield), sa);

            list = ComputerUtil.filterAITgts(sa, ai, list, true);

            list = CardLists.getNotKeyword(list, Keyword.INDESTRUCTIBLE);
            if (CardLists.getNotType(list, "Creature").isEmpty()) {
                list = ComputerUtilCard.prioritizeCreaturesWorthRemovingNow(ai, list, false);
            }
            if (!playReusable(ai, sa)) {
                Predicate<Card> hasCounter = CardPredicates.hasCounter(CounterEnumType.SHIELD, 1);
                list = CardLists.filter(list, hasCounter.negate());

                list = CardLists.filter(list, c -> {
                    //Check for cards that can be sacrificed in response
                    for (final SpellAbility ability : c.getAllSpellAbilities()) {
                        if (ability.isActivatedAbility()) {
                            final Cost cost = ability.getPayCosts();
                            for (final CostPart part : cost.getCostParts()) {
                                if (!(part instanceof CostSacrifice)) {
                                    continue;
                                }
                                CostSacrifice sacCost = (CostSacrifice) part;
                                if (sacCost.payCostFromSource() && ComputerUtilCost.canPayCost(ability, c.getController(), false)) {
                                    return false;
                                }
                            }
                        }
                    }
                    if (c.hasSVar("SacMe")) {
                        return false;
                    }
                    //Check for undying
                    return !c.hasKeyword(Keyword.UNDYING) || c.getCounters(CounterEnumType.P1P1) > 0;
                });
            }

            // If NoRegen is not set, filter out creatures that have a
            // regeneration shield
            if (!noRegen) {
                // TODO filter out things that might be tougher?
                list = CardLists.filter(list, c -> c.getShieldCount() == 0 && !ComputerUtil.canRegenerate(ai, c));
            }

            // Try to avoid targeting creatures that are dead on board
            list = ComputerUtil.filterCreaturesThatWillDieThisTurn(ai, list, sa);
            if (list.isEmpty()) {
                return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
            }

            // target loop
            // TODO use can add more Targets
            while (sa.getTargets().size() < maxTargets) {
                // filter by MustTarget requirement
                CardCollection originalList = new CardCollection(list);
                boolean mustTargetFiltered = StaticAbilityMustTarget.filterMustTargetCards(ai, list, sa);

                list = CardLists.canSubsequentlyTarget(list, sa);

                if (list.isEmpty()) {
                    if (!sa.isMinTargetChosen() || sa.isZeroTargets()) {
                        sa.resetTargets();
                        return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
                    } else {
                        // TODO is this good enough? for up to amounts?
                        break;
                    }
                }

                Card choice = null;
                // If the targets are only of one type, take the best
                if (CardLists.getNotType(list, "Creature").isEmpty()) {
                    choice = ComputerUtilCard.getBestCreatureAI(list);
                    if ("OppDestroyYours".equals(logic)) {
                        Card aiBest = ComputerUtilCard.getBestCreatureAI(ai.getCreaturesInPlay());
                        if (ComputerUtilCard.evaluateCreature(aiBest) > ComputerUtilCard.evaluateCreature(choice) - 40) {
                            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                        }
                    }
                } else if (CardLists.getNotType(list, "Land").isEmpty()) {
                    choice = ComputerUtilCard.getBestLandAI(list);

                    if ("LandForLand".equals(logic) || "GhostQuarter".equals(logic)) {
                        // Strip Mine, Wasteland - cut short if the relevant logic fails
                        if (!doLandForLandRemovalLogic(sa, ai, choice, logic)) {
                            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                        }
                    }
                } else {
                    // TODO look for "exiled until leaves" of own stuff
                    choice = ComputerUtilCard.getMostExpensivePermanentAI(list);
                }
                //option to hold removal instead only applies for single targeted removal
                if (!sa.isTrigger() && sa.getMaxTargets() == 1) {
                    if (choice == null || !ComputerUtilCard.useRemovalNow(sa, choice, 0, ZoneType.Graveyard)) {
                        return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
                    }
                }

                if (choice == null) { // can't find anything left
                    if (!sa.isMinTargetChosen() || sa.isZeroTargets()) {
                        sa.resetTargets();
                        return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
                    } else {
                        // TODO is this good enough? for up to amounts?
                        break;
                    }
                } else {
                    // Don't destroy stolen permanents when the stealing aura can be destroyed
                    if (choice.getOwner() == ai) {
                        for (Card aura : choice.getEnchantedBy()) {
                            SpellAbility sp = aura.getFirstSpellAbility();
                            if (sp != null && "GainControl".equals(sp.getParam("AILogic"))
                                && aura.getController() != ai && sa.canTarget(aura)) {
                                list.remove(choice);
                                choice = aura;
                            }
                        }
                        // TODO What about stolen permanents we're getting back at the end of the turn?
                    }
                }

                // Restore original list for next loop if filtered by MustTarget requirement
                if (mustTargetFiltered) {
                    list = originalList;
                }

                list.remove(choice);
                if (sa.canTarget(choice)) {
                    sa.getTargets().add(choice);
                }
            }
        } else if (sa.hasParam("Defined")) {
            list = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa);
            if ("WillSkipTurn".equals(logic) && (source.getController().equals(ai)
                || ai.getCreaturesInPlay().size() < ai.getWeakestOpponent().getCreaturesInPlay().size()
                || !source.getGame().getPhaseHandler().isPlayerTurn(ai)
                || ai.getLife() <= 5)) {
                // Basic ai logic for Lethal Vapors
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            } else if ("Always".equals(logic)) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }

            if (list.isEmpty()
                    || !CardLists.filterControlledBy(list, ai).isEmpty()
                    || CardLists.getNotKeyword(list, Keyword.INDESTRUCTIBLE).isEmpty()) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final boolean noRegen = sa.hasParam("NoRegen");
        if (sa.usesTargeting()) {
            sa.resetTargets();

            CardCollection list = CardLists.getTargetableCards(ai.getGame().getCardsIn(ZoneType.Battlefield), sa);

            if (list.isEmpty() || list.size() < sa.getMinTargets()) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }

            // Try to avoid targeting creatures that are dead on board
            list = ComputerUtil.filterCreaturesThatWillDieThisTurn(ai, list, sa);

            CardCollection preferred = CardLists.getNotKeyword(list, Keyword.INDESTRUCTIBLE);
            preferred = CardLists.filterControlledBy(preferred, ai.getOpponents());
            Predicate<Card> hasCounter = CardPredicates.hasCounter(CounterEnumType.SHIELD, 1);
            preferred = CardLists.filter(preferred, hasCounter.negate());
            if (CardLists.getNotType(preferred, "Creature").isEmpty()) {
                preferred = ComputerUtilCard.prioritizeCreaturesWorthRemovingNow(ai, preferred, false);
            }

            // If NoRegen is not set, filter out creatures that have a
            // regeneration shield
            if (!noRegen) {
                // TODO filter out things that could regenerate in response?
                // might be tougher?
                preferred = CardLists.filter(preferred, c -> c.getShieldCount() == 0);
            }

            // Filter AI-specific targets if provided
            preferred = ComputerUtil.filterAITgts(sa, ai, preferred, true);

            list.removeAll(preferred);

            if (preferred.isEmpty() && !mandatory) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }

            while (sa.canAddMoreTarget()) {
                if (preferred.isEmpty()) {
                    if (!sa.isMinTargetChosen()) {
                        if (!mandatory) {
                            sa.resetTargets();
                            return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
                        } else {
                            break;
                        }
                    } else {
                        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                    }
                } else {
                    Card c = ComputerUtilCard.getBestAI(preferred);

                    if (sa.canTarget(c)) {
                        sa.getTargets().add(c);
                    }
                    preferred.remove(c);
                }
            }

            while (!sa.isMinTargetChosen()) {
                if (list.isEmpty()) {
                    break;
                } else {
                    Card c;
                    if (CardLists.getNotType(list, "Creature").isEmpty()) {
                        if (!sa.getUniqueTargets().isEmpty() && sa.getParent().getApi() == ApiType.Destroy
                                && sa.getUniqueTargets().get(0) instanceof Card) {
                            // basic ai for Diaochan
                            c = (Card) sa.getUniqueTargets().get(0);
                        } else {
                            c = ComputerUtilCard.getWorstCreatureAI(list);
                        }
                    } else {
                        c = ComputerUtilCard.getCheapestPermanentAI(list, sa, false);
                    }
                    if (sa.canTarget(c)) {
                        sa.getTargets().add(c);
                    }
                    list.remove(c);
                }
            }

            if (sa.isTargetNumberValid()) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            } else {
                sa.resetTargets();
                return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
            }
        } else {
            if (mandatory) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            } else {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }
    }

    public boolean doLandForLandRemovalLogic(SpellAbility sa, Player ai, Card tgtLand, String logic) {
        if (tgtLand == null) { return false; }

        Player tgtPlayer = tgtLand.getController();
        int oppLandsOTB = tgtPlayer.getLandsInPlay().size();

        // AI profile-dependent properties
        int amountNoTempoCheck = AiProfileUtil.getIntProperty(ai, AiProps.STRIPMINE_MIN_LANDS_OTB_FOR_NO_TEMPO_CHECK);
        int amountNoTimingCheck = AiProfileUtil.getIntProperty(ai, AiProps.STRIPMINE_MIN_LANDS_FOR_NO_TIMING_CHECK);
        int amountLandsInHand = AiProfileUtil.getIntProperty(ai, AiProps.STRIPMINE_MIN_LANDS_IN_HAND_TO_ACTIVATE);
        int amountLandsToManalock = AiProfileUtil.getIntProperty(ai, AiProps.STRIPMINE_MAX_LANDS_TO_ATTEMPT_MANALOCKING);
        boolean highPriorityIfNoLandDrop = AiProfileUtil.getBoolProperty(ai, AiProps.STRIPMINE_HIGH_PRIORITY_ON_SKIPPED_LANDDROP);

        // if the opponent didn't play a land and has few lands OTB, might be worth mana-locking him
        PhaseHandler ph = ai.getGame().getPhaseHandler();
        boolean oppSkippedLandDrop = (tgtPlayer.getLandsPlayedLastTurn() == 0 && ph.isPlayerTurn(ai))
                || (tgtPlayer.getLandsPlayedThisTurn() == 0 && ph.isPlayerTurn(tgtPlayer) && ph.getPhase().isAfter(PhaseType.MAIN2));
        boolean canManaLock = oppLandsOTB <= amountLandsToManalock && oppSkippedLandDrop;

        // Best target is a basic land, and there's only one of it, so destroying it may potentially color-lock the opponent
        // (triggers either if the opponent skipped a land drop or if there are quite a few lands already in play but only one of the given type)
        CardCollection oppLands = tgtPlayer.getLandsInPlay();
        boolean canColorLock = (oppSkippedLandDrop || oppLands.size() > 3)
                && tgtLand.isBasicLand() && CardLists.count(oppLands, CardPredicates.nameEquals(tgtLand.getName())) == 1;

        // Non-basic lands are currently not ranked in any way in ComputerUtilCard#getBestLandAI, so if a non-basic land is best target,
        // consider killing it off unless there's too much potential tempo loss.
        // TODO: actually rank non-basics in that method and then kill off the potentially dangerous (manlands, Valakut) or lucrative
        // (dual/triple mana that opens access to a certain color) lands
        boolean nonBasicTgt = !tgtLand.isBasicLand();

        // Try not to lose tempo too much and not to mana-screw yourself when considering this logic
        int numLandsInHand = CardLists.count(ai.getCardsIn(ZoneType.Hand), CardPredicates.LANDS_PRODUCING_MANA);
        int numLandsOTB = CardLists.count(ai.getCardsIn(ZoneType.Battlefield), CardPredicates.LANDS_PRODUCING_MANA);

        // If the opponent skipped a land drop, consider not looking at having the extra land in hand if the profile allows it
        boolean isHighPriority = highPriorityIfNoLandDrop && oppSkippedLandDrop;

        boolean timingCheck = canManaLock || canColorLock || nonBasicTgt;
        boolean tempoCheck = numLandsOTB >= amountNoTempoCheck
                || ((numLandsInHand >= amountLandsInHand || isHighPriority) && ((numLandsInHand + numLandsOTB >= amountNoTimingCheck) || timingCheck));

        // For Ghost Quarter, only use it if you have either more lands in play than your opponent
        // or the same number of lands but an extra land in hand (otherwise the AI plays too suboptimally)
        if ("GhostQuarter".equals(logic)) {
            return tempoCheck && (numLandsOTB > oppLands.size() || (numLandsOTB == oppLands.size() && numLandsInHand > 0));
        } else {
            return tempoCheck;
        }
    }

    @Override
    public boolean willPayUnlessCost(Player payer, SpellAbility sa, Cost cost, boolean alreadyPaid, FCollectionView<Player> payers) {
        final Card host = sa.getHostCard();
        if (alreadyPaid) {
            return false;
        }

        if (sa.hasParam("Defined")) {
            CardCollection cards = AbilityUtils.getDefinedCards(host, sa.getParam("Defined"), sa);
            if (!cards.anyMatch(CardPredicates.isController(payer))) {
                return false;
            }
        }

        return super.willPayUnlessCost(payer, sa, cost, alreadyPaid, payers);
    }
}
