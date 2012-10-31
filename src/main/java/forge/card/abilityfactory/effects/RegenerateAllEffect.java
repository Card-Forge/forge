package forge.card.abilityfactory.effects;

import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.Command;
import forge.Singletons;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class RegenerateAllEffect extends SpellEffect {
    /**
     * <p>
     * regenerateAllStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    @Override
    public String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final Card host = sa.getAbilityFactory().getHostCard();
    
        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(host).append(" - ");
        }
    
        String desc = "";
        if (params.containsKey("SpellDescription")) {
            desc = params.get("SpellDescription");
        } else {
            desc = "Regenerate all valid cards.";
        }
    
        sb.append(desc);
    
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }
    
        return sb.toString();
    }

    /**
     * <p>
     * regenerateAllResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final Card hostCard = sa.getAbilityFactory().getHostCard();
        String valid = "";

        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }

        List<Card> list = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        list = CardLists.getValidCards(list, valid.split(","), hostCard.getController(), hostCard);

        for (final Card c : list) {
            final Command untilEOT = new Command() {
                private static final long serialVersionUID = 259368227093961103L;

                @Override
                public void execute() {
                    c.resetShield();
                }
            };

            if (c.isInPlay()) {
                c.addShield();
                Singletons.getModel().getGame().getEndOfTurn().addUntil(untilEOT);
            }
        }
    } // regenerateAllResolve

} // end class AbilityFactory_Regenerate