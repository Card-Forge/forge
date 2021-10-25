package forge.ai.ability;

import com.google.common.base.Predicate;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class FlipOntoBattlefieldAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        PhaseHandler ph = sa.getHostCard().getGame().getPhaseHandler();
        String logic = sa.getParamOrDefault("AILogic", "");

        if (!SpellAbilityAi.isSorcerySpeed(sa) && sa.getPayCosts().hasManaCost()) {
            return ph.is(PhaseType.END_OF_TURN);
        }

        if ("DamageCreatures".equals(logic)) {
            int maxToughness = Integer.valueOf(sa.getSubAbility().getParam("NumDmg"));
            CardCollectionView rightToughness = CardLists.filter(aiPlayer.getOpponents().getCreaturesInPlay(), new Predicate<Card>() {
                @Override
                public boolean apply(Card card) {
                    return card.getNetToughness() <= maxToughness && card.canBeDestroyed();
                }
            });
            return !rightToughness.isEmpty();
        }

        return !aiPlayer.getOpponents().getCardsIn(ZoneType.Battlefield).isEmpty();
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return canPlayAI(aiPlayer, sa) || mandatory;
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        return true;
    }
}
