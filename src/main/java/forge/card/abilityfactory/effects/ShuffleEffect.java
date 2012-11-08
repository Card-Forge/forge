package forge.card.abilityfactory.effects;

import java.util.Iterator;
import java.util.List;

import forge.Card;
import forge.GameActionUtil;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;

public class ShuffleEffect extends SpellEffect {
    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getSourceCard();
        final boolean optional = sa.hasParam("Optional");

        final List<Player> tgtPlayers = getTargetPlayers(sa); 

        final Target tgt = sa.getTarget();

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                if (optional && sa.getActivatingPlayer().isHuman()
                        && !GameActionUtil.showYesNoDialog(host, "Have " + p + " shuffle?")) {
                } else {
                    p.shuffle();
                }
            }
        }
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
    

        final String conditionDesc = sa.getParam("ConditionDescription");
        if (conditionDesc != null) {
            sb.append(conditionDesc).append(" ");
        }
    
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
            sb.append("s his or her library");
        }
        sb.append(".");
    
        return sb.toString();
    }

} 