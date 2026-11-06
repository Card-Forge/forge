package forge.deck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import forge.game.GameFormat;
import forge.game.GameType;
import forge.gui.util.ListHeading;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.Localizer;

/** The entries of the Format dropdowns in the lobby and the deck editor. Each entry is a GameType, a GameFormat, a ListHeading or ARCHIVED. */
public final class FormatChoices {
    public static final List<GameType> COMMANDER_GAME_TYPES = List.of(
            GameType.Commander, GameType.Oathbreaker, GameType.Brawl, GameType.TinyLeaders);
    public static final List<GameType> GENERATED_GAME_TYPES = List.of(GameType.MomirBasic, GameType.MoJhoSto);
    /** Game types that replace Constructed's deck rules. Only one of them can be applied at a time. */
    public static final List<GameType> FORMAT_GAME_TYPES = Stream.concat(
            COMMANDER_GAME_TYPES.stream(), GENERATED_GAME_TYPES.stream()).collect(Collectors.toList());
    /** Game types the lobby's Variants list applies on top of the format. Several can be applied at once. */
    public static final List<GameType> STACKED_GAME_TYPES = List.of(
            GameType.Vanguard, GameType.Planechase, GameType.Archenemy, GameType.ArchenemyRumble);

    /** The entry that opens the archived formats, which are too many to list. */
    public static final Object ARCHIVED = new Object() {
        @Override
        public String toString() {
            return Localizer.getInstance().getMessage("lblArchivedFormats") + "...";
        }
    };

    private FormatChoices() {
    }

    /**
     * Constructed, the legality formats grouped by type, then the Commander game types, each group under a heading.
     * With generatedDecks, Momir Basic and MoJhoSto follow under their own heading. ARCHIVED comes last, when archived
     * formats are turned on.
     */
    public static List<Object> get(final boolean generatedDecks) {
        final Localizer localizer = Localizer.getInstance();
        final List<Object> choices = new ArrayList<>();
        choices.add(GameType.Constructed);
        GameFormat.FormatType group = null;
        for (final GameFormat format : FModel.getFormats().getOrderedList()) {
            // Archived formats open from ARCHIVED. Commander card pools are listed through their game types below.
            if (format.getFormatType() == GameFormat.FormatType.ARCHIVED
                    || format.getFormatSubType() == GameFormat.FormatSubType.COMMANDER) {
                continue;
            }
            if (format.getFormatType() != group) {
                group = format.getFormatType();
                choices.add(new ListHeading(localizer.getMessage(switch (group) {
                    case SANCTIONED -> "lblSanctioned";
                    case CASUAL -> "lblCasual";
                    case DIGITAL -> "lblDigital";
                    case CUSTOM -> "lblCustom";
                    default -> "lblOther";
                })));
            }
            choices.add(format);
        }
        choices.add(new ListHeading(localizer.getMessage("lblCommander")));
        choices.addAll(COMMANDER_GAME_TYPES);
        if (generatedDecks) {
            choices.add(new ListHeading(localizer.getMessage("lblOther")));
            choices.addAll(GENERATED_GAME_TYPES);
        }
        if (FModel.getPreferences().getPrefBoolean(FPref.LOAD_ARCHIVED_FORMATS)) {
            choices.add(new ListHeading(localizer.getMessage("lblArchived")));
            choices.add(ARCHIVED);
        }
        return choices;
    }

    /**
     * The archived formats under a heading for each folder their files are in, newest first in each folder. Formats
     * in no folder follow under Other.
     */
    public static List<Object> getArchivedChoices() {
        final Map<String, List<GameFormat>> folders = new TreeMap<>();
        for (final GameFormat format : FModel.getFormats().getArchivedList()) {
            folders.computeIfAbsent(String.join(" / ", folderPath(format)), k -> new ArrayList<>()).add(format);
        }
        final List<GameFormat> unfiled = folders.remove("");
        final List<Object> choices = new ArrayList<>();
        folders.forEach((folder, formats) -> addFolder(choices, folder, formats));
        if (unfiled != null) {
            addFolder(choices, Localizer.getInstance().getMessage("lblOther"), unfiled);
        }
        return choices;
    }

    private static void addFolder(final List<Object> choices, final String heading, final List<GameFormat> formats) {
        choices.add(new ListHeading(heading));
        formats.sort(Comparator.comparing(GameFormat::getEffectiveDate).reversed());
        choices.addAll(formats);
    }

    /** The format's folders, without a top folder named after a format type, such as Archived or Casual. */
    private static List<String> folderPath(final GameFormat format) {
        final List<String> path = new ArrayList<>(Arrays.asList(format.getFolder().split("/")));
        path.removeIf(String::isEmpty);
        if (!path.isEmpty() && Arrays.stream(GameFormat.FormatType.values()).anyMatch(t -> t.name().equalsIgnoreCase(path.get(0)))) {
            path.remove(0);
        }
        return path;
    }
}
