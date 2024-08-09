package forge.game.spellability;

import com.google.common.base.Predicate;

import forge.game.CardTraitBase;
import forge.game.CardTraitPredicates;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;

public final class SpellAbilityPredicates extends CardTraitPredicates {
    public static Predicate<SpellAbility> isApi(final ApiType type) {
        return sa -> type.equals(sa.getApi());
    }

    public static Predicate<SpellAbility> hasSubAbilityApi(final ApiType type) {
        return sa -> sa.findSubAbilityByType(type) != null;
    }

    public static Predicate<SpellAbility> isMandatory() {
        return SpellAbility::isMandatory;
    }

    public static Predicate<SpellAbility> isManaAbility() {
        return SpellAbility::isManaAbility;
    }
    
    public static Predicate<SpellAbility> isIntrinsic() {
        return CardTraitBase::isIntrinsic;
    }

    public static Predicate<SpellAbility> isChapter() {
        return SpellAbility::isChapter;
    }

    public static Predicate<SpellAbility> isTrigger() {
        return SpellAbility::isTrigger;
    }

    public static Predicate<SpellAbility> isValid(String[] restrictions, Player sourceController, Card source, CardTraitBase spellAbility) {
        return sa -> sa.isValid(restrictions, sourceController, source, spellAbility);
    }
}
