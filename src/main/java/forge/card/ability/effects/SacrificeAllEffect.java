package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class SacrificeAllEffect extends SpellEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        // when getStackDesc is called, just build exactly what is happening

        final StringBuilder sb = new StringBuilder();

        /*
         * This is not currently targeted ArrayList<Player> tgtPlayers;
         * 
         * Target tgt = af.getAbTgt(); if (tgt != null) tgtPlayers =
         * tgt.getTargetPlayers(); else tgtPlayers =
         * AbilityFactory.getDefinedPlayers(sa.getSourceCard(),
         * sa.get("Defined"), sa);
         */

        sb.append("Sacrifice permanents.");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {

        final Card card = sa.getSourceCard();

        String valid = "";

        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        List<Card> list;
        if (sa.hasParam("Defined")) {
            list = new ArrayList<Card>(AbilityUtils.getDefinedCards(sa.getSourceCard(), sa.getParam("Defined"), sa));
        } else {
            list = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        }

        final boolean remSacrificed = sa.hasParam("RememberSacrificed");
        if (remSacrificed) {
            card.clearRemembered();
        }

        list = AbilityUtils.filterListByType(list, valid, sa);

        for (int i = 0; i < list.size(); i++) {
            if (Singletons.getModel().getGame().getAction().sacrifice(list.get(i), sa) && remSacrificed) {
                card.addRemembered(list.get(i));
            }
        }
    }

}
