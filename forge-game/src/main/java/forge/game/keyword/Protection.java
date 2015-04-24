package forge.game.keyword;

public class Protection extends KeywordInstance<Protection> {
    private String fromWhat;

    @Override
    protected void parse(String details) {
    }

    @Override
    protected String formatReminderText(String reminderText) {
        return String.format(reminderText, fromWhat);
    }
}
