package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Map;

import forge.Card;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.GameLossReason;
import forge.game.player.Player;

public class GameLossEffect extends SpellEffect {
    
    /* (non-Javadoc)
         * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
         */
    @Override
    protected String getStackDescription(Map<String, String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final Card source = sa.getSourceCard();
    
        if (!(sa instanceof AbilitySub)) {
            sb.append(source.getName()).append(" - ");
        } else {
            sb.append(" ");
        }
    
        final Target tgt = sa.getTarget();
        ArrayList<Player> players = null;
        if (sa.getTarget() != null) {
            players = tgt.getTargetPlayers();
        } else {
            players = AbilityFactory.getDefinedPlayers(source, params.get("Defined"), sa);
        }
    
        for (final Player p : players) {
            sb.append(p.getName()).append(" ");
        }
    
        sb.append("loses the game.");
        return sb.toString();
    }

    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final Card card = sa.getAbilityFactory().getHostCard();

        final Target tgt = sa.getTarget();
        ArrayList<Player> players = null;
        if (sa.getTarget() != null) {
            players = tgt.getTargetPlayers();
        } else {
            players = AbilityFactory.getDefinedPlayers(card, params.get("Defined"), sa);
        }

        for (final Player p : players) {
            p.loseConditionMet(GameLossReason.SpellEffect, card.getName());
        }
    }

} 