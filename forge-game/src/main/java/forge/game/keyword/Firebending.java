package forge.game.keyword;

public class Firebending extends KeywordWithAmount {

    @Override
    protected String formatReminderText(String reminderText) {
        String fire;
        if (withX) {
            fire = "X {R}";
        } else {
            fire = "{R}".repeat(amount);
        }
        return String.format(reminderText, fire);
    }
}
