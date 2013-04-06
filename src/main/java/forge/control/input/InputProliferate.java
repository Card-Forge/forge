package forge.control.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.Card;
import forge.CounterType;
import forge.GameEntity;
import forge.game.player.Player;
import forge.gui.GuiChoose;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public final class InputProliferate extends InputSelectManyBase<GameEntity> {
    private static final long serialVersionUID = -1779224307654698954L;
    private Map<GameEntity, CounterType> chosenCounters = new HashMap<GameEntity, CounterType>();

    public InputProliferate() {
        super(1, Integer.MAX_VALUE);
    }

    @Override
    protected String getMessage() {
        StringBuilder sb = new StringBuilder("Choose permanents and/or players with counters on them to add one more counter of that type.");
        sb.append("\n\nYou've selected so far:\n");
        if( selected.isEmpty()) 
            sb.append("(none)");
        else 
            for(GameEntity ge : selected ) {
                if( ge instanceof Player )
                    sb.append("* A poison counter to player ").append(ge).append("\n");
                else
                    sb.append("* ").append(ge).append(" -> ").append(chosenCounters.get(ge)).append("counter\n");
            }
        
        return sb.toString();
    }

    @Override
    protected void onCardSelected(Card card) {
        if( !selectEntity(card) )
            return;
        
        if( selected.contains(card) ) {
            final List<CounterType> choices = new ArrayList<CounterType>();
            for (final CounterType ct : CounterType.values()) {
                if (card.getCounters(ct) > 0) {
                    choices.add(ct);
                }
            }
            
            CounterType toAdd = choices.size() == 1 ? choices.get(0) : GuiChoose.one("Select counter type", choices);
            chosenCounters.put(card, toAdd);
        }
        
        refresh();
    }

    @Override
    public void selectPlayer(final Player player) {
        if( !selectEntity(player) )
            return;
        refresh();
    }

    @Override
    protected boolean isValidChoice(GameEntity choice) {
        if (choice instanceof Player)
            return ((Player) choice).getPoisonCounters() > 0 && !choice.hasKeyword("You can't get poison counters");
        
        if (choice instanceof Card)
            return ((Card) choice).hasCounters();
        
        return false;
    }
    
    public CounterType getCounterFor(GameEntity ge) {
        return chosenCounters.get(ge);
    }
}