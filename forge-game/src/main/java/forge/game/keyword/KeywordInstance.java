package forge.game.keyword;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.google.common.collect.Lists;

import forge.game.card.Card;
import forge.game.card.CardFactoryUtil;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.util.Lang;

public abstract class KeywordInstance<T extends KeywordInstance<?>> implements KeywordInterface {
    private Keyword keyword;
    private String original;
    
    
    private boolean hidden;
    
    private List<Trigger> triggers = Lists.<Trigger>newArrayList();
    private List<ReplacementEffect> replacements = Lists.<ReplacementEffect>newArrayList();
    private List<SpellAbility> abilities = Lists.<SpellAbility>newArrayList();
    private List<StaticAbility> staticAbilities = Lists.<StaticAbility>newArrayList();


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
    

    public final void addKeywords(final Card host, final boolean intrinsic) {
        CardFactoryUtil.addTriggerAbility(this, host, intrinsic);
        CardFactoryUtil.addReplacementEffect(this, host, intrinsic);
        CardFactoryUtil.addSpellAbility(this, host, intrinsic);
        CardFactoryUtil.addStaticAbility(this, host, intrinsic);
    }

    
    public final void addTrigger(final Trigger trg) {
        triggers.add(trg);
    }
    
    public final void addReplacement(final ReplacementEffect trg) {
        replacements.add(trg);
    }

    public final void addSpellAbility(final SpellAbility s) {
        abilities.add(s);
    }
    public final void addStaticAbility(final StaticAbility st) {
        staticAbilities.add(st);
    }
    
    public final void removeKeywords(final Card host) {
        for (Trigger t : triggers) {
            host.removeTrigger(t);
        }
        for (ReplacementEffect r : replacements) {
            host.removeReplacementEffect(r);
        }
        for (SpellAbility s : abilities) {
            host.removeSpellAbility(s);
        }
        for (StaticAbility st : staticAbilities) {
            host.removeStaticAbility(st);
        }
    }
    
    /* (non-Javadoc)
     * @see forge.game.keyword.KeywordInterface#getHidden()
     */
    @Override
    public boolean getHidden() {
        return hidden;
    }
    /* (non-Javadoc)
     * @see forge.game.keyword.KeywordInterface#setHidden(boolean)
     */
    @Override
    public void setHidden(boolean val) {
        hidden = val;        
    }
}
