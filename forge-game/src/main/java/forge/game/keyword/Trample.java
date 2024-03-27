package forge.game.keyword;

public class Trample extends KeywordWithType {
    @Override
    protected String formatReminderText(String reminderText) {
        if (!type.isEmpty()) {
            return "This creature can deal excess combat damage to the controller of the planeswalker it's attacking.";
        }
        return reminderText;
    }
}
