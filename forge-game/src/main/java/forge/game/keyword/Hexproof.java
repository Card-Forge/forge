package forge.game.keyword;

import java.util.Collection;

public class Hexproof extends KeywordInstance<Hexproof> {
    private String type = "";

    @Override
    protected void parse(String details) {
        if (!details.isEmpty()) {
            type = details.split(":")[1];
        }
    }

    @Override
    protected String formatReminderText(String reminderText) {
        if (type.isEmpty()) {
            return "This can't be the target of spells or abilities your opponents control.";
        }
        return String.format(reminderText, type);
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
