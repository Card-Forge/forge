package forge.game.keyword;

import forge.game.cost.Cost;

public class KeywordWithCost extends KeywordInstance<KeywordWithCost> implements KeywordWithCostInterface {

    protected Cost cost;
    protected String costString;

    @Override
    public Cost getCost() {
        if ("ManaCost".equals(costString)) {
            return new Cost(this.getHostCard().getManaCost(), false);
        }
        return cost;
    }
    @Override
    public String getCostString() { return costString; }

    @Override
    protected void parse(String details) {
        String[] allDetails = details.split(":");
        costString = allDetails[0].split("\\|", 2)[0].trim();
        if (!"ManaCost".equals(costString)) {
            cost = new Cost(costString, true);
        }
    }

    @Override
    public String costReminderText() {
        // granted without a cost, it uses the card's mana cost
        if (costString.isEmpty()) {
            return new Cost(getHostCard().getManaCost(), false).toSimpleString();
        }
        return KeywordWithCostInterface.super.costReminderText();
    }

    @Override
    protected String formatReminderText(String reminderText) {
        // some reminder does not contain cost
        if (reminderText.contains("%")) {
            return String.format(reminderText, costReminderText());
        }
        return reminderText;
    }
}
