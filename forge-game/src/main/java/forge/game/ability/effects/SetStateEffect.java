package forge.game.ability.effects;

import forge.card.CardStateName;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.spellability.SpellAbility;

import java.util.Iterator;
import java.util.List;

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
            sb.append(it.next());

            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(".");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {

        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final List<Card> tgtCards = getTargetCards(sa);

        final boolean remChanged = sa.hasParam("RememberChanged");

        for (final Card tgt : tgtCards) {
            if (sa.usesTargeting() && !tgt.canBeTargetedBy(sa)) {
                continue;
            }

            boolean hasTransformed = changeCardState(tgt, sa.getParam("Mode"), sa.getParam("NewState"));
            if ( hasTransformed ) {
                game.fireEvent(new GameEventCardStatsChanged(tgt));
            }
            if ( hasTransformed && remChanged) {
                host.addRemembered(tgt);
            }
        }
    }

    private boolean changeCardState(final Card tgt, final String mode, final String customState) {
        if (mode == null)
            return tgt.changeToState(CardStateName.smartValueOf(customState));

        // flip and face-down don't overlap. That is there is no chance to turn face down a flipped permanent
        // and then any effect have it turn upface again and demand its former flip state to be restored
        // Proof: Morph cards never have ability that makes them flip, Ixidron does not suppose cards to be turned face up again, 
        // Illusionary Mask affects cards in hand.
        CardStateName oldState = tgt.getCurrentStateName();
        if (mode.equals("Transform") && tgt.isDoubleFaced()) {
            if (tgt.hasKeyword("CARDNAME can't transform")) {
                return false;
            }
            CardStateName destState = oldState == CardStateName.Transformed ? CardStateName.Original : CardStateName.Transformed;
            return tgt.changeToState(destState);
            
        } else if (mode.equals("Flip") && tgt.isFlipCard()) {
            CardStateName destState = oldState == CardStateName.Flipped ? CardStateName.Original : CardStateName.Flipped;
            return tgt.changeToState(destState);
        } else if (mode.equals("TurnFace")) {
            if (oldState == CardStateName.Original) {
                // Reset cloned state if Vesuvan Shapeshifter
                if (tgt.isCloned() && tgt.getState(CardStateName.Cloner).getName().equals("Vesuvan Shapeshifter")) {
                    tgt.switchStates(CardStateName.Cloner, CardStateName.Original, false);
                    tgt.setState(CardStateName.Original, false);
                    tgt.clearStates(CardStateName.Cloner, false);
                }
                return tgt.turnFaceDown();
            } else if (oldState == CardStateName.FaceDown) {
                return tgt.turnFaceUp();
            }
        }
        return false;
    }

}
