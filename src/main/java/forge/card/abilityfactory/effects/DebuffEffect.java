package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import forge.Card;
import forge.Command;
import forge.Singletons;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;

public class DebuffEffect extends SpellEffect {
    
    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final List<String> kws = params.containsKey("Keywords") ? Arrays.asList(params.get("Keywords").split(" & ")) : new ArrayList<String>();
        final StringBuilder sb = new StringBuilder();
    
        final List<Card> tgtCards = getTargetCards(sa, params);

    
        if (tgtCards.size() > 0) {

    
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

        return sb.toString();
    }

    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final List<String> kws = params.containsKey("Keywords") ? Arrays.asList(params.get("Keywords").split(" & ")) : new ArrayList<String>();

        for (final Card tgtC : getTargetCards(sa, params)) {
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

}