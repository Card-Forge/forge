package forge.ai.ability;

import java.util.List;
import java.util.Random;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.SpecialCardAi;
import forge.ai.SpellAbilityAi;
import forge.ai.SpellApiToAi;
import forge.game.Game;
import forge.game.GlobalRuleChange;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.combat.CombatUtil;
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
        final Random r = MyRandom.getRandom();
        boolean randomReturn = r.nextFloat() <= .6667;
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
            	final int count = CardLists.count(ai.getCardsIn(ZoneType.Hand), new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        return (c.isInstant() || c.isSorcery()) && c != sa.getHostCard() && ComputerUtil.hasReasonToPlayCardThisTurn(ai, c);
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
                        return (c.isInstant() || c.isSorcery()) && !c.hasKeyword("Rebound") && ComputerUtil.hasReasonToPlayCardThisTurn(ai, c);
                    }
                });

                if(count == 0) {
                	return false;
                }

                randomReturn = true;
            } else if (logic.equals("Always")) {
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
                        && !game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noPrevention)  // no prevent damage
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
                if (topStack.getActivatingPlayer().isOpponentOf(ai) && topStack.getApi() == ApiType.GainLife) {
                	return true;
                } else {
                	return false;
                }
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
                    String k[] = logic.split(":");
                    Integer i = Integer.valueOf(k[1]);
                    if (ai.getCreaturesInPlay().size() < i) {
                        return false;
                    }
                }
                return true;
            } else if (logic.equals("CastFromGraveThisTurn")) {
                CardCollection list = new CardCollection(game.getCardsIn(ZoneType.Graveyard));
                list = CardLists.getValidCards(list, sa.getTargetRestrictions().getValidTgts(), ai, sa.getHostCard(), sa);
                if (!ComputerUtil.targetPlayableSpellCard(ai, list, sa, false)) {
                    return false;
                }
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
}
