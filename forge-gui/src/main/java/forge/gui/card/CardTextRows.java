package forge.gui.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import forge.game.card.CardView.CardStateView;

/**
 * Splits the rules text of a card into rows for the card image renderers, so that Station levels and
 * planeswalker loyalty abilities can be drawn the way they are printed: a badge on the left of the text
 * box ("6+" or "-1") and, for Station levels with a power/toughness box, that P/T on the right.
 *
 * <p>The badge, and the P/T of a Station level, are removed from the text of their row.
 * The input can be either the Oracle text ("STATION 6+" on its own line, "[-1]: ...") or the
 * description of the abilities ("STATION 6+ text [5/5]", "-1: ...").</p>
 */
public final class CardTextRows {
    private CardTextRows() { }

    public static final class Row {
        /** "6+" for a Station level, "+1", "0", "-X" for a loyalty ability, null for a plain paragraph. */
        public final String badge;
        /** "5/5" for a Station level with a P/T box, otherwise null. */
        public final String pt;
        public final String text;
        public final boolean isStation;

        private Row(String badge, String pt, String text, boolean isStation) {
            this.badge = badge;
            this.pt = pt;
            this.text = text;
            this.isStation = isStation;
        }

        public boolean hasBadge() {
            return badge != null;
        }
    }

    private static final Pattern LINE_BREAK = Pattern.compile("\\r?\\n");
    // "[+1]: text", "[-X]: text", "[0]: text", "+1: text". U+2212 and U+2013 are used as minus in some Oracle texts.
    private static final Pattern LOYALTY = Pattern.compile("^\\[?([+\\-−–]?(?:\\d+|X))\\]?:\\s*(.*)$", Pattern.DOTALL);
    // "STATION 6+ text" as printed, or "{6+} text" as some scripts write the level
    private static final Pattern STATION = Pattern.compile("^(?:STATION\\s+|\\{)(\\d+)\\+\\}?\\s*(.*)$", Pattern.DOTALL);
    private static final Pattern TRAILING_PT = Pattern.compile("\\s*\\[(\\d+)/(\\d+)]\\s*$");
    private static final Pattern BARE_PT = Pattern.compile("^(\\d+)/(\\d+)$");
    // "Station (Tap another creature you control: ...)", the keyword itself, as opposed to "STATION 12+"
    private static final Pattern KEYWORD_LINE = Pattern.compile("^Station\\b(?!\\s+\\d)");

    /**
     * @return the rows of the text, or an empty list if the card has no Station levels or loyalty abilities in
     * its text. In that case the text should be drawn the usual way.
     */
    public static List<Row> parse(CardStateView state, String text) {
        if (state == null || text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        final boolean station = state.isSpaceCraft() || !state.getStationLevels().isEmpty(); // Planets have Station too
        final boolean loyalty = state.isPlaneswalker();
        if (!station && !loyalty) {
            return Collections.emptyList();
        }

        final List<Row> rows = new ArrayList<>();
        StationBuilder current = null;
        boolean hasBadge = false;
        // ",,," is a line break in the description of the abilities
        for (String raw : LINE_BREAK.split(text.replace(",,,", "\n"))) {
            final String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (loyalty) {
                final Matcher m = LOYALTY.matcher(line);
                if (m.matches()) {
                    rows.add(new Row(normalizeMinus(m.group(1)), null, m.group(2).trim(), false));
                    hasBadge = true;
                    continue;
                }
            }
            if (station) {
                final Matcher m = STATION.matcher(line);
                if (m.matches()) {
                    if (current != null) {
                        rows.add(current.build(state));
                    }
                    current = new StationBuilder(Integer.parseInt(m.group(1)));
                    current.addLine(m.group(2));
                    hasBadge = true;
                    continue;
                }
                if (current != null) {
                    if (KEYWORD_LINE.matcher(line).find()) {
                        // the reminder of the Station keyword is not part of the level before it: close the row
                        rows.add(current.build(state));
                        current = null;
                    } else {
                        final Matcher pt = BARE_PT.matcher(line);
                        if (pt.matches()) {
                            current.pt = line;
                        } else {
                            current.addLine(line);
                        }
                        continue;
                    }
                }
            }
            rows.add(new Row(null, null, line, false));
        }
        if (current != null) {
            rows.add(current.build(state));
        }
        return hasBadge ? rows : Collections.<Row>emptyList();
    }

    private static String normalizeMinus(String s) {
        return s.replace('−', '-').replace('–', '-');
    }

    private static final class StationBuilder {
        private final int threshold;
        private final StringBuilder text = new StringBuilder();
        private String pt;

        private StationBuilder(int threshold) {
            this.threshold = threshold;
        }

        private void addLine(String line) {
            line = line.trim();
            final Matcher m = TRAILING_PT.matcher(line);
            if (m.find()) { // "[7/15]" ends a line of the level, not only the last one
                if (pt == null) {
                    pt = m.group(1) + "/" + m.group(2);
                }
                line = line.substring(0, m.start()).trim();
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
            return new Row(threshold + "+", pt, body.trim(), true);
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
