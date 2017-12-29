package forge.game.keyword;

import forge.game.cost.Cost;

public class KeywordWithCostAndAmount extends KeywordInstance<KeywordWithCostAndAmount> {
    private Cost cost;
    private boolean withX;
    private int amount;

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    protected void parse(String details) {
        final String[] k = details.split(":");
        if (k[0].startsWith("X")) {
            withX = true;
        } else {
            amount = Integer.parseInt(k[0]);
        }
        cost = new Cost(k[1].split("\\|", 2)[0].trim(), false);
    }

    @Override
    protected String formatReminderText(String reminderText) {
        String formatStr = reminderText;
        if (withX) {
            formatStr = reminderText.replaceAll("\\%(\\d+\\$)?d", "%$1s");
        }
        return String.format(formatStr, cost.toSimpleString(), withX ? "X" : amount);
    }
}
