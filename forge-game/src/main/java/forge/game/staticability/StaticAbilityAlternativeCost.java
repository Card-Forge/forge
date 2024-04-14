package forge.game.staticability;

import java.util.List;

import com.google.common.collect.Lists;

import forge.card.mana.ManaCostParser;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class StaticAbilityAlternativeCost {

    static String MODE = "AlternativeCost";

    public static List<SpellAbility> alternativeCosts(final SpellAbility sa, final Card source, final Player pl) {
        List<SpellAbility> result = Lists.newArrayList();
        final Game game = sa.getHostCard().getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(MODE)) {
                    continue;
                }

                if (!apply(stAb, sa, source, pl)) {
                    continue;
                }

                Cost cost = new Cost(stAb.getParam("Cost"), sa.isAbility());
                // set the cost to this directly to bypass non mana cost
                final SpellAbility newSA = sa.copyWithDefinedCost(cost);
                newSA.setActivatingPlayer(pl);
                newSA.setBasicSpell(false);

                // CostDesc only for ManaCost?
                newSA.putParam("CostDesc", stAb.hasParam("CostDesc") ? ManaCostParser.parse(stAb.getParam("CostDesc")) : cost.toSimpleString());

                // makes new SpellDescription
                final StringBuilder sb = new StringBuilder();
                sb.append(newSA.getCostDescription());
                // skip reminder text for now, Keywords might be too complicated
                //sb.append("(").append(newKi.getReminderText()).append(")");
                if (sa.isSpell()) {
                    sb.append(" ").append(sa.getDescription());
                }
                newSA.setDescription(sb.toString());

                result.add(newSA);
            }
        }
        return result;
    }

    private static boolean apply(final StaticAbility stAb, final SpellAbility sa, final Card source, final Player pl) {
        if (!stAb.matchesValidParam("ValidSA", sa)) {
            return false;
        }
        if (!stAb.matchesValidParam("ValidCard", source)) {
            return false;
        }
        if (!stAb.matchesValidParam("ValidPlayer", pl)) {
            return false;
        }

        return true;
    }

}
