package forge.game.keyword;

public class KeywordWithType extends KeywordInstance<KeywordWithType> {
    private String type;

    @Override
    protected void parse(String details) {
        if ("Creature".equals(details)) {
            type = "creature";
        } else if (details.contains(":")) {
            type = details.split(":")[1];
        } else {
            type = details;
        }
    }

    @Override
    protected String formatReminderText(String reminderText) {
        return String.format(reminderText, type);
    }
}
