package forge.game.keyword;

import java.util.List;
import java.util.StringJoiner;

import org.testng.collections.Lists;

import forge.game.cost.Cost;
import forge.util.Lang;

public class Ward extends KeywordWithCost {

    List<Cost> costs = Lists.newArrayList();

    @Override
    protected void parse(String details) {
        String[] allDetails = details.split(":");

        if (allDetails.length > 1) {
            for (String costStr : allDetails) {
                costs.add(new Cost(costStr, true));
            }
        } else {
            super.parse(details);
        }
    }

    @Override
    public boolean isComplexCost() {
        return !costs.isEmpty() || super.isComplexCost();
    }

    @Override
    public String getTitleCost() {
        if (costs.isEmpty()) {
            return super.getTitleCost();
        }

        return Lang.joinHomogenous(costs, c -> (c.isOnlyManaCost() ? "pay " : "") + c.toSimpleString(), "or");
    }

    @Override
    public String costReminderText() {
        if (costs.isEmpty()) {
            return costToReminderDesc(getCost());
        }

        StringJoiner sj = new StringJoiner(" or ");
        for (Cost c : costs) {
            sj.add(costToReminderDesc(c));
        }

        return sj.toString();
    }

    protected String costToReminderDesc(Cost c) {
        String costString = c.toSimpleString();
        if (costString.startsWith("Pay ")) {
            costString = costString.replace("Pay ", "pays");
        } else if (costString.startsWith("Discard ")) {
            costString = costString.replace("Discard", "discards");
        } else if (c.isOnlyManaCost()) {
            costString = "pays " + costString;
        }
        return costString;
    }
}
