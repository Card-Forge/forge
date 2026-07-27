package forge.screens.home;
import forge.util.Localizer;
/**
 * Submenus each belong to a menu group, which
 * is used for several functions, such as expanding
 * and collapsing in the menu.
 * 
 * <br><br><i>(E at beginning of class name denotes an enum.)</i>
 */
public enum EMenuGroup {
    SANCTIONED ("lblSanctionedFormats"),
    PLAY ("lblCommander"),
    GAUNTLET ("lblGauntlets"),
    ONLINE ("lblOnlineMultiplayer"),
    QUEST ("lblQuestMode"),
    PUZZLE ("lblPuzzleMode"),
    SETTINGS ("lblGameSettings");

    private final String strTitle;

    /** @param {@link java.lang.String} */
    EMenuGroup(final String s0) { strTitle = s0; }

    /** @return {@link java.lang.String} */
    public String getTitle() {
        final Localizer localizer = Localizer.getInstance();
        String t = localizer.getMessage(this.strTitle);
        return t;
    }
}
