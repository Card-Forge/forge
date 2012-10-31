package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import forge.Card;
import forge.CardLists;
import forge.Command;
import forge.Singletons;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class DebuffAllEffect extends SpellEffect {
    
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(Map<String, String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        String desc = "";
        if (params.containsKey("SpellDescription")) {
            desc = params.get("SpellDescription");
        } else if (params.containsKey("DebuffAllDescription")) {
            desc = params.get("DebuffAllDescription");
        }

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }

        sb.append(desc);
        return sb.toString();
    } // debuffAllStackDescription()

    /**
     * <p>
     * debuffAllResolve.
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
        final List<String> kws = params.containsKey("Keywords") ? Arrays.asList(params.get("Keywords").split(" & ")) : new ArrayList<String>();
                
        String valid = "";
    
        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }
    
        List<Card> list = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        list = CardLists.getValidCards(list, valid.split(","), hostCard.getController(), hostCard);
    
        for (final Card tgtC : list) {
            final ArrayList<String> hadIntrinsic = new ArrayList<String>();
            if (tgtC.isInPlay() && tgtC.canBeTargetedBy(sa)) {
                for (final String kw : kws) {
                    if (tgtC.getIntrinsicKeyword().contains(kw)) {
                        hadIntrinsic.add(kw);
                    }
                    tgtC.removeIntrinsicKeyword(kw);
                    tgtC.removeExtrinsicKeyword(kw);
                }
            }
            if (!params.containsKey("Permanent")) {
                Singletons.getModel().getGame().getEndOfTurn().addUntil(new Command() {
                    private static final long serialVersionUID = 7486231071095628674L;
    
                    @Override
                    public void execute() {
                        if (tgtC.isInPlay()) {
                            for (final String kw : hadIntrinsic) {
                                tgtC.addIntrinsicKeyword(kw);
                            }
                        }
                    }
                });
            }
        }
    } // debuffAllResolve()

} // end class AbilityFactory_Debuff