package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Map;

import forge.Card;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;

public class GameWinEffect extends SpellEffect {
    

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(Map<String, String> params, SpellAbility sa) {

        final Card card = sa.getAbilityFactory().getHostCard();

        final ArrayList<Player> players = AbilityFactory.getDefinedPlayers(card, params.get("Defined"), sa);

        for (final Player p : players) {
            p.altWinBySpellEffect(card.getName());
        }
    }

    @Override
    protected String getStackDescription(java.util.Map<String, String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard().getName()).append(" - ");
        } else {
            sb.append(" ");
        }

        // Let the spell description also be the stack description
        sb.append(sa.getDescription());

        return sb.toString();
    }
}