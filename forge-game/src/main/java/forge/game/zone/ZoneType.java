package forge.game.zone;

import forge.util.Localizer;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * The Enum Zone.
 */
public enum ZoneType {
    Hand(true, "lblHandZone"),
    Library(true, "lblLibraryZone"),
    Graveyard(false, "lblGraveyardZone"),
    Battlefield(false, "lblBattlefieldZone"),
    Exile(false, "lblExileZone"),
    Flashback(false, "lblFlashbackZone"),
    Command(false, "lblCommandZone"),
    Stack(false, "lblStackZone"),
    Sideboard(true, "lblSideboardZone"),
    Ante(false, "lblAnteZone"),
    Merged(false, "lblBattlefieldZone"),
    SchemeDeck(true, "lblSchemeDeckZone"),
    PlanarDeck(true, "lblPlanarDeckZone"),
    AttractionDeck(true, "lblAttractionDeckZone"),
    Junkyard(false, "lblJunkyardZone"),
    ContraptionDeck(true, "lblContraptionDeckZone"),
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
    private final String zoneName;
    ZoneType(boolean holdsHidden, String name) {
        holdsHiddenInfo = holdsHidden;
        zoneName = Localizer.getInstance().getMessage(name);
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

    public String getTranslatedName() {
        return zoneName;
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
