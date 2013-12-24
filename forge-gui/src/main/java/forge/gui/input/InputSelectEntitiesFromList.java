package forge.gui.input;

import java.awt.event.MouseEvent;
import java.util.Collection;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.player.Player;

public class InputSelectEntitiesFromList<T extends GameEntity> extends InputSelectManyBase<T> {
    private static final long serialVersionUID = -6609493252672573139L;

    private final Collection<T> validChoices;
    public InputSelectEntitiesFromList(int min, int max, Collection<T> validChoices) {
        super(Math.min(min, validChoices.size()), Math.min(max, validChoices.size()));
        this.validChoices = validChoices;

        if ( min > validChoices.size() )
            System.out.println(String.format("Trying to choose at least %d cards from a list with only %d cards!", min, validChoices.size()));

    }
 
    @Override
    protected void onCardSelected(final Card c, final MouseEvent triggerEvent) {
        if (!selectEntity(c)) {
            return;
        }
        refresh();
    }

    @Override
    protected void onPlayerSelected(final Player p, final MouseEvent triggerEvent) {
        if (!selectEntity(p)) {
            return;
        }
        refresh();
    }
    
    
    
    @Override
    protected final boolean isValidChoice(GameEntity choice) {
        return validChoices.contains(choice);
    }
        
}