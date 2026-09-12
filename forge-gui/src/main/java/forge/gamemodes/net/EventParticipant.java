package forge.gamemodes.net;

import java.io.Serializable;
import java.util.List;

import forge.util.Localizer;

/**
 * A player or AI in a network draft/sealed event.
 * <p>
 * {@code seatIndex} is the position in the draft pod's circular pack-passing order
 * (0 to podSize-1). Randomized before draft start. Used to index into BoosterDraft
 * player lists and determine pack-passing neighbors.
 * <p>
 * {@code lobbySlotIndex} is the player's position in the network lobby UI. Used to
 * look up the RemoteClient for sending network messages. AI-fill seats that have no
 * lobby slot use -1.
 */
public final class EventParticipant implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type { HUMAN, AI }

    private final String name;
    private final Type type;
    private final int seatIndex;
    private final int lobbySlotIndex;

    public EventParticipant(String name, Type type, int seatIndex, int lobbySlotIndex) {
        this.name = name;
        this.type = type;
        this.seatIndex = seatIndex;
        this.lobbySlotIndex = lobbySlotIndex;
    }

    public String getName() { return name; }
    public Type getType() { return type; }
    public int getSeatIndex() { return seatIndex; }
    public int getLobbySlotIndex() { return lobbySlotIndex; }
    public boolean isHuman() { return type == Type.HUMAN; }
    public boolean isAI() { return type == Type.AI; }

    public static EventParticipant findBySeat(List<EventParticipant> list, int seatIndex) {
        if (list == null) return null;
        for (EventParticipant p : list) {
            if (p.getSeatIndex() == seatIndex) return p;
        }
        return null;
    }

    /**
     * Display name for a seat, looking up first in the given view's participants then falling
     * back to the current event's list. Appends " (AI)" for AI seats. If no participant found,
     * returns a localized "Seat N" placeholder.
     */
    public static String resolveName(int seatIndex, List<EventParticipant> viewParticipants,
            List<EventParticipant> currentEventParticipants) {
        Localizer localizer = Localizer.getInstance();
        EventParticipant p = findBySeat(viewParticipants, seatIndex);
        if (p == null) p = findBySeat(currentEventParticipants, seatIndex);
        if (p == null) return localizer.getMessage("lblSeatN", String.valueOf(seatIndex));
        return p.isAI() ? p.getName() + " (" + localizer.getMessage("lblAI") + ")" : p.getName();
    }
}
