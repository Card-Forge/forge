package forge.game.ability.effects;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardUtil;
import forge.game.card.CardZoneTable;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

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

        if (sa.hasParam("RememberDestroyed") || sa.hasParam("RememberAttached")) {
            card.clearRemembered();
        }

        CardCollection tgtCards = getTargetCards(sa);
        CardCollection untargetedCards = CardUtil.getRadiance(sa);

        if (tgtCards.size() > 1) {
            tgtCards = (CardCollection) GameActionUtil.orderCardsByTheirOwners(game, tgtCards, ZoneType.Graveyard, sa);
        }

        CardZoneTable table = new CardZoneTable();
        Map<Integer, Card> cachedMap = Maps.newHashMap();
        for (final Card tgtC : tgtCards) {
            if (tgtC.isInPlay() && (!sa.usesTargeting() || tgtC.canBeTargetedBy(sa))) {
                Card gameCard = game.getCardState(tgtC, null);
                // gameCard is LKI in that case, the card is not in game anymore
                // or the timestamp did change
                // this should check Self too
                if (gameCard == null || !tgtC.equalsWithTimestamp(gameCard)) {
                    continue;
                }
                internalDestroy(gameCard, sa, table, cachedMap);
            }
        }

        if (untargetedCards.size() > 1) {
            untargetedCards = (CardCollection) GameActionUtil.orderCardsByTheirOwners(game, untargetedCards, ZoneType.Graveyard, sa);
        }

        for (final Card unTgtC : untargetedCards) {
            if (unTgtC.isInPlay()) {
                internalDestroy(unTgtC, sa, table, cachedMap);
            }
        }

        table.triggerChangesZoneAll(game, sa);
    }

    protected void internalDestroy(Card gameCard, SpellAbility sa, CardZoneTable table, Map<Integer, Card> cachedMap) {
        final Card card = sa.getHostCard();
        final Game game = card.getGame();

        final boolean remDestroyed = sa.hasParam("RememberDestroyed");
        final boolean remAttached = sa.hasParam("RememberAttached");
        final boolean noRegen = sa.hasParam("NoRegen");
        final boolean sac = sa.hasParam("Sacrifice");

        boolean destroyed = false;
        final Card lki = CardUtil.getLKICopy(gameCard, cachedMap);
        if (remAttached) {
            card.addRemembered(gameCard.getAttachedCards());
        }
        if (sac) {
            destroyed = game.getAction().sacrifice(gameCard, sa, table) != null;
        } else {
            destroyed = game.getAction().destroy(gameCard, sa, !noRegen, table);
        }
        if (destroyed && remDestroyed) {
            card.addRemembered(gameCard);
        }
        if (destroyed && sa.hasParam("RememberLKI")) {
            card.addRemembered(lki);
        }
    }

}
