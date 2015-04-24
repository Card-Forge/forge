package forge.game.keyword;

public class KeywordWithType extends KeywordInstance<KeywordWithType> {
    private static final long serialVersionUID = 4959671775512932812L;

    private String type;

    @Override
    protected void parse(String details) {
    }

    @Override
    protected String formatReminderText(String reminderText) {
        return String.format(reminderText, type);
    }
}
