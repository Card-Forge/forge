package forge.card.ability.effects;


import forge.Card;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;

public class GameWinEffect extends SpellAbilityEffect {

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
