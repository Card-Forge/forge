package forge.game.decision;

import forge.card.CardStateName;
import forge.game.ability.AbilityFactory;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Common, ownership-neutral semantic admission for the exact Blood Operative ETB slice. */
final class BloodOperativeEtbProfile {
    private static final String BLOOD_OPERATIVE = "Blood Operative";
    private static final String BLOOD_TRIGGER = "TrigChangeZone";
    private static final Map<String, String> BLOOD_TRIGGER_PARAMS = Map.of(
            "Mode", "ChangesZone",
            "Origin", "Any",
            "Destination", "Battlefield",
            "ValidCard", "Card.Self",
            "OptionalDecider", "You",
            "Execute", BLOOD_TRIGGER);
    private static final Map<String, String> BLOOD_EFFECT_PARAMS = Map.of(
            "DB", "ChangeZone",
            "Origin", "Graveyard",
            "Destination", "Exile",
            "ValidTgts", "Card");
    private static final Set<String> BLOOD_STATIC_EFFECT_PARAMS = Set.of(
            "DB", "Origin", "Destination", "ValidTgts", "TgtPrompt", "ValidTgtsDesc");
    private static final Set<String> BLOOD_LIVE_EFFECT_PARAMS = Set.of(
            "DB", "Origin", "Destination", "ValidTgts", "TgtPrompt", "ValidTgtsDesc",
            "TgtZone", "TargetMin", "TargetMax");

    private BloodOperativeEtbProfile() {
    }

    static Validation validateCommonSemanticProfile(final WrappedAbility wrapper) {
        if (wrapper == null) {
            return Validation.failed(Failure.NULL_INPUT);
        }

        final SpellAbility liveAbility;
        final Trigger trigger;
        final Card source;
        try {
            liveAbility = wrapper.getWrappedAbility();
            trigger = wrapper.getTrigger();
            source = wrapper.getHostCard();
        } catch (final RuntimeException ex) {
            return Validation.failed(Failure.NULL_INPUT);
        }
        if (liveAbility == null || trigger == null || source == null) {
            return Validation.failed(Failure.NULL_INPUT);
        }

        try {
            if (!BLOOD_OPERATIVE.equals(source.getName())) {
                return Validation.failed(Failure.SOURCE_IDENTITY);
            }
        } catch (final RuntimeException ex) {
            return Validation.failed(Failure.SOURCE_IDENTITY);
        }
        try {
            if (source.getCurrentStateName() != CardStateName.Original) {
                return Validation.failed(Failure.SOURCE_STATE);
            }
        } catch (final RuntimeException ex) {
            return Validation.failed(Failure.SOURCE_STATE);
        }
        try {
            if (source.isCloned() || wrapper.isCopied() || liveAbility.isCopied()
                    || !wrapper.isIntrinsic() || !liveAbility.isIntrinsic()) {
                return Validation.failed(Failure.SOURCE_PROVENANCE);
            }
        } catch (final RuntimeException ex) {
            return Validation.failed(Failure.SOURCE_PROVENANCE);
        }

        try {
            if (!trigger.isIntrinsic()) {
                return Validation.failed(Failure.SOURCE_PROVENANCE);
            }
            if (trigger.isStatic() || trigger.getMode() != TriggerType.ChangesZone
                    || trigger.getSpawningAbility() != null) {
                return Validation.failed(Failure.TRIGGER_DEFINITION);
            }
        } catch (final RuntimeException ex) {
            return Validation.failed(Failure.TRIGGER_DEFINITION);
        }
        try {
            if (!BLOOD_TRIGGER_PARAMS.equals(normalize(trigger.getOriginalMapParams(), "TriggerDescription"))
                    || !BLOOD_TRIGGER_PARAMS.equals(normalize(trigger.getMapParams(), "TriggerDescription"))) {
                return Validation.failed(Failure.TRIGGER_DEFINITION);
            }
        } catch (final RuntimeException ex) {
            return Validation.failed(Failure.TRIGGER_DEFINITION);
        }

        final Validation staticValidation = validateStaticEffect(source);
        if (!staticValidation.isAdmitted()) {
            return staticValidation;
        }
        return validateLiveEffect(liveAbility);
    }

    private static Validation validateStaticEffect(final Card source) {
        try {
            if (!source.hasSVar(BLOOD_TRIGGER)) {
                return Validation.failed(Failure.STATIC_EFFECT_DEFINITION);
            }
            final Map<String, String> params = AbilityFactory.getMapParams(source.getSVar(BLOOD_TRIGGER));
            if (params == null || !BLOOD_STATIC_EFFECT_PARAMS.containsAll(params.keySet())
                    || !BLOOD_EFFECT_PARAMS.equals(normalize(params, "TgtPrompt", "ValidTgtsDesc"))) {
                return Validation.failed(Failure.STATIC_EFFECT_DEFINITION);
            }
            return Validation.admitted();
        } catch (final RuntimeException ex) {
            return Validation.failed(Failure.STATIC_EFFECT_DEFINITION);
        }
    }

    private static Validation validateLiveEffect(final SpellAbility liveAbility) {
        final Map<String, String> params;
        try {
            params = liveAbility.getMapParams();
            if (params == null || !BLOOD_LIVE_EFFECT_PARAMS.containsAll(params.keySet())) {
                return Validation.failed(Failure.LIVE_EFFECT_DEFINITION);
            }
        } catch (final RuntimeException ex) {
            return Validation.failed(Failure.LIVE_EFFECT_DEFINITION);
        }

        try {
            if (liveAbility.hasParam("Optional") || liveAbility.hasParam("TargetingPlayer")
                    || liveAbility.getTargetingPlayer() != null) {
                return Validation.failed(Failure.LIVE_EFFECT_DEFINITION);
            }
        } catch (final RuntimeException ex) {
            return Validation.failed(Failure.LIVE_EFFECT_DEFINITION);
        }

        for (final Map.Entry<String, String> expected : BLOOD_EFFECT_PARAMS.entrySet()) {
            if (!expected.getValue().equals(params.get(expected.getKey()))) {
                return Validation.failed(Failure.TARGETING_SHAPE);
            }
        }

        try {
            if (liveAbility.getApi() != ApiType.ChangeZone) {
                return Validation.failed(Failure.TARGETING_SHAPE);
            }
            final TargetRestrictions restrictions = liveAbility.getTargetRestrictions();
            final Cost payCosts = liveAbility.getPayCosts();
            if (!liveAbility.usesTargeting() || restrictions == null || restrictions.isRandomTarget()
                    || restrictions.isRandomNumTargets() || !List.of(ZoneType.Graveyard).equals(restrictions.getZone())
                    || liveAbility.getMinTargets() != 1 || liveAbility.getMaxTargets() != 1
                    || liveAbility.getSubAbility() != null || !liveAbility.getAdditionalAbilities().isEmpty()
                    || !liveAbility.getAdditionalAbilityLists().isEmpty() || payCosts == null
                    || !payCosts.isFree()
                    || (params.containsKey("TgtZone") && !"Graveyard".equals(params.get("TgtZone")))
                    || (params.containsKey("TargetMin") && !"1".equals(params.get("TargetMin")))
                    || (params.containsKey("TargetMax") && !"1".equals(params.get("TargetMax")))) {
                return Validation.failed(Failure.TARGETING_SHAPE);
            }
            return Validation.admitted();
        } catch (final RuntimeException ex) {
            return Validation.failed(Failure.TARGETING_SHAPE);
        }
    }

    private static Map<String, String> normalize(final Map<String, String> params, final String... ignoredKeys) {
        if (params == null) {
            return Map.of();
        }
        final Map<String, String> normalized = new HashMap<>(params);
        for (final String ignoredKey : ignoredKeys) {
            normalized.remove(ignoredKey);
        }
        return normalized;
    }

    enum Failure {
        NULL_INPUT,
        SOURCE_IDENTITY,
        SOURCE_STATE,
        SOURCE_PROVENANCE,
        TRIGGER_DEFINITION,
        STATIC_EFFECT_DEFINITION,
        LIVE_EFFECT_DEFINITION,
        TARGETING_SHAPE
    }

    static final class Validation {
        private final Failure failure;

        private Validation(final Failure failure0) {
            failure = failure0;
        }

        static Validation admitted() {
            return new Validation(null);
        }

        static Validation failed(final Failure failure) {
            return new Validation(failure);
        }

        boolean isAdmitted() {
            return failure == null;
        }

        Failure getFailure() {
            return failure;
        }
    }
}
