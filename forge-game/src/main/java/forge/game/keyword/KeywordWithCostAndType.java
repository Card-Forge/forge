package forge.game.keyword;

import forge.game.cost.Cost;

public class KeywordWithCostAndType extends KeywordInstance<KeywordWithCostAndType> {
    private static final long serialVersionUID = -5457093564089570731L;

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
