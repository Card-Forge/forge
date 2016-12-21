package forge.game.keyword;

import forge.game.cost.Cost;

public class KeywordWithCostAndType extends KeywordInstance<KeywordWithCostAndType> {
    private Cost cost;
    private String type;

    @Override
    protected void parse(String details) {
        final String[] k = details.split(":");
        type = k[0];
        cost = new Cost(k[1], false);
    }

    @Override
    protected String formatReminderText(String reminderText) {
        return String.format(reminderText, cost.toSimpleString(), type);
    }
}
