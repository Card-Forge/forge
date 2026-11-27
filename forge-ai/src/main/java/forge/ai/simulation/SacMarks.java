package forge.ai.simulation;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * The {@code SacMe} marks that the plans put on a creature so that the AI's own cost payment sacrifices that one. A mark is
 * not planner state: the AI reads it in its other choices too. So a mark remembers what the card had before
 * ({@code SacMeWas}) and is taken back when the step that set it was not carried out (the approval said no, no plan had a
 * step), and at the start of the next step; the cost is paid straight after a plan hands its ability back, so that is
 * long enough.
 */
public final class SacMarks {
    private static final String WAS = "SacMeWas";
    private static final String NONE = "-";

    private SacMarks() {
    }

    public static void mark(final Card c) {
        if (!c.hasSVar(WAS)) {
            c.setSVar(WAS, c.hasSVar("SacMe") ? c.getSVar("SacMe") : NONE);
        }
        c.setSVar("SacMe", "6");
    }

    /** Gives every mark that the plans set back to what the card had. */
    public static void restore(final Player ai) {
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (!c.hasSVar(WAS)) {
                continue;
            }
            final String was = c.getSVar(WAS);
            c.removeSVar(WAS);
            if (NONE.equals(was)) {
                c.removeSVar("SacMe");
            } else {
                c.setSVar("SacMe", was);
            }
        }
    }
}
