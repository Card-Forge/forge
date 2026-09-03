package forge.game.card;

import forge.card.CardStateName;
import forge.trackable.Tracker;

// this class should be used in games without GUI
public class DummyCardView extends CardView {

    public DummyCardView(final int id0, final Tracker tracker) {
        super(id0, tracker);
    }

    @Override
    public CardStateView createAlternateState(final CardStateName state0) {
        return new DummyCardStateView(getId(), state0, tracker);
    }

    public class DummyCardStateView extends CardStateView {

        public DummyCardStateView(final int id0, final CardStateName state0, final Tracker tracker) {
            super(id0, state0, tracker);
        }

        @Override
        void updateAbilityText(Card c, CardState state) {}
    }
}
