package forge.ai.ability;

import java.util.List;

import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class BidLifeAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Game game = source.getGame();
        TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null) {
            sa.resetTargets();
            if (tgt.canTgtCreature()) {
                List<Card> list = CardLists.getTargetableCards(aiPlayer.getOpponent().getCardsIn(ZoneType.Battlefield), sa);
                list = CardLists.getValidCards(list, tgt.getValidTgts(), source.getController(), source, sa);
                if (list.isEmpty()) {
                    return false;
                }
                Card c = ComputerUtilCard.getBestCreatureAI(list);
                if (sa.canTarget(c)) {
                    sa.getTargets().add(c);
                } else {
                    return false;
                }
            } else if (tgt.getZone().contains(ZoneType.Stack)) {
                if (game.getStack().isEmpty()) {
                    return false;
                }
                final SpellAbility topSA = game.getStack().peekAbility();
                if (!CardFactoryUtil.isCounterableBy(topSA.getHostCard(), sa) || aiPlayer.equals(topSA.getActivatingPlayer())) {
                    return false;
                }
                if (sa.canTargetSpellAbility(topSA)) {
                    sa.getTargets().add(topSA);
                } else {
                    return false;
                }
            }
        }
        boolean chance = MyRandom.getRandom().nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());
        return chance;
    }

}
