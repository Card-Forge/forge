package forge.game.ability.effects;

import forge.game.GameEndReason;
import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class GameDrawEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "The game is a draw.";
    }

    @Override
    public void resolve(SpellAbility sa) {
        for (Player p : sa.getHostCard().getGame().getPlayers()) {
            p.intentionalDraw();
        }
        sa.getHostCard().getGame().setGameOver(GameEndReason.Draw);
    }

}
