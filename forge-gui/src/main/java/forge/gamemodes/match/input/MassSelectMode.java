package forge.gamemodes.match.input;

/**
 * Different actions for "select all" targets.
 * MINE: Select all targets belonging to player.
 * ALL: Select all valid targets in game (including players)
 * NONE: Clear current selections
 */
public enum MassSelectMode {
    MINE("lblMine"),
    ALL("lblAll"),
    NONE("lblClearAll");

    final String labelName;

    MassSelectMode(String labelName) {
        this.labelName = labelName;
    }

    public String getLabelName () {
        return labelName;
    }

    private static final MassSelectMode[] vals = values();

    public MassSelectMode next() {
        return vals[(this.ordinal() + 1) % vals.length];
    }
}
