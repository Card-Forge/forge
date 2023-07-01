package forge.game.spellability;

import com.google.common.base.Predicate;

import forge.game.CardTraitBase;
import forge.game.CardTraitPredicates;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;

public final class SpellAbilityPredicates extends CardTraitPredicates {
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

    public static final Predicate<SpellAbility> isMandatory() {
        return new Predicate<SpellAbility>() {
            @Override
            public boolean apply(final SpellAbility sa) {
                return sa.isMandatory();
            }
        };
    }

    public static final Predicate<SpellAbility> isManaAbility() {
        return new Predicate<SpellAbility>() {
            @Override
            public boolean apply(final SpellAbility sa) {
                return sa.isManaAbility();
            }
        };
    }
    
    public static final Predicate<SpellAbility> isIntrinsic() {
        return new Predicate<SpellAbility>() {
            @Override
            public boolean apply(final SpellAbility sa) {
                return sa.isIntrinsic();
            }
        };
    }

    public static final Predicate<SpellAbility> isChapter() {
        return new Predicate<SpellAbility>() {
            @Override
            public boolean apply(final SpellAbility sa) {
                return sa.isChapter();
            }
        };
    }

    public static final Predicate<SpellAbility> isTrigger() {
        return new Predicate<SpellAbility>() {
            @Override
            public boolean apply(final SpellAbility sa) {
                return sa.isTrigger();
            }
        };
    }

    public static final Predicate<SpellAbility> isValid(String[] restrictions, Player sourceController, Card source, CardTraitBase spellAbility) {
        return new Predicate<SpellAbility>() {
            @Override
            public boolean apply(final SpellAbility sa) {
                return sa.isValid(restrictions, sourceController, source, spellAbility);
            }
        };
    }
}
