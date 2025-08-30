package forge.game.card;

import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public record CardTraitChanges(Collection<SpellAbility> abilities, Collection<SpellAbility> removedAbilities,
        Collection<Trigger> triggers, Collection<ReplacementEffect> replacements, Collection<StaticAbility> staticAbilities,
        boolean removeAll, boolean removeNonMana) {

    /**
     * @return the triggers
     */
    public Collection<Trigger> getTriggers() {
        return Objects.requireNonNullElse(triggers, List.of());
    }
    /**
     * @return the replacements
     */
    public Collection<ReplacementEffect> getReplacements() {
        return Objects.requireNonNullElse(replacements, List.of());
    }

    /**
     * @return the abilities
     */
    public Collection<SpellAbility> getAbilities() {
        return Objects.requireNonNullElse(abilities, List.of());
    }

    /**
     * @return the abilities
     */
    public Collection<SpellAbility> getRemovedAbilities() {
        return Objects.requireNonNullElse(removedAbilities, List.of());
    }

    /**
     * @return the staticAbilities
     */
    public Collection<StaticAbility> getStaticAbilities() {
        return Objects.requireNonNullElse(staticAbilities, List.of());
    }

    public boolean isRemoveAll() {
        return removeAll;
    }

    public boolean isRemoveNonMana() {
        return removeNonMana;
    }

    public CardTraitChanges copy(Card host, boolean lki) {
        return new CardTraitChanges(
                this.getAbilities().stream().map(sa -> sa.copy(host, lki)).collect(Collectors.toList()),
                this.getRemovedAbilities().stream().map(sa -> sa.copy(host, lki)).collect(Collectors.toList()),
                this.getTriggers().stream().map(tr -> tr.copy(host, lki)).collect(Collectors.toList()),
                this.getReplacements().stream().map(tr -> tr.copy(host, lki)).collect(Collectors.toList()),
                this.getStaticAbilities().stream().map(st -> st.copy(host, lki)).collect(Collectors.toList()),
                removeAll, removeNonMana
        );
    }

    public void changeText() {
        for (SpellAbility sa : this.getAbilities()) {
            sa.changeText();
        }

        for (Trigger tr : this.getTriggers()) {
            tr.changeText();
        }

        for (ReplacementEffect re : this.getReplacements()) {
            re.changeText();
        }

        for (StaticAbility sa : this.getStaticAbilities()) {
            sa.changeText();
        }
    }
}
