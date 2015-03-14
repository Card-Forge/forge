package forge.match;

import java.io.Serializable;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import forge.AIOption;
import forge.deck.Deck;
import forge.net.game.LobbySlotType;
import forge.net.game.UpdateLobbyPlayerEvent;

public final class LobbySlot implements Serializable {
    private static final long serialVersionUID = 6918205436608794289L;

    private LobbySlotType type;
    private String name;
    private int avatarIndex;
    private int team;
    private boolean isArchenemy;
    private Deck deck;
    private ImmutableSet<AIOption> aiOptions;

    public LobbySlot(final LobbySlotType type, final String name, final int avatarIndex, final int team, final boolean isArchenemy, final Set<AIOption> aiOptions) {
        this.type = type;
        this.name = name;
        this.avatarIndex = avatarIndex;
        this.team = team;
        this.isArchenemy = isArchenemy;
        this.setAiOptions(aiOptions);
    }

    void apply(final UpdateLobbyPlayerEvent data) {
        if (data.getType() != null) {
            setType(data.getType());
        }
        if (data.getName() != null) {
            setName(data.getName());
        }
        if (data.getAvatarIndex() != -1) {
            setAvatarIndex(data.getAvatarIndex());
        }
        if (data.getTeam() != -1) {
            setTeam(data.getTeam());
        }
        if (data.getArchenemy() != null) {
            setIsArchenemy(data.getArchenemy().booleanValue());
        }
        if (data.getAiOptions() != null) {
            setAiOptions(data.getAiOptions());
        }
        if (data.getDeck() != null) {
            setDeck(data.getDeck());
        } else if (getDeck() != null && data.getSection() != null && data.getCards() != null) {
            getDeck().putSection(data.getSection(), data.getCards());
        }
    }

    public LobbySlotType getType() {
        return type;
    }
    public void setType(final LobbySlotType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }
    public void setName(final String name) {
        this.name = name;
    }

    public int getAvatarIndex() {
        return avatarIndex;
    }
    public void setAvatarIndex(final int avatarIndex) {
        this.avatarIndex = avatarIndex;
    }

    public int getTeam() {
        return team;
    }
    public void setTeam(final int team) {
        this.team = team;
    }

    public boolean isArchenemy() {
        return isArchenemy;
    }
    public void setIsArchenemy(final boolean isArchenemy) {
        this.isArchenemy = isArchenemy;
    }

    public Deck getDeck() {
        return deck;
    }
    public void setDeck(final Deck deck) {
        this.deck = deck;
    }

    public ImmutableSet<AIOption> getAiOptions() {
        return aiOptions;
    }

    public void setAiOptions(final Set<AIOption> aiOptions) {
        this.aiOptions = aiOptions == null ? ImmutableSet.<AIOption>of() : ImmutableSet.copyOf(aiOptions);
    }

}