package forge.match.input;

import java.util.Collection;
import java.util.List;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.player.PlayerControllerHuman;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import forge.util.ITriggerEvent;

public class InputSelectEntitiesFromList<T extends GameEntity> extends InputSelectManyBase<T> {
    private static final long serialVersionUID = -6609493252672573139L;

    private final FCollectionView<T> validChoices;
    protected final FCollection<T> selected = new FCollection<T>();

    public InputSelectEntitiesFromList(final PlayerControllerHuman controller, final int min, final int max, final FCollectionView<T> validChoices0) {
        super(controller, Math.min(min, validChoices0.size()), Math.min(max, validChoices0.size()));
        validChoices = validChoices0;

        if (min > validChoices.size()) {
            System.out.println(String.format("Trying to choose at least %d cards from a list with only %d cards!", min, validChoices.size()));
        }
    }

    @Override
    protected boolean onCardSelected(final Card c, final List<Card> otherCardsToSelect, final ITriggerEvent triggerEvent) {
        if (!selectEntity(c)) {
            return false;
        }
        refresh();
        return true;
    }

    @Override
    public String getActivateAction(final Card card) {
        if (validChoices.contains(card)) {
            if (selected.contains(card)) {
                return "unselect card";
            }
            return "select card";
        }
        return null;
    }

    @Override
    protected void onPlayerSelected(final Player p, final ITriggerEvent triggerEvent) {
        if (!selectEntity(p)) {
            return;
        }
        refresh();
    }

    @Override
    public final Collection<T> getSelected() {
        return selected;
    }

    @SuppressWarnings("unchecked")
    protected boolean selectEntity(final GameEntity c) {
        if (!validChoices.contains(c)) {
            return false;
        }

        final boolean entityWasSelected = selected.contains(c);
        if (entityWasSelected) {
            selected.remove(c);
        }
        else {
            selected.add((T)c);
        }
        onSelectStateChanged(c, !entityWasSelected);

        return true;
    }

    // might re-define later
    @Override
    protected boolean hasEnoughTargets() { return selected.size() >= min; }
    @Override
    protected boolean hasAllTargets() { return selected.size() >= max; }

    @Override
    protected String getMessage() {
        return max == Integer.MAX_VALUE
                ? String.format(message, selected.size())
                : String.format(message, max - selected.size());
    }
}