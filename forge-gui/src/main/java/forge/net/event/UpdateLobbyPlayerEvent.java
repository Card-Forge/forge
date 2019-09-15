package forge.net.event;

import java.util.Collections;
import java.util.Set;

import forge.ai.AIOption;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.match.LobbySlotType;
import forge.net.server.RemoteClient;

public final class UpdateLobbyPlayerEvent implements NetEvent {
    private static final long serialVersionUID = -7354695008599789571L;

    private LobbySlotType type = null;
    private String name = null;
    private int avatarIndex = -1;
    private int team = -1;
    private Boolean isArchenemy = null;
    private Boolean isReady = null;
    private Boolean isDevMode = null;
    private Deck deck = null;
    private DeckSection section = null;
    private CardPool cards = null;
    private Set<AIOption> aiOptions = null;


    public static UpdateLobbyPlayerEvent create(final LobbySlotType type, final String name, final int avatarIndex, final int team, final boolean isArchenemy, final boolean isReady, final Set<AIOption> aiOptions) {
        return new UpdateLobbyPlayerEvent(type, name, avatarIndex, team, isArchenemy, isReady, aiOptions);
    }
    public static UpdateLobbyPlayerEvent create(final LobbySlotType type, final String name, final int avatarIndex, final int team, final boolean isArchenemy, final boolean isReady, final boolean isDevMode, final Set<AIOption> aiOptions) {
        return new UpdateLobbyPlayerEvent(type, name, avatarIndex, team, isArchenemy, isReady, isDevMode, aiOptions);
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

    private UpdateLobbyPlayerEvent(String name) {
        this.name = name;
    }

    private UpdateLobbyPlayerEvent(final Deck deck) {
        this.deck = deck;
    }

    private UpdateLobbyPlayerEvent(final DeckSection section, final CardPool cards) {
        this.section = section;
        this.cards = cards;
    }

    private UpdateLobbyPlayerEvent(
            final LobbySlotType type,
            final String name,
            final int avatarIndex,
            final int team,
            final boolean isArchenemy,
            final boolean isReady,
            final Set<AIOption> aiOptions) {
        this.type = type;
        this.name = name;
        this.avatarIndex = avatarIndex;
        this.team = team;
        this.isArchenemy = isArchenemy;
        this.isReady = isReady;
        this.aiOptions = aiOptions;
    }

    private UpdateLobbyPlayerEvent(
            final LobbySlotType type,
            final String name,
            final int avatarIndex,
            final int team,
            final boolean isArchenemy,
            final boolean isReady,
            final boolean isDevMode,
            final Set<AIOption> aiOptions) {
        this.type = type;
        this.name = name;
        this.avatarIndex = avatarIndex;
        this.team = team;
        this.isArchenemy = isArchenemy;
        this.isReady = isReady;
        this.isDevMode = isDevMode;
        this.aiOptions = aiOptions;
    }

    @Override
    public void updateForClient(final RemoteClient client) {
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
}
