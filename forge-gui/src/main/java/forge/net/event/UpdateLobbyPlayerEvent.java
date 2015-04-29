package forge.net.event;

import java.util.Collections;
import java.util.Set;

import forge.AIOption;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.match.LobbySlotType;
import forge.net.server.RemoteClient;

public final class UpdateLobbyPlayerEvent implements NetEvent {
    private static final long serialVersionUID = -5073305607515425968L;

    private final LobbySlotType type;
    private final String name;
    private final int avatarIndex;
    private final int team;
    private final Boolean isArchenemy;
    private final Boolean isReady;
    private final Deck deck;
    private final DeckSection section;
    private final CardPool cards;
    private final Set<AIOption> aiOptions;

    public static UpdateLobbyPlayerEvent create(final LobbySlotType type, final String name, final int avatarIndex, final int team, final boolean isArchenemy, final boolean isReady, final Set<AIOption> aiOptions) {
        return new UpdateLobbyPlayerEvent(type, name, avatarIndex, team, isArchenemy, isReady, null, null, null, aiOptions);
    }
    public static UpdateLobbyPlayerEvent deckUpdate(final Deck deck) {
        return new UpdateLobbyPlayerEvent(null, null, -1, -1, null, null, deck, null, null, null);
    }
    public static UpdateLobbyPlayerEvent deckUpdate(final DeckSection section, final CardPool cards) {
        return new UpdateLobbyPlayerEvent(null, null, -1, -1, null, null, null, section, cards, null);
    }

    private UpdateLobbyPlayerEvent(final LobbySlotType type, final String name, final int avatarIndex, final int team, final Boolean isArchenemy, final Boolean isReady, final Deck deck, final DeckSection section, final CardPool cards, final Set<AIOption> aiOptions) {
        this.type = type;
        this.name = name;
        this.avatarIndex = avatarIndex;
        this.team = team;
        this.isArchenemy = isArchenemy;
        this.isReady = isReady;
        this.deck = deck;
        this.section = section;
        this.cards = cards;
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
