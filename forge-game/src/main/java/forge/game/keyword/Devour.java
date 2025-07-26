package forge.game.keyword;

import forge.card.CardType;

public class Devour extends KeywordWithAmountAndType {
    @Override
    protected void parse(String details) {
        super.parse(details);
        String[] d = details.split(":");
        type = "creatures";
        if (d.length > 1 && !d[1].isEmpty())
            type = CardType.getPluralType(d[1]);
    }
}
