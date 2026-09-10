package forge.game.keyword;

import java.util.Map;

import com.google.common.collect.Maps;

import forge.game.cost.Cost;
import forge.util.Lang;

public class Ward extends KeywordWithCost {

    protected Map<String, Cost> costs;

    @Override
    protected void parse(String details) {
        String[] allDetails = details.split(":");

        if (allDetails.length > 1) {
            costs = Maps.newLinkedHashMapWithExpectedSize(allDetails.length);
            for (String costStr : allDetails) {
                costs.put(costStr, new Cost(costStr, true));
            }
        } else {
            super.parse(details);
        }
    }

    public Map<String, Cost> getCosts() {
        return costs;
    }

    @Override
    public boolean isComplexCost() {
        return costs != null || super.isComplexCost();
    }

    @Override
    public String getTitleCost() {
        if (costs == null) {
            return super.getTitleCost() + (!cost.isOnlyManaCost() ? "." : "");
        }

        return Lang.joinHomogenous(costs.values(), c -> (c.isOnlyManaCost() ? "pay " : "") + c.toSimpleString(), "or");
    }

    @Override
    public String costReminderText() {
        if (costs == null) {
            return costToReminderDesc(getCost());
        }

        return Lang.joinHomogenous(costs.values(), this::costToReminderDesc, "or");
    }

    protected String costToReminderDesc(Cost c) {
        String costString = c.toSimpleString();
        if (costString.startsWith("Pay ")) {
            costString = costString.replace("Pay ", "pays ");
        } else if (costString.startsWith("Discard ")) {
            costString = costString.replace("Discard", "discards");
        } else if (c.isOnlyManaCost()) {
            costString = "pays " + costString;
        }
        return costString;
    }
}
