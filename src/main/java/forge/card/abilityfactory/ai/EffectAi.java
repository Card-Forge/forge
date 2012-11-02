package forge.card.abilityfactory.ai;

import java.util.List;
import java.util.Random;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.GameState;
import forge.game.phase.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class EffectAi extends SpellAiLogic {
    @Override
    public boolean canPlayAI(Player ai, java.util.Map<String,String> params, SpellAbility sa) {
        final GameState game = Singletons.getModel().getGame();
        final Random r = MyRandom.getRandom();
        boolean randomReturn = r.nextFloat() <= .6667;
        final Player opp = ai.getOpponent();
        String logic = "";

        if (params.containsKey("AILogic")) {
            logic = params.get("AILogic");
            final PhaseHandler phase = game.getPhaseHandler();
            if (logic.equals("BeginningOfOppTurn")) {
                if (phase.isPlayerTurn(ai.getOpponent()) || phase.getPhase().isAfter(PhaseType.DRAW)) {
                    return false;
                }
                randomReturn = true;
            } else if (logic.equals("Fog")) {
                if (game.getPhaseHandler().isPlayerTurn(sa.getActivatingPlayer())) {
                    return false;
                }
                if (!game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
                    return false;
                }
                if (game.getStack().size() != 0) {
                    return false;
                }
                if (game.getPhaseHandler().isPreventCombatDamageThisTurn()) {
                    return false;
                }
                if (!CombatUtil.lifeInDanger(ai, game.getCombat())) {
                    return false;
                }
                final Target tgt = sa.getTarget();
                if (tgt != null) {
                    tgt.resetTargets();
                    List<Card> list = game.getCombat().getAttackerList();
                    list = CardLists.getValidCards(list, tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getSourceCard());
                    list = CardLists.getTargetableCards(list, sa);
                    Card target = CardFactoryUtil.getBestCreatureAI(list);
                    if (target == null) {
                        return false;
                    }
                    tgt.addTarget(target);
                }
                randomReturn = true;
            } else if (logic.equals("Always")) {
                randomReturn = true;
            } else if (logic.equals("Evasion")) {
                List<Card> comp = ai.getCreaturesInPlay();
                List<Card> human = opp.getCreaturesInPlay();

                // only count creatures that can attack or block
                comp = CardLists.filter(comp, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        return CombatUtil.canAttack(c, opp);
                    }
                });
                human = CardLists.filter(human, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        return CombatUtil.canBlock(c);
                    }
                });
                if (comp.size() < 2 || human.size() < 1) {
                    randomReturn = false;
                }
            }
        } else { //no AILogic
            return false;
        }

        final String stackable = params.get("Stackable");

        if ((stackable != null) && stackable.equals("False")) {
            String name = params.get("Name");
            if (name == null) {
                name = sa.getSourceCard().getName() + "'s Effect";
            }
            final List<Card> list = sa.getActivatingPlayer().getCardsIn(ZoneType.Battlefield, name);
            if (list.size() != 0) {
                return false;
            }
        }

        final Target tgt = sa.getTarget();
        if (tgt != null && tgt.canTgtPlayer()) {
            tgt.resetTargets();
            if (tgt.canOnlyTgtOpponent() || logic.equals("BeginningOfOppTurn")) {
                tgt.addTarget(ai.getOpponent());
            } else {
                tgt.addTarget(ai);
            }
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            randomReturn &= subAb.chkAIDrawback();
        }

        return randomReturn;
    }

    @Override
    public boolean doTriggerAINoCost(Player aiPlayer, java.util.Map<String,String> params, SpellAbility sa, boolean mandatory) {
        // TODO: Add targeting effects

        // check SubAbilities DoTrigger?
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            return abSub.doTrigger(mandatory);
        }

        return true;
    }
}