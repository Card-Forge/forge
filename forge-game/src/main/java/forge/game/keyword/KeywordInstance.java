package forge.game.keyword;

public abstract class KeywordInstance<T extends KeywordInstance<?>> {
    public int getAmount() {
        return 1;
    }
    protected abstract void parse(String details);
    protected abstract String formatReminderText(String reminderText);
}
