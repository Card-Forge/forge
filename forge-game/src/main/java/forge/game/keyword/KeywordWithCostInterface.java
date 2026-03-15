package forge.game.keyword;

import forge.game.cost.Cost;

public interface KeywordWithCostInterface {

    Cost getCost();

    String getCostString();

    String getTitleWithoutCost();

}