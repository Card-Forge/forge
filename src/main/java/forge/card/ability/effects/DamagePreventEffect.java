package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.CardUtil;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;

public class DamagePreventEffect extends SpellEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Object> tgts = getTargetObjects(sa);

        sb.append("Prevent the next ");
        sb.append(sa.getParam("Amount"));
        sb.append(" damage that would be dealt to ");
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

        if (sa.hasParam("Radiance") && (sa.getTarget() != null)) {
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
        Card host = sa.getSourceCard();
        int numDam = AbilityUtils.calculateAmount(host, sa.getParam("Amount"), sa);

        ArrayList<Object> tgts;
        final ArrayList<Card> untargetedCards = new ArrayList<Card>();
        if (sa.getTarget() == null) {
            tgts = AbilityUtils.getDefinedObjects(sa.getSourceCard(), sa.getParam("Defined"), sa);
        } else {
            tgts = sa.getTarget().getTargets();
        }

        if (sa.hasParam("Radiance") && (sa.getTarget() != null)) {
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

        final boolean targeted = (sa.getTarget() != null);

        for (final Object o : tgts) {
            numDam = (sa.getTarget() != null && sa.hasParam("DividedAsYouChoose")) ? sa.getTarget().getDividedValue(o) : numDam;
            if (o instanceof Card) {
                final Card c = (Card) o;
                if (c.isInPlay() && (!targeted || c.canBeTargetedBy(sa))) {
                    c.addPreventNextDamage(numDam);
                }

            } else if (o instanceof Player) {
                final Player p = (Player) o;
                if (!targeted || p.canBeTargetedBy(sa)) {
                    p.addPreventNextDamage(numDam);
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
