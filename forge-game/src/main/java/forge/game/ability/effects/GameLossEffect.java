package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.GameLossReason;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import java.util.List;

public class GameLossEffect extends SpellAbilityEffect {

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
        final Card card = sa.getHostCard();

        for (final Player p : getTargetPlayers(sa)) {
            p.loseConditionMet(GameLossReason.SpellEffect, card.getName());
        }
    }

}
