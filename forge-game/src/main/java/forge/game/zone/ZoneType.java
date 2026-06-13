package forge.game.zone;

import forge.trackable.TrackableProperty;
import forge.util.ITranslatable;
import forge.util.Localizer;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * The Enum Zone.
 */
public enum ZoneType implements ITranslatable {
    Hand(true, "lblHandZone", TrackableProperty.Hand),
    Library(true, "lblLibraryZone", TrackableProperty.Library),
    Graveyard(false, "lblGraveyardZone", TrackableProperty.Graveyard),
    Battlefield(false, "lblBattlefieldZone", TrackableProperty.Battlefield),
    Exile(false, "lblExileZone", TrackableProperty.Exile),
    Flashback(false, "lblFlashbackZone", TrackableProperty.Flashback),
    Command(false, "lblCommandZone", TrackableProperty.Command),
    Stack(false, "lblStackZone"),
    Sideboard(true, "lblSideboardZone", TrackableProperty.Sideboard),
    Ante(false, "lblAnteZone", TrackableProperty.Ante),
    Merged(false, "lblBattlefieldZone"),
    SchemeDeck(true, "lblSchemeDeckZone", TrackableProperty.SchemeDeck),
    PlanarDeck(true, "lblPlanarDeckZone", TrackableProperty.PlanarDeck),
    AttractionDeck(true, "lblAttractionDeckZone", TrackableProperty.AttractionDeck),
    Junkyard(false, "lblJunkyardZone", TrackableProperty.Junkyard),
    ContraptionDeck(true, "lblContraptionDeckZone", TrackableProperty.ContraptionDeck),
    //Scrapyard is like the Junkyard but for contraptions; just going to recycle the Junkyard for this.
    Subgame(true, "lblSubgameZone"),
    // ExtraHand is used for Backup Plan for temporary extra hands
    ExtraHand(true, "lblHandZone"),
    None(true, "lblNoneZone");

    public static final EnumSet<ZoneType> STATIC_ABILITIES_SOURCE_ZONES = EnumSet.of(Battlefield, Graveyard, Exile, Command, Stack/*, Hand*/);
    public static final EnumSet<ZoneType> PART_OF_COMMAND_ZONE = EnumSet.of(Command, SchemeDeck, PlanarDeck, AttractionDeck, ContraptionDeck, Junkyard);
    public static final EnumSet<ZoneType> DECK_ZONES = EnumSet.of(Library, SchemeDeck, PlanarDeck, AttractionDeck, ContraptionDeck);
    public static final EnumSet<ZoneType> ORDERED_ZONES = EnumSet.of(Library, SchemeDeck, PlanarDeck, AttractionDeck, ContraptionDeck, Hand, Graveyard, Stack);

    private final boolean holdsHiddenInfo;
    private final String label;
    private final TrackableProperty trackableProperty;

    ZoneType(boolean holdsHidden, String label) {
        this(holdsHidden, label, null);
    }
    ZoneType(boolean holdsHidden, String label, TrackableProperty trackableProperty) {
        holdsHiddenInfo = holdsHidden;
        this.label = label;
        this.trackableProperty = trackableProperty;
    }

    /** Returns the TrackableProperty that holds this zone's cards on PlayerView, or null. */
    public TrackableProperty getTrackableProperty() {
        return trackableProperty;
    }

    public static ZoneType smartValueOf(final String value) {
        if (value == null) {
            return null;
        }
        if ("All".equals(value)) {
            return null;
        }
        final String valToCompate = value.trim();
        for (final ZoneType v : ZoneType.values()) {
            if (v.name().compareToIgnoreCase(valToCompate) == 0) {
                return v;
            }
        }
        throw new IllegalArgumentException("No element named " + value + " in enum Zone");
    }

    public static List<ZoneType> listValueOf(final String values) {
        if ("All".equals(values)) {
            return List.of(Battlefield, Hand, Graveyard, Exile, Stack, Library, Command);
        }
        final List<ZoneType> result = new ArrayList<>();
        for (final String s : values.split("[, ]+")) {
            ZoneType zt = ZoneType.smartValueOf(s);
            if (zt != null) {
                result.add(zt);
            }
        }
        return result;
    }

    public boolean isHidden() {
        return holdsHiddenInfo;
    }

    public boolean isKnown() {
        return !holdsHiddenInfo;
    }

    public boolean isPartOfCommandZone() {
        return PART_OF_COMMAND_ZONE.contains(this);
    }

    /**
     * Indicates that this zone behaves as a deck - an ordered pile of face down cards
     * such as the Library or Planar Deck.
     */
    public boolean isDeck() {
        return DECK_ZONES.contains(this);
    }

    @Override
    public String getName() {
        return name();
    }
    @Override
    public String getTranslatedName() {
        return Localizer.getInstance().getMessage(label);
    }

    public static boolean isHidden(final String origin) {
        List<ZoneType> zone = ZoneType.listValueOf(origin);

        if (zone.isEmpty()) {
            return true;
        }

        for (ZoneType z : zone) {
            if (z.isHidden()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isKnown(final String origin) {
        return !isHidden(origin);
    }
}
