package forge.game.keyword;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import forge.util.Lang;

public abstract class KeywordInstance<T extends KeywordInstance<?>> implements KeywordInterface {
    private Keyword keyword;
    private String original;

    /* (non-Javadoc)
     * @see forge.game.keyword.KeywordInterface#getOriginal()
     */
    @Override
    public String getOriginal() {
        return original;
    }
    /* (non-Javadoc)
     * @see forge.game.keyword.KeywordInterface#getKeyword()
     */
    @Override
    public Keyword getKeyword() {
        return keyword;
    }
    /* (non-Javadoc)
     * @see forge.game.keyword.KeywordInterface#getReminderText()
     */
    @Override
    public String getReminderText() {
        String result = formatReminderText(keyword.reminderText);
        Matcher m = Pattern.compile("\\{(\\w):(.+?)\\}").matcher(result);
        
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, Lang.nounWithNumeral(m.group(1), m.group(2)));
        }
        m.appendTail(sb);
        return sb.toString();
    }
    /* (non-Javadoc)
     * @see forge.game.keyword.KeywordInterface#getAmount()
     */
    @Override
    public int getAmount() {
        return 1;
    }
    protected void initialize(String original0, Keyword keyword0, String details) {
        original = original0;
        keyword = keyword0;
        parse(details);
    }
    protected abstract void parse(String details);
    protected abstract String formatReminderText(String reminderText);
}
