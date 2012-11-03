package forge.card.abilityfactory.effects;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;

public class DrainManaEffect extends SpellEffect {
    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
    
        final List<Player> tgtPlayers = getTargetPlayers(sa, params);

        sb.append(StringUtils.join(tgtPlayers, ", "));
        sb.append(" empties his or her mana pool.");
    
        return sb.toString();
    }

    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final Target tgt = sa.getTarget();
    
        for (final Player p : getTargetPlayers(sa, params)) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                p.getManaPool().clearPool(false);
            }
        }
    }

} 