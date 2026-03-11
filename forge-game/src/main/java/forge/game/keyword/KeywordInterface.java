package forge.game.keyword;

import java.util.Collection;

import forge.game.IHasSVars;
import forge.game.card.Card;
import forge.game.card.ICardTraitChanges;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;

public interface KeywordInterface extends Cloneable, IHasSVars, ICardTraitChanges {

    Card getHostCard();
    void setHostCard(final Card host);
    boolean isIntrinsic();
    void setIntrinsic(final boolean value);

    String getOriginal();

    Keyword getKeyword();

    String getTitle();
    String getReminderText();

    int getAmount();
    String getAmountString();

    StaticAbility getStatic();
    void setStatic(StaticAbility st);

    long getIdx();
    void setIdx(long i);

    void createTraits(final Card host, final boolean intrinsic);
    void createTraits(final Card host, final boolean intrinsic, final boolean clear);

    void createTraits(final Player player);
    void createTraits(final Player player, final boolean clear);

    boolean hasTraits();

    void addTrigger(final Trigger trg);

    void addReplacement(final ReplacementEffect trg);

    void addSpellAbility(final SpellAbility s);
    void addStaticAbility(final StaticAbility st);


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