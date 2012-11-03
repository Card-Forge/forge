package forge.card.abilityfactory.effects;

import java.util.List;

import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;

public class ScryEffect extends SpellEffect {
    
    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Player> tgtPlayers = getTargetPlayers(sa, params);

        for (final Player p : tgtPlayers) {
            sb.append(p.toString()).append(" ");
        }

        int num = 1;
        if (params.containsKey("ScryNum")) {
            num = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("ScryNum"), sa);
        }

        sb.append("scrys (").append(num).append(").");
        return sb.toString();
    }

    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
    
        int num = 1;
        if (params.containsKey("ScryNum")) {
            num = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("ScryNum"), sa);
        }
    
        final Target tgt = sa.getTarget();
        final List<Player> tgtPlayers = getTargetPlayers(sa, params);
    
        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                p.scry(num);
            }
        }
    }

}