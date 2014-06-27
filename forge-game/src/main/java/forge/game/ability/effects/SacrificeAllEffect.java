package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.List;

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

        List<Card> list;
        if (sa.hasParam("Defined")) {
            list = new ArrayList<Card>(AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa));
        } else {
            list = AbilityUtils.filterListByType(game.getCardsIn(ZoneType.Battlefield), valid, sa);
        }
        if (sa.hasParam("Controller")) {
            list = CardLists.filterControlledBy(list, AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Controller"), sa));
        }

        final boolean remSacrificed = sa.hasParam("RememberSacrificed");
        if (remSacrificed) {
            card.clearRemembered();
        }

        for (int i = 0; i < list.size(); i++) {
            if (game.getAction().sacrifice(list.get(i), sa) != null && remSacrificed) {
                card.addRemembered(list.get(i));
            }
        }
    }

}
