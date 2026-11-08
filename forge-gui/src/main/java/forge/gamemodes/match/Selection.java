package forge.gamemodes.match;

import forge.game.GameEntityView;
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
 * {@code defenders}: while attackers are declared, what each possible defender allows (a look that shows the choice of
 * defender reads it; the others ignore it).
 */
public record Selection(List<CardView> cards, int min, int max, List<TrackableObject> targets,
                        List<TrackableObject> chosen, List<CardView> actionable, List<DefenderOption> defenders) implements Serializable {
    @Serial
    private static final long serialVersionUID = 2L;

    /** Whether a defender can be attacked now, and on what terms. */
    public enum DefenderState { OPEN, COSTS, CLOSED }

    /**
     * One possible defender while attackers are declared: whether any of your creatures can attack it, the cost to do
     * so if every creature that could attack it has to pay ({@code cost}, e.g. "{2}"), and how many of your creatures
     * are goaded by it (they must attack someone else if they can).
     */
    public record DefenderOption(GameEntityView defender, DefenderState state, String cost, int goaded) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }

    public Selection(final List<CardView> cards, final int min, final int max, final List<TrackableObject> targets,
            final List<TrackableObject> chosen, final List<CardView> actionable) {
        this(cards, min, max, targets, chosen, actionable, List.of());
    }

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
        return new Selection(cards, min, max, targets, all, actionable, defenders);
    }

    public Selection withActionable(final List<CardView> actionable) {
        return new Selection(cards, min, max, targets, chosen, actionable, defenders);
    }

    public Selection withDefenders(final List<DefenderOption> defenders) {
        return new Selection(cards, min, max, targets, chosen, actionable, defenders);
    }

    public boolean offersStackItems() {
        return targets.stream().anyMatch(StackItemView.class::isInstance);
    }
}
