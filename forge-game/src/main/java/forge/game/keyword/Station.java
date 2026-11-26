package forge.game.keyword;

import forge.game.card.Card;
import forge.game.staticability.StaticAbility;

public class Station extends SimpleKeyword {

    @Override
    protected String formatReminderText(String reminderText) {
        Card card = this.getHostCard();
        if (card == null) {
            return String.format(reminderText, "Spacecraft");
        }
        String extra = "";
        if (card.getType().hasSubtype("Spacecraft")) {
            reminderText = String.format(reminderText, "Spacecraft");
        } else {
            reminderText = String.format(reminderText, "Planet");
        }
        for (StaticAbility st : card.getStaticAbilities()) {
            if (!st.isIntrinsic() || !st.hasParam("Station") || !"Creature".equals(st.getParam("AddType"))) {
                continue;
            }
            extra = " It's an artifact creature at " + st.getParam("Station") + "+.";
            break;
        }
        return reminderText + extra;
    }
}
