package forge.screens.match;

import java.util.ArrayList;
import java.util.List;

import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.match.input.Input;
import forge.match.input.InputPassPriority;
import forge.toolbox.FCardZoom;
import forge.toolbox.FCardZoom.ZoomController;

public class InputSelectCard {
    private InputSelectCard() {
    }

    public static void selectCard(Card card, List<Card> orderedCards) {
        Input currentInput = FControl.getInputQueue().getInput();
        if (currentInput == null) { return; }

        List<Card> orderedCardOptions = new ArrayList<Card>(orderedCards); //copy list to allow it being modified

        if (currentInput instanceof InputPassPriority) {
            FCardZoom.show("Select a spell/ability", card, orderedCardOptions, new ZoomController<SpellAbility>() {
                @Override
                public List<SpellAbility> getOptions(final Card card) {
                    return card.getAllPossibleAbilities(FControl.getCurrentPlayer(), true);
                }

                @Override
                public boolean selectOption(final Card card, final SpellAbility option) {
                    FControl.getInputProxy().selectAbility(option);
                    return true; //TODO: Avoid hiding card zoom when selecting mana abilities
                }
            });
        }
        else {
            FControl.getInputProxy().selectCard(card, null);
        }
    }
}
