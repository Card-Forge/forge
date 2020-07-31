package forge.match;

import java.io.Serializable;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import forge.ai.AIOption;
import forge.deck.Deck;
import forge.net.event.UpdateLobbyPlayerEvent;

public final class LobbySlot implements Serializable {
    private static final long serialVersionUID = 9203252798721142264L;

    private LobbySlotType type;
    private String name;
    private int avatarIndex;
    private int sleeveIndex;
    private int team;
    private boolean isArchenemy;
    private boolean isReady;
    private boolean isDevMode;
    private Deck deck;
    private ImmutableSet<AIOption> aiOptions;
    private String AvatarVanguard;
    private String SchemeDeckName;
    private String PlanarDeckName;
    private String DeckName;

    public LobbySlot(final LobbySlotType type, final String name, final int avatarIndex, final int sleeveIndex, final int team, final boolean isArchenemy, final boolean isReady, final Set<AIOption> aiOptions) {
        this.type = type;
        this.name = name;
        this.avatarIndex = avatarIndex;
        this.sleeveIndex = sleeveIndex;
        this.team = team;
        this.isArchenemy = isArchenemy;
        this.isReady = isReady;
        this.isDevMode = false;
        this.setAiOptions(aiOptions);
    }

    boolean apply(final UpdateLobbyPlayerEvent data) {
        boolean changed = false;
        if (data.getType() != null) {
            setType(data.getType());
            changed = true;
        }
        if (data.getName() != null) {
            setName(data.getName());
            changed = true;
        }
        if (data.getAvatarIndex() != -1) {
            setAvatarIndex(data.getAvatarIndex());
            changed = true;
        }
        if (data.getSleeveIndex() != -1) {
            setSleeveIndex(data.getSleeveIndex());
            changed = true;
        }
        if (data.getTeam() != -1) {
            setTeam(data.getTeam());
            changed = true;
        }
        if (data.getArchenemy() != null) {
            setIsArchenemy(data.getArchenemy().booleanValue());
            changed = true;
        }
        if (data.getReady() != null) {
            setIsReady(data.getReady().booleanValue());
            changed = true;
        }
        if (data.getDevMode() != null) {
            setIsDevMode(data.getDevMode().booleanValue());
            changed = true;
        }
        if (data.getAiOptions() != null) {
            setAiOptions(data.getAiOptions());
            changed = true;
        }
        final Deck oldDeck = getDeck();
        if (data.getDeck() != null) {
            setDeck(data.getDeck());
        } else if (oldDeck != null && data.getSection() != null && data.getCards() != null) {
            oldDeck.putSection(data.getSection(), data.getCards());
        }
        if (data.getSchemeDeckName() != null) {
            setSchemeDeckName(data.getSchemeDeckName());
            changed = true;
        }
        if (data.getAvatarVanguard() != null) {
            setAvatarVanguard(data.getAvatarVanguard());
            changed = true;
        }
        if (data.getPlanarDeckName() != null) {
            setPlanarDeckName(data.getPlanarDeckName());
            changed = true;
        }
        if (data.getDeckName() != null) {
            setDeckName(data.getDeckName());
            changed = true;
        }
        return changed;
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
    public int getSleeveIndex() {
        return sleeveIndex;
    }
    public void setAvatarIndex(final int avatarIndex) {
        this.avatarIndex = avatarIndex;
    }
    public void setSleeveIndex(final int sleeveIndex) {
        this.sleeveIndex = sleeveIndex;
    }

    public int getTeam() {
        return team;
    }
    public void setTeam(final int team) {
        this.team = team;
    }

    public String getSchemeDeckName() { return SchemeDeckName; }
    public String getAvatarVanguard() { return AvatarVanguard; }
    public String getPlanarDeckName() { return PlanarDeckName; }
    public String getDeckName() { return DeckName; }

    public void setSchemeDeckName(String schemeDeckName) { this.SchemeDeckName = schemeDeckName; }
    public void setAvatarVanguard(String avatarVanguard) { this.AvatarVanguard = avatarVanguard; }
    public void setPlanarDeckName(String planarDeckName) { this.PlanarDeckName = planarDeckName; }
    public void setDeckName(String DeckName) { this.DeckName = DeckName; }

    public boolean isArchenemy() {
        return isArchenemy;
    }
    public void setIsArchenemy(final boolean isArchenemy) {
        this.isArchenemy = isArchenemy;
    }

    public boolean isReady() {
        return type == LobbySlotType.AI || isReady;
    }
    public void setIsReady(final boolean isReady) {
        this.isReady = isReady;
    }

    public boolean isDevMode() {
        return isDevMode;
    }
    public void setIsDevMode(final boolean isDevMode) {
        this.isDevMode = isDevMode;
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
        this.aiOptions = aiOptions == null ? ImmutableSet.of() : ImmutableSet.copyOf(aiOptions);
    }

}