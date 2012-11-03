package forge.card.abilityfactory.effects;

import java.util.Map;

import forge.Card;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;

public class GameWinEffect extends SpellEffect {
    

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(Map<String, String> params, SpellAbility sa) {
        final Card card = sa.getAbilityFactory().getHostCard();

        for (final Player p : getTargetPlayers(sa, params)) {
            p.altWinBySpellEffect(card.getName());
        }
    }

}