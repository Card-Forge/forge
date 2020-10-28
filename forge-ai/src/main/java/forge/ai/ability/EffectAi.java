package forge.ai.ability;

import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.ai.*;
import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.*;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.cost.Cost;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class EffectAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(final Player ai,final SpellAbility sa) {
        final Game game = ai.getGame();
        boolean randomReturn = MyRandom.getRandom().nextFloat() <= .6667;
        String logic = "";

        if (sa.hasParam("AILogic")) {
            logic = sa.getParam("AILogic");
            final PhaseHandler phase = game.getPhaseHandler();
            if (logic.equals("BeginningOfOppTurn")) {
                if (!phase.getPlayerTurn().isOpponentOf(ai) || phase.getPhase().isAfter(PhaseType.DRAW)) {
                    return false;
                }
                randomReturn = true;
            } else if (logic.equals("EndOfOppTurn")) {
                if (!phase.getPlayerTurn().isOpponentOf(ai) || phase.getPhase().isBefore(PhaseType.END_OF_TURN)) {
                    return false;
                }
                randomReturn = true;
            } else if (logic.equals("KeepOppCreatsLandsTapped")) {
                for (Player opp : ai.getOpponents()) {
                    boolean worthHolding = false;
                    CardCollectionView oppCreatsLands = CardLists.filter(opp.getCardsIn(ZoneType.Battlefield),
                        Predicates.or(CardPredicates.Presets.LANDS, CardPredicates.Presets.CREATURES));
                    CardCollectionView oppCreatsLandsTapped = CardLists.filter(oppCreatsLands, CardPredicates.Presets.TAPPED);

                    if (oppCreatsLandsTapped.size() >= 3 || oppCreatsLands.size() == oppCreatsLandsTapped.size()) {
                        worthHolding = true;
                    }
                    if (!worthHolding) {
                        return false;
                    }
                    randomReturn = true;
                }
            } else if (logic.equals("Fog")) {
                if (game.getPhaseHandler().isPlayerTurn(sa.getActivatingPlayer())) {
                    return false;
                }
                if (!game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                    return false;
                }
                if (!game.getStack().isEmpty()) {
                    return false;
                }
                if (game.getPhaseHandler().isPreventCombatDamageThisTurn()) {
                    return false;
                }
                if (!ComputerUtilCombat.lifeInDanger(ai, game.getCombat())) {
                    return false;
                }
                final TargetRestrictions tgt = sa.getTargetRestrictions();
                if (tgt != null) {
                    sa.resetTargets();
                    if (tgt.canOnlyTgtOpponent()) {
                        boolean canTgt = false;

                        for (Player opp2 : ai.getOpponents()) {
                            if (sa.canTarget(opp2)) {
                                sa.getTargets().add(opp2);
                                canTgt = true;
                                break;
                            }
                        }

                        if (!canTgt) {
                            return false;
                        }
                    } else {
                        List<Card> list = game.getCombat().getAttackers();
                        list = CardLists.getValidCards(list, tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getHostCard(), sa);
                        list = CardLists.getTargetableCards(list, sa);
                        Card target = ComputerUtilCard.getBestCreatureAI(list);
                        if (target == null) {
                            return false;
                        }
                        sa.getTargets().add(target);
                    }
                }
                randomReturn = true;
            } else if (logic.equals("ChainVeil")) {
                if (!phase.isPlayerTurn(ai) || !phase.getPhase().equals(PhaseType.MAIN2) 
                		|| CardLists.getType(ai.getCardsIn(ZoneType.Battlefield), "Planeswalker").isEmpty()) {
                    return false;
                }
                randomReturn = true;
            } else if (logic.equals("SpellCopy")) {
            	// fetch Instant or Sorcery and AI has reason to play this turn
            	// does not try to get itself
                final ManaCost costSa = sa.getPayCosts().getTotalMana();
            	final int count = CardLists.count(ai.getCardsIn(ZoneType.Hand), new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        if (!(c.isInstant() || c.isSorcery()) || c.equals(sa.getHostCard())) {
                            return false;
                        }
                        for (SpellAbility ab : c.getSpellAbilities()) {
                            if (ComputerUtilAbility.getAbilitySourceName(sa).equals(ComputerUtilAbility.getAbilitySourceName(ab))
                                    || ab.hasParam("AINoRecursiveCheck")) {
                                // prevent infinitely recursing mana ritual and other abilities with reentry
                                continue;
                            } else if ("SpellCopy".equals(ab.getParam("AILogic")) && ab.getApi() == ApiType.Effect) {
                                // don't copy another copy spell, too complex for the AI
                                continue;
                            }
                            if (!ab.canPlay()) {
                                continue;
                            }
                            AiPlayDecision decision = ((PlayerControllerAi)ai.getController()).getAi().canPlaySa(ab);
                            // see if we can pay both for this spell and for the Effect spell we're considering
                            if (decision == AiPlayDecision.WillPlay || decision == AiPlayDecision.WaitForMain2) {
                                ManaCost costAb = ab.getPayCosts().getTotalMana();
                                ManaCost total = ManaCost.combine(costSa, costAb);
                                SpellAbility combinedAb = ab.copyWithDefinedCost(new Cost(total, false));
                                // can we pay both costs?
                                if (ComputerUtilMana.canPayManaCost(combinedAb, ai, 0)) {
                                    return true;
                                }
                            }
                        }

                        return false;
                    }
                });

                if(count == 0) {
                	return false;
                }

                randomReturn = true;            	
            } else if (logic.equals("NarsetRebound")) {
            	// should be done in Main2, but it might broke for other cards
            	//if (phase.getPhase().isBefore(PhaseType.MAIN2)) {
                //    return false;
                //}

                // fetch Instant or Sorcery without Rebound and AI has reason to play this turn
            	// only need count, not the list
            	final int count = CardLists.count(ai.getCardsIn(ZoneType.Hand), new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        if (!(c.isInstant() || c.isSorcery()) || c.hasKeyword(Keyword.REBOUND)) {
                            return false;
                        }
                        for (SpellAbility ab : c.getSpellAbilities()) {
                            if (ComputerUtilAbility.getAbilitySourceName(sa).equals(ComputerUtilAbility.getAbilitySourceName(ab))
                                    || ab.hasParam("AINoRecursiveCheck")) {
                                // prevent infinitely recursing mana ritual and other abilities with reentry
                                continue;
                            }
                            if (!ab.canPlay()) {
                                continue;
                            }
                            AiPlayDecision decision = ((PlayerControllerAi)ai.getController()).getAi().canPlaySa(ab);
                            if (decision == AiPlayDecision.WillPlay || decision == AiPlayDecision.WaitForMain2) {
                                if (ComputerUtilMana.canPayManaCost(ab, ai, 0)) {
                                    return true;
                                }
                            }
                        }
                        return false;
                    }
                });

                if(count == 0) {
                	return false;
                }

                randomReturn = true;
            } else if (logic.equals("Always")) {
                randomReturn = true;
            } else if (logic.equals("Main1")) {
                if (phase.getPhase().isBefore(PhaseType.MAIN1)) {
                    return false;
                }
                randomReturn = true;
            } else if (logic.equals("Main2")) {
                if (phase.getPhase().isBefore(PhaseType.MAIN2)) {
                    return false;
                }
                randomReturn = true;
            } else if (logic.equals("Evasion")) {

            	if (!phase.isPlayerTurn(ai)) {
            		return false;
            	}

                boolean shouldPlay = false;

                List<Card> comp = ai.getCreaturesInPlay();

                for (final Player opp : ai.getOpponents()) {
                    List<Card> human = opp.getCreaturesInPlay();

                    // only count creatures that can attack or block
                    comp = CardLists.filter(comp, new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card c) {
                            return CombatUtil.canAttack(c, opp);
                        }
                    });
                    if (comp.size() < 2) {
                        continue;
                    }
                    final List<Card> attackers = comp;
                    human = CardLists.filter(human, new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card c) {
                            return CombatUtil.canBlockAtLeastOne(c, attackers);
                        }
                    });
                    if (human.isEmpty()) {
                        continue;
                    }

                    shouldPlay = true;
                    break;
                }

                return shouldPlay;
            } else if (logic.equals("RedirectSpellDamageFromPlayer")) {
                if (game.getStack().isEmpty()) {
                    return false;
                }
                boolean threatened = false;
                for (final SpellAbilityStackInstance stackInst : game.getStack()) {
                    if (!stackInst.isSpell()) { continue; }
                    SpellAbility stackSpellAbility = stackInst.getSpellAbility(true);
                    if (stackSpellAbility.getApi() == ApiType.DealDamage) {
                        final SpellAbility saTargeting = stackSpellAbility.getSATargetingPlayer();
                        if (saTargeting != null && Iterables.contains(saTargeting.getTargets().getTargetPlayers(), ai)) {
                            threatened = true;
                        }
                    }
                }
                randomReturn = threatened;
            } else if (logic.equals("Prevent")) {   // prevent burn spell from opponent
                if (game.getStack().isEmpty()) {
                    return false;
                }
                final SpellAbility saTop = game.getStack().peekAbility();
                final Card host = saTop.getHostCard();
                if (saTop.getActivatingPlayer() != ai   // from opponent
                        && host.canDamagePrevented(false)  // no prevent damage
                        && host != null && (host.isInstant() || host.isSorcery())
                        && !host.hasKeyword("Prevent all damage that would be dealt by CARDNAME.")) {  // valid target
                    final ApiType type = saTop.getApi();
                    if (type == ApiType.DealDamage || type == ApiType.DamageAll) {  // burn spell
                        sa.getTargets().add(host);
                        return true;
                    }
                }
                return false;
            } else if (logic.equals("NoGain")) {
            	// basic logic to cancel GainLife on stack
                if (game.getStack().isEmpty()) {
                    return false;
                }
                final SpellAbility topStack = game.getStack().peekAbility();
                return topStack.getActivatingPlayer().isOpponentOf(ai) && topStack.getApi() == ApiType.GainLife;
            } else if (logic.equals("Fight")) {
                return FightAi.canFightAi(ai, sa, 0, 0);
            } else if (logic.equals("Burn")) {
                // for DamageDeal sub-abilities (eg. Wild Slash, Skullcrack)
                SpellAbility burn = sa.getSubAbility();
                return SpellApiToAi.Converter.get(burn.getApi()).canPlayAIWithSubs(ai, burn);
            } else if (logic.equals("YawgmothsWill")) {
                return SpecialCardAi.YawgmothsWill.consider(ai, sa);
            } else if (logic.startsWith("NeedCreatures")) {
                if (ai.getCreaturesInPlay().isEmpty()) {
                    return false;
                }
                if (logic.contains(":")) {
                    String[] k = logic.split(":");
                    Integer i = Integer.valueOf(k[1]);
                    return ai.getCreaturesInPlay().size() >= i;
                }
                return true;
            } else if (logic.equals("CastFromGraveThisTurn")) {
                CardCollection list = new CardCollection(game.getCardsIn(ZoneType.Graveyard));
                list = CardLists.getValidCards(list, sa.getTargetRestrictions().getValidTgts(), ai, sa.getHostCard(), sa);
                if (!ComputerUtil.targetPlayableSpellCard(ai, list, sa, false)) {
                    return false;
                }
            } else if (logic.equals("Bribe")) {
                Card host = sa.getHostCard();
                Combat combat = game.getCombat();
                if (combat != null && combat.isAttacking(host, ai) && !combat.isBlocked(host)
                        && game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)
                        && !AiCardMemory.isRememberedCard(ai, host, AiCardMemory.MemorySet.ACTIVATED_THIS_TURN)) {
                    AiCardMemory.rememberCard(ai, host, AiCardMemory.MemorySet.ACTIVATED_THIS_TURN); // ideally needs once per combat or something
                    return true;
                }
                return false;
            }
        } else { //no AILogic
            return false;
        }

        if ("False".equals(sa.getParam("Stackable"))) {
            String name = sa.getParam("Name");
            if (name == null) {
                name = sa.getHostCard().getName() + "'s Effect";
            }
            if (sa.getActivatingPlayer().isCardInCommand(name)) {
                return false;
            }
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null && tgt.canTgtPlayer()) {
            sa.resetTargets();
            if (tgt.canOnlyTgtOpponent() || logic.equals("BeginningOfOppTurn")) {
                boolean canTgt = false;
                for (Player opp : ai.getOpponents()) {
                    if (sa.canTarget(opp)) {
                        sa.getTargets().add(opp);
                        canTgt = true;
                        break;
                    }
                }
                return canTgt;
            } else {
                sa.getTargets().add(ai);
            }
        }

        return randomReturn;
    }

    @Override
    protected boolean doTriggerAINoCost(final Player aiPlayer, final SpellAbility sa, final boolean mandatory) {
        String aiLogic = sa.getParamOrDefault("AILogic", "");

        // E.g. Nova Pentacle
        if (aiLogic.equals("RedirectFromOppToCreature")) {
            // try to target the opponent's best targetable permanent, if able
            CardCollection oppPerms = CardLists.getValidCards(aiPlayer.getOpponents().getCardsIn(ZoneType.Battlefield), sa.getTargetRestrictions().getValidTgts(), aiPlayer, sa.getHostCard(), sa);
            if (!oppPerms.isEmpty()) {
                sa.resetTargets();
                sa.getTargets().add(ComputerUtilCard.getBestAI(oppPerms));
                return true;
            }

            if (mandatory) {
                // try to target the AI's worst targetable permanent, if able
                CardCollection aiPerms = CardLists.getValidCards(aiPlayer.getCardsIn(ZoneType.Battlefield), sa.getTargetRestrictions().getValidTgts(), aiPlayer, sa.getHostCard(), sa);
                if (!aiPerms.isEmpty()) {
                    sa.resetTargets();
                    sa.getTargets().add(ComputerUtilCard.getWorstAI(aiPerms));
                    return true;
                }
            }

            return false;
        }

        return super.doTriggerAINoCost(aiPlayer, sa, mandatory);

    }
}
