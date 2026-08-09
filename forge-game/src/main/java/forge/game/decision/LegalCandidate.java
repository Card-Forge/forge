package forge.game.decision;

import forge.card.CardStateName;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.mana.Mana;
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
    private final TargetCandidateKind targetKind;
    private final int targetEntityId;
    private final String targetName;
    private final ZoneType targetZone;
    private final GameObject target;
    private final PaymentCandidateKind paymentKind;
    private final Mana mana;
    private final Integer xValue;

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
        this.targetKind = null;
        this.targetEntityId = -1;
        this.targetName = "";
        this.targetZone = null;
        this.target = null;
        this.paymentKind = null;
        this.mana = null;
        this.xValue = null;
    }

    private LegalCandidate(final int candidateId, final TargetCandidateKind targetKind, final GameObject target,
            final int targetEntityId, final String targetName, final ZoneType targetZone, final String semanticKey) {
        this.candidateId = candidateId;
        this.kind = null;
        this.sourceCardId = -1;
        this.sourceName = "";
        this.sourceZone = null;
        this.sourceState = null;
        this.abilityDescription = "";
        this.semanticKey = Objects.requireNonNull(semanticKey);
        this.spellAbility = null;
        this.targetKind = Objects.requireNonNull(targetKind);
        this.targetEntityId = targetEntityId;
        this.targetName = Objects.requireNonNull(targetName);
        this.targetZone = targetZone;
        this.target = target;
        this.paymentKind = null;
        this.mana = null;
        this.xValue = null;
    }

    private LegalCandidate(final int candidateId, final PaymentCandidateKind paymentKind, final Card source,
            final SpellAbility spellAbility, final Mana mana, final String semanticKey) {
        this.candidateId = candidateId;
        this.kind = null;
        this.sourceCardId = source == null ? -1 : source.getId();
        this.sourceName = source == null ? "" : source.getName();
        this.sourceZone = source == null || source.getZone() == null ? null : source.getZone().getZoneType();
        this.sourceState = source == null ? null : source.getCurrentStateName();
        this.abilityDescription = spellAbility == null ? "" : spellAbility.getDescription();
        this.semanticKey = Objects.requireNonNull(semanticKey);
        this.spellAbility = spellAbility;
        this.targetKind = null;
        this.targetEntityId = -1;
        this.targetName = "";
        this.targetZone = null;
        this.target = null;
        this.paymentKind = Objects.requireNonNull(paymentKind);
        this.mana = mana;
        this.xValue = null;
    }

    private LegalCandidate(final int candidateId, final int xValue) {
        this.candidateId = candidateId;
        this.kind = null;
        this.sourceCardId = -1;
        this.sourceName = "";
        this.sourceZone = null;
        this.sourceState = null;
        this.abilityDescription = "";
        this.semanticKey = "X|" + xValue;
        this.spellAbility = null;
        this.targetKind = null;
        this.targetEntityId = -1;
        this.targetName = "";
        this.targetZone = null;
        this.target = null;
        this.paymentKind = null;
        this.mana = null;
        this.xValue = xValue;
    }

    static LegalCandidate pass(final int candidateId) {
        return new LegalCandidate(candidateId, PriorityActionKind.PASS, null, null, "PASS");
    }

    static LegalCandidate action(final int candidateId, final PriorityActionKind kind, final Card source,
            final SpellAbility spellAbility, final String semanticKey) {
        return new LegalCandidate(candidateId, kind, source, spellAbility, semanticKey);
    }

    static LegalCandidate target(final int candidateId, final TargetCandidateKind targetKind,
            final GameObject target, final int targetEntityId, final String targetName, final ZoneType targetZone,
            final String semanticKey) {
        return new LegalCandidate(candidateId, targetKind, target, targetEntityId, targetName, targetZone,
                semanticKey);
    }

    static LegalCandidate done(final int candidateId) {
        return new LegalCandidate(candidateId, TargetCandidateKind.DONE, null, -1, "", null, "DONE");
    }

    static LegalCandidate paymentSource(final int candidateId, final Card source,
            final SpellAbility manaAbility, final String semanticKey) {
        return new LegalCandidate(candidateId, PaymentCandidateKind.ACTIVATE_MANA_SOURCE, source,
                manaAbility, null, semanticKey);
    }

    static LegalCandidate floatingMana(final int candidateId, final Mana mana, final String semanticKey) {
        return new LegalCandidate(candidateId, PaymentCandidateKind.USE_FLOATING_MANA, mana.getSourceCard(),
                null, mana, semanticKey);
    }

    static LegalCandidate xValue(final int candidateId, final int value) {
        return new LegalCandidate(candidateId, value);
    }

    public int getCandidateId() {
        return candidateId;
    }

    public PriorityActionKind getKind() {
        return kind;
    }

    /** The TARGET candidate kind, or {@code null} when this candidate is a priority action. */
    public TargetCandidateKind getTargetKind() {
        return targetKind;
    }

    /** The PAYMENT candidate kind, or {@code null} for another decision family. */
    public PaymentCandidateKind getPaymentKind() {
        return paymentKind;
    }

    /** Announced X value for X_VALUE candidates, otherwise {@code null}. */
    public Integer getXValue() {
        return xValue;
    }

    /** Stable Forge entity or stack-instance identifier for a TARGET candidate; {@code -1} for DONE. */
    public int getTargetEntityId() {
        return targetEntityId;
    }

    /** Player-visible target name, omitted for a legally targetable face-down object. */
    public String getTargetName() {
        return targetName;
    }

    public ZoneType getTargetZone() {
        return targetZone;
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

    GameObject getTarget() {
        return target;
    }

    Mana getMana() {
        return mana;
    }
}
