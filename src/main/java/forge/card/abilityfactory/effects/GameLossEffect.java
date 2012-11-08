package forge.card.abilityfactory.effects;

import java.util.List;

import forge.Card;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.game.GameLossReason;
import forge.game.player.Player;

public class GameLossEffect extends SpellEffect {
    
    /* (non-Javadoc)
         * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
         */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Player> tgtPlayers = getTargetPlayers(sa);
        for (final Player p : tgtPlayers) {
            sb.append(p.getName()).append(" ");
        }
    
        sb.append("loses the game.");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getSourceCard();

        for (final Player p : getTargetPlayers(sa)) {
            p.loseConditionMet(GameLossReason.SpellEffect, card.getName());
        }
    }

} 