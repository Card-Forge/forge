package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import forge.Card;
import forge.Command;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;

public class DebuffEffect extends SpellEffect {
    
    @Override
    public String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final Card host = sa.getAbilityFactory().getHostCard();
        final List<String> kws = params.containsKey("Keywords") ? Arrays.asList(params.get("Keywords").split(" & ")) : new ArrayList<String>();
        final StringBuilder sb = new StringBuilder();
    
        ArrayList<Card> tgtCards;
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
        }
    
        if (tgtCards.size() > 0) {
            if (sa instanceof AbilitySub) {
                sb.append(" ");
            } else {
                sb.append(host).append(" - ");
            }
    
            final Iterator<Card> it = tgtCards.iterator();
            while (it.hasNext()) {
                final Card tgtC = it.next();
                if (tgtC.isFaceDown()) {
                    sb.append("Morph");
                } else {
                    sb.append(tgtC);
                }
    
                if (it.hasNext()) {
                    sb.append(" ");
                }
            }
            sb.append(" loses ");
            /*
             * Iterator<String> kwit = kws.iterator(); while(it.hasNext()) {
             * String kw = kwit.next(); sb.append(kw); if(it.hasNext())
             * sb.append(" "); }
             */
            sb.append(kws);
            if (!params.containsKey("Permanent")) {
                sb.append(" until end of turn");
            }
            sb.append(".");
        }
    
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }
    
        return sb.toString();
    }

    /**
     * <p>
     * debuffResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final Card host = sa.getAbilityFactory().getHostCard();

        final List<String> kws = params.containsKey("Keywords") ? Arrays.asList(params.get("Keywords").split(" & ")) : new ArrayList<String>();

        ArrayList<Card> tgtCards;
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(host, params.get("Defined"), sa);
        }

        for (final Card tgtC : tgtCards) {
            final ArrayList<String> hadIntrinsic = new ArrayList<String>();
            if (tgtC.isInPlay() && tgtC.canBeTargetedBy(sa)) {
                for (final String kw : kws) {
                    if (tgtC.getIntrinsicKeyword().contains(kw)) {
                        hadIntrinsic.add(kw);
                    }
                    tgtC.removeIntrinsicKeyword(kw);
                    tgtC.removeAllExtrinsicKeyword(kw);
                }
            }
            if (!params.containsKey("Permanent")) {
                Singletons.getModel().getGame().getEndOfTurn().addUntil(new Command() {
                    private static final long serialVersionUID = 5387486776282932314L;

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

    } // debuffResolve

    // *************************************************************************
    // ***************************** DebuffAll *********************************
    // *************************************************************************

}