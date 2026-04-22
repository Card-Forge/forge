package forge.game.keyword;

public class Protection extends KeywordInstance<Protection> {
    private String fromWhat = "";

    @Override
    public String getTitle() {
        return "Protection from " + fromWhat;
    }

    @Override
    protected void parse(String details) {
        fromWhat = details.startsWith("from ") ? details.substring(5) : details;
        // Details may use "type:description" format (e.g. "Card.MultiColor:multicolored",
        // "instants:instants") — use the description part for display
        final int colon = fromWhat.indexOf(':');
        if (colon >= 0) {
            fromWhat = fromWhat.substring(colon + 1);
        }
    }

    @Override
    protected String formatReminderText(String reminderText) {
        return String.format(reminderText, fromWhat);
    }
}
