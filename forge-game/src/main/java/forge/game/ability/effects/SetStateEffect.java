package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.spellability.SpellAbility;

import java.util.Iterator;
import java.util.List;

public class SetStateEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Card> tgtCards = getTargetCards(sa);

        if (sa.hasParam("Flip")) {
            sb.append("Flip");
        } else {
            sb.append("Transform ");
        }

        final Iterator<Card> it = tgtCards.iterator();
        while (it.hasNext()) {
            sb.append(it.next());

            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(".");
        return sb.toString();
    }

    @Override
    public void resolve(final SpellAbility sa) {

        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final List<Card> tgtCards = getTargetCards(sa);

        final boolean remChanged = sa.hasParam("RememberChanged");

        for (final Card tgt : tgtCards) {
            if (sa.usesTargeting() && !tgt.canBeTargetedBy(sa)) {
                continue;
            }

            boolean hasTransformed = tgt.changeCardState(sa.getParam("Mode"), sa.getParam("NewState"));
            if ( hasTransformed ) {
                game.fireEvent(new GameEventCardStatsChanged(tgt));
            }
            if ( hasTransformed && remChanged) {
                host.addRemembered(tgt);
            }
        }
    }
}
