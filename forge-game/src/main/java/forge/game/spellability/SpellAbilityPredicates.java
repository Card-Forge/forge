package forge.game.spellability;

import com.google.common.base.Predicate;

import forge.game.ability.ApiType;

public final class SpellAbilityPredicates {
    public static final Predicate<SpellAbility> isApi(final ApiType type) {
        return new Predicate<SpellAbility>() {
            @Override
            public boolean apply(final SpellAbility sa) {
                return type.equals(sa.getApi());
            }
        };
    }

    public static final Predicate<SpellAbility> hasSubAbilityApi(final ApiType type) {
        return new Predicate<SpellAbility>() {
            @Override
            public boolean apply(final SpellAbility sa) {
                return sa.findSubAbilityByType(type) != null;
            }
        };
    }

    public static final Predicate<SpellAbility> hasParam(final String name) {
        return new Predicate<SpellAbility>() {
            @Override
            public boolean apply(final SpellAbility sa) {
                return sa.hasParam(name);
            }
        };
    }

    public static final Predicate<SpellAbility> isMandatory() {
        return new Predicate<SpellAbility>() {
            @Override
            public boolean apply(final SpellAbility sa) {
                return sa.isMandatory();
            }
        };
    }
}
