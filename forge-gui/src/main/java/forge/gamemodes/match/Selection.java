package forge.gamemodes.match;

import forge.game.card.CardView;
import forge.game.spellability.StackItemView;
import forge.trackable.TrackableObject;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * What the waiting prompt marks: the cards (with how many to pick), players and stack items it accepts, what is already
 * chosen, and the cards worth acting on. A card listed twice in {@code actionable} is marked more strongly.
 */
public record Selection(List<CardView> cards, int min, int max, List<TrackableObject> targets,
                        List<TrackableObject> chosen, List<CardView> actionable) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final Selection NONE = new Selection(List.of(), 0, 0, List.of(), List.of(), List.of());

    public static Selection ofChosen(final List<? extends TrackableObject> chosen) {
        return new Selection(List.of(), 0, 0, List.of(), new ArrayList<>(chosen), List.of());
    }

    public Selection withChosen(final List<? extends TrackableObject> more) {
        if (more.isEmpty()) {
            return this;
        }
        final List<TrackableObject> all = new ArrayList<>(chosen);
        all.addAll(more);
        return new Selection(cards, min, max, targets, all, actionable);
    }

    public Selection withActionable(final List<CardView> actionable) {
        return new Selection(cards, min, max, targets, chosen, actionable);
    }

    public boolean offersStackItems() {
        return targets.stream().anyMatch(StackItemView.class::isInstance);
    }
}
