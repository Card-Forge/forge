package forge.game.keyword;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import forge.game.card.Card;
import forge.game.staticability.StaticAbility;

public class Station extends SimpleKeyword {

    static Pattern pattern = Pattern.compile("counters_GE(\\d+)_CHARGE");

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
            if (!st.isIntrinsic()) {
                continue;
            }
            if (!"Creature".equals(st.getParam("AddType"))) {
                continue;
            }
            Matcher m = pattern.matcher(st.getParam("Affected"));
            if (!m.find()) {
                continue;
            }
            extra = "It's an artifact creature at " + m.group(1) + "+.";
            break;
        }
        return reminderText + extra;
    }
}
