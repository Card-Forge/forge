package forge.game.keyword;

public class Hexproof extends KeywordWithType {

    @Override
    protected String formatReminderText(String reminderText) {
        if (type.isEmpty()) {
            return "This can't be the target of spells or abilities your opponents control.";
        }
        return String.format(reminderText, type);
    }
}
