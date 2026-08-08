package forge.game.decision;

import forge.card.CardStateName;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.Objects;

/**
 * Public, request-local description of one legal alternative.
 *
 * <p>The Forge references used to apply the action remain package-private. Consumers receive a semantic
 * description and a request-local identifier rather than a Java object identity.</p>
 */
public final class LegalCandidate {
    private final int candidateId;
    private final PriorityActionKind kind;
    private final int sourceCardId;
    private final String sourceName;
    private final ZoneType sourceZone;
    private final CardStateName sourceState;
    private final String abilityDescription;
    private final String semanticKey;
    private final SpellAbility spellAbility;

    private LegalCandidate(final int candidateId, final PriorityActionKind kind, final Card source,
            final SpellAbility spellAbility, final String semanticKey) {
        this.candidateId = candidateId;
        this.kind = Objects.requireNonNull(kind);
        this.sourceCardId = source == null ? -1 : source.getId();
        this.sourceName = source == null ? "" : source.getName();
        this.sourceZone = source == null || source.getZone() == null ? null : source.getZone().getZoneType();
        this.sourceState = source == null ? null : source.getCurrentStateName();
        this.abilityDescription = spellAbility == null ? "" : spellAbility.getDescription();
        this.semanticKey = Objects.requireNonNull(semanticKey);
        this.spellAbility = spellAbility;
    }

    static LegalCandidate pass(final int candidateId) {
        return new LegalCandidate(candidateId, PriorityActionKind.PASS, null, null, "PASS");
    }

    static LegalCandidate action(final int candidateId, final PriorityActionKind kind, final Card source,
            final SpellAbility spellAbility, final String semanticKey) {
        return new LegalCandidate(candidateId, kind, source, spellAbility, semanticKey);
    }

    public int getCandidateId() {
        return candidateId;
    }

    public PriorityActionKind getKind() {
        return kind;
    }

    /** Forge's in-game card identifier, not a Java object identity or global action identifier. */
    public int getSourceCardId() {
        return sourceCardId;
    }

    public String getSourceName() {
        return sourceName;
    }

    public ZoneType getSourceZone() {
        return sourceZone;
    }

    public CardStateName getSourceState() {
        return sourceState;
    }

    public String getAbilityDescription() {
        return abilityDescription;
    }

    /** A deterministic, request-local semantic ordering key. */
    public String getSemanticKey() {
        return semanticKey;
    }

    SpellAbility getSpellAbility() {
        return spellAbility;
    }
}
