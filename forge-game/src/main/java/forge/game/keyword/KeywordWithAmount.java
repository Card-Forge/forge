package forge.game.keyword;

public class KeywordWithAmount extends KeywordInstance<KeywordWithAmount> {
    private static final long serialVersionUID = -6408982505204494940L;

    private int amount;

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    protected void parse(String details) {
    }

    @Override
    protected String formatReminderText(String reminderText) {
        return String.format(reminderText, amount);
    }
}
