package forge.game.card;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;

public class CardTraitChanges implements Cloneable {

    private List<Trigger> triggers = Lists.newArrayList();
    private List<ReplacementEffect> replacements = Lists.newArrayList();
    private List<SpellAbility> abilities = Lists.newArrayList();
    private List<StaticAbility> staticAbilities = Lists.newArrayList();

    private List<SpellAbility> removedAbilities = Lists.newArrayList();

    private boolean removeAll = false;
    private boolean removeNonMana = false;

    public CardTraitChanges(Collection<SpellAbility> spells, Collection<SpellAbility> removedAbilities,
            Collection<Trigger> trigger, Collection<ReplacementEffect> res, Collection<StaticAbility> st,
            boolean removeAll, boolean removeNonMana) {
        if (spells != null) {
            this.abilities.addAll(spells);
        }
        if (removedAbilities != null) {
            this.removedAbilities.addAll(removedAbilities);
        }
        if (trigger != null) {
            this.triggers.addAll(trigger);
        }
        if (res != null) {
            this.replacements.addAll(res);
        }
        if (st != null) {
            this.staticAbilities.addAll(st);
        }

        this.removeAll |= removeAll;
        this.removeNonMana |= removeNonMana;
    }

    /**
     * @return the triggers
     */
    public Collection<Trigger> getTriggers() {
        return triggers;
    }
    /**
     * @return the replacements
     */
    public Collection<ReplacementEffect> getReplacements() {
        return replacements;
    }

    /**
     * @return the abilities
     */
    public Collection<SpellAbility> getAbilities() {
        return abilities;
    }

    /**
     * @return the abilities
     */
    public Collection<SpellAbility> getRemovedAbilities() {
        return removedAbilities;
    }

    /**
     * @return the staticAbilities
     */
    public Collection<StaticAbility> getStaticAbilities() {
        return staticAbilities;
    }

    public boolean isRemoveAll() {
        return removeAll;
    }

    public boolean isRemoveNonMana() {
        return removeNonMana;
    }

    public CardTraitChanges copy(Card host, boolean lki) {
        try {
            CardTraitChanges result = (CardTraitChanges) super.clone();

            result.abilities = Lists.newArrayList();
            for (SpellAbility sa : this.abilities) {
                result.abilities.add(sa.copy(host, lki));
            }
            result.removedAbilities = Lists.newArrayList();
            for (SpellAbility sa : this.removedAbilities) {
                result.removedAbilities.add(sa.copy(host, lki));
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
            throw new RuntimeException("CardTraitChanges : clone() error", ex);
        }
    }

    public void changeText() {
        for (SpellAbility sa : this.abilities) {
            sa.changeText();
        }

        for (Trigger tr : this.triggers) {
            tr.changeText();
        }

        for (ReplacementEffect re : this.replacements) {
            re.changeText();
        }

        for (StaticAbility sa : this.staticAbilities) {
            sa.changeText();
        }
    }
}
