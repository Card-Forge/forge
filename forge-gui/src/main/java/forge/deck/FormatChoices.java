package forge.deck;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import forge.game.GameFormat;
import forge.game.GameType;
import forge.gui.util.ListHeading;
import forge.model.FModel;
import forge.util.Localizer;

/** The entries of the Format dropdowns in the lobby and the deck editor. Each entry is a GameType or a GameFormat. */
public final class FormatChoices {
    public static final List<GameType> COMMANDER_GAME_TYPES = List.of(
            GameType.Commander, GameType.Oathbreaker, GameType.Brawl, GameType.TinyLeaders);
    public static final List<GameType> GENERATED_GAME_TYPES = List.of(GameType.MomirBasic, GameType.MoJhoSto);
    /** Game types that replace Constructed's deck rules. Only one of them can be applied at a time. */
    public static final List<GameType> FORMAT_GAME_TYPES = Stream.concat(
            COMMANDER_GAME_TYPES.stream(), GENERATED_GAME_TYPES.stream()).collect(Collectors.toList());

    private FormatChoices() {
    }

    /**
     * Constructed, the legality formats grouped by type, then the Commander game types, each group under a heading.
     * With generatedDecks, Momir Basic and MoJhoSto follow under their own heading.
     */
    public static List<Object> get(final boolean generatedDecks) {
        final Localizer localizer = Localizer.getInstance();
        final List<Object> choices = new ArrayList<>();
        choices.add(GameType.Constructed);
        GameFormat.FormatType group = null;
        for (final GameFormat format : FModel.getFormats().getFilterList()) {
            // Commander card pools are listed through their game types below
            if (format.getFormatSubType() == GameFormat.FormatSubType.COMMANDER) {
                continue;
            }
            if (format.getFormatType() != group) {
                group = format.getFormatType();
                choices.add(new ListHeading(localizer.getMessage(switch (group) {
                    case SANCTIONED -> "lblSanctioned";
                    case CASUAL -> "lblCasual";
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
        return choices;
    }
}
