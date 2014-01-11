package forge.game.zone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import forge.game.player.Player;
import forge.net.FServer;
import forge.util.Lang;

/**
 * The Enum Zone.
 */
public enum ZoneType {
    Hand(true),
    Library(true),
    Graveyard(false),
    Battlefield(false),
    Exile(false),
    Command(false),
    Stack(false),
    Sideboard(true),
    Ante(false),
    SchemeDeck(true),
    PlanarDeck(true);

    public static final List<ZoneType> STATIC_ABILITIES_SOURCE_ZONES = Arrays.asList(new ZoneType[]{Battlefield, Graveyard, Exile, Command/*, Hand*/});

    private final boolean holdsHiddenInfo;
    private ZoneType(boolean holdsHidden) {
        holdsHiddenInfo = holdsHidden;
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
        final List<ZoneType> result = new ArrayList<ZoneType>();
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

    public String createMessage(Player player, String prefix) {
        return createMessage(player, prefix, null);
    }
    public String createMessage(Player player, String prefix, String suffix) {
        StringBuilder message = new StringBuilder();

        String owner;
        if (player.getLobbyPlayer() == FServer.instance.getLobby().getGuiPlayer()) {
            owner = "your";
        }
        else {
            owner = Lang.getPossesive(player.getName());
        }

        if (prefix != null && !prefix.isEmpty()) {
            message.append(prefix);
            if (!prefix.endsWith(" ")) {
                message.append(" ");
            }
            message.append(owner);
        }
        else {
            message.append(owner.substring(0, 1).toUpperCase() + owner.substring(1));
        }

        message.append(" " + Lang.splitCompoundWord(this.toString(), Lang.PhraseCase.Lower));

        if (suffix != null && !suffix.isEmpty()) {
            message.append(" " + suffix);
        }

        return message.toString();
    }
}
