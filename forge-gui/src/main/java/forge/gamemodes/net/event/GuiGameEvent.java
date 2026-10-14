package forge.gamemodes.net.event;

import forge.game.card.CardView;
import forge.gamemodes.net.ProtocolMethod;
import forge.trackable.TrackableTypes;
import forge.trackable.Tracker;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

public final class GuiGameEvent implements IdentifiableNetEvent {
    private static final long serialVersionUID = 6223690008522514574L;
    private static int staticId = 0;

    private final int id;
    private final ProtocolMethod method;
    private final Object[] objects;
    private final Integer gameViewId;
    private boolean replaceTrackables = true;
    private transient IdentityHashMap<CardView, Integer> decodedCardViewIdRefs;
    private transient boolean conflictingDecodedCardViewIdRefs;

    public GuiGameEvent(final ProtocolMethod method, final Object ... objects) {
        this(method, true, null, objects);
    }
    public GuiGameEvent(final ProtocolMethod method, boolean replace, final Object ... objects) {
        this(method, replace, null, objects);
    }

    private GuiGameEvent(final ProtocolMethod method, boolean replace, Integer gameViewId, Object[] objects) {
        this.id = staticId++;
        this.method = method;
        this.objects = objects == null ? new Object[0] : objects;
        this.gameViewId = gameViewId;
        replaceTrackables = replace;
    }

    public static GuiGameEvent forGameView(final ProtocolMethod method, int gameViewId,
                                           final Object ... objects) {
        return forGameView(method, gameViewId, true, objects);
    }

    public static GuiGameEvent forGameView(final ProtocolMethod method, int gameViewId, boolean replace,
                                           final Object ... objects) {
        return new GuiGameEvent(method, replace, gameViewId, objects);
    }

    @Override
    public String toString() {
        return String.format("GuiGameEvent %d: %s (%d args)", id, method, objects.length);
    }

    @Override
    public int getId() {
        return id;
    }

    public ProtocolMethod getMethod() {
        return method;
    }

    /** Nullable for legacy wire events that predate controller-to-GameView binding. */
    public Integer getGameViewId() {
        return gameViewId;
    }

    public Object[] getObjects() {
        return objects;
    }

    public boolean replaceTrackables() {
        return replaceTrackables;
    }

    public void setDecodedCardViewIdRefs(IdentityHashMap<CardView, Integer> refs, boolean conflictingRefs) {
        decodedCardViewIdRefs = new IdentityHashMap<>(refs);
        conflictingDecodedCardViewIdRefs = conflictingRefs;
    }

    /** Rebind only getChoices CardViews that arrived as wire IdRefs, after prior EDT deltas apply. */
    public boolean refreshGetChoicesCardViewRefs(final Tracker tracker) {
        if (method != ProtocolMethod.getChoices) {
            return true;
        }
        if (conflictingDecodedCardViewIdRefs) {
            return false;
        }
        if (decodedCardViewIdRefs == null || decodedCardViewIdRefs.isEmpty()) {
            return true;
        }
        if (tracker == null || objects.length < 5
                || !(objects[3] instanceof List<?> choices)) {
            return false;
        }
        final Object selectedArg = objects[4];
        if (selectedArg != null && !(selectedArg instanceof List<?>)) {
            return false;
        }
        final List<?> refreshedChoices = refreshList(choices, tracker);
        if (refreshedChoices == null) {
            return false;
        }
        final List<?> refreshedSelected = selectedArg == null ? null
                : refreshList((List<?>) selectedArg, tracker);
        if (selectedArg != null && refreshedSelected == null) {
            return false;
        }
        objects[3] = refreshedChoices;
        objects[4] = refreshedSelected;
        return true;
    }

    private List<?> refreshList(final List<?> values, final Tracker tracker) {
        List<Object> refreshed = null;
        for (int i = 0; i < values.size(); i++) {
            final Object value = values.get(i);
            if (!(value instanceof CardView card)) {
                continue;
            }
            final Integer cardId = decodedCardViewIdRefs.get(card);
            if (cardId == null) {
                continue;
            }
            final CardView current = tracker.getObj(TrackableTypes.CardViewType, cardId);
            if (current == null || current.getId() != cardId) {
                return null;
            }
            if (current != card) {
                if (refreshed == null) {
                    refreshed = new ArrayList<>(values);
                }
                refreshed.set(i, current);
            }
        }
        return refreshed == null ? values : refreshed;
    }

    public void clearDecodedCardViewIdRefs() {
        decodedCardViewIdRefs = null;
        conflictingDecodedCardViewIdRefs = false;
    }
}
