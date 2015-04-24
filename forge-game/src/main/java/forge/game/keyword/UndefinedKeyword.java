package forge.game.keyword;

public class UndefinedKeyword extends KeywordInstance<UndefinedKeyword> {
    private static final long serialVersionUID = 1016294080901302718L;

    private String keyword;

    @Override
    protected void parse(String details) {
        keyword = details;
    }

    @Override
    protected String formatReminderText(String reminderText) {
        return reminderText;
    }

    @Override
    public final String toString() {
        return keyword;
    }
}
