package forge.game.decision;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.combat.AttackingBand;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.keyword.KeywordInterface;
import forge.game.player.Player;
import forge.game.staticability.StaticAbilityBlockRestrict;
import forge.game.staticability.StaticAbilityCantAttackBlock;
import forge.game.staticability.StaticAbilityMustBlock;
import forge.game.staticability.StaticAbilityMode;
import forge.game.zone.ZoneType;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Callback-local, mutation-free state for one supported BLOCK declaration. */
public final class BlockDeclarationSession {
    private static final long NO_ACTIVE_REQUEST = -1L;

    private final long blockSessionId;
    private final int gameId;
    private final Game game;
    private final Player defendingPlayer;
    private final Player whoDeclares;
    private final Combat combat;
    private final boolean replayOnly;
    private final Map<String, Card> capturedAttackers;
    private final Map<String, BlockDeclarationCard> attackerIdentities;
    private final Map<String, GameEntity> capturedDefenders;
    private final Map<String, Integer> capturedDefenderIds;
    private final Map<String, Card> capturedBlockers;
    private final Map<String, BlockDeclarationCard> blockerIdentities;
    private final List<BandSnapshot> capturedBands;
    private final Map<String, List<String>> capturedPairs;
    private final Map<String, BlockDeclarationAssignment> selectedAssignments = new LinkedHashMap<>();

    private int nextStepIndex;
    private long activeRequestId = NO_ACTIVE_REQUEST;
    private BlockDeclarationCard pendingBlocker;
    private boolean completed;
    private List<BlockDeclarationAssignment> completedAssignments = List.of();

    BlockDeclarationSession(final long blockSessionId, final Player defendingPlayer, final Player whoDeclares,
            final Combat combat) {
        this(blockSessionId, defendingPlayer, whoDeclares, combat, false);
    }

    private BlockDeclarationSession(final long blockSessionId, final Player defendingPlayer,
            final Player whoDeclares, final Combat combat, final boolean replayOnly) {
        this.blockSessionId = blockSessionId;
        this.game = defendingPlayer.getGame();
        this.gameId = game.getId();
        this.defendingPlayer = defendingPlayer;
        this.whoDeclares = whoDeclares;
        this.combat = combat;
        this.replayOnly = replayOnly;
        this.capturedAttackers = new LinkedHashMap<>();
        this.attackerIdentities = new LinkedHashMap<>();
        this.capturedDefenders = new LinkedHashMap<>();
        this.capturedDefenderIds = new LinkedHashMap<>();
        this.capturedBlockers = new LinkedHashMap<>();
        this.blockerIdentities = new LinkedHashMap<>();
        for (final Card attacker : combat.getAttackers()) {
            final String key = identityKey(attacker);
            capturedAttackers.put(key, attacker);
            attackerIdentities.put(key, new BlockDeclarationCard(attacker));
            final GameEntity defender = combat.getDefenderByAttacker(attacker);
            capturedDefenders.put(key, defender);
            capturedDefenderIds.put(key, defender == null ? -1 : defender.getId());
        }
        for (final Card blocker : defendingPlayer.getCreaturesInPlay()) {
            final String key = identityKey(blocker);
            capturedBlockers.put(key, blocker);
            blockerIdentities.put(key, new BlockDeclarationCard(blocker));
        }
        capturedBands = captureBands(combat);
        capturedPairs = new LinkedHashMap<>();
    }

    private BlockDeclarationSession(final BlockDeclarationSession source) {
        this.blockSessionId = source.blockSessionId;
        this.gameId = source.gameId;
        this.game = source.game;
        this.defendingPlayer = source.defendingPlayer;
        this.whoDeclares = source.whoDeclares;
        this.combat = source.combat;
        this.replayOnly = true;
        this.capturedAttackers = source.capturedAttackers;
        this.attackerIdentities = source.attackerIdentities;
        this.capturedDefenders = source.capturedDefenders;
        this.capturedDefenderIds = source.capturedDefenderIds;
        this.capturedBlockers = source.capturedBlockers;
        this.blockerIdentities = source.blockerIdentities;
        this.capturedBands = source.capturedBands;
        this.capturedPairs = source.capturedPairs;
    }

    BlockDeclarationSession copyForReplay() {
        return new BlockDeclarationSession(this);
    }

    public long getBlockSessionId() {
        return blockSessionId;
    }

    public int getGameId() {
        return gameId;
    }

    Player getDefendingPlayer() {
        return defendingPlayer;
    }

    Player getWhoDeclares() {
        return whoDeclares;
    }

    Combat getCombat() {
        return combat;
    }

    List<BlockDeclarationCard> getAttackerIdentities() {
        return List.copyOf(attackerIdentities.values());
    }

    List<BlockDeclarationCard> getBlockerIdentities() {
        return List.copyOf(blockerIdentities.values());
    }

    int getInitialEligibleBlockerCount() {
        return capturedPairs.size();
    }

    boolean capturedPairExists(final BlockDeclarationCard blocker, final BlockDeclarationCard attacker) {
        return blocker != null && attacker != null
                && capturedPairs.getOrDefault(blocker.identityKey(), List.of()).contains(attacker.identityKey());
    }

    boolean capturedAttackDeclarationStillPresent() {
        return sameAttackerDeclaration();
    }

    boolean capturedBlockerDeclarationStillPresent() {
        return sameBlockerDeclaration();
    }

    public List<BlockDeclarationAssignment> getSelectedAssignments() {
        return List.copyOf(selectedAssignments.values());
    }

    List<BlockDeclarationAssignment> getCompletedAssignments() {
        return Collections.unmodifiableList(completedAssignments);
    }

    int allocateStepIndex() {
        return nextStepIndex++;
    }

    public int getNextBlockStepIndex() {
        return nextStepIndex;
    }

    boolean initializePairs() {
        capturedPairs.clear();
        for (final Map.Entry<String, Card> blockerEntry : capturedBlockers.entrySet()) {
            final List<String> legalAttackers = new ArrayList<>();
            for (final Map.Entry<String, Card> attackerEntry : capturedAttackers.entrySet()) {
                if (CombatUtil.canBlock(attackerEntry.getValue(), blockerEntry.getValue(), combat)) {
                    legalAttackers.add(attackerEntry.getKey());
                }
            }
            legalAttackers.sort(String::compareTo);
            if (!legalAttackers.isEmpty()) {
                capturedPairs.put(blockerEntry.getKey(), List.copyOf(legalAttackers));
            }
        }
        return !capturedPairs.isEmpty();
    }

    BlockDeclarationDecisionProvider.Reason admissionReason() {
        final BlockDeclarationDecisionProvider.Reason structural = validateStructure();
        if (structural != null) {
            return structural;
        }
        final BlockDeclarationDecisionProvider.Reason costReason = validatePairCosts();
        if (costReason != null) {
            return costReason;
        }
        if (!hasAnyCurrentPair()) {
            return BlockDeclarationDecisionProvider.Reason.STALE_BLOCK_DECLARATION;
        }
        for (final BlockDeclarationAssignment assignment : selectedAssignments.values()) {
            if (!currentPairIsLegal(assignment.getBlocker().identityKey(), assignment.getAttacker().identityKey())) {
                return BlockDeclarationDecisionProvider.Reason.STALE_BLOCK_DECLARATION;
            }
        }
        if (!replayOnly && CombatUtil.validateBlocks(combat, defendingPlayer) != null) {
            return BlockDeclarationDecisionProvider.Reason.BLOCK_REQUIREMENT;
        }
        if (hasKnownUnsupportedRequirement()) {
            return BlockDeclarationDecisionProvider.Reason.BLOCK_REQUIREMENT;
        }
        return null;
    }

    private BlockDeclarationDecisionProvider.Reason validateStructure() {
        if (game.getId() != gameId || defendingPlayer.getGame() != game || whoDeclares.getGame() != game
                || !defendingPlayer.isInGame() || !whoDeclares.isInGame()) {
            return BlockDeclarationDecisionProvider.Reason.LIVE_STATE_CHANGED;
        }
        if (game.getPlayers().size() != 2) {
            return BlockDeclarationDecisionProvider.Reason.NOT_ONE_V_ONE;
        }
        if (whoDeclares != defendingPlayer) {
            return BlockDeclarationDecisionProvider.Reason.EXTERNAL_DECLARER;
        }
        if (combat.getAttackingPlayer() == defendingPlayer
                || !combat.getAttackingPlayer().isInGame()
                || !combat.getAttackingPlayer().isOpponentOf(defendingPlayer)) {
            return BlockDeclarationDecisionProvider.Reason.UNSUPPORTED_DEFENDER_SHAPE;
        }
        if (capturedAttackers.isEmpty()) {
            return BlockDeclarationDecisionProvider.Reason.NO_ATTACKERS;
        }
        if (!sameAttackerDeclaration()) {
            return BlockDeclarationDecisionProvider.Reason.STALE_ATTACK_DECLARATION;
        }
        if (!sameBlockerDeclaration()) {
            return BlockDeclarationDecisionProvider.Reason.STALE_BLOCK_DECLARATION;
        }
        if (!replayOnly && !combat.getAllBlockers().isEmpty()) {
            return BlockDeclarationDecisionProvider.Reason.PREMUTATED_COMBAT;
        }
        if (capturedBands.stream().anyMatch(band -> band.attackerKeys.size() > 1)) {
            return BlockDeclarationDecisionProvider.Reason.UNSUPPORTED_ATTACKING_BAND;
        }
        final List<GameEntity> actualDefenders = new ArrayList<>();
        for (final GameEntity defender : capturedDefenders.values()) {
            if (defender == null || !actualDefenders.contains(defender)) {
                actualDefenders.add(defender);
            }
        }
        if (actualDefenders.size() != 1 || actualDefenders.get(0) != defendingPlayer) {
            return BlockDeclarationDecisionProvider.Reason.UNSUPPORTED_DEFENDER_SHAPE;
        }
        if (StaticAbilityBlockRestrict.blockRestrictNum(defendingPlayer) != Integer.MAX_VALUE) {
            return BlockDeclarationDecisionProvider.Reason.GLOBAL_BLOCK_RESTRICTION;
        }
        for (final Card blocker : capturedBlockers.values()) {
            if (!capturedPairs.containsKey(identityKey(blocker))) {
                continue;
            }
            if (blocker.canBlockAdditional() != 0 || blocker.canBlockAny()) {
                return BlockDeclarationDecisionProvider.Reason.UNSUPPORTED_MULTI_BLOCKER_ASSIGNMENT;
            }
            if (hasGroupRestriction(blocker)) {
                return BlockDeclarationDecisionProvider.Reason.BLOCKER_GROUP_RESTRICTION;
            }
        }
        for (final Card attacker : capturedAttackers.values()) {
            final Pair<Integer, Integer> minMax = StaticAbilityCantAttackBlock.getMinMaxBlocker(attacker,
                    defendingPlayer);
            if (minMax.getLeft() != 1 || minMax.getRight() != Integer.MAX_VALUE) {
                return BlockDeclarationDecisionProvider.Reason.ATTACKER_BLOCK_COUNT_RESTRICTION;
            }
        }
        return null;
    }

    private boolean sameAttackerDeclaration() {
        final Map<String, Card> current = new LinkedHashMap<>();
        for (final Card attacker : combat.getAttackers()) {
            current.put(identityKey(attacker), attacker);
        }
        if (current.size() != capturedAttackers.size() || !current.keySet().equals(capturedAttackers.keySet())) {
            return false;
        }
        for (final Map.Entry<String, Card> entry : capturedAttackers.entrySet()) {
            final Card currentCard = current.get(entry.getKey());
            final GameEntity currentDefender = combat.getDefenderByAttacker(currentCard);
            if (currentCard != entry.getValue() || currentCard.getController() != combat.getAttackingPlayer()
                    || currentDefender != capturedDefenders.get(entry.getKey())
                    || (currentDefender == null ? -1 : currentDefender.getId())
                            != capturedDefenderIds.get(entry.getKey())) {
                return false;
            }
        }
        if (captureBands(combat).size() != capturedBands.size()) {
            return false;
        }
        final Set<AttackingBand> currentBandSet = Collections.newSetFromMap(new IdentityHashMap<>());
        currentBandSet.addAll(combat.getAttackingBands());
        for (final BandSnapshot expected : capturedBands) {
            if (!currentBandSet.contains(expected.band)
                    || !expected.attackerKeys.equals(attackerKeys(expected.band))) {
                return false;
            }
        }
        return true;
    }

    private boolean sameBlockerDeclaration() {
        final Map<String, Card> current = new LinkedHashMap<>();
        for (final Card blocker : defendingPlayer.getCreaturesInPlay()) {
            current.put(identityKey(blocker), blocker);
        }
        if (current.size() != capturedBlockers.size() || !current.keySet().equals(capturedBlockers.keySet())) {
            return false;
        }
        for (final Map.Entry<String, Card> entry : capturedBlockers.entrySet()) {
            final Card currentCard = current.get(entry.getKey());
            if (currentCard != entry.getValue() || currentCard.getController() != defendingPlayer
                    || currentCard.getZone() == null
                    || currentCard.getZone().getZoneType() != ZoneType.Battlefield
                    || (capturedPairs.containsKey(entry.getKey())
                            && (currentCard.isTapped() || !CombatUtil.canBlock(currentCard)))) {
                return false;
            }
        }
        return true;
    }

    private BlockDeclarationDecisionProvider.Reason validatePairCosts() {
        for (final Map.Entry<String, List<String>> entry : capturedPairs.entrySet()) {
            final Card blocker = capturedBlockers.get(entry.getKey());
            for (final String attackerKey : entry.getValue()) {
                final Card attacker = capturedAttackers.get(attackerKey);
                if (blocker == null || attacker == null || CombatUtil.getBlockCost(game, blocker, attacker) != null) {
                    return BlockDeclarationDecisionProvider.Reason.UNSUPPORTED_BLOCK_COST;
                }
            }
        }
        return null;
    }

    private boolean hasKnownUnsupportedRequirement() {
        for (final Card blocker : capturedBlockers.values()) {
            if (capturedPairs.containsKey(identityKey(blocker))
                    && (!blocker.getMustBlockCards().isEmpty()
                            || StaticAbilityMustBlock.blocksEachCombatIfAble(blocker))) {
                return true;
            }
            if (capturedPairs.containsKey(identityKey(blocker)) && hasActiveCantBlockGroupAbility(blocker)) {
                return true;
            }
        }
        for (final Card attacker : capturedAttackers.values()) {
            if (hasBlockRequirementKeyword(attacker)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasActiveCantBlockGroupAbility(final Card blocker) {
        final List<Card> sources = new ArrayList<>();
        sources.add(blocker);
        sources.addAll(game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES));
        for (final Card source : sources) {
            for (final forge.game.staticability.StaticAbility ability : source.getStaticAbilities()) {
                if (ability.checkConditions(StaticAbilityMode.CantBlock)
                        && StaticAbilityCantAttackBlock.applyCantBlockAbility(ability, blocker)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasGroupRestriction(final Card blocker) {
        return blocker.hasKeyword("CARDNAME can't attack or block alone.")
                || blocker.hasKeyword("CARDNAME can't block alone.")
                || blocker.hasKeyword("CARDNAME can't block unless at least two other creatures block.")
                || blocker.hasKeyword("CARDNAME can't block unless a creature with greater power also blocks.");
    }

    private static boolean hasBlockRequirementKeyword(final Card attacker) {
        if (attacker.hasStartOfKeyword("All creatures able to block CARDNAME do so.")
                || attacker.hasStartOfKeyword("CARDNAME must be blocked if able.")
                || attacker.hasStartOfKeyword("CARDNAME must be blocked by exactly one creature if able.")
                || attacker.hasStartOfKeyword("CARDNAME must be blocked by two or more creatures if able.")) {
            return true;
        }
        for (final KeywordInterface keyword : attacker.getKeywords()) {
            final String original = keyword.getOriginal();
            if (original.startsWith("MustBeBlockedBy ") || original.startsWith("MustBeBlockedByAll")) {
                return true;
            }
        }
        return false;
    }

    private boolean currentPairIsLegal(final String blockerKey, final String attackerKey) {
        final List<String> attackers = capturedPairs.get(blockerKey);
        if (attackers == null || !attackers.contains(attackerKey)) {
            return false;
        }
        final Card blocker = capturedBlockers.get(blockerKey);
        final Card attacker = capturedAttackers.get(attackerKey);
        if (blocker == null || attacker == null) {
            return false;
        }
        return replayOnly ? CombatUtil.canBlock(attacker, blocker)
                : CombatUtil.canBlock(attacker, blocker, combat);
    }

    List<BlockDeclarationCard> remainingBlockers() {
        final List<BlockDeclarationCard> result = new ArrayList<>();
        for (final Map.Entry<String, BlockDeclarationCard> entry : blockerIdentities.entrySet()) {
            if (!selectedAssignments.containsKey(entry.getKey()) && currentPairCount(entry.getKey()) > 0) {
                result.add(entry.getValue());
            }
        }
        result.sort(Comparator.comparing(BlockDeclarationCard::semanticKey));
        return result;
    }

    List<BlockDeclarationCard> currentAttackersFor(final BlockDeclarationCard blocker) {
        final List<BlockDeclarationCard> result = new ArrayList<>();
        if (blocker == null) {
            return result;
        }
        for (final String attackerKey : capturedPairs.getOrDefault(blocker.identityKey(), List.of())) {
            if (currentPairIsLegal(blocker.identityKey(), attackerKey)) {
                result.add(attackerIdentities.get(attackerKey));
            }
        }
        result.sort(Comparator.comparing(BlockDeclarationCard::attackerSemanticKey));
        return result;
    }

    private int currentPairCount(final String blockerKey) {
        int count = 0;
        for (final String attackerKey : capturedPairs.getOrDefault(blockerKey, List.of())) {
            if (currentPairIsLegal(blockerKey, attackerKey)) {
                count++;
            }
        }
        return count;
    }

    private boolean hasAnyCurrentPair() {
        for (final String blockerKey : capturedPairs.keySet()) {
            if (currentPairCount(blockerKey) > 0) {
                return true;
            }
        }
        return false;
    }

    boolean isEligibleBlocker(final BlockDeclarationCard blocker) {
        return blocker != null && blockerIdentities.containsKey(blocker.identityKey())
                && !selectedAssignments.containsKey(blocker.identityKey()) && currentPairCount(blocker.identityKey()) > 0;
    }

    boolean isEligibleAttacker(final BlockDeclarationCard blocker, final BlockDeclarationCard attacker) {
        return blocker != null && attacker != null && pendingBlocker != null
                && pendingBlocker.equals(blocker) && attackerIdentities.containsKey(attacker.identityKey())
                && currentPairIsLegal(blocker.identityKey(), attacker.identityKey());
    }

    void setPendingBlocker(final BlockDeclarationCard blocker) {
        pendingBlocker = blocker;
    }

    BlockDeclarationCard getPendingBlocker() {
        return pendingBlocker;
    }

    boolean select(final BlockDeclarationCard blocker, final BlockDeclarationCard attacker) {
        if (!isEligibleAttacker(blocker, attacker)) {
            return false;
        }
        selectedAssignments.put(blocker.identityKey(), new BlockDeclarationAssignment(blocker, attacker));
        pendingBlocker = null;
        return true;
    }

    boolean isSelected(final BlockDeclarationCard blocker) {
        return blocker != null && selectedAssignments.containsKey(blocker.identityKey());
    }

    boolean isCompleted() {
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
        pendingBlocker = null;
        activeRequestId = NO_ACTIVE_REQUEST;
    }

    Card liveBlocker(final BlockDeclarationCard identity) {
        final Card captured = capturedBlockers.get(identity == null ? null : identity.identityKey());
        if (captured == null) {
            return null;
        }
        for (final Card current : defendingPlayer.getCreaturesInPlay()) {
            if (current.getId() == identity.getCardId() && current.getGameTimestamp() == identity.getGameTimestamp()) {
                return current == captured ? current : null;
            }
        }
        return null;
    }

    Card liveAttacker(final BlockDeclarationCard identity) {
        final Card captured = capturedAttackers.get(identity == null ? null : identity.identityKey());
        if (captured == null) {
            return null;
        }
        for (final Card current : combat.getAttackers()) {
            if (current.getId() == identity.getCardId() && current.getGameTimestamp() == identity.getGameTimestamp()) {
                return current == captured ? current : null;
            }
        }
        return null;
    }

    Map<BlockDeclarationAssignment, Pair<Card, Card>> resolveCompletedAssignments() {
        final Map<BlockDeclarationAssignment, Pair<Card, Card>> result = new LinkedHashMap<>();
        for (final BlockDeclarationAssignment assignment : completedAssignments) {
            final Card blocker = liveBlocker(assignment.getBlocker());
            final Card attacker = liveAttacker(assignment.getAttacker());
            if (blocker == null || attacker == null) {
                return null;
            }
            result.put(assignment, Pair.of(attacker, blocker));
        }
        return result;
    }

    private static String identityKey(final Card card) {
        return card.getId() + "|" + card.getGameTimestamp();
    }

    private static List<BandSnapshot> captureBands(final Combat combat) {
        final List<BandSnapshot> result = new ArrayList<>();
        for (final AttackingBand band : combat.getAttackingBands()) {
            result.add(new BandSnapshot(band, attackerKeys(band)));
        }
        result.sort(Comparator.comparing(BandSnapshot::semanticKey));
        return List.copyOf(result);
    }

    private static List<String> attackerKeys(final AttackingBand band) {
        final List<String> result = new ArrayList<>();
        for (final Card attacker : band.getAttackers()) {
            result.add(identityKey(attacker));
        }
        result.sort(String::compareTo);
        return List.copyOf(result);
    }

    private static final class BandSnapshot {
        private final AttackingBand band;
        private final List<String> attackerKeys;

        private BandSnapshot(final AttackingBand band, final List<String> attackerKeys) {
            this.band = band;
            this.attackerKeys = attackerKeys;
        }

        private String semanticKey() {
            return String.join("|", attackerKeys);
        }
    }
}
