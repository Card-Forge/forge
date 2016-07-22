package forge.game.keyword;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import forge.util.Lang;

public abstract class KeywordInstance<T extends KeywordInstance<?>> {
    private Keyword keyword;

    public Keyword getKeyword() {
        return keyword;
    }
    public String getReminderText() {
        String result = formatReminderText(keyword.reminderText);
        Matcher m = Pattern.compile("\\{(\\w):(.+)\\}").matcher(result);
        
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, Lang.nounWithNumeral(m.group(1), m.group(2)));
        }
        m.appendTail(sb);
        return sb.toString();
    }
    public int getAmount() {
        return 1;
    }
    protected void initialize(Keyword keyword0, String details) {
        keyword = keyword0;
        parse(details);
    }
    protected abstract void parse(String details);
    protected abstract String formatReminderText(String reminderText);
}
