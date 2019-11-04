package forge.game.ability.effects;

import java.util.List;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public class CantUntapTurnEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final long timestamp = game.getNextTimestamp();

        final int n = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Turns", "1"), sa);

        List<Card> cards = getTargetCards(sa);

        for (final Card tgtC : cards) {
            if (sa.usesTargeting() && !tgtC.canBeTargetedBy(sa)) {
                continue;
            }

            tgtC.addCantUntapTurn(timestamp, n);
        }

    }

}
