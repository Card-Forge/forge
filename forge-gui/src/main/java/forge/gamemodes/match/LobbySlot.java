package forge.gamemodes.match;

import com.google.common.collect.ImmutableSet;
import forge.ai.AIOption;
import forge.deck.Deck;
import forge.gamemodes.net.event.UpdateLobbyPlayerEvent;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

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
    private String aiProfile;

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
        changed |= setIfChanged(data.getType(),            this.type,           this::setType);
        changed |= setIfChanged(data.getName(),            this.name,           this::setName);
        changed |= setIntIfChanged(data.getAvatarIndex(),  this.avatarIndex,    this::setAvatarIndex);
        changed |= setIntIfChanged(data.getSleeveIndex(),  this.sleeveIndex,    this::setSleeveIndex);
        changed |= setIntIfChanged(data.getTeam(),         this.team,           this::setTeam);
        changed |= setIfChanged(data.getArchenemy(),       this.isArchenemy,    this::setIsArchenemy);
        changed |= setIfChanged(data.getReady(),           this.isReady,        this::setIsReady);
        changed |= setIfChanged(data.getDevMode(),         this.isDevMode,      this::setIsDevMode);
        changed |= setIfChanged(data.getAiOptions(),       this.aiOptions,      this::setAiOptions);
        changed |= setIfChanged(data.getSchemeDeckName(),  this.SchemeDeckName, this::setSchemeDeckName);
        changed |= setIfChanged(data.getAvatarVanguard(),  this.AvatarVanguard, this::setAvatarVanguard);
        changed |= setIfChanged(data.getPlanarDeckName(),  this.PlanarDeckName, this::setPlanarDeckName);
        changed |= setIfChanged(data.getDeckName(),        this.DeckName,       this::setDeckName);

        final Deck oldDeck = getDeck();
        if (data.getDeck() != null) {
            setDeck(data.getDeck());
        } else if (oldDeck != null && data.getSection() != null && data.getCards() != null) {
            oldDeck.putSection(data.getSection(), data.getCards());
        }
        if (data.getAiProfile() != null) {
            setAiProfile(data.getAiProfile());
        }
        return changed;
    }

    private static <T> boolean setIfChanged(T newValue, T oldValue, Consumer<T> setter) {
        if (newValue == null || Objects.equals(newValue, oldValue)) return false;
        setter.accept(newValue);
        return true;
    }

    // -1 is the "field absent" sentinel for ints in UpdateLobbyPlayerEvent.
    private static boolean setIntIfChanged(int newValue, int oldValue, IntConsumer setter) {
        if (newValue == -1 || newValue == oldValue) return false;
        setter.accept(newValue);
        return true;
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

    public String getAiProfile() {
        return aiProfile;
    }

    public void setAiProfile(String aiProfile) {
        this.aiProfile = aiProfile;
    }
}