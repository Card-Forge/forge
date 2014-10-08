package forge.game.keyword;

import forge.game.cost.Cost;

public class KeywordWithCostAndType extends KeywordInstance<KeywordWithCostAndType> {
    private Cost cost;
    private String type;

    @Override
    protected void parse(String details) {
    }

    @Override
    protected String formatReminderText(String reminderText) {
        return String.format(reminderText, cost.toSimpleString(), type);
    }
}
