package forge.game.keyword;

import forge.game.cost.Cost;

public interface KeywordWithCostInterface extends KeywordInterface {

    Cost getCost();

    String getCostString();

    default String getTitle() {
        StringBuilder sb = new StringBuilder();
        sb.append(getTitleWithoutCost());
        if (isComplexCost()) {
            sb.append("—");
        } else {
            sb.append(" ");
        }
        sb.append(getTitleCost());
        return sb.toString();
    }

    default String getTitleWithoutCost() {
        return getKeyword().toString();
    }

    default boolean isComplexCost() {
        Cost cost = getCost();
        return !cost.isOnlyManaCost();
    }

    default String getTitleCost() {
        Cost cost = getCost();
        return cost.toSimpleString();
    }

    default String costReminderText() {
        return getCost().toSimpleString();
    }
}