package forge.game.keyword;

public class SimpleKeyword extends KeywordInstance<SimpleKeyword> {
    private static final long serialVersionUID = -4662161707875676173L;

    @Override
    protected void parse(String details) {
        //don't need to merge details for simple keywords
    }

    @Override
    protected String formatReminderText(String reminderText) {
        return reminderText;
    }
}
