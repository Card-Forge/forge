package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardUtil;
import forge.game.card.CardZoneTable;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;

public class DestroyEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final boolean noRegen = sa.hasParam("NoRegen");
        final StringBuilder sb = new StringBuilder();

        final List<Card> tgtCards = getTargetCards(sa);
        final boolean justOne = tgtCards.size() == 1;

        sb.append(sa.hasParam("Sacrifice") ? "Sacrifice " : "Destroy ").append(Lang.joinHomogenous(tgtCards));

        if (sa.hasParam("Radiance")) {
            final String thing = sa.getParamOrDefault("ValidTgts", "thing");
            sb.append(" and each other ").append(thing).append(" that shares a color with ");
            sb.append(justOne ? "it" : "them");
        }

        if (noRegen) {
            sb.append(". ").append(justOne ? "It" : "They").append(" can't be regenerated");
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

        CardCollectionView tgtCards = getTargetCards(sa);
        CardCollectionView untargetedCards = CardUtil.getRadiance(sa);

        tgtCards = GameActionUtil.orderCardsByTheirOwners(game, tgtCards, ZoneType.Graveyard, sa);

        Map<AbilityKey, Object> params = AbilityKey.newMap();
        params.put(AbilityKey.LastStateBattlefield, game.copyLastStateBattlefield());

        CardZoneTable table = new CardZoneTable();
        Map<Integer, Card> cachedMap = Maps.newHashMap();
        for (final Card tgtC : tgtCards) {
            if (!tgtC.isInPlay()) {
                continue;
            }
            Card gameCard = game.getCardState(tgtC, null);
            // gameCard is LKI in that case, the card is not in game anymore
            // or the timestamp did change
            // this should check Self too
            if (gameCard == null || !tgtC.equalsWithGameTimestamp(gameCard)) {
                continue;
            }
            internalDestroy(gameCard, sa, table, cachedMap, params);
        }

        untargetedCards = GameActionUtil.orderCardsByTheirOwners(game, untargetedCards, ZoneType.Graveyard, sa);

        for (final Card unTgtC : untargetedCards) {
            if (unTgtC.isInPlay()) {
                internalDestroy(unTgtC, sa, table, cachedMap, params);
            }
        }

        table.triggerChangesZoneAll(game, sa);
    }

    protected void internalDestroy(Card gameCard, SpellAbility sa, CardZoneTable table, Map<Integer, Card> cachedMap, Map<AbilityKey, Object> params) {
        final Card card = sa.getHostCard();
        final Game game = card.getGame();

        final boolean remDestroyed = sa.hasParam("RememberDestroyed");
        final boolean remAttached = sa.hasParam("RememberAttached");
        final boolean noRegen = sa.hasParam("NoRegen");
        final boolean sac = sa.hasParam("Sacrifice");
        final boolean alwaysRem = sa.hasParam("AlwaysRemember");

        boolean destroyed = false;
        final Card lki = sa.hasParam("RememberLKI") ? CardUtil.getLKICopy(gameCard, cachedMap) : null;
        if (remAttached) {
            card.addRemembered(gameCard.getAttachedCards());
        }
        if (sac) {
            destroyed = game.getAction().sacrifice(gameCard, sa, true, table, params) != null;
        } else {
            destroyed = game.getAction().destroy(gameCard, sa, !noRegen, table, params);
        }
        if (destroyed && remDestroyed) {
            card.addRemembered(gameCard);
        }
        if ((destroyed || alwaysRem) && sa.hasParam("RememberLKI")) {
            card.addRemembered(lki);
        }
    }

}
