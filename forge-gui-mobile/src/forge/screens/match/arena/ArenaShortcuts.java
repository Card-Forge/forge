package forge.screens.match.arena;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.badlogic.gdx.Input.Keys;

import forge.Forge;
import forge.Forge.KeyInputAdapter;

/**
 * The match keyboard shortcuts of the Arena look, as data: which key does what, a label for the key and a line saying
 * what it does. MatchScreen.arenaShortcut does the new ones (desktop, Arena look on) and {@link ArenaShortcutSheet}
 * lists them. Keys follow MatchScreen.keyDown where it already handles the same action ({@link Shortcut#existing}
 * says so), so the default look and the Arena look answer the same keys; the new ones use keys keyDown leaves free
 * (F5 revalidates the screen and the arrow keys emulate swipes globally, so those are avoided). Z (ZOOM_CARD) opens the
 * card under the pointer in the focus view (MatchScreen.zoomHoveredCard).
 */
public final class ArenaShortcuts {
    private ArenaShortcuts() { }

    /** Everything a key can do on the Arena match screen. */
    public enum Action {
        /** Press the prompt's OK button: pass priority, confirm, keep. (MatchScreen: Space / Enter.) */
        OK,
        /** Press the prompt's Cancel button. (MatchScreen: Esc.) */
        CANCEL,
        /** Pass priority until the end of this turn: YieldController.endTurn. (MatchScreen: E.) */
        END_TURN,
        /** Pass until the stack has resolved: YieldUpdate.StackYield(local, true, respectsInterrupts). New. */
        PASS_UNTIL_STACK_RESOLVES,
        /** Pass until the next turn begins, whoever's it is: YieldUpdate.SetMarker(next player, UPKEEP, ...). New. */
        PASS_UNTIL_NEXT_TURN,
        /** Pass until my own next turn: YieldUpdate.SetMarker(local player, UPKEEP, ...). New. */
        PASS_UNTIL_MY_TURN,
        /** Toggle auto-pass / stop every yield: YieldController.toggleAutoPassOrStopAll. (MatchScreen: P.) */
        AUTO_PASS,
        /** Attack with everything: alphaStrike. (MatchScreen: A.) */
        ALPHA_STRIKE,
        /** Undo the last action. (MatchScreen: Ctrl+Z.) */
        UNDO,
        /** Always yes for the ability on top of the stack. (MatchScreen: Y.) */
        AUTO_YIELD_YES,
        /** Always no for the ability on top of the stack. (MatchScreen: N.) */
        AUTO_YIELD_NO,
        /** Open the game log. New. */
        OPEN_LOG,
        /** Open the stack. New. */
        OPEN_STACK,
        /** Zoom the card under the pointer. New (Ctrl+Z stays undo). */
        ZOOM_CARD,
        /** Open the game menu / options. New. */
        OPEN_MENU,
        /** Show this list of shortcuts. New. */
        SHOW_SHORTCUTS,
        /** Concede the game. (MatchScreen: Ctrl+Q.) */
        CONCEDE
    }

    /** One key binding. */
    public static final class Shortcut {
        public final Action action;
        /** libGDX {@link Keys} code. */
        public final int keyCode;
        public final boolean ctrl, shift;
        /** Whether MatchScreen.keyDown already does this on this key (in both looks): do not wire it twice. */
        public final boolean existing;
        private final String labelKey, defaultDescription;

        Shortcut(final Action action, final int keyCode, final boolean ctrl, final boolean shift, final boolean existing,
                final String labelKey, final String defaultDescription) {
            this.action = action;
            this.keyCode = keyCode;
            this.ctrl = ctrl;
            this.shift = shift;
            this.existing = existing;
            this.labelKey = labelKey;
            this.defaultDescription = defaultDescription;
        }

        /** The key as it is printed on a keyboard: "Space", "Esc", "Ctrl+Z", "F3". */
        public String keyLabel() {
            String k;
            switch (keyCode) {
                case Keys.SPACE: k = "Space"; break;
                case Keys.ESCAPE: k = "Esc"; break;
                case Keys.ENTER: k = "Enter"; break;
                default: k = Keys.toString(keyCode); break;
            }
            if (k == null) {
                k = "?";
            }
            return (ctrl ? "Ctrl+" : "") + (shift ? "Shift+" : "") + k;
        }

        /** What it does, in the player's language (English when there is no translation). */
        public String description() {
            return Forge.getLocalizer() == null ? defaultDescription
                    : Forge.getLocalizer().getMessageorUseDefault(labelKey, defaultDescription);
        }

        /** Whether this key event (with the modifiers held now) is this shortcut. */
        public boolean matches(final int code) {
            return code == keyCode && ctrl == KeyInputAdapter.isCtrlKeyDown() && shift == KeyInputAdapter.isShiftKeyDown();
        }

        @Override
        public String toString() {
            return keyLabel() + " - " + defaultDescription;
        }
    }

    private static final List<Shortcut> ALL;

    static {
        final List<Shortcut> l = new ArrayList<>();
        l.add(new Shortcut(Action.OK, Keys.SPACE, false, false, true, "lblArenaKeyOk", "Pass priority / OK"));
        l.add(new Shortcut(Action.OK, Keys.ENTER, false, false, true, "lblArenaKeyOk", "Pass priority / OK"));
        l.add(new Shortcut(Action.CANCEL, Keys.ESCAPE, false, false, true, "lblArenaKeyCancel", "Cancel"));
        l.add(new Shortcut(Action.END_TURN, Keys.E, false, false, true, "lblArenaKeyEndTurn", "End turn (pass until end of turn)"));
        l.add(new Shortcut(Action.PASS_UNTIL_STACK_RESOLVES, Keys.F2, false, false, false, "lblArenaKeyPassStack", "Pass until the stack resolves"));
        l.add(new Shortcut(Action.PASS_UNTIL_NEXT_TURN, Keys.F3, false, false, false, "lblArenaKeyPassNextTurn", "Pass until the next turn"));
        l.add(new Shortcut(Action.PASS_UNTIL_MY_TURN, Keys.F4, false, false, false, "lblArenaKeyPassMyTurn", "Pass until my next turn"));
        l.add(new Shortcut(Action.AUTO_PASS, Keys.P, false, false, true, "lblArenaKeyAutoPass", "Auto-pass on / stop all passing"));
        l.add(new Shortcut(Action.ALPHA_STRIKE, Keys.A, false, false, true, "lblArenaKeyAlphaStrike", "Attack with everything"));
        l.add(new Shortcut(Action.UNDO, Keys.Z, true, false, true, "lblArenaKeyUndo", "Undo"));
        l.add(new Shortcut(Action.AUTO_YIELD_YES, Keys.Y, false, false, true, "lblArenaKeyYieldYes", "Always yes to this ability"));
        l.add(new Shortcut(Action.AUTO_YIELD_NO, Keys.N, false, false, true, "lblArenaKeyYieldNo", "Always no to this ability"));
        l.add(new Shortcut(Action.OPEN_LOG, Keys.L, false, false, false, "lblArenaKeyLog", "Open the game log"));
        l.add(new Shortcut(Action.OPEN_STACK, Keys.S, false, false, false, "lblArenaKeyStack", "Open the stack"));
        l.add(new Shortcut(Action.ZOOM_CARD, Keys.Z, false, false, false, "lblArenaKeyZoom", "Zoom the card under the pointer"));
        l.add(new Shortcut(Action.OPEN_MENU, Keys.M, false, false, false, "lblArenaKeyMenu", "Open the menu and options"));
        l.add(new Shortcut(Action.SHOW_SHORTCUTS, Keys.H, false, false, false, "lblArenaKeyHelp", "Show keyboard shortcuts"));
        l.add(new Shortcut(Action.CONCEDE, Keys.Q, true, false, true, "lblArenaKeyConcede", "Concede the game"));
        ALL = Collections.unmodifiableList(l);
    }

    /** Every binding, in the order to list them (an action may have more than one key, e.g. OK on Space and Enter). */
    public static List<Shortcut> all() {
        return ALL;
    }

    /** The first (main) binding of an action. */
    public static Shortcut get(final Action action) {
        for (final Shortcut s : ALL) {
            if (s.action == action) {
                return s;
            }
        }
        throw new IllegalArgumentException(String.valueOf(action));
    }

    /** All bindings of an action. */
    public static List<Shortcut> keysFor(final Action action) {
        final List<Shortcut> out = new ArrayList<>(2);
        for (final Shortcut s : ALL) {
            if (s.action == action) {
                out.add(s);
            }
        }
        return out;
    }

    /** The action this key (with the modifiers held now) is bound to, or null. */
    public static Action match(final int keyCode) {
        for (final Shortcut s : ALL) {
            if (s.matches(keyCode)) {
                return s.action;
            }
        }
        return null;
    }

    /** "Space / Enter" style label of all keys of an action. */
    public static String keyLabels(final Action action) {
        final StringBuilder sb = new StringBuilder();
        for (final Shortcut s : keysFor(action)) {
            if (sb.length() > 0) {
                sb.append(" / ");
            }
            sb.append(s.keyLabel());
        }
        return sb.toString();
    }
}
