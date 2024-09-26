package forge.game.staticability;

import forge.game.card.CardView;
import forge.game.card.IHasCardView;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty;
import forge.trackable.Tracker;

public class StaticAbilityView extends TrackableObject implements IHasCardView {
    private static final long serialVersionUID = 1L;

    StaticAbilityView(StaticAbility st) {
        this(st, st.getHostCard() == null || st.getHostCard().getGame() == null ? null : st.getHostCard().getGame().getTracker());
    }

    StaticAbilityView(StaticAbility st, Tracker tracker) {
        super(st.getId(), tracker);
        updateHostCard(st);
        updateDescription(st);
    }

    @Override
    public CardView getCardView() {
        return this.getHostCard();
    }

    public CardView getHostCard() {
        return get(TrackableProperty.ST_HostCard);
    }

    void updateHostCard(StaticAbility st) {
        set(TrackableProperty.ST_HostCard, CardView.get(st.getHostCard()));
    }

    @Override
    public String toString() {
        return this.getDescription();
    }

    public String getDescription() {
        return get(TrackableProperty.ST_Description);
    }

    void updateDescription(StaticAbility st) {
        set(TrackableProperty.ST_Description, st.toString());
    }
}
