package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import forge.Card;
import forge.CardUtil;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;

public class DamageDealEffect extends SpellEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(Map<String, String> params, SpellAbility sa) {
        // when damageStackDescription is called, just build exactly what is happening
        final StringBuilder sb = new StringBuilder();
        final String damage = params.get("NumDmg");
        final int dmg = AbilityFactory.calculateAmount(sa.getSourceCard(), damage, sa); 

        ArrayList<Object> tgts;
        if (sa.getTarget() == null) {
            tgts = AbilityFactory.getDefinedObjects(sa.getSourceCard(), params.get("Defined"), sa);
        } else {
            tgts = sa.getTarget().getTargets();
        }
    
        if (tgts.size() > 0) {
            if (!(sa instanceof AbilitySub)) {
                sb.append(sa.getSourceCard().getName()).append(" -");
            }
            sb.append(" ");
    
            if (params.containsKey("StackDescription")) {
                sb.append(params.get("StackDescription"));
            }
            else {
                final String conditionDesc = params.get("ConditionDescription");
                if (conditionDesc != null) {
                    sb.append(conditionDesc).append(" ");
                }
    
                final ArrayList<Card> definedSources = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("DamageSource"), sa);
                final Card source = definedSources.get(0);
    
                if (source != sa.getSourceCard()) {
                    sb.append(source.toString()).append(" deals");
                } else {
                    sb.append("Deals");
                }
    
                sb.append(" ").append(dmg).append(" damage ");
                
                if (params.containsKey("DivideEvenly")) {
                    sb.append("divided evenly (rounded down) ");
                }
                
                sb.append("to");
    
                for (int i = 0; i < tgts.size(); i++) {
                    sb.append(" ");
    
                    final Object o = tgts.get(i);
                    if ((o instanceof Card) || (o instanceof Player)) {
                        sb.append(o.toString());
                    }
                }
    
                if (params.containsKey("Radiance")) {
                    sb.append(" and each other ").append(params.get("ValidTgts"))
                            .append(" that shares a color with ");
                    if (tgts.size() > 1) {
                        sb.append("them");
                    } else {
                        sb.append("it");
                    }
                }
    
                sb.append(". ");
            }
        }

        return sb.toString();
    }

    /**
     * <p>
     * dealDamageResolve.
     * </p>
     * 
     * @param saMe
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final String damage = params.get("NumDmg");
        int dmg = AbilityFactory.calculateAmount(sa.getSourceCard(), damage, sa); 

        final boolean noPrevention = params.containsKey("NoPrevention");
        final boolean combatDmg = params.containsKey("CombatDamage");

        ArrayList<Object> tgts;
        if (sa.getTarget() == null) {
            tgts = AbilityFactory.getDefinedObjects(sa.getSourceCard(), params.get("Defined"), sa);
        } else {
            tgts = sa.getTarget().getTargets();
        }

        // Right now for Fireball, maybe later for other stuff
        if (params.containsKey("DivideEvenly")) {
            String evenly = params.get("DivideEvenly");
            if (evenly.equals("RoundedDown")) {
                dmg = tgts.isEmpty() ? 0 : dmg / tgts.size();
            }
        }

        final boolean targeted = (sa.getTarget() != null);

        if (params.containsKey("Radiance") && targeted) {
            Card origin = null;
            for (int i = 0; i < tgts.size(); i++) {
                if (tgts.get(i) instanceof Card) {
                    origin = (Card) tgts.get(i);
                    break;
                }
            }
            // Can't radiate from a player
            if (origin != null) {
                for (final Card c : CardUtil.getRadiance(sa.getSourceCard(), origin,
                        params.get("ValidTgts").split(","))) {
                    tgts.add(c);
                }
            }
        }

        final ArrayList<Card> definedSources = AbilityFactory.getDefinedCards(sa.getSourceCard(),
                params.get("DamageSource"), sa);
        if (definedSources == null) {
            return;
        }
        final Card source = definedSources.get(0);

        for (final Object o : tgts) {
            if (o instanceof Card) {
                final Card c = (Card) o;
                if (c.isInPlay() && (!targeted || c.canBeTargetedBy(sa))) {
                    if (noPrevention) {
                        c.addDamageWithoutPrevention(dmg, source);
                    } else if (combatDmg) {
                        HashMap<Card, Integer> combatmap = new HashMap<Card, Integer>();
                        combatmap.put(source, dmg);
                        c.addCombatDamage(combatmap);
                    } else {
                        c.addDamage(dmg, source);
                    }
                }

            } else if (o instanceof Player) {
                final Player p = (Player) o;
                if (!targeted || p.canBeTargetedBy(sa)) {
                    if (noPrevention) {
                        p.addDamageWithoutPrevention(dmg, source);
                    } else if (combatDmg) {
                        p.addCombatDamage(dmg, source);
                    } else {
                        p.addDamage(dmg, source);
                    }
                }
            }
        }
    }

}