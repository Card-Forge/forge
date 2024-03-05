package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

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
        // up to X targets and chose 0 or similar situations
        if (tgtCards.isEmpty()) return sa.getParamOrDefault("SpellDescription", "");
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
        final Card host = sa.getHostCard();
        final Game game = host.getGame();

        if (sa.hasParam("RememberDestroyed")) {
            host.clearRemembered();
        }

        CardCollectionView untargetedCards = CardUtil.getRadiance(sa);
        CardCollectionView tgtCards = getTargetCards(sa);

        tgtCards = GameActionUtil.orderCardsByTheirOwners(game, tgtCards, ZoneType.Graveyard, sa);
        untargetedCards = GameActionUtil.orderCardsByTheirOwners(game, untargetedCards, ZoneType.Graveyard, sa);

        Map<AbilityKey, Object> params = AbilityKey.newMap();
        CardZoneTable zoneMovements = AbilityKey.addCardZoneTableParams(params, sa);

        for (final Card tgtC : tgtCards) {
            if (!tgtC.isInPlay()) {
                continue;
            }
            Card gameCard = game.getCardState(tgtC, null);
            // gameCard is LKI in that case, the card is not in game anymore
            // or the timestamp did change
            // this should check Self too
            if (gameCard == null || !tgtC.equalsWithTimestamp(gameCard)) {
                continue;
            }
            internalDestroy(gameCard, sa, params, zoneMovements);
        }

        for (final Card unTgtC : untargetedCards) {
            if (unTgtC.isInPlay()) {
                internalDestroy(unTgtC, sa, params, zoneMovements);
            }
        }

        zoneMovements.triggerChangesZoneAll(game, sa);
    }

    protected void internalDestroy(Card gameCard, SpellAbility sa, Map<AbilityKey, Object> params, CardZoneTable zoneMovements) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final boolean remDestroyed = sa.hasParam("RememberDestroyed");
        final boolean noRegen = sa.hasParam("NoRegen");
        final boolean sac = sa.hasParam("Sacrifice");
        final boolean alwaysRem = sa.hasParam("AlwaysRemember");
        boolean destroyed = false;

        SpellAbility cause = sa;
        if (sa.isReplacementAbility()) {
            cause = (SpellAbility) sa.getReplacingObject(AbilityKey.Cause);
        }

        if (sac) {
            destroyed = game.getAction().sacrifice(gameCard, cause, true, params) != null;
        } else {
            destroyed = game.getAction().destroy(gameCard, cause, !noRegen, params);
        }
        if (destroyed && remDestroyed) {
            host.addRemembered(gameCard);
        }
        if ((destroyed || alwaysRem) && sa.hasParam("RememberLKI")) {
            host.addRemembered(zoneMovements.getLastStateBattlefield().get(gameCard));
        }
    }

}
