package forge.game.decision;

import forge.card.CardStateName;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.mana.Mana;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.AbilitySub;
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
    private final Integer modeOrdinal;
    private final String modeDescription;
    private final boolean modeUsesTargeting;
    private final AbilitySub mode;
    private final CardSelectionCandidateKind cardSelectionKind;
    private final CardSelectionCard cardSelectionCard;
    private final AttackDeclarationCandidateKind attackKind;
    private final AttackDeclarationCard attackCard;
    private final AttackDeclarationDefender attackDefender;
    private final BlockDeclarationCandidateKind blockKind;
    private final BlockDeclarationCard blockCard;
    private final BlockDeclarationCard blockAttacker;
    private final ConfirmationCandidateKind confirmationKind;
    private final OrderCandidateKind orderKind;
    private final SimultaneousTriggerOrderItem orderItem;
    private final CopySpellResolveFirstOrderItemKind copySpellResolveFirstOrderKind;
    private final CopySpellResolveFirstOrderItem copySpellResolveFirstOrderItem;
    private final SurveilPartitionCandidateKind surveilPartitionCandidateKind;
    private final SurveilPartitionCard surveilPartitionCard;

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
        this.modeOrdinal = null;
        this.modeDescription = "";
        this.modeUsesTargeting = false;
        this.mode = null;
        this.cardSelectionKind = null;
        this.cardSelectionCard = null;
        this.attackKind = null;
        this.attackCard = null;
        this.attackDefender = null;
        this.blockKind = null;
        this.blockCard = null;
        this.blockAttacker = null;
        this.confirmationKind = null;
        this.orderKind = null;
        this.orderItem = null;
        this.copySpellResolveFirstOrderKind = null;
        this.copySpellResolveFirstOrderItem = null;
        this.surveilPartitionCandidateKind = null;
        this.surveilPartitionCard = null;
    }

    private LegalCandidate(final int candidateId, final String semanticKey) {
        this.candidateId = candidateId;
        this.kind = null;
        this.sourceCardId = -1;
        this.sourceName = "";
        this.sourceZone = null;
        this.sourceState = null;
        this.abilityDescription = "";
        this.semanticKey = Objects.requireNonNull(semanticKey);
        this.spellAbility = null;
        this.targetKind = null;
        this.targetEntityId = -1;
        this.targetName = "";
        this.targetZone = null;
        this.target = null;
        this.paymentKind = null;
        this.mana = null;
        this.xValue = null;
        this.modeOrdinal = null;
        this.modeDescription = "";
        this.modeUsesTargeting = false;
        this.mode = null;
        this.cardSelectionKind = null;
        this.cardSelectionCard = null;
        this.attackKind = null;
        this.attackCard = null;
        this.attackDefender = null;
        this.blockKind = null;
        this.blockCard = null;
        this.blockAttacker = null;
        this.confirmationKind = null;
        this.orderKind = null;
        this.orderItem = null;
        this.copySpellResolveFirstOrderKind = null;
        this.copySpellResolveFirstOrderItem = null;
        this.surveilPartitionCandidateKind = null;
        this.surveilPartitionCard = null;
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
        this.modeOrdinal = null;
        this.modeDescription = "";
        this.modeUsesTargeting = false;
        this.mode = null;
        this.cardSelectionKind = null;
        this.cardSelectionCard = null;
        this.attackKind = null;
        this.attackCard = null;
        this.attackDefender = null;
        this.blockKind = null;
        this.blockCard = null;
        this.blockAttacker = null;
        this.confirmationKind = null;
        this.orderKind = null;
        this.orderItem = null;
        this.copySpellResolveFirstOrderKind = null;
        this.copySpellResolveFirstOrderItem = null;
        this.surveilPartitionCandidateKind = null;
        this.surveilPartitionCard = null;
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
        this.modeOrdinal = null;
        this.modeDescription = "";
        this.modeUsesTargeting = false;
        this.mode = null;
        this.cardSelectionKind = null;
        this.cardSelectionCard = null;
        this.attackKind = null;
        this.attackCard = null;
        this.attackDefender = null;
        this.blockKind = null;
        this.blockCard = null;
        this.blockAttacker = null;
        this.confirmationKind = null;
        this.orderKind = null;
        this.orderItem = null;
        this.copySpellResolveFirstOrderKind = null;
        this.copySpellResolveFirstOrderItem = null;
        this.surveilPartitionCandidateKind = null;
        this.surveilPartitionCard = null;
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
        this.modeOrdinal = null;
        this.modeDescription = "";
        this.modeUsesTargeting = false;
        this.mode = null;
        this.cardSelectionKind = null;
        this.cardSelectionCard = null;
        this.attackKind = null;
        this.attackCard = null;
        this.attackDefender = null;
        this.blockKind = null;
        this.blockCard = null;
        this.blockAttacker = null;
        this.confirmationKind = null;
        this.orderKind = null;
        this.orderItem = null;
        this.copySpellResolveFirstOrderKind = null;
        this.copySpellResolveFirstOrderItem = null;
        this.surveilPartitionCandidateKind = null;
        this.surveilPartitionCard = null;
    }

    private LegalCandidate(final int candidateId, final int modeOrdinal, final AbilitySub mode) {
        this.candidateId = candidateId;
        this.kind = null;
        this.sourceCardId = mode.getHostCard().getId();
        this.sourceName = mode.getHostCard().getName();
        this.sourceZone = mode.getHostCard().getZone() == null ? null : mode.getHostCard().getZone().getZoneType();
        this.sourceState = mode.getHostCard().getCurrentStateName();
        this.abilityDescription = mode.getDescription();
        this.semanticKey = "MODE|" + modeOrdinal;
        this.spellAbility = null;
        this.targetKind = null;
        this.targetEntityId = -1;
        this.targetName = "";
        this.targetZone = null;
        this.target = null;
        this.paymentKind = null;
        this.mana = null;
        this.xValue = null;
        this.modeOrdinal = modeOrdinal;
        this.modeDescription = mode.getParamOrDefault("SpellDescription", mode.getDescription());
        this.modeUsesTargeting = branchUsesTargeting(mode);
        this.mode = mode;
        this.cardSelectionKind = null;
        this.cardSelectionCard = null;
        this.attackKind = null;
        this.attackCard = null;
        this.attackDefender = null;
        this.blockKind = null;
        this.blockCard = null;
        this.blockAttacker = null;
        this.confirmationKind = null;
        this.orderKind = null;
        this.orderItem = null;
        this.copySpellResolveFirstOrderKind = null;
        this.copySpellResolveFirstOrderItem = null;
        this.surveilPartitionCandidateKind = null;
        this.surveilPartitionCard = null;
    }

    private LegalCandidate(final int candidateId, final CardSelectionCandidateKind cardSelectionKind,
            final CardSelectionCard cardSelectionCard) {
        this.candidateId = candidateId;
        this.kind = null;
        this.sourceCardId = -1;
        this.sourceName = "";
        this.sourceZone = null;
        this.sourceState = null;
        this.abilityDescription = "";
        this.semanticKey = cardSelectionKind == CardSelectionCandidateKind.DONE
                ? "DONE" : cardSelectionCard.selectionSemanticKey();
        this.spellAbility = null;
        this.targetKind = null;
        this.targetEntityId = -1;
        this.targetName = "";
        this.targetZone = null;
        this.target = null;
        this.paymentKind = null;
        this.mana = null;
        this.xValue = null;
        this.modeOrdinal = null;
        this.modeDescription = "";
        this.modeUsesTargeting = false;
        this.mode = null;
        this.cardSelectionKind = Objects.requireNonNull(cardSelectionKind);
        this.cardSelectionCard = cardSelectionCard;
        this.attackKind = null;
        this.attackCard = null;
        this.attackDefender = null;
        this.blockKind = null;
        this.blockCard = null;
        this.blockAttacker = null;
        this.confirmationKind = null;
        this.orderKind = null;
        this.orderItem = null;
        this.copySpellResolveFirstOrderKind = null;
        this.copySpellResolveFirstOrderItem = null;
        this.surveilPartitionCandidateKind = null;
        this.surveilPartitionCard = null;
    }

    private LegalCandidate(final int candidateId, final String semanticKey,
            final SurveilPartitionCandidateKind surveilPartitionCandidateKind,
            final SurveilPartitionCard surveilPartitionCard) {
        this.candidateId = candidateId;
        this.kind = null;
        this.sourceCardId = -1;
        this.sourceName = "";
        this.sourceZone = null;
        this.sourceState = null;
        this.abilityDescription = "";
        this.semanticKey = Objects.requireNonNull(semanticKey);
        this.spellAbility = null;
        this.targetKind = null;
        this.targetEntityId = -1;
        this.targetName = "";
        this.targetZone = null;
        this.target = null;
        this.paymentKind = null;
        this.mana = null;
        this.xValue = null;
        this.modeOrdinal = null;
        this.modeDescription = "";
        this.modeUsesTargeting = false;
        this.mode = null;
        this.cardSelectionKind = null;
        this.cardSelectionCard = null;
        this.attackKind = null;
        this.attackCard = null;
        this.attackDefender = null;
        this.blockKind = null;
        this.blockCard = null;
        this.blockAttacker = null;
        this.confirmationKind = null;
        this.orderKind = null;
        this.orderItem = null;
        this.copySpellResolveFirstOrderKind = null;
        this.copySpellResolveFirstOrderItem = null;
        this.surveilPartitionCandidateKind = Objects.requireNonNull(surveilPartitionCandidateKind);
        this.surveilPartitionCard = Objects.requireNonNull(surveilPartitionCard);
    }

    private LegalCandidate(final int candidateId, final AttackDeclarationCandidateKind attackKind,
            final AttackDeclarationCard attackCard, final AttackDeclarationDefender attackDefender) {
        this.candidateId = candidateId;
        this.kind = null;
        this.sourceCardId = -1;
        this.sourceName = "";
        this.sourceZone = null;
        this.sourceState = null;
        this.abilityDescription = "";
        this.attackKind = Objects.requireNonNull(attackKind);
        this.attackCard = attackCard;
        this.attackDefender = attackDefender;
        this.semanticKey = attackKind == AttackDeclarationCandidateKind.DONE
                ? "DONE" : "ADD_ATTACKER|" + attackCard.getCardId() + "|"
                        + attackCard.getGameTimestamp() + "|" + attackDefender.identityKey();
        this.spellAbility = null;
        this.targetKind = null;
        this.targetEntityId = -1;
        this.targetName = "";
        this.targetZone = null;
        this.target = null;
        this.paymentKind = null;
        this.mana = null;
        this.xValue = null;
        this.modeOrdinal = null;
        this.modeDescription = "";
        this.modeUsesTargeting = false;
        this.mode = null;
        this.cardSelectionKind = null;
        this.cardSelectionCard = null;
        this.blockKind = null;
        this.blockCard = null;
        this.blockAttacker = null;
        this.confirmationKind = null;
        this.orderKind = null;
        this.orderItem = null;
        this.copySpellResolveFirstOrderKind = null;
        this.copySpellResolveFirstOrderItem = null;
        this.surveilPartitionCandidateKind = null;
        this.surveilPartitionCard = null;
    }

    private LegalCandidate(final int candidateId, final BlockDeclarationCandidateKind blockKind,
            final BlockDeclarationCard blockCard, final BlockDeclarationCard blockAttacker) {
        this.candidateId = candidateId;
        this.kind = null;
        this.sourceCardId = -1;
        this.sourceName = "";
        this.sourceZone = null;
        this.sourceState = null;
        this.abilityDescription = "";
        this.semanticKey = blockKind == BlockDeclarationCandidateKind.DONE
                ? "DONE" : blockKind.name() + "|" + (blockCard == null ? "" : blockCard.identityKey())
                        + (blockAttacker == null ? "" : "|" + blockAttacker.identityKey());
        this.spellAbility = null;
        this.targetKind = null;
        this.targetEntityId = -1;
        this.targetName = "";
        this.targetZone = null;
        this.target = null;
        this.paymentKind = null;
        this.mana = null;
        this.xValue = null;
        this.modeOrdinal = null;
        this.modeDescription = "";
        this.modeUsesTargeting = false;
        this.mode = null;
        this.cardSelectionKind = null;
        this.cardSelectionCard = null;
        this.attackKind = null;
        this.attackCard = null;
        this.attackDefender = null;
        this.blockKind = Objects.requireNonNull(blockKind);
        this.blockCard = blockCard;
        this.blockAttacker = blockAttacker;
        this.confirmationKind = null;
        this.orderKind = null;
        this.orderItem = null;
        this.copySpellResolveFirstOrderKind = null;
        this.copySpellResolveFirstOrderItem = null;
        this.surveilPartitionCandidateKind = null;
        this.surveilPartitionCard = null;
    }

    private static boolean branchUsesTargeting(final SpellAbility first) {
        SpellAbility current = first;
        while (current != null) {
            if (current.usesTargeting()) {
                return true;
            }
            current = current.getSubAbility();
        }
        return false;
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

    static LegalCandidate mode(final int candidateId, final int modeOrdinal, final AbilitySub mode) {
        return new LegalCandidate(candidateId, modeOrdinal, mode);
    }

    static LegalCandidate selectCard(final int candidateId, final CardSelectionCard card) {
        return new LegalCandidate(candidateId, CardSelectionCandidateKind.SELECT_CARD, Objects.requireNonNull(card));
    }

    public static LegalCandidate surveilPartition(final int candidateId,
            final SurveilPartitionCandidateKind kind, final SurveilPartitionCard item) {
        final SurveilPartitionCandidateKind requiredKind = Objects.requireNonNull(kind);
        final SurveilPartitionCard requiredItem = Objects.requireNonNull(item);
        final String operation = requiredKind == SurveilPartitionCandidateKind.CLASSIFY_GRAVEYARD
                ? "CLASSIFY_GRAVEYARD" : "CLASSIFY_RETAIN";
        return new LegalCandidate(candidateId,
                "SURVEIL_PARTITION|" + operation + "|" + requiredItem.getItemId(),
                requiredKind, requiredItem);
    }

    static LegalCandidate cardSelectionDone(final int candidateId) {
        return new LegalCandidate(candidateId, CardSelectionCandidateKind.DONE, null);
    }

    static LegalCandidate addAttacker(final int candidateId, final AttackDeclarationCard card,
            final AttackDeclarationDefender defender) {
        return new LegalCandidate(candidateId, AttackDeclarationCandidateKind.ADD_ATTACKER, card, defender);
    }

    static LegalCandidate attackDone(final int candidateId) {
        return new LegalCandidate(candidateId, AttackDeclarationCandidateKind.DONE, null, null);
    }

    static LegalCandidate chooseBlocker(final int candidateId, final BlockDeclarationCard blocker) {
        return new LegalCandidate(candidateId, BlockDeclarationCandidateKind.CHOOSE_BLOCKER, blocker, null);
    }

    static LegalCandidate chooseAttacker(final int candidateId, final BlockDeclarationCard blocker,
            final BlockDeclarationCard attacker) {
        return new LegalCandidate(candidateId, BlockDeclarationCandidateKind.CHOOSE_ATTACKER_FOR_BLOCKER,
                blocker, attacker);
    }

    static LegalCandidate blockDone(final int candidateId) {
        return new LegalCandidate(candidateId, BlockDeclarationCandidateKind.DONE, null, null);
    }

    static LegalCandidate mulligan(final int candidateId, final MulliganCandidateKind kind) {
        return new LegalCandidate(candidateId, Objects.requireNonNull(kind).semanticKey());
    }

    static LegalCandidate confirmation(final int candidateId, final ConfirmationCandidateKind kind) {
        return new LegalCandidate(candidateId, Objects.requireNonNull(kind));
    }

    static LegalCandidate order(final int candidateId, final OrderCandidateKind kind,
            final SimultaneousTriggerOrderItem item) {
        return new LegalCandidate(candidateId, Objects.requireNonNull(kind), Objects.requireNonNull(item));
    }

    static LegalCandidate copySpellResolveFirstOrder(final int candidateId,
            final CopySpellResolveFirstOrderItemKind kind,
            final CopySpellResolveFirstOrderItem item) {
        return new LegalCandidate(candidateId, Objects.requireNonNull(kind), Objects.requireNonNull(item));
    }

    private LegalCandidate(final int candidateId, final ConfirmationCandidateKind kind) {
        this.candidateId = candidateId;
        this.kind = null;
        this.sourceCardId = -1;
        this.sourceName = "";
        this.sourceZone = null;
        this.sourceState = null;
        this.abilityDescription = "";
        this.semanticKey = kind.semanticKey();
        this.spellAbility = null;
        this.targetKind = null;
        this.targetEntityId = -1;
        this.targetName = "";
        this.targetZone = null;
        this.target = null;
        this.paymentKind = null;
        this.mana = null;
        this.xValue = null;
        this.modeOrdinal = null;
        this.modeDescription = "";
        this.modeUsesTargeting = false;
        this.mode = null;
        this.cardSelectionKind = null;
        this.cardSelectionCard = null;
        this.attackKind = null;
        this.attackCard = null;
        this.attackDefender = null;
        this.blockKind = null;
        this.blockCard = null;
        this.blockAttacker = null;
        this.confirmationKind = kind;
        this.orderKind = null;
        this.orderItem = null;
        this.copySpellResolveFirstOrderKind = null;
        this.copySpellResolveFirstOrderItem = null;
        this.surveilPartitionCandidateKind = null;
        this.surveilPartitionCard = null;
    }

    private LegalCandidate(final int candidateId, final OrderCandidateKind kind,
            final SimultaneousTriggerOrderItem item) {
        this.candidateId = candidateId;
        this.kind = null;
        this.sourceCardId = -1;
        this.sourceName = "";
        this.sourceZone = null;
        this.sourceState = null;
        this.abilityDescription = "";
        this.semanticKey = "RESOLVE_FIRST|" + item.getItemId();
        this.spellAbility = null;
        this.targetKind = null;
        this.targetEntityId = -1;
        this.targetName = "";
        this.targetZone = null;
        this.target = null;
        this.paymentKind = null;
        this.mana = null;
        this.xValue = null;
        this.modeOrdinal = null;
        this.modeDescription = "";
        this.modeUsesTargeting = false;
        this.mode = null;
        this.cardSelectionKind = null;
        this.cardSelectionCard = null;
        this.attackKind = null;
        this.attackCard = null;
        this.attackDefender = null;
        this.blockKind = null;
        this.blockCard = null;
        this.blockAttacker = null;
        this.confirmationKind = null;
        this.orderKind = kind;
        this.orderItem = item;
        this.copySpellResolveFirstOrderKind = null;
        this.copySpellResolveFirstOrderItem = null;
        this.surveilPartitionCandidateKind = null;
        this.surveilPartitionCard = null;
    }

    private LegalCandidate(final int candidateId, final CopySpellResolveFirstOrderItemKind kind,
            final CopySpellResolveFirstOrderItem item) {
        this.candidateId = candidateId;
        this.kind = null;
        this.sourceCardId = -1;
        this.sourceName = "";
        this.sourceZone = null;
        this.sourceState = null;
        this.abilityDescription = "";
        this.semanticKey = "RESOLVE_FIRST|" + item.getItemId();
        this.spellAbility = null;
        this.targetKind = null;
        this.targetEntityId = -1;
        this.targetName = "";
        this.targetZone = null;
        this.target = null;
        this.paymentKind = null;
        this.mana = null;
        this.xValue = null;
        this.modeOrdinal = null;
        this.modeDescription = "";
        this.modeUsesTargeting = false;
        this.mode = null;
        this.cardSelectionKind = null;
        this.cardSelectionCard = null;
        this.attackKind = null;
        this.attackCard = null;
        this.attackDefender = null;
        this.blockKind = null;
        this.blockCard = null;
        this.blockAttacker = null;
        this.confirmationKind = null;
        this.orderKind = null;
        this.orderItem = null;
        this.copySpellResolveFirstOrderKind = Objects.requireNonNull(kind);
        this.copySpellResolveFirstOrderItem = Objects.requireNonNull(item);
        this.surveilPartitionCandidateKind = null;
        this.surveilPartitionCard = null;
        if (kind != item.getKind()) {
            throw new IllegalArgumentException("L1C candidate kind does not match item kind");
        }
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

    /** Original zero-based position in the root Choices list, otherwise {@code null}. */
    public Integer getModeOrdinal() {
        return modeOrdinal;
    }

    public String getModeDescription() {
        return modeDescription;
    }

    public boolean isModeUsesTargeting() {
        return modeUsesTargeting;
    }

    /** CARD_SELECTION operation, otherwise {@code null}. */
    public CardSelectionCandidateKind getCardSelectionKind() {
        return cardSelectionKind;
    }

    /** Visible stable card identity for SELECT_CARD, otherwise {@code null}. */
    public CardSelectionCard getCardSelectionCard() {
        return cardSelectionCard;
    }

    /** Surveil partition operation, otherwise {@code null}. */
    public SurveilPartitionCandidateKind getSurveilPartitionCandidateKind() {
        return surveilPartitionCandidateKind;
    }

    /** Chooser-visible Surveil item projection, otherwise {@code null}. */
    public SurveilPartitionCard getSurveilPartitionCard() {
        return surveilPartitionCard;
    }

    /** ATTACK candidate kind, otherwise {@code null}. */
    public AttackDeclarationCandidateKind getAttackKind() {
        return attackKind;
    }

    /** Visible stable attacker identity for ADD_ATTACKER, otherwise {@code null}. */
    public AttackDeclarationCard getAttackCard() {
        return attackCard;
    }

    /** Visible stable defender identity for ADD_ATTACKER, otherwise {@code null}. */
    public AttackDeclarationDefender getAttackDefender() {
        return attackDefender;
    }

    /** BLOCK candidate kind, otherwise {@code null}. */
    public BlockDeclarationCandidateKind getBlockKind() {
        return blockKind;
    }

    /** MULLIGAN candidate kind, or {@code null} for another decision family. */
    public MulliganCandidateKind getMulliganKind() {
        return MulliganCandidateKind.fromSemanticKey(semanticKey);
    }

    /** CONFIRMATION candidate kind, or {@code null} for another decision family. */
    public ConfirmationCandidateKind getConfirmationKind() {
        return confirmationKind;
    }

    public OrderCandidateKind getOrderKind() {
        return orderKind;
    }

    public SimultaneousTriggerOrderItem getOrderItem() {
        return orderItem;
    }

    public CopySpellResolveFirstOrderItemKind getCopySpellResolveFirstOrderKind() {
        return copySpellResolveFirstOrderKind;
    }

    public CopySpellResolveFirstOrderItem getCopySpellResolveFirstOrderItem() {
        return copySpellResolveFirstOrderItem;
    }

    /** Stable blocker identity for BLOCK candidates, otherwise {@code null}. */
    public BlockDeclarationCard getBlockerCard() {
        return blockCard;
    }

    /** Stable attacker identity for CHOOSE_ATTACKER_FOR_BLOCKER candidates, otherwise {@code null}. */
    public BlockDeclarationCard getBlockAttackerCard() {
        return blockAttacker;
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

    AbilitySub getMode() {
        return mode;
    }
}
