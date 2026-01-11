package forge.game.keyword;

import forge.game.card.Card;
import forge.util.Lang;

public class Amplify extends KeywordWithAmount {

    @Override
    protected String formatReminderText(String reminderText) {
        Card card = getHostCard();
        String type = "creature";
        if (card != null) {
            type = Lang.getInstance().buildValidDesc(card.getType().getCreatureTypes(), true);
        }
        return String.format(reminderText, amount, type);
    }
}
