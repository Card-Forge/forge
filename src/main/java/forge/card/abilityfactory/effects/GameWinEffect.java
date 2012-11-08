package forge.card.abilityfactory.effects;


import forge.Card;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;

public class GameWinEffect extends SpellEffect {
    

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getSourceCard();

        for (final Player p : getTargetPlayers(sa)) {
            p.altWinBySpellEffect(card.getName());
        }
    }

}