package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class SacrificeAllEffect extends SpellEffect {
    /**
     * <p>
     * sacrificeAllStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     * @since 1.0.15
     */
    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        // when getStackDesc is called, just build exactly what is happening
    
        final StringBuilder sb = new StringBuilder();
        final Card host = sa.getAbilityFactory().getHostCard();

    
        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(host).append(" - ");
        }
    
        final String conditionDesc = params.get("ConditionDescription");
        if (conditionDesc != null) {
            sb.append(conditionDesc).append(" ");
        }
    
        /*
         * This is not currently targeted ArrayList<Player> tgtPlayers;
         * 
         * Target tgt = af.getAbTgt(); if (tgt != null) tgtPlayers =
         * tgt.getTargetPlayers(); else tgtPlayers =
         * AbilityFactory.getDefinedPlayers(sa.getSourceCard(),
         * params.get("Defined"), sa);
         */
    
        sb.append("Sacrifice permanents.");
        return sb.toString();
    }

    /**
     * <p>
     * sacrificeAllResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    
    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {

        final Card card = sa.getSourceCard();

        String valid = "";

        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }

        // Ugh. If calculateAmount needs to be called with DestroyAll it _needs_
        // to use the X variable
        // We really need a better solution to this
        if (valid.contains("X")) {
            valid = valid.replace("X", Integer.toString(AbilityFactory.calculateAmount(card, "X", sa)));
        }

        List<Card> list;
        if (params.containsKey("Defined")) {
            list = new ArrayList<Card>(AbilityFactory.getDefinedCards(sa.getAbilityFactory().getHostCard(), params.get("Defined"), sa));
        } else {
            list = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        }

        final boolean remSacrificed = params.containsKey("RememberSacrificed");
        if (remSacrificed) {
            card.clearRemembered();
        }

        list = AbilityFactory.filterListByType(list, valid, sa);

        for (int i = 0; i < list.size(); i++) {
            if (Singletons.getModel().getGame().getAction().sacrifice(list.get(i), sa) && remSacrificed) {
                card.addRemembered(list.get(i));
            }
        }
    }

} // end class AbilityFactory_Sacrifice