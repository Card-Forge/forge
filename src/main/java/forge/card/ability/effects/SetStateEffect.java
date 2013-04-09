package forge.card.ability.effects;

import java.util.Iterator;
import java.util.List;

import forge.Card;
import forge.CardCharacteristicName;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;

public class SetStateEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Card> tgtCards = getTargetCards(sa);

        if (sa.hasParam("Flip")) {
            sb.append("Flip");
        } else {
            sb.append("Transform ");
        }

        final Iterator<Card> it = tgtCards.iterator();
        while (it.hasNext()) {
            final Card tgtC = it.next();
            if (tgtC.isFaceDown()) {
                sb.append("Morph ").append("(").append(tgtC.getUniqueNumber()).append(")");
            } else {
                sb.append(tgtC);
            }

            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(".");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {

        final Card host = sa.getSourceCard();
        final List<Card> tgtCards = getTargetCards(sa);

        final boolean remChanged = sa.hasParam("RememberChanged");

        for (final Card tgt : tgtCards) {
            if (sa.getTarget() != null && !tgt.canBeTargetedBy(sa)) {
                continue;
            }

            boolean hasTransformed = changeCardState(tgt, sa.getParam("Mode"), sa.getParam("NewState"));
            if ( hasTransformed && remChanged) {
                host.addRemembered(tgt);
            }
        }
    }

    private boolean changeCardState(final Card tgt, final String mode, final String customState) {
        if (mode == null)
            return tgt.changeToState(CardCharacteristicName.smartValueOf(customState));

        // flip and face-down don't overlap. That is there is no chance to turn face down a flipped permanent
        // and then any effect have it turn upface again and demand its former flip state to be restored
        // Proof: Morph cards never have ability that makes them flip, Ixidron does not suppose cards to be turned face up again, 
        // Illusionary Mask affects cards in hand.
        CardCharacteristicName oldState = tgt.getCurState();
        if (mode.equals("Transform") && tgt.isDoubleFaced()) {
            if (tgt.hasKeyword("CARDNAME can't transform")) {
                return false;
            }
            CardCharacteristicName destState = oldState == CardCharacteristicName.Transformed ? CardCharacteristicName.Original : CardCharacteristicName.Transformed;
            return tgt.changeToState(destState);
            
        } else if (mode.equals("Flip") && tgt.isFlipCard()) {
            CardCharacteristicName destState = oldState == CardCharacteristicName.Flipped ? CardCharacteristicName.Original : CardCharacteristicName.Flipped;
            return tgt.changeToState(destState);
        } else if (mode.equals("TurnFace")) {
            if (oldState == CardCharacteristicName.Original) {
                // Reset cloned state if Vesuvan Shapeshifter
                if (tgt.isCloned() && tgt.getState(CardCharacteristicName.Cloner).getName().equals("Vesuvan Shapeshifter")) {
                    tgt.switchStates(CardCharacteristicName.Cloner, CardCharacteristicName.Original);
                    tgt.setState(CardCharacteristicName.Original);
                    tgt.clearStates(CardCharacteristicName.Cloner);
                }
                return tgt.turnFaceDown();
            } else if (oldState == CardCharacteristicName.FaceDown) {
                return tgt.turnFaceUp();
            }
        }
        return false;
    }

}
