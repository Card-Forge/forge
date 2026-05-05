package forge.gamemodes.net;

import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.gamemodes.limited.BoosterDraft;
import forge.gamemodes.limited.LimitedPoolType;
import forge.gamemodes.limited.SealedCardPoolGenerator;
import forge.model.FModel;
import forge.util.Localizer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Model and helpers for a network limited event (draft or sealed).
 * <p>
 * The instance side is the server-side mutable event — in-memory only during the event
 * session, discarded after pool distribution. The wire-safe representation is
 * {@link NetworkEventView}. Each participant's pool is persisted as a Deck with event
 * metadata in tags.
 * <p>
 * The static side is a home for cross-platform event helpers — tag I/O
 * ({@link #setEventTags}, {@link #findEventTags}), display formatting
 * ({@link #poolNameFor}, {@link #getEventDisplayLabel}, {@link #computeEventPanelText}),
 * and the {@link EventChoice} / {@link EventPanelText} data records — so desktop and
 * mobile UIs share identical behaviour without re-deriving the logic per platform.
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

    /** An event id paired with its display label, e.g., for dialog-driven event selection. */
    public record EventChoice(String id, String label) {
        @Override public String toString() { return label; }
    }

    /** Pre-computed display strings for the event-details panel — platform-agnostic. */
    public record EventPanelText(
            String formatText,
            String productText,
            String timerText,
            String dateText,
            String statusText) { }

    /** Returns {eventFormat, eventProduct, eventDate} for the deck tagged with eventId, or null. */
    public static String[] findEventTags(String eventId) {
        for (Deck d : FModel.getDecks().getNetworkEventDecks()) {
            if (eventId.equals(DeckProxy.getEventTag(d, "eventId"))) {
                return new String[] {
                        DeckProxy.getEventTag(d, "eventFormat"),
                        DeckProxy.getEventTag(d, "eventProduct"),
                        DeckProxy.getEventTag(d, "eventDate"),
                };
            }
        }
        return null;
    }

    /** Short display label for a past event id, e.g., "Draft — Innistrad — (2026-04-20 10:15)". */
    public static String getEventDisplayLabel(String eventId) {
        String[] tags = findEventTags(eventId);
        if (tags == null) return eventId;
        String displayFormat = EventFormat.BOOSTER_DRAFT.name().equals(tags[0]) ? "Draft" : "Sealed";
        return displayFormat + " — " + (tags[1] == null ? "" : tags[1])
                + " — (" + (tags[2] == null ? "" : tags[2]) + ")";
    }

    /**
     * Compute the five display strings for the event-details panel, given state + lobby snapshot.
     * Platform-agnostic: both desktop and mobile views can call this and get identical text.
     *
     * @param isHost          whether this client owns the lobby
     * @param activeEventId   id of the loaded past event, or null if none loaded
     * @param currentEvent    host's in-flight event (null on client and when no event is being set up)
     * @param lastEventView   last-received wire snapshot (null before any broadcast arrives)
     */
    public static EventPanelText computeEventPanelText(
            boolean isHost, String activeEventId,
            NetworkEvent currentEvent, NetworkEventView lastEventView) {
        if (activeEventId != null) {
            return textForLoadedEvent(activeEventId);
        }
        boolean inFlight = isHost ? currentEvent != null : lastEventView != null;
        if (inFlight) {
            return textForInFlightEvent(isHost, currentEvent, lastEventView);
        }
        return emptyEventText();
    }

    /** Text for a past event loaded from disk, read off the deck tags. */
    private static EventPanelText textForLoadedEvent(String eventId) {
        Localizer localizer = Localizer.getInstance();
        String formatText = "—";
        String productText = "—";
        String dateText = "—";
        String[] tags = findEventTags(eventId);
        if (tags != null) {
            if (tags[0] != null) {
                formatText = EventFormat.BOOSTER_DRAFT.name().equals(tags[0])
                        ? localizer.getMessage("lblNetworkModeDraft")
                        : localizer.getMessage("lblNetworkModeSealed");
            }
            if (tags[1] != null && !tags[1].isEmpty()) productText = tags[1];
            if (tags[2] != null && !tags[2].isEmpty()) dateText = tags[2];
        }
        return new EventPanelText(formatText, productText, "—", dateText, "");
    }

    /** Text for an event currently being configured/drafted, read off the event model. */
    private static EventPanelText textForInFlightEvent(boolean isHost,
            NetworkEvent currentEvent, NetworkEventView lastEventView) {
        Localizer localizer = Localizer.getInstance();
        EventFormat evFormat;
        int timerSec;
        String desc;
        LimitedPoolType pool = null;
        if (isHost) {
            evFormat = currentEvent.getFormat();
            timerSec = currentEvent.getPickTimerSeconds();
            desc = currentEvent.getProductDescription();
            pool = currentEvent.getPoolType();
        } else {
            evFormat = lastEventView.getFormat();
            timerSec = lastEventView.getPickTimerSeconds();
            desc = lastEventView.getProductDescription();
        }
        String formatText = (evFormat == EventFormat.BOOSTER_DRAFT)
                ? localizer.getMessage("lblNetworkModeDraft")
                : localizer.getMessage("lblNetworkModeSealed");
        String productText = "—";
        if (desc != null && !desc.isEmpty()) {
            productText = desc;
        } else if (pool != null) {
            productText = pool.toString();
        }
        String timerText = (evFormat == EventFormat.BOOSTER_DRAFT)
                ? (timerSec > 0 ? timerSec + "s" : "—")
                : localizer.getMessage("lblNetworkPickTimerNotApplicable");
        String dateText = localizer.getMessage(evFormat == EventFormat.SEALED
                ? "lblNetworkNewEventNoPools" : "lblNetworkNewEventNotDrafted");
        return new EventPanelText(formatText, productText, timerText, dateText, "");
    }

    /** Placeholder text when no event is configured yet — caller typically shows a "waiting" message. */
    private static EventPanelText emptyEventText() {
        Localizer localizer = Localizer.getInstance();
        return new EventPanelText("—", "—", "—", "—",
                localizer.getMessage("lblNetworkWaitingForHost"));
    }
}
