package forge.gamemodes.net.event;

import forge.ai.AIOption;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.gamemodes.match.LobbySlotType;

import java.util.Collections;
import java.util.Set;

public final class UpdateLobbyPlayerEvent implements NetEvent {
    private static final long serialVersionUID = -7354695008599789571L;

    private LobbySlotType type = null;
    private String name = null;
    private int avatarIndex = -1;
    private int sleeveIndex = -1;
    private int team = -1;
    private Boolean isArchenemy = null;
    private Boolean isReady = null;
    private Boolean isDevMode = null;
    private Deck deck = null;
    private DeckSection section = null;
    private CardPool cards = null;
    private Set<AIOption> aiOptions = null;
    private String AvatarVanguard = null;
    private String SchemeDeckName = null;
    private String PlanarDeckName = null;
    private String DeckName = null;
    private String aiProfile = null;


    public static UpdateLobbyPlayerEvent create(final LobbySlotType type, final String name, final int avatarIndex, final int sleeveIndex, final int team, final boolean isArchenemy, final boolean isDevMode, final Set<AIOption> aiOptions, final String aiProfile) {
        return new UpdateLobbyPlayerEvent(type, name, avatarIndex, sleeveIndex, team, isArchenemy, isDevMode, aiOptions, aiProfile);
    }
    public static UpdateLobbyPlayerEvent deckUpdate(final Deck deck) {
        return new UpdateLobbyPlayerEvent(deck);
    }
    public static UpdateLobbyPlayerEvent deckUpdate(final DeckSection section, final CardPool cards) {
        return new UpdateLobbyPlayerEvent(section, cards);
    }
    public static UpdateLobbyPlayerEvent nameUpdate(final String name) {
        return new UpdateLobbyPlayerEvent(name);
    }

    public static UpdateLobbyPlayerEvent aiProfileUpdate(String aiProfile) {
        UpdateLobbyPlayerEvent event = new UpdateLobbyPlayerEvent();
        event.setAiProfile(aiProfile);
        return event;
    }

    // Private default constructure for setting specific fields.
    private UpdateLobbyPlayerEvent() { }

    private UpdateLobbyPlayerEvent(String name) {
        this.name = name;
    }

    public static UpdateLobbyPlayerEvent avatarUpdate(final int index) {
        return new UpdateLobbyPlayerEvent(index, true);
    }
    public static UpdateLobbyPlayerEvent sleeveUpdate(final int index) {
        return new UpdateLobbyPlayerEvent(index, false);
    }
    public static UpdateLobbyPlayerEvent isReadyUpdate(final boolean isReady) {
        return new UpdateLobbyPlayerEvent(isReady);
    }
    public static UpdateLobbyPlayerEvent devModeUpdate(final boolean isDevMode) {
        final UpdateLobbyPlayerEvent event = new UpdateLobbyPlayerEvent();
        event.isDevMode = isDevMode;
        return event;
    }
    public static UpdateLobbyPlayerEvent teamUpdate(int team) {
        return new UpdateLobbyPlayerEvent(team);
    }
    public static UpdateLobbyPlayerEvent setDeckSchemePlaneVanguard(final String DeckName, final String Scheme, final String Plane, final String Vanguard) {
        return new UpdateLobbyPlayerEvent(DeckName, Scheme, Plane, Vanguard);
    }
    private UpdateLobbyPlayerEvent(final int index, final boolean avatar) {
        if (avatar)
            this.avatarIndex = index;
        else
            this.sleeveIndex = index;
    }
    private UpdateLobbyPlayerEvent(final int team) {
        this.team = team;
    }
    private UpdateLobbyPlayerEvent(final String DeckName, final String Scheme, final String Plane, final String Vanguard) {
        this.SchemeDeckName = Scheme;
        this.PlanarDeckName = Plane;
        this.AvatarVanguard = Vanguard;
        this.DeckName = DeckName;
    }

    private UpdateLobbyPlayerEvent(final Deck deck) {
        this.deck = deck;
    }

    private UpdateLobbyPlayerEvent(final boolean isReady) {
        this.isReady = isReady;
    }

    private UpdateLobbyPlayerEvent(final DeckSection section, final CardPool cards) {
        this.section = section;
        this.cards = cards;
    }

    private UpdateLobbyPlayerEvent(
            final LobbySlotType type,
            final String name,
            final int avatarIndex,
            final int sleeveIndex,
            final int team,
            final boolean isArchenemy,
            final boolean isDevMode,
            final Set<AIOption> aiOptions,
            final String aiProfile) {
        this.type = type;
        this.name = name;
        this.avatarIndex = avatarIndex;
        this.sleeveIndex = sleeveIndex;
        this.team = team;
        this.isArchenemy = isArchenemy;
        this.isDevMode = isDevMode;
        this.aiOptions = aiOptions;
        this.aiProfile = aiProfile;
    }

    /**
     * Clear the fields a remote client has no business setting, returning
     * whether any were present. Slot type is server-owned lifecycle state
     * ({@code connectPlayer} sets REMOTE, {@code disconnectPlayer} OPEN) and
     * the AI fields configure AI slots, which a REMOTE slot is not; the client
     * UI offers none of them, so this is a no-op for honest traffic.
     *
     * <p>Left alone deliberately: {@code isDevMode} and {@code isArchenemy},
     * which clients do set legitimately today — whether they should be able to
     * is a game-design question, not a protocol-authorization one.
     *
     * <p>In place rather than into a copy: the event is freshly deserialized
     * and never re-broadcast, and a field-by-field copy would silently drop any
     * field added later.
     */
    public boolean clearServerOwnedFields() {
        if (type == null && aiOptions == null && aiProfile == null) {
            return false;
        }
        type = null;
        aiOptions = null;
        aiProfile = null;
        return true;
    }

    public LobbySlotType getType() {
        return type;
    }
    public String getName() {
        return name;
    }
    public int getAvatarIndex() {
        return avatarIndex;
    }
    public int getSleeveIndex() {
        return sleeveIndex;
    }
    public int getTeam() {
        return team;
    }
    public Boolean getArchenemy() {
        return isArchenemy;
    }
    public Boolean getReady() {
        return isReady;
    }
    public Boolean getDevMode() {
        return isDevMode;
    }
    public Deck getDeck() {
        return deck;
    }
    public DeckSection getSection() {
        return section;
    }
    public CardPool getCards() {
        return cards;
    }
    public Set<AIOption> getAiOptions() {
        return aiOptions == null ? null : Collections.unmodifiableSet(aiOptions);
    }
    public String getAvatarVanguard() { return AvatarVanguard; }
    public String getSchemeDeckName() { return SchemeDeckName; }
    public String getPlanarDeckName() { return PlanarDeckName; }
    public String getDeckName() { return DeckName; }

    /** Lets the server replace a client-supplied name with a sanitised one. */
    public void setName(String name) {
        this.name = name;
    }

    public void setAiProfile(String aiProfile) {
        this.aiProfile = aiProfile;
    }

    public String getAiProfile() {
        return aiProfile;
    }
}
