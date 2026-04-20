package forge.gamemodes.net;

import forge.deck.Deck;
import forge.gamemodes.limited.BoosterDraft;
import forge.gamemodes.limited.LimitedPoolType;
import forge.gamemodes.limited.SealedCardPoolGenerator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Server-side mutable model for a network limited event (draft or sealed).
 * Lives in memory only during the event session — discarded after pool distribution.
 * Each participant's pool is persisted as a Deck with event metadata in tags.
 * <p>
 * Not serializable — the wire-safe representation is {@link NetworkEventView}.
 */
public final class NetworkEvent {
    private final String eventId;
    private final EventFormat format;
    private EventPhase phase;
    private final List<EventParticipant> participants;
    private final LocalDateTime createdAt;
    private int pickTimerSeconds;
    private int disconnectGraceSeconds;
    private String productDescription;
    private LimitedPoolType poolType;
    private int numRounds = 3;
    private SealedCardPoolGenerator sealedGenerator;
    private BoosterDraft draft;

    public NetworkEvent(EventFormat format) {
        this.eventId = UUID.randomUUID().toString().substring(0, 8);
        this.format = format;
        this.phase = EventPhase.LOBBY_GATHER;
        this.participants = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.pickTimerSeconds = 60;
        this.disconnectGraceSeconds = 120;
        this.productDescription = "";
        this.poolType = LimitedPoolType.Full;
    }

    public String getEventId() { return eventId; }
    public EventFormat getFormat() { return format; }
    public EventPhase getPhase() { return phase; }
    public void setPhase(EventPhase phase) { this.phase = phase; }
    public List<EventParticipant> getParticipants() { return participants; }
    public int getPickTimerSeconds() { return pickTimerSeconds; }
    public void setPickTimerSeconds(int seconds) { this.pickTimerSeconds = seconds; }
    public int getDisconnectGraceSeconds() { return disconnectGraceSeconds; }
    public void setDisconnectGraceSeconds(int seconds) { this.disconnectGraceSeconds = seconds; }
    public String getProductDescription() { return productDescription; }
    public void setProductDescription(String desc) { this.productDescription = desc; }
    public LimitedPoolType getPoolType() { return poolType; }
    public void setPoolType(LimitedPoolType poolType) { this.poolType = poolType; }
    public SealedCardPoolGenerator getSealedGenerator() { return sealedGenerator; }
    public void setSealedGenerator(SealedCardPoolGenerator gen) { this.sealedGenerator = gen; }
    public BoosterDraft getDraft() { return draft; }
    public void setDraft(BoosterDraft draft) { this.draft = draft; }
    public int getNumRounds() { return numRounds; }
    public void setNumRounds(int numRounds) { this.numRounds = numRounds; }

    public void addParticipant(EventParticipant participant) {
        participants.add(participant);
    }

    private static final DateTimeFormatter EVENT_DATE_TAG =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static void setEventTags(Deck deck, NetworkEvent event) {
        deck.getTags().add("eventId:" + event.getEventId());
        deck.getTags().add("eventFormat:" + event.getFormat().name());
        deck.getTags().add("eventProduct:" + event.getProductDescription());
        deck.getTags().add("eventDate:" + event.createdAt.format(EVENT_DATE_TAG));
    }

    private static final DateTimeFormatter POOL_NAME_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** Conventional pool name: "Format - Product - YYYY-MM-DD". */
    public static String poolNameFor(NetworkEvent event) {
        String formatLabel = event.getFormat() == EventFormat.BOOSTER_DRAFT ? "Draft" : "Sealed";
        String product = productLabelFor(event.getProductDescription());
        return formatLabel
                + " - " + product
                + " - " + event.createdAt.format(POOL_NAME_DATE);
    }

    /** Strip the "PoolType: " prefix ("Full: Innistrad" -> "Innistrad"), drop any trailing
     *  parenthetical (e.g., set-combo codes shown in the config panel), and remove
     *  filesystem-illegal chars — yields a concise label suitable for a deck filename. */
    private static String productLabelFor(String description) {
        if (description == null || description.isEmpty()) return "";
        int sep = description.indexOf(": ");
        String label = sep >= 0 ? description.substring(sep + 2) : description;
        label = label.replaceAll("\\s*\\([^)]*\\)\\s*$", "");
        return label.replaceAll("[\\\\/:*?\"<>|]", "").trim();
    }

    public NetworkEventView toView() {
        return new NetworkEventView(eventId, format, phase,
                participants, pickTimerSeconds, productDescription, numRounds);
    }
}
