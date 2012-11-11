package forge.control.input;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import forge.game.player.Player;

public class InputSelectManyPlayers extends InputSelectMany<Player> {
    private static final long serialVersionUID = -8209690791522735L;
    
    protected final Function<List<Player>, Input> onComplete; 
    private final Predicate<Player> allowedFilter;

    public InputSelectManyPlayers(final Predicate<Player> allowedRule, int min, int max, final Function<List<Player>, Input> onDone)
    {
        super(min, max);
        allowedFilter = allowedRule;
        onComplete = onDone;
    }


    @Override
    public void selectPlayer(final Player p) {
        selectEntity(p);
    }
    
    protected boolean isValidChoice(Player choice) {
        if ( allowedFilter != null && !allowedFilter.apply(choice)) return false;
        return true;
    }

    @Override
    protected Input onDone() {
        return onComplete.apply(selected);
    }
}