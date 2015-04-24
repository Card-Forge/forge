package forge.game.keyword;

public class KeywordWithType extends KeywordInstance<KeywordWithType> {
    private String type;

    @Override
    protected void parse(String details) {
    }

    @Override
    protected String formatReminderText(String reminderText) {
        return String.format(reminderText, type);
    }
}
