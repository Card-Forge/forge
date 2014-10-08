package forge.game.keyword;

public class Cycling extends KeywordWithCost {
    private String type;

    @Override
    protected void parse(String details) {
    }

    @Override
    protected String formatReminderText(String reminderText) {
        if (type == null) {
            return super.formatReminderText(reminderText);
        }
        //handle special case of type cycling
        return super.formatReminderText("%s, Discard this card: Search your library for a " + type + " card, reveal it, and put it into your hand. Then shuffle your library.");
    }
}
