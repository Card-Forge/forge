package forge.game.keyword;

import forge.game.cost.Cost;

public class KeywordWithCost extends KeywordInstance<KeywordWithCost> {
    protected Cost cost;

    @Override
    protected void parse(String details) {
        cost = new Cost(details.split("\\|", 2)[0].trim(), false);
    }

    @Override
    protected String formatReminderText(String reminderText) {
        // some reminder does not contain cost
        if (reminderText.contains("%")) {
            String costString = cost.toSimpleString();
            if (reminderText.contains("pays %")) {
                if (costString.startsWith("Pay ")) {
                    costString = costString.substring(4);
                } else if (costString.startsWith("Discard ")) {
                    reminderText = reminderText.replace("pays", "");
                    costString = costString.replace("Discard", "discards");
                }
            }
            return String.format(reminderText, costString);
        } else {
            return reminderText;
        }
    }
}
