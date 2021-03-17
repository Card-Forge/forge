package forge.game.zone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Function;

import forge.util.Localizer;

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
    Subgame(true, "lblSubgameZone"),
    None(true, "lblNoneZone");

    public static final List<ZoneType> STATIC_ABILITIES_SOURCE_ZONES = Arrays.asList(Battlefield, Graveyard, Exile, Command/*, Hand*/);

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

    public static class Accessors {
        public static Function<ZoneType, String> GET_TRANSLATED_NAME = new Function<ZoneType, String>() {
            @Override
            public String apply(final ZoneType arg0) {
                return arg0.getTranslatedName();
            }
        };
    }
}
