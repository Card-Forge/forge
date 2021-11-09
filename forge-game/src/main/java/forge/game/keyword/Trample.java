package forge.game.keyword;

import java.util.Collection;

public class Trample  extends KeywordInstance<Trample> {
    private String type = "";

    @Override
    protected void parse(String details) {
        if (!details.isEmpty()) {
            type = details.split(":")[0];
        }
    }

    @Override
    protected String formatReminderText(String reminderText) {
        if (!type.isEmpty()) {
            return "This creature can deal excess combat damage to the controller of the planeswalker it's attacking.";
        }
        return reminderText;
    }

    /* (non-Javadoc)
     * @see forge.game.keyword.KeywordInstance#redundant(java.util.Collection)
     */
    @Override
    public boolean redundant(Collection<KeywordInterface> list) {
        for (KeywordInterface i : list) {
            if (i.getOriginal().equals(getOriginal())) {
                return true;
            }
        }
        return false;
    }
}
