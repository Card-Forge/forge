package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class DamageEachEffect extends SpellEffect {
    
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(Map<String, String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final String damage = params.get("NumDmg");
        final int iDmg = AbilityFactory.calculateAmount(sa.getSourceCard(), damage, sa); 
        
        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }
    
        ArrayList<Player> tgtPlayers;
    
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("DefinedPlayers"), sa);
        }
    
        String desc = params.get("ValidCards");
        if (params.containsKey("ValidDescription")) {
            desc = params.get("ValidDescription");
        }
    
        String dmg = "";
        if (params.containsKey("DamageDesc")) {
            dmg = params.get("DamageDesc");
        } else {
            dmg += iDmg + " damage";
        }
    
        if (params.containsKey("StackDescription")) {
            sb.append(params.get("StackDescription"));
        } else {
            sb.append("Each ").append(desc).append(" deals ").append(dmg).append(" to ");
            for (final Player p : tgtPlayers) {
                sb.append(p);
            }
            if (params.containsKey("DefinedCards")) {
                if (params.get("DefinedCards").equals("Self")) {
                    sb.append(" itself");
                }
            }
        }
        sb.append(".");
        return sb.toString();
    }

    
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(Map<String, String> params, SpellAbility sa) {
        final Card card = sa.getSourceCard();

        List<Card> sources = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        if (params.containsKey("ValidCards")) {
            sources = CardLists.getValidCards(sources, params.get("ValidCards"), card.getController(), card);
        }

        ArrayList<Object> tgts = new ArrayList<Object>();
        if (sa.getTarget() == null) {
            tgts = AbilityFactory.getDefinedObjects(sa.getSourceCard(), params.get("DefinedPlayers"), sa);
        } else {
            tgts = sa.getTarget().getTargets();
        }

        final boolean targeted = (sa.getTarget() != null);

        for (final Object o : tgts) {
            for (final Card source : sources) {
                final int dmg = CardFactoryUtil.xCount(source, sa.getSVar("X"));
                // System.out.println(source+" deals "+dmg+" damage to "+o.toString());
                if (o instanceof Card) {
                    final Card c = (Card) o;
                    if (c.isInPlay() && (!targeted || c.canBeTargetedBy(sa))) {
                        c.addDamage(dmg, source);
                    }

                } else if (o instanceof Player) {
                    final Player p = (Player) o;
                    if (!targeted || p.canBeTargetedBy(sa)) {
                        p.addDamage(dmg, source);
                    }
                }
            }
        }

        if (params.containsKey("DefinedCards")) {
            if (params.get("DefinedCards").equals("Self")) {
                for (final Card source : sources) {
                    final int dmg = CardFactoryUtil.xCount(source, card.getSVar("X"));
                    // System.out.println(source+" deals "+dmg+" damage to "+source);
                    source.addDamage(dmg, source);
                }
            }
            if (params.get("DefinedCards").equals("Remembered")) {
                for (final Card source : sources) {
                    final int dmg = CardFactoryUtil.xCount(source, card.getSVar("X"));
                    Card rememberedcard;
                    for (final Object o : sa.getSourceCard().getRemembered()) {
                        if (o instanceof Card) {
                            rememberedcard = (Card) o;
                            // System.out.println(source + " deals " + dmg + " damage to " + rememberedcard);
                            rememberedcard.addDamage(dmg, source);
                        }
                    }
                }
            }
        }
    }
}