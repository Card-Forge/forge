package forge.game.keyword;

public class Ninjutsu extends KeywordWithCost {

    protected boolean commander = false;

    /* (non-Javadoc)
     * @see forge.game.keyword.KeywordWithCost#parse(java.lang.String)
     */
    @Override
    protected void parse(String details) {
        if (details.contains(":")) {
            String k[] = details.split(":");
            details = k[0];
            if (k[1].equals("Commander")) {
                commander = true;
            }
        }
        super.parse(details);
    }

    /* (non-Javadoc)
     * @see forge.game.keyword.KeywordWithCost#formatReminderText(java.lang.String)
     */
    @Override
    protected String formatReminderText(String reminderText) {
        String zone = commander ? "hand or the command zone" : "hand";
        return String.format(reminderText, cost.toSimpleString(), zone);
    }

}
