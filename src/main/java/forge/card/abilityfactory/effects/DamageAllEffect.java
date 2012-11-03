package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class DamageAllEffect extends SpellEffect {
    /**
     * <p>
     * damageAllStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    
    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        String desc = "";
        if (params.containsKey("ValidDescription")) {
            desc = params.get("ValidDescription");
        }
        
        final String damage = params.get("NumDmg");
        final int dmg = AbilityFactory.calculateAmount(sa.getSourceCard(), damage, sa); 
    

    
        final ArrayList<Card> definedSources = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("DamageSource"), sa);
        final Card source = definedSources.get(0);

        if (source != sa.getSourceCard()) {
            sb.append(source.toString()).append(" deals");
        } else {
            sb.append("Deals");
        }

        sb.append(" ").append(dmg).append(" damage to ").append(desc);

            return sb.toString();
    }

    /**
     * <p>
     * damageAllResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final ArrayList<Card> definedSources = AbilityFactory.getDefinedCards(sa.getSourceCard(),
                params.get("DamageSource"), sa);
        final Card card = definedSources.get(0);
        final Card source = sa.getSourceCard();

        final String damage = params.get("NumDmg");
        final int dmg = AbilityFactory.calculateAmount(sa.getSourceCard(), damage, sa); 

        final Target tgt = sa.getTarget();
        Player targetPlayer = null;
        if (tgt != null) {
            targetPlayer = tgt.getTargetPlayers().get(0);
        }

        String players = "";
        List<Card> list = new ArrayList<Card>();

        if (params.containsKey("ValidPlayers")) {
            players = params.get("ValidPlayers");
        }

        if (params.containsKey("ValidCards")) {
            list = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        }

        if (targetPlayer != null) {
            list = CardLists.filterControlledBy(list, targetPlayer);
        }

        list = AbilityFactory.filterListByType(list, params.get("ValidCards"), sa);

        for (final Card c : list) {
            if (c.addDamage(dmg, card) && params.containsKey("RememberDamaged")) {
                source.addRemembered(c);
            }
        }

        if (!players.equals("")) {
            final ArrayList<Player> playerList = AbilityFactory.getDefinedPlayers(card, players, sa);
            for (final Player p : playerList) {
                if (p.addDamage(dmg, card) && params.containsKey("RememberDamaged")) {
                    source.addRemembered(p);
                }
            }
        }
    }
}