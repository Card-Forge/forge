package forge.game.keyword;

public class Protection extends KeywordInstance<Protection> {
    private static final long serialVersionUID = 1369152260269203025L;

    private String fromWhat;

    @Override
    protected void parse(String details) {
    }

    @Override
    protected String formatReminderText(String reminderText) {
        return String.format(reminderText, fromWhat);
    }
}
