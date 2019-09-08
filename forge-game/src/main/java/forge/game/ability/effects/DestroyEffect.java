package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardUtil;
import forge.game.card.CardZoneTable;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.Iterator;
import java.util.List;

public class DestroyEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final boolean noRegen = sa.hasParam("NoRegen");
        final StringBuilder sb = new StringBuilder();

        final List<Card> tgtCards = getTargetCards(sa);

        if (sa.hasParam("Sacrifice")) {
            sb.append("Sacrifice ");
        } else {
            sb.append("Destroy ");
        }

        final Iterator<Card> it = tgtCards.iterator();
        while (it.hasNext()) {
            sb.append(it.next());

            if (it.hasNext()) {
                sb.append(", ");
            }
        }

        if (sa.hasParam("Radiance")) {
            sb.append(" and each other ").append(sa.getParam("ValidTgts"))
                    .append(" that shares a color with ");
            if (tgtCards.size() > 1) {
                sb.append("them");
            } else {
                sb.append("it");
            }
        }

        if (noRegen) {
            sb.append(". ");
            if (tgtCards.size() == 1) {
                sb.append("It");
            } else {
                sb.append("They");
            }
            sb.append(" can't be regenerated");
        }
        sb.append(".");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final Game game = card.getGame();

        final boolean remDestroyed = sa.hasParam("RememberDestroyed");
        final boolean remAttached = sa.hasParam("RememberAttached");
        if (remDestroyed || remAttached) {
            card.clearRemembered();
        }

        final boolean noRegen = sa.hasParam("NoRegen");
        final boolean sac = sa.hasParam("Sacrifice");

        CardCollection tgtCards = getTargetCards(sa);
        CardCollection untargetedCards = new CardCollection();

        if (sa.hasParam("Radiance")) {
            untargetedCards.addAll(CardUtil.getRadiance(card, tgtCards.get(0),
                    sa.getParam("ValidTgts").split(",")));
        }

        if (tgtCards.size() > 1) {
            tgtCards = (CardCollection) GameActionUtil.orderCardsByTheirOwners(game, tgtCards, ZoneType.Graveyard);
        }

        CardZoneTable table = new CardZoneTable();
        for (final Card tgtC : tgtCards) {
            if (tgtC.isInPlay() && (!sa.usesTargeting() || tgtC.canBeTargetedBy(sa))) {
                boolean destroyed = false;
                final Card lki = CardUtil.getLKICopy(tgtC);
                if (remAttached) {
                    card.addRemembered(tgtC.getAttachedCards());
                }
                if (sac) {
                    destroyed = game.getAction().sacrifice(tgtC, sa, table) != null;
                } else {
                    destroyed = game.getAction().destroy(tgtC, sa, !noRegen, table);
                }
                if (destroyed && remDestroyed) {
                    card.addRemembered(tgtC);
                }
                if (destroyed && sa.hasParam("RememberLKI")) {
                    card.addRemembered(lki);
                }
            }
        }

        if (untargetedCards.size() > 1) {
            untargetedCards = (CardCollection) GameActionUtil.orderCardsByTheirOwners(game, untargetedCards, ZoneType.Graveyard);
        }

        for (final Card unTgtC : untargetedCards) {
            if (unTgtC.isInPlay()) {
                boolean destroyed = false;
                if (sac) {
                    destroyed = game.getAction().sacrifice(unTgtC, sa, table) != null;
                } else {
                    destroyed = game.getAction().destroy(unTgtC, sa, !noRegen, table);
                } if (destroyed  && remDestroyed) {
                    card.addRemembered(unTgtC);
                }
            }
        }

        table.triggerChangesZoneAll(game);
    }

}
