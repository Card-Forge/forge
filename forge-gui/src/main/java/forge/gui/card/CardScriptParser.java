package forge.gui.card;

import java.util.Map;
import java.util.TreeMap;

/**
 * Error regions of a card script for the Card Workshop's syntax highlighting, from
 * {@link CardScriptLinter}'s ERRORs: the offending token where the linter names one, else the line.
 */
public final class CardScriptParser {

    private final String script;

    public CardScriptParser(final String script) {
        this.script = script;
    }

    /** Maps the start offset of each error region to its length. */
    public Map<Integer, Integer> getErrorRegions() {
        final Map<Integer, Integer> result = new TreeMap<>();
        // the text pane holds \n line ends, so offsets count one char per line break
        final String[] lines = script.split("\r?\n", -1);
        final int[] starts = new int[lines.length];
        for (int i = 1; i < lines.length; i++) {
            starts[i] = starts[i - 1] + lines[i - 1].length() + 1;
        }
        for (final CardScriptLinter.Finding f : new CardScriptLinter().lint(script)) {
            if (f.severity() != CardScriptLinter.Severity.ERROR) {
                continue;
            }
            final String line = lines[f.line() - 1];
            // a param finding names the key: find "Key$" before falling back to the bare token
            int at = f.token() == null ? -1 : line.indexOf(f.token() + "$");
            if (at < 0 && f.token() != null) {
                at = line.indexOf(f.token());
            }
            if (at >= 0) {
                result.put(starts[f.line() - 1] + at, f.token().length());
            } else {
                result.put(starts[f.line() - 1], line.length());
            }
        }
        return result;
    }
}
