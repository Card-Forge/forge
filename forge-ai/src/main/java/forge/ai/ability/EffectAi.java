package forge.ai.ability;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.SpellAbilityAi;
import forge.ai.SpellApiToAi;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetRestrictions;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class EffectAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Game game = ai.getGame();
        final Random r = MyRandom.getRandom();
        boolean randomReturn = r.nextFloat() <= .6667;
        final Player opp = ai.getOpponent();
        String logic = "";

        if (sa.hasParam("AILogic")) {
            logic = sa.getParam("AILogic");
            final PhaseHandler phase = game.getPhaseHandler();
            if (logic.equals("BeginningOfOppTurn")) {
                if (phase.isPlayerTurn(ai) || phase.getPhase().isAfter(PhaseType.DRAW)) {
                    return false;
                }
                randomReturn = true;
            } else if (logic.equals("EndOfOppTurn")) {
                if (phase.isPlayerTurn(ai) || phase.getPhase().isBefore(PhaseType.END_OF_TURN)) {
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
                        if (sa.canTarget(ai.getOpponent())) {
                            sa.getTargets().add(ai.getOpponent());
                        } else {
                            return false;
                        }
                    } else {
                        List<Card> list = game.getCombat().getAttackers();
                        list = CardLists.getValidCards(list, tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getHostCard());
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

                List<Card> comp = ai.getCreaturesInPlay();
                List<Card> human = opp.getCreaturesInPlay();

                // only count creatures that can attack or block
                comp = CardLists.filter(comp, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        return CombatUtil.canAttack(c, opp);
                    }
                });
                if (comp.size() < 2) {
                	return false;
                }
                final List<Card> attackers = comp;
                human = CardLists.filter(human, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        return CombatUtil.canBlockAtLeastOne(c, attackers);
                    }
                });
                if (human.isEmpty()) {
                	return false;
                }
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
            } else if (logic.equals("Fight")) {
                CardCollection humCreatures = ai.getOpponent().getCreaturesInPlay();
                humCreatures = CardLists.getTargetableCards(humCreatures, sa);
                ComputerUtilCard.sortByEvaluateCreature(humCreatures);

                final AbilitySub tgtFight = sa.getSubAbility();
                CardCollection aiCreatures = ai.getCreaturesInPlay();
                aiCreatures = CardLists.getTargetableCards(aiCreatures, tgtFight);
                aiCreatures = ComputerUtil.getSafeTargets(ai, tgtFight, aiCreatures);
                ComputerUtilCard.sortByEvaluateCreature(aiCreatures);

                if (humCreatures.isEmpty() || aiCreatures.isEmpty()) {
                	return false;
                }
                int buffedAtk = 0, buffedDef = 0;
                for (Card humanCreature : humCreatures) {
                	for (Card aiCreature : aiCreatures) {
                	    if (sa.isSpell()) {   //heroic triggers adding counters
                            for (Trigger t : aiCreature.getTriggers()) {
                                if (t.getMode() == TriggerType.SpellCast) {
                                    final Map<String, String> params = t.getMapParams();
                                    if ("Card.Self".equals(params.get("TargetsValid")) && "You".equals(params.get("ValidActivatingPlayer")) 
                                    		&& params.containsKey("Execute")) {
                                        SpellAbility heroic = AbilityFactory.getAbility(aiCreature.getSVar(params.get("Execute")),aiCreature);
                                        if ("Self".equals(heroic.getParam("Defined")) && "P1P1".equals(heroic.getParam("CounterType"))) {
                                            int amount = AbilityUtils.calculateAmount(aiCreature, heroic.getParam("CounterNum"), heroic);
                                            buffedAtk += amount;
                                            buffedDef += amount;
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                		if (FightAi.shouldFight(aiCreature, humanCreature, buffedAtk, buffedDef)) {
                			tgtFight.getTargets().add(aiCreature);
                			sa.getTargets().add(humanCreature);
                			return true;
                		} else {
                		    return false;
                		}
                	}
                }
            } else if (logic.equals("Burn")) {
                // for DamageDeal sub-abilities (eg. Wild Slash, Skullcrack)
                SpellAbility burn = sa.getSubAbility();
                return SpellApiToAi.Converter.get(burn.getApi()).canPlayAIWithSubs(ai, burn);
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
                sa.getTargets().add(ai.getOpponent());
            } else {
                sa.getTargets().add(ai);
            }
        }

        return randomReturn;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {

        final Player opp = aiPlayer.getOpponent();

        if (sa.usesTargeting()) {
            sa.resetTargets();
            if (mandatory && sa.canTarget(opp)) {
                sa.getTargets().add(opp);
            } else if (mandatory && sa.canTarget(aiPlayer)) {
                sa.getTargets().add(aiPlayer);
            }
        }

        return true;
    }
}
