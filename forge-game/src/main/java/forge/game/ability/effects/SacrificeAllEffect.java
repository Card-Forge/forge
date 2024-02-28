package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;

public class SacrificeAllEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        // when getStackDesc is called, just build exactly what is happening
        final StringBuilder sb = new StringBuilder();

        if (sa.hasParam("Controller")) {
            List<Player> conts = getDefinedPlayersOrTargeted(sa, "Controller");
            sb.append(Lang.joinHomogenous(conts)).append(conts.size() == 1 ? " sacrifices " : " sacrifice ");
        } else {
            sb.append("Sacrifice ");
        }
        if (sa.hasParam("Defined")) {
            List<Card> toSac = getDefinedCardsOrTargeted(sa);
            sb.append(Lang.joinHomogenous(toSac)).append(".");
        } else {
            sb.append("permanents.");
        }
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();

        CardCollectionView list;
        if (sa.hasParam("Defined")) {
            list = AbilityUtils.getDefinedCards(host, sa.getParam("Defined"), sa);
        } else {
            list = game.getCardsIn(ZoneType.Battlefield);
            if (sa.hasParam("ValidCards")) {
                list = AbilityUtils.filterListByType(list, sa.getParam("ValidCards"), sa);
            }
        }

        final boolean remSacrificed = sa.hasParam("RememberSacrificed");
        if (remSacrificed) {
            host.clearRemembered();
        }

        // update cards that where using LKI
        CardCollection gameList = new CardCollection();
        for (Card sac : list) {
            if (!sac.canBeSacrificedBy(sa, true)) {
                continue;
            }
            gameList.add(game.getCardState(sac, null));
        }

        list = gameList;

        // Do controller check after LKI got updated
        if (sa.hasParam("Controller")) {
            list = CardLists.filterControlledBy(list, AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Controller"), sa));
        }

        list = GameActionUtil.orderCardsByTheirOwners(game, list, ZoneType.Graveyard, sa);

        Map<AbilityKey, Object> params = AbilityKey.newMap();
        CardZoneTable zoneMovements = AbilityKey.addCardZoneTableParams(params, sa);

        for (Card sac : list) {
            final Card lKICopy = zoneMovements.getLastStateBattlefield().get(sac);
            if (game.getAction().sacrifice(sac, sa, true, params) != null) {
                if (remSacrificed) {
                    host.addRemembered(lKICopy);
                }
                if (sa.hasParam("ImprintSacrificed")) {
                    host.addImprintedCard(lKICopy);
                }
            }
        }

        zoneMovements.triggerChangesZoneAll(game, sa);
    }

}
