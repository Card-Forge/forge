package forge.gamemodes.match.input;

import forge.util.ITranslatable;
import forge.util.Localizer;

/**
 * Different actions for "select all" targets.
 * MINE: Select all targets belonging to player.
 * ALL: Select all valid targets in game (including players)
 * NONE: Clear current selections
 */
public enum MassSelectMode implements ITranslatable {
    MINE("lblMine"),
    ALL("lblAll"),
    NONE("lblClearAll");

    final String labelName;

    MassSelectMode(String labelName) {
        this.labelName = labelName;
    }

    private static final MassSelectMode[] vals = values();

    public MassSelectMode next() {
        return vals[(this.ordinal() + 1) % vals.length];
    }

    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public String getTranslatedName() {
        return Localizer.getInstance().getMessage(labelName);
    }
}
