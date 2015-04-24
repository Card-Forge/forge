package forge.game.keyword;

import forge.game.cost.Cost;

public class KeywordWithCostAndAmount extends KeywordInstance<KeywordWithCostAndAmount> {
    private static final long serialVersionUID = 1905194735777019844L;

    private Cost cost;
    private int amount;

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    protected void parse(String details) {
    }

    @Override
    protected String formatReminderText(String reminderText) {
        return String.format(reminderText, cost.toSimpleString(), amount);
    }
}
