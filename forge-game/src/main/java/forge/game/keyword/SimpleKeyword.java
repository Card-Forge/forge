package forge.game.keyword;

public class SimpleKeyword extends KeywordInstance<SimpleKeyword> {

    public String getTitle() {
        return getKeyword().toString();
    }

    @Override
    protected void parse(String details) {
        //don't need to merge details for simple keywords
    }

    @Override
    protected String formatReminderText(String reminderText) {
        return reminderText;
    }
}
