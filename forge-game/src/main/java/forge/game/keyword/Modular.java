package forge.game.keyword;

public class Modular extends KeywordWithAmount {
    private boolean sunburst = false;

    @Override
    protected void parse(String details) {
        if ("Sunburst".equals(details)) {
            sunburst = true;
        } else {
            super.parse(details);
        }
    }

    @Override
    protected String formatReminderText(String reminderText) {
        if (sunburst) {
            return "This enters the battlefield with a +1/+1 counter on it for each color of mana spent to cast it. When it dies, you may put its +1/+1 counters on target artifact creature.";
        } else {
            return super.formatReminderText(reminderText);
        }
    }
}
