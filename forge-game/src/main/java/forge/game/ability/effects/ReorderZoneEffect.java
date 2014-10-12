package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.MyRandom;

import java.util.Collections;
import java.util.List;
import java.util.Random;

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
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        for (final Player p : getTargetPlayers(sa)) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                CardCollection list = new CardCollection(p.getCardsIn(zone));
                if (shuffle) {
                    final Random ran = MyRandom.getRandom();
                    Collections.shuffle(list, ran);
                    p.getZone(zone).setCards(list);
                }
                else {
                    p.getController().orderMoveToZoneList(list, zone);
                }
            }
        }
    }
}
