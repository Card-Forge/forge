package forge.game.keyword;

import forge.game.cost.Cost;

public class KeywordWithCost extends KeywordInstance<KeywordWithCost> {
    private Cost cost;

    @Override
    protected void parse(String details) {
        cost = new Cost(details.split("\\|", 2)[0].trim(), false);
    }

    @Override
    protected String formatReminderText(String reminderText) {
        // some reminder does not contain cost
        if (reminderText.contains("%")) {
            return String.format(reminderText, cost.toSimpleString());
        } else {
            return reminderText;
        }
    }
}
