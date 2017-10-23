package forge.game.keyword;

import forge.game.card.Card;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;

public interface KeywordInterface {

    String getOriginal();

    Keyword getKeyword();

    String getReminderText();

    int getAmount();
    
    boolean getHidden();
    void setHidden(boolean val);
    
    void addKeywords(final Card host, final boolean intrinsic);
    
    public void addTrigger(final Trigger trg);
    
    public void addReplacement(final ReplacementEffect trg);

    public void addSpellAbility(final SpellAbility s);
    public void addStaticAbility(final StaticAbility st);
    
    void removeKeywords(final Card host);
}