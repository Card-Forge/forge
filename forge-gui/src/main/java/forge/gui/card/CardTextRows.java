package forge.gui.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import forge.game.card.CardView.CardStateView;

/**
 * Splits the rules text of a card into rows for the card image renderers, so that the parts of the text that are
 * printed with a marker are drawn the way they are printed: the marker as a badge on the left of the text box and
 * the rest as the text of its row.
 * <ul>
 * <li>Station levels: "6+" and, for a level with a power/toughness box, that P/T on the right.</li>
 * <li>Planeswalker loyalty abilities: "+1", "0", "-X".</li>
 * <li>Saga chapters: "I", "I, II".</li>
 * <li>Class levels: the level, the line with the cost to reach it stays in the text of the row.</li>
 * <li>Leveler ranges: "1-2", "3+" and the P/T of that range on the right.</li>
 * </ul>
 *
 * <p>The badge, and the P/T of a Station or Leveler row, are removed from the text of their row.
 * The input can be either the Oracle text ("STATION 6+" on its own line, "[-1]: ...") or the
 * description of the abilities ("STATION 6+ text [5/5]", "-1: ...", "LEVEL 1-2 2/3 text").</p>
 */
public final class CardTextRows {
    private CardTextRows() { }

    public enum Kind { PLAIN, STATION, LOYALTY, SAGA, CLASS, LEVEL }

    public static final class Row {
        public final Kind kind;
        /** "6+" for a Station level, "+1", "0", "-X" for a loyalty ability, "I, II" for a Saga chapter,
         * "2" for a Class level, "1-2" for a Leveler range, null for a plain paragraph. */
        public final String badge;
        /** "5/5" for a Station level or a Leveler range with a P/T box, otherwise null. */
        public final String pt;
        public final String text;
        public final boolean isStation;

        private Row(Kind kind, String badge, String pt, String text) {
            this.kind = kind;
            this.badge = badge;
            this.pt = pt;
            this.text = text;
            this.isStation = kind == Kind.STATION;
        }

        public boolean hasBadge() {
            return badge != null;
        }

        /** True if the badge is drawn as a circle: the threshold of a Station and the level of a Class. */
        public boolean isRoundBadge() {
            return kind == Kind.STATION || kind == Kind.CLASS;
        }

        /** The badges of the row: one for every chapter of a Saga row ("I, II"), otherwise the badge itself. */
        public List<String> badgeParts() {
            if (badge == null) {
                return Collections.emptyList();
            }
            return kind == Kind.SAGA ? splitChapters(badge) : Collections.singletonList(badge);
        }

        /**
         * @return for a Class level: the cost with its colon, the level and the text of the level, see
         * {@link CardTextRows#splitClassRow(String)}. Otherwise null.
         */
        public String[] classHeader() {
            return kind == Kind.CLASS ? splitClassRow(text) : null;
        }
    }

    private static final Pattern LINE_BREAK = Pattern.compile("\\r?\\n");
    // "[+1]: text", "[-X]: text", "[0]: text", "+1: text". U+2212 and U+2013 are used as minus in some Oracle texts.
    private static final Pattern LOYALTY = Pattern.compile("^\\[?([+\\-−–]?(?:\\d+|X))\\]?:\\s*(.*)$", Pattern.DOTALL);
    // "STATION 6+ text" as printed, or "{6+} text" as some scripts write the level
    private static final Pattern STATION = Pattern.compile("^(?:STATION\\s+|\\{)(\\d+)\\+\\}?\\s*(.*)$", Pattern.DOTALL);
    private static final Pattern TRAILING_PT = Pattern.compile("\\s*\\[(\\d+)/(\\d+)]\\s*$");
    private static final Pattern BARE_PT = Pattern.compile("^(\\d+)/(\\d+)$");
    // "I — text", "I, II — text", "IV — text" (the dash is an em dash in the scripts)
    private static final Pattern SAGA_CHAPTER = Pattern.compile("^([IVX]+(?:\\s*,\\s*[IVX]+)*)\\s*[—–-]\\s*(.*)$", Pattern.DOTALL);
    // "{W}: Level 2", "{4}{W}: Level 3": the cost to reach a level of a Class, followed by the text of that level
    private static final Pattern CLASS_LEVEL = Pattern.compile("^(.+?):\\s*Level\\s+(\\d+)\\s*$");
    // "LEVEL 1-2", "LEVEL 3+", with the P/T and the text of the level on the same line in the description of the abilities
    private static final Pattern LEVEL = Pattern.compile("^LEVEL\\s+(\\d+(?:-\\d+|\\+))(?:\\s+(\\d+/\\d+))?(?:\\s+(.*))?$", Pattern.DOTALL);
    // "Station (Tap another creature you control: ...)", the keyword itself, as opposed to "STATION 12+"
    private static final Pattern KEYWORD_LINE = Pattern.compile("^Station\\b(?!\\s+\\d)");

    /**
     * @return the rows of the text, or an empty list if the text has nothing that is drawn with a badge.
     * In that case the text should be drawn the usual way.
     */
    public static List<Row> parse(CardStateView state, String text) {
        if (state == null || text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        // ",,," is a line break in the description of the abilities
        final String[] lines = LINE_BREAK.split(text.replace(",,,", "\n"));
        final boolean station = state.isSpaceCraft() || !state.getStationLevels().isEmpty(); // Planets have Station too
        final boolean loyalty = state.isPlaneswalker();
        final boolean saga = state.getType().hasSubtype("Saga");
        final boolean classCard = state.getType().hasSubtype("Class");
        final boolean leveler = hasLevelLine(lines);
        if (!station && !loyalty && !saga && !classCard && !leveler) {
            return Collections.emptyList();
        }

        final List<Row> rows = new ArrayList<>();
        Section current = null;
        boolean hasBadge = false;
        for (String raw : lines) {
            final String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (loyalty) {
                final Matcher m = LOYALTY.matcher(line);
                if (m.matches()) {
                    if (current != null) {
                        rows.add(current.build(state));
                        current = null;
                    }
                    rows.add(new Row(Kind.LOYALTY, normalizeMinus(m.group(1)), null, m.group(2).trim()));
                    hasBadge = true;
                    continue;
                }
            }

            Section start = null;
            Matcher m;
            if (station && (m = STATION.matcher(line)).matches()) {
                start = new Section(Kind.STATION, m.group(1) + "+", Integer.parseInt(m.group(1)));
                start.addLine(m.group(2));
            } else if (saga && (m = SAGA_CHAPTER.matcher(line)).matches()) {
                start = new Section(Kind.SAGA, m.group(1).replaceAll("\\s*,\\s*", ", "), 0);
                start.addLine(m.group(2));
            } else if (classCard && (m = CLASS_LEVEL.matcher(line)).matches()) {
                // the line with the cost stays in the text, only the level gets a badge
                start = new Section(Kind.CLASS, m.group(2), 0);
                start.addLine(line);
            } else if (leveler && (m = LEVEL.matcher(line)).matches()) {
                start = new Section(Kind.LEVEL, m.group(1), 0);
                start.pt = m.group(2);
                start.addLine(m.group(3) == null ? "" : m.group(3));
            }
            if (start != null) {
                if (current != null) {
                    rows.add(current.build(state));
                }
                current = start;
                hasBadge = true;
                continue;
            }

            if (current != null) {
                if (current.kind == Kind.STATION && KEYWORD_LINE.matcher(line).find()) {
                    // the reminder of the Station keyword is not part of the level before it: close the row
                    rows.add(current.build(state));
                    current = null;
                } else {
                    if ((current.kind == Kind.STATION || current.kind == Kind.LEVEL) && BARE_PT.matcher(line).matches()) {
                        current.pt = line;
                    } else {
                        current.addLine(line);
                    }
                    continue;
                }
            }
            rows.add(new Row(Kind.PLAIN, null, null, line));
        }
        if (current != null) {
            rows.add(current.build(state));
        }
        return hasBadge ? rows : Collections.<Row>emptyList();
    }

    /**
     * Splits the text of a Class level, which starts with the line with the cost to reach the level.
     *
     * @return {"{1}{B}:", "Level 2", the rest of the text}, or null if the first line is not a cost to reach a level
     */
    public static String[] splitClassRow(String text) {
        final int lineEnd = text.indexOf('\n');
        final String first = lineEnd < 0 ? text : text.substring(0, lineEnd);
        final Matcher m = CLASS_LEVEL.matcher(first.trim());
        if (!m.matches()) {
            return null;
        }
        return new String[] { m.group(1).trim() + ":", "Level " + m.group(2), lineEnd < 0 ? "" : text.substring(lineEnd + 1).trim() };
    }

    /** The chapters of a Saga badge: "I, II" gives "I" and "II". */
    public static List<String> splitChapters(String badge) {
        final List<String> chapters = new ArrayList<>();
        for (String chapter : badge.split("\\s*,\\s*")) {
            if (!chapter.isEmpty()) {
                chapters.add(chapter);
            }
        }
        return chapters;
    }

    private static boolean hasLevelLine(String[] lines) {
        for (String line : lines) {
            if (LEVEL.matcher(line.trim()).matches()) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeMinus(String s) {
        return s.replace('−', '-').replace('–', '-');
    }

    /** A badge and everything that belongs to it: the lines of text up to the next badge. */
    private static final class Section {
        private final Kind kind;
        private final String badge;
        private final int threshold; // Station only
        private final StringBuilder text = new StringBuilder();
        private String pt;

        private Section(Kind kind, String badge, int threshold) {
            this.kind = kind;
            this.badge = badge;
            this.threshold = threshold;
        }

        private void addLine(String line) {
            line = line.trim();
            if (kind == Kind.STATION) {
                final Matcher m = TRAILING_PT.matcher(line);
                if (m.find()) { // "[7/15]" ends a line of the level, not only the last one
                    if (pt == null) {
                        pt = m.group(1) + "/" + m.group(2);
                    }
                    line = line.substring(0, m.start()).trim();
                }
            }
            if (line.isEmpty()) {
                return;
            }
            if (text.length() > 0) {
                text.append('\n');
            }
            text.append(line);
        }

        private Row build(CardStateView state) {
            String body = text.toString();
            if (kind == Kind.STATION) {
                final Matcher m = TRAILING_PT.matcher(body);
                if (m.find()) {
                    if (pt == null) {
                        pt = m.group(1) + "/" + m.group(2);
                    }
                    body = body.substring(0, m.start());
                }
                if (pt == null) {
                    pt = stationPt(state, threshold);
                }
                // a Station that is a creature shows its live P/T in the box of the card: the level would repeat it
                if (pt != null && state.isCreature() && pt.equals(state.getPower() + "/" + state.getToughness())) {
                    pt = null;
                }
            }
            return new Row(kind, badge, pt, body.trim());
        }
    }

    /** The P/T of a Station level as given by the state, see {@link CardStateView#getStationLevels()}. */
    private static String stationPt(CardStateView state, int threshold) {
        final String levels = state.getStationLevels();
        if (levels == null || levels.isEmpty()) {
            return null;
        }
        for (String level : levels.split(";")) {
            final String[] parts = level.split(":", -1);
            if (parts.length == 3 && parts[0].equals(String.valueOf(threshold))
                    && !parts[1].isEmpty() && !parts[2].isEmpty()) {
                return parts[1] + "/" + parts[2];
            }
        }
        return null;
    }
}
