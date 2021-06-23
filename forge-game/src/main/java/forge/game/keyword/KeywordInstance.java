package forge.game.keyword;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;

import forge.game.card.Card;
import forge.game.card.CardFactoryUtil;
import forge.game.player.Player;
import forge.game.player.PlayerFactoryUtil;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.util.Lang;
import io.sentry.Sentry;
import io.sentry.event.BreadcrumbBuilder;

public abstract class KeywordInstance<T extends KeywordInstance<?>> implements KeywordInterface {
    private Keyword keyword;
    private String original;

    private boolean hidden;

    private List<Trigger> triggers = Lists.newArrayList();
    private List<ReplacementEffect> replacements = Lists.newArrayList();
    private List<SpellAbility> abilities = Lists.newArrayList();
    private List<StaticAbility> staticAbilities = Lists.newArrayList();


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


    /*
     * (non-Javadoc)
     * @see forge.game.keyword.KeywordInterface#createTraits(forge.game.card.Card, boolean)
     */
    public final void createTraits(final Card host, final boolean intrinsic) {
        createTraits(host, intrinsic, false);
    }

    /*
     * (non-Javadoc)
     * @see forge.game.keyword.KeywordInterface#createTraits(forge.game.card.Card, boolean, boolean)
     */
    public final void createTraits(final Card host, final boolean intrinsic, final boolean clear) {
        if (clear) {
            triggers.clear();
            replacements.clear();
            abilities.clear();
            staticAbilities.clear();
        }

        try {
            String msg = "KeywordInstance:createTraits: make Traits for Keyword";
            Sentry.getContext().recordBreadcrumb(
                    new BreadcrumbBuilder().setMessage(msg)
                    .withData("Card", host.getName()).withData("Keyword", this.original).build()
            );

            // add Extra for debugging
            Sentry.getContext().addExtra("Card", host);
            Sentry.getContext().addExtra("Keyword", this.original);

            CardFactoryUtil.addTriggerAbility(this, host, intrinsic);
            CardFactoryUtil.addReplacementEffect(this, host.getCurrentState(), intrinsic);
            CardFactoryUtil.addSpellAbility(this, host.getCurrentState(), intrinsic);
            CardFactoryUtil.addStaticAbility(this, host.getCurrentState(), intrinsic);
        } catch (Exception e) {
            String msg = "KeywordInstance:createTraits: failed Traits for Keyword";
            Sentry.getContext().recordBreadcrumb(
                    new BreadcrumbBuilder().setMessage(msg)
                    .withData("Card", host.getName()).withData("Keyword", this.original).build()
            );
            //rethrow
            throw new RuntimeException("Error in Keyword " + this.original + " for card " + host.getName(), e);
        } finally {
            // remove added extra
            Sentry.getContext().removeExtra("Card");
            Sentry.getContext().removeExtra("Keyword");
        }
    }

    /* (non-Javadoc)
     * @see forge.game.keyword.KeywordInterface#createTraits(forge.game.player.Player)
     */
    @Override
    public void createTraits(Player player) {
        createTraits(player, false);
    }
    /* (non-Javadoc)
     * @see forge.game.keyword.KeywordInterface#createTraits(forge.game.player.Player, boolean)
     */
    @Override
    public void createTraits(Player player, boolean clear) {
        if (clear) {
            triggers.clear();
            replacements.clear();
            abilities.clear();
            staticAbilities.clear();
        }
        try {
            String msg = "KeywordInstance:createTraits: make Traits for Keyword";
            Sentry.getContext().recordBreadcrumb(
                    new BreadcrumbBuilder().setMessage(msg)
                    .withData("Player", player.getName()).withData("Keyword", this.original).build()
            );

            // add Extra for debugging
            Sentry.getContext().addExtra("Player", player);
            Sentry.getContext().addExtra("Keyword", this.original);

            PlayerFactoryUtil.addTriggerAbility(this, player);
            PlayerFactoryUtil.addReplacementEffect(this, player);
            PlayerFactoryUtil.addSpellAbility(this, player);
            PlayerFactoryUtil.addStaticAbility(this, player);
        } catch (Exception e) {
            String msg = "KeywordInstance:createTraits: failed Traits for Keyword";
            Sentry.getContext().recordBreadcrumb(
                    new BreadcrumbBuilder().setMessage(msg)
                    .withData("Player", player.getName()).withData("Keyword", this.original).build()
            );
            //rethrow
            throw new RuntimeException("Error in Keyword " + this.original + " for player " + player.getName(), e);
        } finally {
            // remove added extra
            Sentry.getContext().removeExtra("Player");
            Sentry.getContext().removeExtra("Keyword");
        }
    }
    /*
     * (non-Javadoc)
     * @see forge.game.keyword.KeywordInterface#addTrigger(forge.game.trigger.Trigger)
     */
    public final void addTrigger(final Trigger trg) {
        triggers.add(trg);
    }

    /*
     * (non-Javadoc)
     * @see forge.game.keyword.KeywordInterface#addReplacement(forge.game.replacement.ReplacementEffect)
     */
    public final void addReplacement(final ReplacementEffect trg) {
        replacements.add(trg);
    }

    /*
     * (non-Javadoc)
     * @see forge.game.keyword.KeywordInterface#addSpellAbility(forge.game.spellability.SpellAbility)
     */
    public final void addSpellAbility(final SpellAbility s) {
        abilities.add(s);
    }

    /*
     * (non-Javadoc)
     * @see forge.game.keyword.KeywordInterface#addStaticAbility(forge.game.staticability.StaticAbility)
     */
    public final void addStaticAbility(final StaticAbility st) {
        staticAbilities.add(st);
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

    /*
     * (non-Javadoc)
     * @see forge.game.keyword.KeywordInterface#getTriggers()
     */
    public Collection<Trigger> getTriggers() {
        return triggers;
    }
    /*
     * (non-Javadoc)
     * @see forge.game.keyword.KeywordInterface#getReplacements()
     */
    public Collection<ReplacementEffect> getReplacements() {
        return replacements;
    }
    /*
     * (non-Javadoc)
     * @see forge.game.keyword.KeywordInterface#getAbilities()
     */
    public Collection<SpellAbility> getAbilities() {
        return abilities;
    }
    /*
     * (non-Javadoc)
     * @see forge.game.keyword.KeywordInterface#getStaticAbilities()
     */
    public Collection<StaticAbility> getStaticAbilities() {
        return staticAbilities;
    }

    /*
     * (non-Javadoc)
     * @see forge.game.keyword.KeywordInterface#copy()
     */
    public KeywordInterface copy(final Card host, final boolean lki) {
        try {
            KeywordInstance<?> result = (KeywordInstance<?>) super.clone();
            
            result.abilities = Lists.newArrayList();
            for (SpellAbility sa : this.abilities) {
                result.abilities.add(sa.copy(host, lki));
            }
            
            result.triggers = Lists.newArrayList();
            for (Trigger tr : this.triggers) {
                result.triggers.add(tr.copy(host, lki));
            }
            
            result.replacements = Lists.newArrayList();
            for (ReplacementEffect re : this.replacements) {
                result.replacements.add(re.copy(host, lki));
            }
            
            result.staticAbilities = Lists.newArrayList();
            for (StaticAbility sa : this.staticAbilities) {
                result.staticAbilities.add(sa.copy(host, lki));
            }
            
            return result;
        } catch (final Exception ex) {
            throw new RuntimeException("KeywordInstance : clone() error", ex);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.getOriginal();
    }

    /* (non-Javadoc)
     * @see forge.game.keyword.KeywordInterface#redundant(java.util.Collection)
     */
    @Override
    public boolean redundant(Collection<KeywordInterface> list) {
        return !list.isEmpty() && keyword.isMultipleRedundant;
    }

    /* (non-Javadoc)
     * @see forge.game.keyword.KeywordInterface#setHostCard(forge.game.card.Card)
     */
    @Override
    public void setHostCard(Card host) {
        for (SpellAbility sa : this.abilities) {
            sa.setHostCard(host);
        }

        for (Trigger tr : this.triggers) {
            tr.setHostCard(host);
        }

        for (ReplacementEffect re : this.replacements) {
            re.setHostCard(host);
        }

        for (StaticAbility sa : this.staticAbilities) {
            sa.setHostCard(host);
        }
    }
}
