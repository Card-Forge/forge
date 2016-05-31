package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.zone.ZoneType;
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

            // Cards which are not on the battlefield should not be able to transform.
            if (!tgt.isInZone(ZoneType.Battlefield)) {
                continue;
            }

            if ("Transform".equals(sa.getParam("Mode")) && tgt.equals(host)) {
                // If want to Transform, and host is trying to transform self, skip if not in alignment
                boolean skip = tgt.getTransformedTimestamp() != Long.parseLong(sa.getSVar("StoredTransform"));
                // Clear SVar from SA so it doesn't get reused accidentally
                sa.getSVars().remove("StoredTransform");
                if (skip) {
                    continue;
                }
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
