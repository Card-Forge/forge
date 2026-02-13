package forge.game.keyword;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;

import forge.game.IHasSVars;
import forge.game.card.Card;
import forge.game.card.CardFactoryUtil;
import forge.game.player.Player;
import forge.game.player.PlayerFactoryUtil;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.util.Lang;
import io.sentry.Breadcrumb;
import io.sentry.Sentry;

public abstract class KeywordInstance<T extends KeywordInstance<?>> implements KeywordInterface {
    private Card hostCard = null;
    private boolean intrinsic = false;

    private Keyword keyword;
    private String original;
    private StaticAbility st = null;
    private long idx = -1;

    private List<Trigger> triggers = Lists.newArrayList();
    private List<ReplacementEffect> replacements = Lists.newArrayList();
    private List<SpellAbility> abilities = Lists.newArrayList();
    private List<StaticAbility> staticAbilities = Lists.newArrayList();

    @Override
    public String getOriginal() {
        return original;
    }
    @Override
    public Keyword getKeyword() {
        return keyword;
    }
    @Override
    public String getReminderText() {
        String result = formatReminderText(keyword.reminderText);
        Matcher m = Pattern.compile("\\{(\\w+):(.+?)\\}").matcher(result);

        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, Lang.nounWithNumeralExceptOne(m.group(1), m.group(2)));
        }
        m.appendTail(sb);
        return sb.toString();
    }
    @Override
    public int getAmount() {
        return 1;
    }
    @Override
    public String getAmountString() {
        return String.valueOf(getAmount());
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
        this.hostCard = host;
        this.intrinsic = intrinsic;
        if (clear) {
            triggers.clear();
            replacements.clear();
            abilities.clear();
            staticAbilities.clear();
        }

        try {
            String msg = "KeywordInstance:createTraits: make Traits for Keyword";

            Breadcrumb bread = new Breadcrumb(msg);
            bread.setData("Card", host.getName());
            bread.setData("Keyword", this.original);
            Sentry.addBreadcrumb(bread);

            // add Extra for debugging
            Sentry.setExtra("Card", host.getName());
            Sentry.setExtra("Keyword", this.original);

            CardFactoryUtil.addTriggerAbility(this, host, intrinsic);
            CardFactoryUtil.addReplacementEffect(this, host.getCurrentState(), intrinsic);
            CardFactoryUtil.addSpellAbility(this, host.getCurrentState(), intrinsic);
            CardFactoryUtil.addStaticAbility(this, host.getCurrentState(), intrinsic);
        } catch (Exception e) {
            String msg = "KeywordInstance:createTraits: failed Traits for Keyword";

            Breadcrumb bread = new Breadcrumb(msg);
            bread.setData("Card", host.getName());
            bread.setData("Keyword", this.original);
            Sentry.addBreadcrumb(bread);

            //rethrow
            throw new RuntimeException("Error in Keyword " + this.original + " for card " + host.getName(), e);
        } finally {
            // remove added extra
            Sentry.removeExtra("Card");
            Sentry.removeExtra("Keyword");
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

            Breadcrumb bread = new Breadcrumb(msg);
            bread.setData("Player", player.getName());
            bread.setData("Keyword", this.original);
            Sentry.addBreadcrumb(bread);

            // add Extra for debugging
            Sentry.setExtra("Player", player.getName());
            Sentry.setExtra("Keyword", this.original);

            PlayerFactoryUtil.addTriggerAbility(this, player);
            PlayerFactoryUtil.addReplacementEffect(this, player);
            PlayerFactoryUtil.addSpellAbility(this, player);
            PlayerFactoryUtil.addStaticAbility(this, player);
        } catch (Exception e) {
            String msg = "KeywordInstance:createTraits: failed Traits for Keyword";

            Breadcrumb bread = new Breadcrumb(msg);
            bread.setData("Player", player.getName());
            bread.setData("Keyword", this.original);
            Sentry.addBreadcrumb(bread);

            //rethrow
            throw new RuntimeException("Error in Keyword " + this.original + " for player " + player.getName(), e);
        } finally {
            // remove added extra
            Sentry.removeExtra("Player");
            Sentry.removeExtra("Keyword");
        }
    }
    /*
     * (non-Javadoc)
     * @see forge.game.keyword.KeywordInterface#addTrigger(forge.game.trigger.Trigger)
     */
    public final void addTrigger(final Trigger trg) {
        trg.setKeyword(this);
        triggers.add(trg);
    }

    /*
     * (non-Javadoc)
     * @see forge.game.keyword.KeywordInterface#addReplacement(forge.game.replacement.ReplacementEffect)
     */
    public final void addReplacement(final ReplacementEffect trg) {
        trg.setKeyword(this);
        replacements.add(trg);
    }

    /*
     * (non-Javadoc)
     * @see forge.game.keyword.KeywordInterface#addSpellAbility(forge.game.spellability.SpellAbility)
     */
    public final void addSpellAbility(final SpellAbility s) {
        s.setKeyword(this);
        abilities.add(s);
    }

    /*
     * (non-Javadoc)
     * @see forge.game.keyword.KeywordInterface#addStaticAbility(forge.game.staticability.StaticAbility)
     */
    public final void addStaticAbility(final StaticAbility st) {
        st.setKeyword(this);
        staticAbilities.add(st);
    }

    public boolean hasTraits() {
        if (!getAbilities().isEmpty())
            return true;
        if (!getTriggers().isEmpty())
            return true;
        if (!getReplacements().isEmpty())
            return true;
        if (!getStaticAbilities().isEmpty())
            return true;
        return false;
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


    public List<SpellAbility> applySpellAbility(List<SpellAbility> list) {
        list.addAll(getAbilities());
        return list;
    }
    public List<Trigger> applyTrigger(List<Trigger> list) {
        list.addAll(getTriggers());
        return list;
    }
    public List<ReplacementEffect> applyReplacementEffect(List<ReplacementEffect> list) {
        list.addAll(getReplacements());
        return list;
    }
    public List<StaticAbility> applyStaticAbility(List<StaticAbility> list) {
        list.addAll(getStaticAbilities());
        return list;
    }

    /*
     * (non-Javadoc)
     * @see forge.game.keyword.KeywordInterface#copy()
     */
    public KeywordInterface copy(final Card host, final boolean lki) {
        try {
            KeywordInstance<?> result = (KeywordInstance<?>) super.clone();
            result.hostCard = host;
            result.abilities = Lists.newArrayList();
            for (SpellAbility sa : this.abilities) {
                SpellAbility copy = sa.copy(host, lki);
                copy.setKeyword(result);
                result.abilities.add(copy);
            }

            result.triggers = Lists.newArrayList();
            for (Trigger tr : this.triggers) {
                Trigger copy = tr.copy(host, lki);
                copy.setKeyword(result);
                result.triggers.add(copy);
            }

            result.replacements = Lists.newArrayList();
            for (ReplacementEffect re : this.replacements) {
                ReplacementEffect copy = re.copy(host, lki);
                copy.setKeyword(result);
                result.replacements.add(copy);
            }

            result.staticAbilities = Lists.newArrayList();
            for (StaticAbility sa : this.staticAbilities) {
                StaticAbility copy = sa.copy(host, lki);
                copy.setKeyword(result);
                result.staticAbilities.add(copy);
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
        if (!keyword.isMultipleRedundant) {
            return false;
        }
        for (KeywordInterface i : list) {
            if (i.getOriginal().equals(getOriginal())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Card getHostCard() {
        return hostCard;
    }

    /* (non-Javadoc)
     * @see forge.game.keyword.KeywordInterface#setHostCard(forge.game.card.Card)
     */
    @Override
    public void setHostCard(Card host) {
        this.hostCard = host;
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

    @Override
    public boolean isIntrinsic() {
        return intrinsic;
    }

    @Override
    public void setIntrinsic(final boolean value) {
        this.intrinsic = value;
        for (SpellAbility sa : this.abilities) {
            sa.setIntrinsic(value);
        }

        for (Trigger tr : this.triggers) {
            tr.setIntrinsic(value);
        }

        for (ReplacementEffect re : this.replacements) {
            re.setIntrinsic(value);
        }

        for (StaticAbility sa : this.staticAbilities) {
            sa.setIntrinsic(value);
        }
    }

    public StaticAbility getStatic() {
        return this.st;
    }
    public void setStatic(StaticAbility st) {
        this.st = st;
    }

    public long getIdx() {
        return idx;
    }
    public void setIdx(long i) {
        idx = i;
    }

    protected IHasSVars getSVarFallback() {
        if (getStatic() != null) {
            return getStatic();
        }
        return getHostCard();
    }

    @Override
    public String getSVar(final String name) {
        return getSVarFallback().getSVar(name);
    }

    @Override
    public boolean hasSVar(final String name) {
        return getSVarFallback().hasSVar(name);
    }

    @Override
    public final void setSVar(final String name, final String value) {

    }

    @Override
    public Map<String, String> getSVars() {
        return getSVarFallback().getSVars();
    }

    @Override
    public void setSVars(Map<String, String> newSVars) {
    }

    @Override
    public void removeSVar(String var) {
    }
}
