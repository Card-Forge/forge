package forge.game.keyword;

public class Partner extends SimpleKeyword {

    private String with = null;

    @Override
    protected void parse(String details) {
        with = details;
    }

    @Override
    protected String formatReminderText(String reminderText) {
        if (with == null) {
            return reminderText;
        } else {
            return "You can have two commanders if both have this ability.";
        }
    }
}
