package forge.game.keyword;

public class Partner extends SimpleKeyword {

    private String with = null;

    public Partner() {
    }

    @Override
    protected void parse(String details) {
        if (!details.isEmpty()) {
            if (details.contains(":")) {
                with = details.split(":")[1];
            } else {
                with = details;
            }
        }
    }

    @Override
    protected String formatReminderText(String reminderText) {
        if (with == null) {
            return reminderText;
        } else {
            final StringBuilder sb = new StringBuilder();

            sb.append("When this creature enters the battlefield, target player may put ");
            sb.append(with);
            sb.append(" into their hand from their library, then shuffle.");

            return sb.toString();
        }
    }
}
