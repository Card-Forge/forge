package forge.card.ability.effects;

import java.util.Iterator;
import java.util.List;

import forge.Card;
import forge.Command;
import forge.Singletons;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;

public class RegenerateEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final List<Card> tgtCards = getTargetCards(sa);

        if (tgtCards.size() > 0) {
            sb.append("Regenerate ");

            final Iterator<Card> it = tgtCards.iterator();
            while (it.hasNext()) {
                final Card tgtC = it.next();
                if (tgtC.isFaceDown()) {
                    sb.append("Morph");
                } else {
                    sb.append(tgtC);
                }

                if (it.hasNext()) {
                    sb.append(", ");
                }
            }
        }
        sb.append(".");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Target tgt = sa.getTarget();

        for (final Card tgtC : getTargetCards(sa)) {
            final Command untilEOT = new Command() {
                private static final long serialVersionUID = 1922050611313909200L;

                @Override
                public void execute() {
                    tgtC.resetShield();
                }
            };

            if (tgtC.isInPlay() && ((tgt == null) || tgtC.canBeTargetedBy(sa))) {
                tgtC.addShield();
                Singletons.getModel().getGame().getEndOfTurn().addUntil(untilEOT);
            }
        }
    } // regenerateResolve

}
