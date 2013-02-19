package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class DamageEachEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final String damage = sa.getParam("NumDmg");
        final int iDmg = AbilityUtils.calculateAmount(sa.getSourceCard(), damage, sa);

        String desc = sa.getParam("ValidCards");
        if (sa.hasParam("ValidDescription")) {
            desc = sa.getParam("ValidDescription");
        }

        String dmg = "";
        if (sa.hasParam("DamageDesc")) {
            dmg = sa.getParam("DamageDesc");
        } else {
            dmg += iDmg + " damage";
        }

        if (sa.hasParam("StackDescription")) {
            sb.append(sa.getParam("StackDescription"));
        } else {
            sb.append("Each ").append(desc).append(" deals ").append(dmg).append(" to ");
            for (final Player p : getTargetPlayers(sa)) {
                sb.append(p);
            }
            if (sa.hasParam("DefinedCards")) {
                if (sa.getParam("DefinedCards").equals("Self")) {
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
    public void resolve(SpellAbility sa) {
        final Card card = sa.getSourceCard();

        List<Card> sources = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        if (sa.hasParam("ValidCards")) {
            sources = CardLists.getValidCards(sources, sa.getParam("ValidCards"), card.getController(), card);
        }

        ArrayList<Object> tgts = new ArrayList<Object>();
        if (sa.getTarget() == null) {
            tgts = AbilityUtils.getDefinedObjects(sa.getSourceCard(), sa.getParam("DefinedPlayers"), sa);
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

        if (sa.hasParam("DefinedCards")) {
            if (sa.getParam("DefinedCards").equals("Self")) {
                for (final Card source : sources) {
                    final int dmg = CardFactoryUtil.xCount(source, card.getSVar("X"));
                    // System.out.println(source+" deals "+dmg+" damage to "+source);
                    source.addDamage(dmg, source);
                }
            }
            if (sa.getParam("DefinedCards").equals("Remembered")) {
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
