package forge.ai.ability;

import com.google.common.base.Predicate;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

import java.util.List;

public class MustBlockAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        // disabled for the AI until he/she can make decisions about who to make
        // block
        return false;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        return false;
    }

    @Override
    protected boolean doTriggerAINoCost(final Player ai, SpellAbility sa, boolean mandatory) {
        final Card source = sa.getHostCard();
        final TargetRestrictions abTgt = sa.getTargetRestrictions();

        // only use on creatures that can attack
        if (!ai.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)) {
            return false;
        }

        Card attacker = null;
        if (sa.hasParam("DefinedAttacker")) {
            final List<Card> cards = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("DefinedAttacker"), sa);
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
            list = CardLists.getValidCards(list, abTgt.getValidTgts(), source.getController(), source, sa);
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
            final Card blocker = ComputerUtilCard.getBestCreatureAI(list);
            if (blocker == null) {
                return false;
            }
            sa.getTargets().add(blocker);
            chance = true;
        } else {
            return false;
        }

        return chance;
    }
}
