package forge.game.keyword;

public class Mayhem extends KeywordWithCost {

    @Override
    public String getTitle() {
        if (cost == null) {
            return getTitleWithoutCost();
        }
        return super.getTitle();
    }

    @Override
    protected void parse(String details) {
        if (!details.isEmpty()) {
            super.parse(details);
        } else {
            this.cost = null;
        }
    }

    @Override
    protected String formatReminderText(String reminderText) {
        if (cost == null) {
            return "You may play this card from your graveyard if you discarded it this turn. Timing rules still apply.";
        }
        return super.formatReminderText(reminderText);
    }
}
