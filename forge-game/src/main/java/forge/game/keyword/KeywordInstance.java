package forge.game.keyword;

public abstract class KeywordInstance<T extends KeywordInstance<?>> {
    private Keyword keyword;

    public Keyword getKeyword() {
        return keyword;
    }
    public String getReminderText() {
        return formatReminderText(keyword.reminderText);
    }
    public int getAmount() {
        return 1;
    }
    protected void initialize(Keyword keyword0, String details) {
        keyword = keyword0;
        parse(details);
    }
    protected abstract void parse(String details);
    protected abstract String formatReminderText(String reminderText);
}
