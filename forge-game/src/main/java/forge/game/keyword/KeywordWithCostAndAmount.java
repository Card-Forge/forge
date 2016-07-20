package forge.game.keyword;

import forge.game.cost.Cost;

public class KeywordWithCostAndAmount extends KeywordInstance<KeywordWithCostAndAmount> {
    private Cost cost;
    private int amount;

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    protected void parse(String details) {
        final String[] k = details.split(":");
        amount = Integer.parseInt(k[0]);
        cost = new Cost(k[1].split("\\|", 2)[0].trim(), false);
    }

    @Override
    protected String formatReminderText(String reminderText) {
        return String.format(reminderText, cost.toSimpleString(), amount);
    }
}
