package forge.game.decision;

import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCostShard;
import forge.game.GameActionUtil;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.cost.CostTap;
import forge.game.mana.Mana;
import forge.game.mana.ManaConversionMatrix;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.PlaySpellAbility;
import forge.game.player.Player;
import forge.game.replacement.ReplacementLayer;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Generates sequential PAYMENT requests from Forge's live remaining-cost state. */
public final class PaymentDecisionProvider {
    public enum Status {
        DECISION,
        COMPLETE,
        INVALID_PAYMENT,
        UNSUPPORTED
    }

    public enum UnsupportedReason {
        VARIABLE_MANA_OUTPUT,
        COMPLEX_MANA_SOURCE_COST,
        MULTIPLE_MANA_PARTS,
        PHYREXIAN_MANA,
        SNOW_MANA_PROVENANCE,
        MANA_CONVERSION_MATRIX,
        MANA_SOURCE_ALTERNATIVE_COST,
        MANA_PRODUCTION_REPLACEMENT,
        DYNAMIC_MANA_PRODUCTION,
        NONTRIVIAL_MANA_SUBABILITY
    }

    private long nextRequestId;

    public Generation generatePaymentRequest(final ManaCostBeingPaid remainingCost, final SpellAbility ability,
            final Player payer, final ManaConversionMatrix matrix, final ActionContinuation continuation) {
        final long startedAtNanos = System.nanoTime();
        Objects.requireNonNull(remainingCost);
        Objects.requireNonNull(ability);
        Objects.requireNonNull(payer);
        Objects.requireNonNull(matrix);
        if (remainingCost.isPaid()) {
            return Generation.complete(System.nanoTime() - startedAtNanos);
        }

        final PrototypeResult prototypeResult = collectPrototypes(remainingCost, ability, payer, matrix);
        if (prototypeResult.unsupportedReason() != null) {
            return Generation.unsupported(prototypeResult.unsupportedReason(),
                    System.nanoTime() - startedAtNanos);
        }
        if (prototypeResult.prototypes().isEmpty()) {
            return Generation.invalid(System.nanoTime() - startedAtNanos);
        }

        final List<LegalCandidate> candidates = new ArrayList<>();
        for (final CandidatePrototype prototype : prototypeResult.prototypes()) {
            if (prototype.kind() == PaymentCandidateKind.USE_FLOATING_MANA) {
                candidates.add(LegalCandidate.floatingMana(candidates.size(), prototype.mana(),
                        prototype.semanticKey()));
            } else {
                candidates.add(LegalCandidate.paymentSource(candidates.size(), prototype.source(),
                        prototype.ability(), prototype.semanticKey()));
            }
        }
        final Integer subdecisionIndex = continuation == null ? null : continuation.nextSubdecisionIndex();
        final PaymentDecisionContext context = new PaymentDecisionContext(payer, ability.getRootAbility(),
                remainingCost, matrix, continuation, subdecisionIndex);
        final DecisionRequest request = new DecisionRequest(nextRequestId++, DecisionType.PAYMENT, candidates,
                context);
        return Generation.decision(request, System.nanoTime() - startedAtNanos);
    }

    /** Revalidates and applies one request-local atomic payment operation through Forge. */
    public Generation apply(final DecisionRequest request, final LegalCandidate selected) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(selected);
        if (request.getDecisionType() != DecisionType.PAYMENT || request.getPaymentContext() == null) {
            throw new IllegalArgumentException("Expected a PAYMENT request");
        }
        final int candidateId = selected.getCandidateId();
        if (candidateId < 0 || candidateId >= request.getCandidates().size()
                || request.getCandidates().get(candidateId) != selected) {
            throw new IllegalArgumentException("Candidate does not belong to this PAYMENT request");
        }

        final PaymentDecisionContext context = request.getPaymentContext();
        final PrototypeResult current = collectPrototypes(context.getRemainingCost(), context.getAbility(),
                context.getPayer(), context.getMatrix());
        if (current.unsupportedReason() != null) {
            throw new IllegalStateException("Payment state became unsupported: " + current.unsupportedReason());
        }
        final CandidatePrototype live = current.prototypes().stream()
                .filter(candidate -> candidate.kind() == selected.getPaymentKind()
                        && candidate.semanticKey().equals(selected.getSemanticKey()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Stale or no longer legal PAYMENT candidate"));

        if (live.kind() == PaymentCandidateKind.USE_FLOATING_MANA) {
            if (!context.getPayer().getManaPool().tryPayCostWithManaInstance(context.getAbility(),
                    context.getRemainingCost(), live.mana(), context.getAbility().getPayingMana())) {
                throw new IllegalStateException("Floating mana candidate failed live Forge revalidation");
            }
        } else {
            if (!PlaySpellAbility.playSpellAbility(context.getPayer().getController(),
                    context.getPayer(), live.ability())) {
                throw new IllegalStateException("Forge rejected mana source activation");
            }
            context.getPayer().getManaPool().payManaFromAbility(context.getAbility(),
                    context.getRemainingCost(), live.ability());
        }

        return generatePaymentRequest(context.getRemainingCost(), context.getAbility(), context.getPayer(),
                context.getMatrix(), context.getContinuation());
    }

    private static PrototypeResult collectPrototypes(final ManaCostBeingPaid remainingCost,
            final SpellAbility ability, final Player payer, final ManaConversionMatrix matrix) {
        if (matrix != payer.getManaPool() && !matrix.isIdentity()) {
            return PrototypeResult.unsupported(UnsupportedReason.MANA_CONVERSION_MATRIX);
        }
        for (final ManaCostShard shard : remainingCost.getUnpaidShards()) {
            if (shard.isPhyrexian()) {
                return PrototypeResult.unsupported(UnsupportedReason.PHYREXIAN_MANA);
            }
            if (shard.isSnow()) {
                return PrototypeResult.unsupported(UnsupportedReason.SNOW_MANA_PROVENANCE);
            }
        }
        if (hasPlayableAlternativeManaActivation(payer)) {
            return PrototypeResult.unsupported(UnsupportedReason.MANA_SOURCE_ALTERNATIVE_COST);
        }
        final List<CandidatePrototype> prototypes = floatingManaPrototypes(remainingCost, ability, payer);
        final List<Card> cards = new ArrayList<>(payer.getCardsIn(ZoneType.Battlefield));
        cards.sort(Comparator.comparingInt(Card::getId).thenComparingLong(Card::getGameTimestamp));
        for (final Card card : cards) {
            if (!payer.equals(card.getController())) {
                continue;
            }
            int abilityIndex = 0;
            for (final SpellAbility manaAbility : card.getManaAbilities()) {
                manaAbility.setActivatingPlayer(payer);
                if (!manaAbility.canPlay()) {
                    abilityIndex++;
                    continue;
                }
                final List<AbilityManaPart> parts = manaAbility.getAllManaParts();
                if (parts.size() != 1) {
                    return PrototypeResult.unsupported(UnsupportedReason.MULTIPLE_MANA_PARTS);
                }
                final AbilityManaPart part = parts.get(0);
                if (hasProduceManaReplacement(manaAbility, part, payer)) {
                    return PrototypeResult.unsupported(UnsupportedReason.MANA_PRODUCTION_REPLACEMENT);
                }
                if (manaAbility.getSubAbility() != null) {
                    return PrototypeResult.unsupported(UnsupportedReason.NONTRIVIAL_MANA_SUBABILITY);
                }
                if (manaAbility.hasParam("Amount")
                        && !manaAbility.getParam("Amount").matches("\\d+")) {
                    return PrototypeResult.unsupported(UnsupportedReason.DYNAMIC_MANA_PRODUCTION);
                }
                if (!productionCouldAdvance(part, remainingCost, ability, payer)) {
                    abilityIndex++;
                    continue;
                }
                if (!isTapOnly(manaAbility)) {
                    return PrototypeResult.unsupported(UnsupportedReason.COMPLEX_MANA_SOURCE_COST);
                }
                if (!isFixedProduction(part)) {
                    return PrototypeResult.unsupported(UnsupportedReason.VARIABLE_MANA_OUTPUT);
                }
                final String key = "SOURCE|" + card.getId() + "|" + card.getGameTimestamp() + "|"
                        + abilityIndex + "|" + part.getOrigProduced();
                prototypes.add(CandidatePrototype.source(card, manaAbility, key));
                abilityIndex++;
            }
        }
        prototypes.sort(Comparator.comparing(CandidatePrototype::semanticKey));
        return PrototypeResult.supported(prototypes);
    }

    private static boolean hasPlayableAlternativeManaActivation(final Player payer) {
        final List<Card> cards = new ArrayList<>(payer.getGame().getCardsIn(ZoneType.Battlefield));
        cards.sort(Comparator.comparingInt(Card::getId).thenComparingLong(Card::getGameTimestamp));
        for (final Card card : cards) {
            for (final SpellAbility baseAbility : card.getManaAbilities()) {
                for (final SpellAbility alternative
                        : GameActionUtil.getAlternativeCosts(baseAbility, payer, false)) {
                    alternative.setActivatingPlayer(payer);
                    if (alternative.isManaAbility() && alternative.canPlay(true)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean hasProduceManaReplacement(final SpellAbility manaAbility,
            final AbilityManaPart part, final Player payer) {
        final Card source = manaAbility.getHostCard();
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromAffected(source);
        runParams.put(AbilityKey.Mana, part.getOrigProduced());
        runParams.put(AbilityKey.Player, payer);
        runParams.put(AbilityKey.AbilityMana, manaAbility.getRootAbility());
        runParams.put(AbilityKey.Activator, payer);
        return !source.getGame().getReplacementHandler().getReplacementList(
                ReplacementType.ProduceMana, runParams, ReplacementLayer.Other).isEmpty();
    }

    private static List<CandidatePrototype> floatingManaPrototypes(final ManaCostBeingPaid remainingCost,
            final SpellAbility ability, final Player payer) {
        final List<Mana> legal = new ArrayList<>();
        for (final Mana mana : payer.getManaPool()) {
            if (mana.meetsManaRestrictions(ability)
                    && ability.allowsPayingWithShard(mana.getSourceCard(), mana.getColor())
                    && remainingCost.isNeeded(mana, payer.getManaPool())) {
                legal.add(mana);
            }
        }
        legal.sort(Comparator.comparing(PaymentDecisionProvider::floatingManaBaseKey));
        final Map<String, Integer> occurrenceByBaseKey = new HashMap<>();
        final List<CandidatePrototype> result = new ArrayList<>();
        for (final Mana mana : legal) {
            final String baseKey = floatingManaBaseKey(mana);
            final int occurrence = occurrenceByBaseKey.merge(baseKey, 1, Integer::sum) - 1;
            result.add(CandidatePrototype.floating(mana, baseKey + "|" + occurrence));
        }
        return result;
    }

    private static String floatingManaBaseKey(final Mana mana) {
        final AbilityManaPart part = mana.getManaAbility();
        return "FLOATING|" + mana.getSourceCard().getId() + "|"
                + mana.getSourceCard().getGameTimestamp() + "|" + mana.getColor() + "|" + mana.isSnow()
                + "|" + (part == null ? "" : part.getOrigProduced())
                + "|" + (part == null ? "" : part.getManaRestrictions())
                + "|" + (part == null ? "" : part.getExtraManaRestriction())
                + "|" + mana.isPersistentMana() + "|" + mana.isCombatMana()
                + "|" + mana.triggersWhenSpent()
                + "|" + (part != null && part.isCannotCounterPaidWith())
                + "|" + (part != null && part.addsCounters(null))
                + "|" + (part == null ? "" : part.getKeywords())
                + "|" + (part == null ? "" : part.getAddsKeywordsType())
                + "|" + (part == null ? "" : part.getAddsKeywordsUntil());
    }

    private static boolean isTapOnly(final SpellAbility ability) {
        final Cost cost = ability.getPayCosts();
        return cost != null && cost.getCostParts().size() == 1
                && cost.getCostParts().get(0) instanceof CostTap;
    }

    private static boolean isFixedProduction(final AbilityManaPart part) {
        final String produced = part.getOrigProduced();
        return !produced.isEmpty() && !part.isAnyMana() && !part.isComboMana() && !part.isSpecialMana()
                && !produced.contains("Chosen");
    }

    private static boolean productionCouldAdvance(final AbilityManaPart part,
            final ManaCostBeingPaid remainingCost, final SpellAbility ability, final Player payer) {
        if (!part.meetsManaRestrictions(ability)) {
            return false;
        }
        final String produced = part.getOrigProduced();
        if (part.isAnyMana() || part.isComboMana() || part.isSpecialMana() || produced.contains("Chosen")) {
            for (final byte color : new byte[] {ManaAtom.WHITE, ManaAtom.BLUE, ManaAtom.BLACK,
                    ManaAtom.RED, ManaAtom.GREEN, ManaAtom.COLORLESS}) {
                if (ability.allowsPayingWithShard(part.getSourceCard(), color)
                        && !remainingCost.getPaymentVariants(color, part.isSnow(), part,
                                payer.getManaPool()).isEmpty()) {
                    return true;
                }
            }
            return false;
        }
        for (final String value : produced.split(" ")) {
            final byte color = value.chars().allMatch(Character::isDigit)
                    ? (byte) ManaAtom.COLORLESS : ManaAtom.fromName(value);
            if (color != 0 && ability.allowsPayingWithShard(part.getSourceCard(), color)
                    && !remainingCost.getPaymentVariants(color, part.isSnow(), part,
                            payer.getManaPool()).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private record CandidatePrototype(PaymentCandidateKind kind, Card source, SpellAbility ability,
                                      Mana mana, String semanticKey) {
        private static CandidatePrototype source(final Card source, final SpellAbility ability,
                final String key) {
            return new CandidatePrototype(PaymentCandidateKind.ACTIVATE_MANA_SOURCE, source, ability, null, key);
        }

        private static CandidatePrototype floating(final Mana mana, final String key) {
            return new CandidatePrototype(PaymentCandidateKind.USE_FLOATING_MANA, mana.getSourceCard(),
                    null, mana, key);
        }
    }

    private record PrototypeResult(List<CandidatePrototype> prototypes,
                                   UnsupportedReason unsupportedReason) {
        private static PrototypeResult supported(final List<CandidatePrototype> prototypes) {
            return new PrototypeResult(prototypes, null);
        }

        private static PrototypeResult unsupported(final UnsupportedReason reason) {
            return new PrototypeResult(List.of(), reason);
        }
    }

    public static final class Generation {
        private final Status status;
        private final DecisionRequest request;
        private final UnsupportedReason unsupportedReason;
        private final long requestGenerationNanos;

        private Generation(final Status status, final DecisionRequest request,
                final UnsupportedReason unsupportedReason, final long requestGenerationNanos) {
            this.status = status;
            this.request = request;
            this.unsupportedReason = unsupportedReason;
            this.requestGenerationNanos = requestGenerationNanos;
        }

        private static Generation decision(final DecisionRequest request, final long duration) {
            return new Generation(Status.DECISION, request, null, duration);
        }

        private static Generation complete(final long duration) {
            return new Generation(Status.COMPLETE, null, null, duration);
        }

        private static Generation invalid(final long duration) {
            return new Generation(Status.INVALID_PAYMENT, null, null, duration);
        }

        private static Generation unsupported(final UnsupportedReason reason, final long duration) {
            return new Generation(Status.UNSUPPORTED, null, reason, duration);
        }

        public Status getStatus() {
            return status;
        }

        public DecisionRequest getRequest() {
            return request;
        }

        public UnsupportedReason getUnsupportedReason() {
            return unsupportedReason;
        }

        public long getRequestGenerationNanos() {
            return requestGenerationNanos;
        }
    }
}
