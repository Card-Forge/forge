package forge.game.ability.effects;

import java.util.Map;

import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class SacrificeAllEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        // when getStackDesc is called, just build exactly what is happening

        final StringBuilder sb = new StringBuilder();

        /*
         * This is not currently targeted ArrayList<Player> tgtPlayers;
         * 
         * Target tgt = af.getAbTgt(); if (tgt != null) tgtPlayers =
         * tgt.getTargetPlayers(); else tgtPlayers =
         * AbilityFactory.getDefinedPlayers(sa.getHostCard(),
         * sa.get("Defined"), sa);
         */

        sb.append("Sacrifice permanents.");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();

        CardCollectionView list;
        if (sa.hasParam("Defined")) {
            list = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa);
        } else {
            list = game.getCardsIn(ZoneType.Battlefield);
            if (sa.hasParam("ValidCards")) {
                list = AbilityUtils.filterListByType(list, sa.getParam("ValidCards"), sa);
            }
        }

        final boolean remSacrificed = sa.hasParam("RememberSacrificed");
        if (remSacrificed) {
            card.clearRemembered();
        }

        // update cards that where using LKI
        CardCollection gameList = new CardCollection();
        for (Card sac : list) {
            final Card gameCard = game.getCardState(sac, null);
            // gameCard is LKI in that case, the card is not in game anymore
            // or the timestamp did change
            // this should check Self too
            if (gameCard == null || !sac.equalsWithTimestamp(gameCard) || !gameCard.canBeSacrificedBy(sa)) {
                continue;
            }
            gameList.add(gameCard);
        }

        list = gameList;

        // Do controller check after LKI got updated
        if (sa.hasParam("Controller")) {
            list = CardLists.filterControlledBy(list, AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Controller"), sa));
        }

        if (list.size() > 1) {
            list = GameActionUtil.orderCardsByTheirOwners(game, list, ZoneType.Graveyard, sa);
        }

        CardZoneTable table = new CardZoneTable();
        Map<Integer, Card> cachedMap = Maps.newHashMap();
        for (Card sac : list) {
            final Card lKICopy = CardUtil.getLKICopy(sac, cachedMap);
            if (game.getAction().sacrifice(sac, sa, table) != null) {
                if (remSacrificed) {
                    card.addRemembered(lKICopy);
                }
                if (sa.hasParam("ImprintSacrificed")) {
                    card.addImprintedCard(lKICopy);
                }
            }
        }
        table.triggerChangesZoneAll(game, sa);
    }

}
