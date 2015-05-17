package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class DamagePreventEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<GameObject> tgts = getTargets(sa);

        sb.append("Prevent the next ");
        sb.append(sa.getParam("Amount"));
        sb.append(" damage that would be dealt ");
        if (sa.hasParam("DividedAsYouChoose")) {
            sb.append("between ");
        } else {
            sb.append("to ");
        }
        for (int i = 0; i < tgts.size(); i++) {
            if (i != 0) {
                sb.append(" ");
            }

            final Object o = tgts.get(i);
            if (o instanceof Card) {
                final Card tgtC = (Card) o;
                if (tgtC.isFaceDown()) {
                    sb.append("Morph");
                } else {
                    sb.append(tgtC);
                }
            } else if (o instanceof Player) {
                sb.append(o.toString());
            }
        }

        if (sa.hasParam("Radiance") && (sa.usesTargeting())) {
            sb.append(" and each other ").append(sa.getParam("ValidTgts"))
                    .append(" that shares a color with ");
            if (tgts.size() > 1) {
                sb.append("them");
            } else {
                sb.append("it");
            }
        }
        sb.append(" this turn.");
        return sb.toString();
    }

        /* (non-Javadoc)
         * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
         */
        @Override
    public void resolve(SpellAbility sa) {
        Card host = sa.getHostCard();
        int numDam = AbilityUtils.calculateAmount(host, sa.getParam("Amount"), sa);

        final List<GameObject> tgts = getTargets(sa);
        final List<Card> untargetedCards = new ArrayList<Card>();
        
        if (sa.hasParam("Radiance") && (sa.usesTargeting())) {
            Card origin = null;
            for (int i = 0; i < tgts.size(); i++) {
                if (tgts.get(i) instanceof Card) {
                    origin = (Card) tgts.get(i);
                    break;
                }
            }
            if (origin != null) {
                // Can't radiate from a player
                for (final Card c : CardUtil.getRadiance(host, origin, sa.getParam("ValidTgts").split(","))) {
                    untargetedCards.add(c);
                }
            }
        }

        final boolean targeted = (sa.usesTargeting());
        final boolean preventionWithEffect = sa.hasParam("PreventionSubAbility");

        for (final Object o : tgts) {
            numDam = (sa.usesTargeting() && sa.hasParam("DividedAsYouChoose")) ? sa.getTargetRestrictions().getDividedValue(o) : numDam;
            if (o instanceof Card) {
                final Card c = (Card) o;
                if (c.isInPlay() && (!targeted || c.canBeTargetedBy(sa))) {
                    if (preventionWithEffect) {
                        Map<String, String> effectMap = new TreeMap<String, String>();
                        effectMap.put("EffectString", sa.getSVar(sa.getParam("PreventionSubAbility")));
                        effectMap.put("ShieldAmount", String.valueOf(numDam));
                        if (sa.hasParam("ShieldEffectTarget")) {
                            String effTgtString = "";
                            List<GameObject> effTgts = new ArrayList<GameObject>();
                            effTgts = AbilityUtils.getDefinedObjects(host, sa.getParam("ShieldEffectTarget"), sa);
                            for (final Object effTgt : effTgts) {
                                if (effTgt instanceof Card) {
                                    effTgtString = String.valueOf(((Card) effTgt).getId());
                                    effectMap.put("ShieldEffectTarget", "CardUID_" + effTgtString);
                                } else if (effTgt instanceof Player) {
                                    effTgtString = ((Player) effTgt).getName();
                                    effectMap.put("ShieldEffectTarget", "PlayerNamed_" + effTgtString);
                                }
                            }
                        }
                        c.addPreventNextDamageWithEffect(host, effectMap);
                    } else {
                        c.addPreventNextDamage(numDam);
                    }
                }

            } else if (o instanceof Player) {
                final Player p = (Player) o;
                if (!targeted || p.canBeTargetedBy(sa)) {
                    if (preventionWithEffect) {
                        Map<String, String> effectMap = new TreeMap<String, String>();
                        effectMap.put("EffectString", sa.getSVar(sa.getParam("PreventionSubAbility")));
                        effectMap.put("ShieldAmount", String.valueOf(numDam));
                        if (sa.hasParam("ShieldEffectTarget")) {
                            String effTgtString = "";
                            List<GameObject> effTgts = new ArrayList<GameObject>();
                            effTgts = AbilityUtils.getDefinedObjects(host, sa.getParam("ShieldEffectTarget"), sa);
                            for (final Object effTgt : effTgts) {
                                if (effTgt instanceof Card) {
                                    effTgtString = String.valueOf(((Card) effTgt).getId());
                                    effectMap.put("ShieldEffectTarget", "CardUID_" + effTgtString);
                                } else if (effTgt instanceof Player) {
                                    effTgtString = ((Player) effTgt).getName();
                                    effectMap.put("ShieldEffectTarget", "PlayerNamed_" + effTgtString);
                                }
                            }
                        }
                        p.addPreventNextDamageWithEffect(host, effectMap);
                    } else {
                        p.addPreventNextDamage(numDam);
                    }
                }
            }
        }

        for (final Card c : untargetedCards) {
            if (c.isInPlay()) {
                c.addPreventNextDamage(numDam);
            }
        }
    } // preventDamageResolve
}
