package forge.game.ability.effects;

import java.util.Map;

import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.*;
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

        String valid = "";

        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        CardCollectionView list;
        if (sa.hasParam("Defined")) {
            list = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa);
        }
        else {
            list = AbilityUtils.filterListByType(game.getCardsIn(ZoneType.Battlefield), valid, sa);
        }
        if (sa.hasParam("Controller")) {
            list = CardLists.filterControlledBy(list, AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Controller"), sa));
        }
        list = CardLists.filter(list, CardPredicates.canBeSacrificedBy(sa));

        final boolean remSacrificed = sa.hasParam("RememberSacrificed");
        if (remSacrificed) {
            card.clearRemembered();
        }

        if (list.size() > 1) {
            list = GameActionUtil.orderCardsByTheirOwners(game, list, ZoneType.Graveyard);
        }

        CardZoneTable table = new CardZoneTable();
        Map<Integer, Card> cachedMap = Maps.newHashMap();
        for (Card sac : list) {
            final Card lKICopy = CardUtil.getLKICopy(sac, cachedMap);
            if (game.getAction().sacrifice(sac, sa, table) != null && remSacrificed) {
                card.addRemembered(lKICopy);
            }
        }
        table.triggerChangesZoneAll(game);
    }

}
