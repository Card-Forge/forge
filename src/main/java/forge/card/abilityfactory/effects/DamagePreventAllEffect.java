package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import forge.Card;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class DamagePreventAllEffect extends SpellEffect {
    
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(Map<String, String> params, SpellAbility sa) {
        final Card source = sa.getSourceCard();
        final int numDam = AbilityFactory.calculateAmount(sa.getAbilityFactory().getHostCard(), params.get("Amount"), sa);

        String players = "";
        List<Card> list = new ArrayList<Card>();

        if (params.containsKey("ValidPlayers")) {
            players = params.get("ValidPlayers");
        }

        if (params.containsKey("ValidCards")) {
            list = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        }

        list = AbilityFactory.filterListByType(list, params.get("ValidCards"), sa);

        for (final Card c : list) {
            c.addPreventNextDamage(numDam);
        }

        if (!players.equals("")) {
            final ArrayList<Player> playerList = new ArrayList<Player>(Singletons.getModel().getGame().getPlayers());
            for (final Player p : playerList) {
                if (p.isValid(players, source.getController(), source)) {
                    p.addPreventNextDamage(numDam);
                }
            }
        }
    } // preventDamageAllResolve

    @Override
    protected String getStackDescription(Map<String, String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        String desc = sa.getDescription();
    
        if (desc.contains(":")) {
            desc = desc.split(":")[1];
        }
        sb.append(desc);
    
        return sb.toString();
    }

} 