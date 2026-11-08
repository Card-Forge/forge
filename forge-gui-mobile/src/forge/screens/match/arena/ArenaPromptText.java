package forge.screens.match.arena;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import forge.Forge;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.phase.PhaseType;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.Selection;
import forge.screens.match.MatchController;
import forge.trackable.TrackableObject;
import forge.util.Localizer;

/**
 * The game's prompt texts in the words a player reads on the Arena board: the engine's internal ids gone ("Hill Giant
 * (50)" is "Hill Giant"), a line saying what to click, and buttons named for what they do ("No Attacks", "Attack with
 * 3", "No Blocks", "Cancel spell", "Pay automatically"). Only the Arena look uses it; texts are made when what they say
 * changes, not per frame.
 */
public final class ArenaPromptText {
    private ArenaPromptText() { }

    /** An engine id after a name: " (24)". */
    private static final Pattern IDS = Pattern.compile(" ?\\(\\d+\\)");
    private static Pattern chooseMode;
    private static final String NL = String.valueOf((char) 10); // a line break in the dialogs' text

    /** The text without the engine's ids, and a modal spell's "James activated X - Choose a mode" as "X: choose a mode". */
    public static String clean(final String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String s = IDS.matcher(text).replaceAll("");
        final Matcher m = chooseModePattern().matcher(s);
        if (m.matches()) {
            s = Forge.getLocalizer().getMessage("lblArenaChooseModeFor", m.group(2));
        }
        // a vote's running tally, "Current Votes:{Hill Giant=[James, Vivien]}", as one line per choice with its voters
        final String tally = Forge.getLocalizer().getMessage("lblCurrentVote") + ":{";
        final int at = s.indexOf(tally);
        if (at >= 0 && s.endsWith("}")) {
            final String body = s.substring(at + tally.length(), s.length() - 1).replace("=[", ": ").replace("], ", NL).replace("]", "");
            s = s.substring(0, at) + Forge.getLocalizer().getMessage("lblArenaVotesSoFar") + NL + body;
        }
        return s;
    }

    /** The localized "{0} activated {1} - Choose a mode" as a pattern (built once). */
    private static Pattern chooseModePattern() {
        if (chooseMode == null) {
            final String marker0 = String.valueOf((char) 1), marker1 = String.valueOf((char) 2);
            final String template = Forge.getLocalizer().getMessage("lblPlayerActivatedCardChooseMode", marker0, marker1);
            chooseMode = Pattern.compile(Pattern.quote(template).replace(marker0, "\\E(.+?)\\Q").replace(marker1, "\\E(.+)\\Q"));
        }
        return chooseMode;
    }

    // ---- what to click ----------------------------------------------------------------------------------------------

    private static final Map<String, String> LINES = new HashMap<>();

    private static String line(final String key) {
        String s = LINES.get(key);
        if (s == null) {
            s = Forge.getLocalizer().getMessage(key);
            LINES.put(key, s);
        }
        return s;
    }

    /**
     * The prompt's words for this selection: the engine's message cleaned, then a line saying what to click (attackers,
     * blockers, highlighted cards or players, paying mana). Called when the message or the selection changes.
     */
    public static String promptText(final String message, final Selection sel, final String okText) {
        if (!sel.defenders().isEmpty()) { // declaring attackers: one plain sentence instead of the engine's
            final String who = currentDefenderName(sel);
            return Forge.getLocalizer().getMessage(sel.defenders().size() > 1 ? "lblArenaGuideAttackChoose" : "lblArenaGuideAttack",
                    who == null ? "" : who);
        }
        if (isDeclaringBlockers() && !sel.chosen().isEmpty() && sel.chosen().get(0) instanceof CardView attacker) {
            final String name = forge.util.CardTranslation.getTranslatedName(attacker.getName());
            if (!name.equals(blockingFor)) { // made when the attacker you assign blockers to changes
                blockingFor = name;
                blockingText = Forge.getLocalizer().getMessage("lblArenaGuideBlocking", name);
            }
            return blockingText;
        }
        final String clean = clean(message);
        final String guide = guidance(sel, okText);
        if (guide == null) {
            return clean;
        }
        return clean == null || clean.isEmpty() ? guide : clean + "\n" + guide;
    }

    private static String guidance(final Selection sel, final String okText) {
        final Localizer l = Forge.getLocalizer();
        if (isDeclaringBlockers()) {
            return line("lblArenaGuideBlock");
        }
        if (l.getMessage("lblAuto").equals(okText)) {
            return line("lblArenaGuidePay");
        }
        boolean cards = !sel.cards().isEmpty(), players = false, stack = false;
        for (final TrackableObject t : sel.targets()) {
            if (t instanceof PlayerView) {
                players = true;
            } else if (t instanceof CardView) {
                cards = true;
            } else {
                stack = true;
            }
        }
        if (stack) {
            return line("lblArenaGuideStack");
        }
        if (cards && players) {
            return line("lblArenaGuideCardOrPlayer");
        }
        if (players) {
            return line("lblArenaGuidePlayer");
        }
        return cards ? line("lblArenaGuideCard") : null;
    }

    private static String blockingFor, blockingText;

    /** The defender your clicks send attackers to now (the one the prompt marks), by name, or null. */
    private static String currentDefenderName(final Selection sel) {
        for (final Selection.DefenderOption o : sel.defenders()) {
            if (sel.chosen().contains(o.defender())) {
                if (o.defender() instanceof PlayerView p) {
                    return p.getName();
                }
                return o.defender() instanceof CardView c ? forge.util.CardTranslation.getTranslatedName(c.getName()) : null;
            }
        }
        return null;
    }

    /** Whether you are declaring blockers now (the defending player's declare-blockers step, the prompt waiting on you). */
    public static boolean isDeclaringBlockers() {
        final GameView game = MatchController.instance.getGameView();
        return game != null && game.getPhase() == PhaseType.COMBAT_DECLARE_BLOCKERS && !ArenaBoard.isMe(game.getPlayerTurn())
                && game.getCombat() != null;
    }

    // ---- what the buttons do -----------------------------------------------------------------------------------------

    private static final Map<Integer, String> ATTACK_WITH = new HashMap<>();
    private static final Map<Long, String> ATTACK_WITH_PAY = new HashMap<>();

    /**
     * What the OK button says on the Arena board: "No Attacks" / "Attack with 3" (with what it costs when attacking
     * costs mana: "Attack with 3 · Pay 6"), "No Blocks" / "Done" while blocking, "Pay automatically" for the engine's
     * "Auto"; otherwise the engine's own word.
     */
    public static String okLabel(final String engineText) {
        final Localizer l = Forge.getLocalizer();
        if (!l.getMessage("lblOK").equals(engineText)) {
            return l.getMessage("lblAuto").equals(engineText) ? line("lblArenaPayAutomatically") : engineText;
        }
        final Selection sel = MatchController.instance.getSelection();
        if (!sel.defenders().isEmpty()) {
            final int n = ArenaBoard.myAttackerCount();
            if (n == 0) {
                return line("lblArenaNoAttacks");
            }
            final int cost = ArenaBoard.myAttackCost();
            if (cost > 0) {
                final long key = ((long) n << 32) | cost;
                String s = ATTACK_WITH_PAY.get(key);
                if (s == null) {
                    s = l.getMessage("lblArenaAttackWithPay", String.valueOf(n), String.valueOf(cost));
                    ATTACK_WITH_PAY.put(key, s);
                }
                return s;
            }
            String s = ATTACK_WITH.get(n);
            if (s == null) {
                s = l.getMessage("lblArenaAttackWith", String.valueOf(n));
                ATTACK_WITH.put(n, s);
            }
            return s;
        }
        if (isDeclaringBlockers()) {
            return line(ArenaBoard.myBlockerCount() == 0 ? "lblArenaNoBlocks" : "lblArenaBlocksDone");
        }
        return engineText;
    }

    /** What the Cancel button says: "Cancel spell" while casting a spell (paying for it, choosing its targets). */
    public static String cancelLabel(final String engineText, final CardView promptCard) {
        final Localizer l = Forge.getLocalizer();
        if (promptCard != null && l.getMessage("lblCancel").equals(engineText) && promptCard.getZone() != ZoneType.Battlefield) {
            return line("lblArenaCancelSpell");
        }
        return engineText;
    }
}
