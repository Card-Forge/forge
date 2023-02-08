package forge.game.ability.effects;

import java.util.Iterator;
import java.util.List;

import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;

public class ShuffleEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final boolean optional = sa.hasParam("Optional");

        for (final Player p : getTargetPlayers(sa)) {
            if (!p.isInGame()) {
                continue;
            }
            boolean mustShuffle = !optional || sa.getActivatingPlayer().getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblHaveTargetShuffle", p.getName()), null);
            if (mustShuffle)
                p.shuffle(sa);
        }
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Player> tgtPlayers = getTargetPlayers(sa);

        if (tgtPlayers.size() > 0) {
            final Iterator<Player> it = tgtPlayers.iterator();
            while (it.hasNext()) {
                sb.append(it.next().getName());
                if (it.hasNext()) {
                    sb.append(" and ");
                }
            }
        } else {
            sb.append("Error - no target players for Shuffle. ");
        }
        sb.append(" shuffle");
        if (tgtPlayers.size() > 1) {
            sb.append(" their libraries");
        } else {
            sb.append("s their library");
        }
        sb.append(".");

        return sb.toString();
    }

}
