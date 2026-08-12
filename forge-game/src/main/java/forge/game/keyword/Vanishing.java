package forge.game.keyword;

public class Vanishing extends KeywordWithAmount {

    boolean withoutAmount = false;

    public String getTitle() {
        if (withoutAmount) {
            return getKeyword().toString();
        }
        return super.getTitle();
    }

    @Override
    protected void parse(String details) {
        if ("".equals(details)) {
            withoutAmount = true;
        } else {
            super.parse(details);
        }
    }

    @Override
    protected String formatReminderText(String reminderText) {
        if (withoutAmount) {
            return "At the beginning of your upkeep, remove a time counter from this enchantment. When the last is removed, sacrifice it.";
        } else {
            return super.formatReminderText(reminderText);
        }
    }
}
