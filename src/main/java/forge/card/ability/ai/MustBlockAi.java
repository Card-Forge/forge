package forge.card.ability.ai;

import java.util.List;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAiLogic;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.ai.ComputerUtilCombat;
import forge.game.phase.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.AIPlayer;
import forge.game.zone.ZoneType;

public class MustBlockAi extends SpellAiLogic {

    @Override
    protected boolean canPlayAI(AIPlayer aiPlayer, SpellAbility sa) {
        // disabled for the AI until he/she can make decisions about who to make
        // block
        return false;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, AIPlayer aiPlayer) {
        return false;
    }

    @Override
    protected boolean doTriggerAINoCost(final AIPlayer ai, SpellAbility sa, boolean mandatory) {
        final Card source = sa.getSourceCard();
        final Target abTgt = sa.getTarget();

        // only use on creatures that can attack
        if (!Singletons.getModel().getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)) {
            return false;
        }

        Card attacker = null;
        if (sa.hasParam("DefinedAttacker")) {
            final List<Card> cards = AbilityUtils.getDefinedCards(sa.getSourceCard(), sa.getParam("DefinedAttacker"), sa);
            if (cards.isEmpty()) {
                return false;
            }

            attacker = cards.get(0);
        }

        if (attacker == null) {
            attacker = source;
        }

        final Card definedAttacker = attacker;

        boolean chance = false;

        if (abTgt != null) {
            List<Card> list = CardLists.filter(ai.getOpponent().getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.CREATURES);
            list = CardLists.getTargetableCards(list, sa);
            list = CardLists.getValidCards(list, abTgt.getValidTgts(), source.getController(), source);
            list = CardLists.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    boolean tapped = c.isTapped();
                    c.setTapped(false);
                    if (!CombatUtil.canBlock(definedAttacker, c)) {
                        return false;
                    }
                    if (ComputerUtilCombat.canDestroyAttacker(ai, definedAttacker, c, null, false)) {
                        return false;
                    }
                    if (!ComputerUtilCombat.canDestroyBlocker(ai, c, definedAttacker, null, false)) {
                        return false;
                    }
                    c.setTapped(tapped);
                    return true;
                }
            });
            if (list.isEmpty()) {
                return false;
            }
            final Card blocker = CardFactoryUtil.getBestCreatureAI(list);
            if (blocker == null) {
                return false;
            }
            abTgt.addTarget(blocker);
            chance = true;
        } else {
            return false;
        }

        return chance;
    }
}
