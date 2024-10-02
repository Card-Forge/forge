package forge.game.ability.effects;

import java.util.Collections;
import java.util.List;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.MyRandom;

public class ReorderZoneEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final ZoneType zone = ZoneType.smartValueOf(sa.getParam("Zone"));
        final List<Player> tgtPlayers = getTargetPlayers(sa);
        boolean shuffle = sa.hasParam("Random");

        return "Reorder " + Lang.joinHomogenous(tgtPlayers) + " " + zone.toString() + " " + (shuffle ? "at random." : "as your choose.");
    }

    @Override
    public void resolve(SpellAbility sa) {
        final ZoneType zone = ZoneType.smartValueOf(sa.getParam("Zone"));
        boolean shuffle = sa.hasParam("Random");

        for (final Player p : getTargetPlayers(sa)) {
            if (!p.isInGame()) {
                continue;
            }

            CardCollection list = new CardCollection(p.getCardsIn(zone));
            if (shuffle) {
                Collections.shuffle(list, MyRandom.getRandom());
                p.getZone(zone).setCards(list);
            } else {
                CardCollectionView orderedCards = p.getController().orderMoveToZoneList(list, zone, sa);
                p.getZone(zone).setCards(orderedCards);
            }
        }
    }
}
