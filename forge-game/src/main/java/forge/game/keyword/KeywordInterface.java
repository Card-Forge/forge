package forge.game.keyword;

import java.util.Collection;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;

public interface KeywordInterface extends Cloneable {

    String getOriginal();

    Keyword getKeyword();

    String getReminderText();

    int getAmount();

    void createTraits(final Card host, final boolean intrinsic);
    void createTraits(final Card host, final boolean intrinsic, final boolean clear);
    
    void createTraits(final Player player);
    void createTraits(final Player player, final boolean clear);

    void addTrigger(final Trigger trg);
    
    void addReplacement(final ReplacementEffect trg);

    void addSpellAbility(final SpellAbility s);
    void addStaticAbility(final StaticAbility st);
    
    void setHostCard(final Card host);

    /**
     * @return the triggers
     */
    Collection<Trigger> getTriggers();
    /**
     * @return the replacements
     */
    Collection<ReplacementEffect> getReplacements();
    /**
     * @return the abilities
     */
    Collection<SpellAbility> getAbilities();
    /**
     * @return the staticAbilities
     */
    Collection<StaticAbility> getStaticAbilities();
    
    KeywordInterface copy(final Card host, final boolean lki);

    boolean redundant(final Collection<KeywordInterface> list);
}