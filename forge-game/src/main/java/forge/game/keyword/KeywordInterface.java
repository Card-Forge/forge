package forge.game.keyword;

import java.util.Collection;

import forge.game.card.Card;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;

public interface KeywordInterface extends Cloneable {

    String getOriginal();

    Keyword getKeyword();

    String getReminderText();

    int getAmount();
    
    boolean getHidden();
    void setHidden(boolean val);
    
    public void createTraits(final Card host, final boolean intrinsic);
    void createTraits(final Card host, final boolean intrinsic, final boolean clear);
    
    public void addTrigger(final Trigger trg);
    
    public void addReplacement(final ReplacementEffect trg);

    public void addSpellAbility(final SpellAbility s);
    public void addStaticAbility(final StaticAbility st);
    
    public void setHostCard(final Card host);

    /**
     * @return the triggers
     */
    public Collection<Trigger> getTriggers();
    /**
     * @return the replacements
     */
    public Collection<ReplacementEffect> getReplacements();
    /**
     * @return the abilities
     */
    public Collection<SpellAbility> getAbilities();
    /**
     * @return the staticAbilities
     */
    public Collection<StaticAbility> getStaticAbilities();
    
    public KeywordInterface copy(final Card host, final boolean lki);

    public boolean redundant(final Collection<KeywordInterface> list);
}