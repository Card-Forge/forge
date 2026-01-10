package forge.game.keyword;

import forge.util.Lang;

public class Amplify extends KeywordWithAmount {

    @Override
    protected String formatReminderText(String reminderText) {
        String type = Lang.getInstance().buildValidDesc(getHostCard().getType().getCreatureTypes(), true);
        return String.format(reminderText, amount, type);
    }
}
