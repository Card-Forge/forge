package forge.game.decision;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.combat.AttackConstraints;
import forge.game.combat.AttackRequirement;
import forge.game.combat.AttackRestriction;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.combat.GlobalAttackRestrictions;
import forge.game.cost.CostEnlist;
import forge.game.cost.CostExert;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Callback-local, mutation-free state for a constraint-free ATTACK declaration. */
public final class AttackDeclarationSession {
    private static final long NO_ACTIVE_REQUEST = -1L;

    private final long attackSessionId;
    private final int gameId;
    private final Player attackingPlayer;
    private final Player whoDeclares;
    private final Combat combat;
    private final AttackDeclarationDefender soleDefender;
    private final GameEntity soleDefenderEntity;
    private final Map<String, Card> eligibleCards;
    private final Map<String, AttackDeclarationCard> eligibleIdentities;
    private final Map<String, AttackDeclarationAssignment> selectedAssignments = new LinkedHashMap<>();
    private final boolean allowPremutatedCombat;
    private int nextStepIndex;
    private long activeRequestId = NO_ACTIVE_REQUEST;
    private boolean completed;
    private List<AttackDeclarationAssignment> completedAssignments = List.of();

    AttackDeclarationSession(final long attackSessionId, final Player attackingPlayer, final Player whoDeclares,
            final Combat combat, final AttackDeclarationDefender soleDefender, final GameEntity soleDefenderEntity,
            final Iterable<Card> eligibleCards) {
        this(attackSessionId, attackingPlayer, whoDeclares, combat, soleDefender, soleDefenderEntity, eligibleCards,
                false);
    }

    private AttackDeclarationSession(final long attackSessionId, final Player attackingPlayer, final Player whoDeclares,
            final Combat combat, final AttackDeclarationDefender soleDefender, final GameEntity soleDefenderEntity,
            final Iterable<Card> eligibleCards, final boolean allowPremutatedCombat) {
        this.attackSessionId = attackSessionId;
        this.gameId = attackingPlayer.getGame().getId();
        this.attackingPlayer = attackingPlayer;
        this.whoDeclares = whoDeclares;
        this.combat = combat;
        this.soleDefender = soleDefender;
        this.soleDefenderEntity = soleDefenderEntity;
        this.allowPremutatedCombat = allowPremutatedCombat;
        this.eligibleCards = new LinkedHashMap<>();
        this.eligibleIdentities = new LinkedHashMap<>();
        for (final Card card : eligibleCards) {
            final AttackDeclarationCard identity = new AttackDeclarationCard(card.getId(), card.getGameTimestamp(),
                    card.getName(), card.getZone() == null ? null : card.getZone().getZoneType(),
                    card.getController() == null ? -1 : card.getController().getId());
            this.eligibleCards.put(identity.identityKey(), card);
            this.eligibleIdentities.put(identity.identityKey(), identity);
        }
    }

    AttackDeclarationSession copyForReplay() {
        return new AttackDeclarationSession(attackSessionId, attackingPlayer, whoDeclares, combat, soleDefender,
                soleDefenderEntity, eligibleCards.values(), true);
    }

    public long getAttackSessionId() {
        return attackSessionId;
    }

    public int getGameId() {
        return gameId;
    }

    Player getAttackingPlayer() {
        return attackingPlayer;
    }

    Player getWhoDeclares() {
        return whoDeclares;
    }

    Combat getCombat() {
        return combat;
    }

    public AttackDeclarationDefender getSoleDefenderIdentity() {
        return soleDefender;
    }

    public List<AttackDeclarationCard> getEligibleIdentities() {
        return List.copyOf(eligibleIdentities.values());
    }

    public List<AttackDeclarationAssignment> getSelectedAssignments() {
        return List.copyOf(selectedAssignments.values());
    }

    public int getNextAttackStepIndex() {
        return nextStepIndex;
    }

    int allocateStepIndex() {
        return nextStepIndex++;
    }

    List<AttackDeclarationCard> remainingIdentities() {
        final List<AttackDeclarationCard> remaining = new ArrayList<>();
        for (final AttackDeclarationCard identity : eligibleIdentities.values()) {
            if (!selectedAssignments.containsKey(identity.identityKey())) {
                remaining.add(identity);
            }
        }
        return remaining;
    }

    Card liveCard(final AttackDeclarationCard identity) {
        for (final Card card : attackingPlayer.getCardsIn(ZoneType.Battlefield)) {
            if (card.getId() == identity.getCardId()
                    && card.getGameTimestamp() == identity.getGameTimestamp()) {
                return card;
            }
        }
        return null;
    }

    boolean isEligible(final AttackDeclarationCard identity) {
        return identity != null && eligibleIdentities.containsKey(identity.identityKey());
    }

    boolean isSelected(final AttackDeclarationCard identity) {
        return identity != null && selectedAssignments.containsKey(identity.identityKey());
    }

    boolean select(final AttackDeclarationCard identity) {
        if (!isEligible(identity) || isSelected(identity) || liveCard(identity) == null) {
            return false;
        }
        selectedAssignments.put(identity.identityKey(), new AttackDeclarationAssignment(identity, soleDefender));
        return true;
    }

    boolean revalidate() {
        if (!attackingPlayer.isInGame() || !whoDeclares.isInGame()
                || attackingPlayer.getGame().getId() != gameId || whoDeclares.getGame().getId() != gameId
                || selectedAssignments.size() > eligibleIdentities.size()) {
            return false;
        }
        if (!allowPremutatedCombat && !combat.getAttackers().isEmpty()) {
            return false;
        }
        if (!isConstraintFree()) {
            return false;
        }
        final GameEntity currentDefender = liveDefender();
        if (currentDefender != soleDefenderEntity || !(currentDefender instanceof Player)
                || !((Player) currentDefender).isOpponentOf(attackingPlayer)
                || currentDefender.getId() != soleDefender.getEntityId()) {
            return false;
        }
        final CardCollection currentEligible = new CardCollection();
        for (final AttackDeclarationCard identity : eligibleIdentities.values()) {
            final Card live = liveCard(identity);
            if (live == null || live.getController() != attackingPlayer
                    || !CombatUtil.canAttack(live, currentDefender)
                    || CombatUtil.getAttackCost(attackingPlayer.getGame(), live, currentDefender) != null
                    || live.hasKeyword(Keyword.BANDING) || live.hasKeyword(Keyword.BANDSWITH)) {
                return false;
            }
            currentEligible.add(live);
        }
        if (!CombatUtil.getOptionalAttackCostCreatures(currentEligible, CostExert.class).isEmpty()
                || !CombatUtil.getOptionalAttackCostCreatures(currentEligible, CostEnlist.class).isEmpty()) {
            return false;
        }
        return true;
    }

    private boolean isConstraintFree() {
        final AttackConstraints constraints = combat.getAttackConstraints();
        final GlobalAttackRestrictions global = constraints.getGlobalRestrictions();
        if (global.getMax() != null || !global.getDefenderMax().isEmpty()) {
            return false;
        }
        for (final AttackRestriction restriction : constraints.getRestrictions().values()) {
            if (!restriction.getTypes().isEmpty()) {
                return false;
            }
        }
        for (final AttackRequirement requirement : constraints.getRequirements().values()) {
            if (requirement.hasRequirement()) {
                return false;
            }
        }
        return constraints.countViolations(Collections.emptyMap()) == 0;
    }

    private GameEntity liveDefender() {
        return attackingPlayer.getGame().getPlayers().stream()
                .filter(player -> player.getId() == soleDefender.getEntityId())
                .findFirst().orElse(null);
    }

    GameEntity liveDefenderEntity() {
        return soleDefenderEntity;
    }

    Map<Card, GameEntity> resolveAssignments() {
        final Map<Card, GameEntity> result = new LinkedHashMap<>();
        for (final AttackDeclarationAssignment assignment : selectedAssignments.values()) {
            final Card live = liveCard(assignment.getCard());
            if (live == null) {
                return null;
            }
            result.put(live, soleDefenderEntity);
        }
        return result;
    }

    public boolean isCompleted() {
        return completed;
    }

    boolean hasActiveRequest() {
        return activeRequestId != NO_ACTIVE_REQUEST;
    }

    void setActiveRequestId(final long requestId) {
        activeRequestId = requestId;
    }

    boolean ownsActiveRequest(final long requestId) {
        return activeRequestId == requestId;
    }

    boolean consumeActiveRequest(final long requestId) {
        if (!ownsActiveRequest(requestId)) {
            return false;
        }
        activeRequestId = NO_ACTIVE_REQUEST;
        return true;
    }

    void markCompleted() {
        completedAssignments = List.copyOf(selectedAssignments.values());
        completed = true;
        activeRequestId = NO_ACTIVE_REQUEST;
    }

    List<AttackDeclarationAssignment> getCompletedAssignments() {
        return Collections.unmodifiableList(completedAssignments);
    }
}
