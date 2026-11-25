package forge.gamemodes.rogue.flow;

import forge.game.GameView;
import forge.gamemodes.rogue.CodexHelper;
import forge.gamemodes.rogue.PreviewReference;
import forge.gamemodes.rogue.PreviewReferenceType;
import forge.gamemodes.rogue.RogueConfig;
import forge.gamemodes.rogue.RogueDeck;
import forge.gamemodes.rogue.RogueIO;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.RoguePlanebound;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.RogueRunHistoryEntry;
import forge.gamemodes.rogue.RogueStats;
import forge.gamemodes.rogue.RogueWinLoseController;
import forge.gamemodes.rogue.TextHelper;
import forge.gamemodes.rogue.effect.*;
import forge.gamemodes.rogue.flow.RogueDecisions.CardChoice;
import forge.gamemodes.rogue.flow.RogueDecisions.CardChoiceKind;
import forge.gamemodes.rogue.flow.RogueDecisions.Pick;
import forge.gamemodes.rogue.flow.RogueDecisions.ResultKind;
import forge.gamemodes.rogue.flow.RogueDecisions.Section;
import forge.gamemodes.rogue.npc.NPCContext;
import forge.gamemodes.rogue.npc.NPCEncounterComposite;
import forge.gamemodes.rogue.path.NodeBazaar;
import forge.gamemodes.rogue.path.NodeChest;
import forge.gamemodes.rogue.path.NodeEvent;
import forge.gamemodes.rogue.path.NodePlanebound;
import forge.gamemodes.rogue.path.NodeSanctum;
import forge.gamemodes.rogue.path.RoguePathGenerator;
import forge.gamemodes.rogue.path.RoguePathNode;
import forge.gui.interfaces.IWinLoseView;
import forge.item.PaperCard;
import forge.localinstance.achievements.RogueCommanderAchievements;
import forge.localinstance.properties.ForgePreferences;
import forge.sound.SoundEffectType;
import forge.sound.SoundSystem;
import forge.util.Localizer;
import forge.util.MyRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.tinylog.Logger;

/** A client's current Rogue Commander run, and the steps of playing it. */
public final class RogueFlow {
    private final RogueDecisions decisions;
    private final NodeBazaarHelper nodeBazaarHelper = new NodeBazaarHelper(this);
    private final NodeChestHelper nodeChestHelper = new NodeChestHelper(this);
    private final NodePlaneboundHelper nodePlaneboundHelper = new NodePlaneboundHelper(this);
    private final NodeSanctumHelper nodeSanctumHelper = new NodeSanctumHelper(this);
    private final NodeEffectResultHelper nodeEffectResultHelper =
        new NodeEffectResultHelper(this, nodeBazaarHelper, nodeChestHelper, nodeSanctumHelper);
    private final NodeEventHelper nodeEventHelper = new NodeEventHelper(this, nodeEffectResultHelper);

    private RogueRun currentRun;
    private RogueRun starting;
    private NodePlanebound pendingBattle;

    public RogueFlow(RogueDecisions decisions) {
        this.decisions = decisions;
        nodeChestHelper.setEffectResultHelper(nodeEffectResultHelper);
    }

    RogueDecisions decisions() {
        return decisions;
    }

    public RogueRun run() {
        return currentRun;
    }

    public void setRun(RogueRun run) {
        this.currentRun = run;
        this.starting = null;
    }

    /** The run whose time is counting: the one begin is asking its start-of-run questions for, else the current one. */
    public RogueRun timedRun() {
        return starting != null ? starting : currentRun;
    }

    /** The run in memory, else the most recent one that can be continued. */
    public Optional<RogueRun> load() {
        if (currentRun == null) {
            currentRun = RogueIO.loadMostRecentContinuableRun().orElse(null);
        }
        return Optional.ofNullable(currentRun);
    }

    public boolean select(int nodeIndex) {
        if (currentRun == null) {
            return false;
        }

        RoguePathNode clickedNode = currentRun.getPath().getNode(nodeIndex);
        if (clickedNode == null) {
            return false;
        }

        RoguePathNode currentNode = currentRun.getCurrentNode();
        if (currentNode == null) {
            return false;
        }

        // Only allow selecting nodes in the current row
        if (clickedNode.getRowIndex() != currentNode.getRowIndex()) {
            return false;
        }

        // Only allow selecting reachable nodes in the current row
        List<Integer> visibleInCurrentRow = currentRun.getPath()
            .getVisibleNodesInCurrentRow(currentNode.getRowIndex(), pathContext());
        if (!visibleInCurrentRow.contains(nodeIndex)) {
            return false; // Not reachable, ignore click
        }

        // Check if this is a multi-node row - only allow selection changes if multiple nodes
        List<RoguePathNode> currentRowNodes = currentRun.getPath()
            .getNodesInRow(currentNode.getRowIndex());
        if (currentRowNodes.size() == 1) {
            return false; // Single node row - no selection needed, already auto-selected
        }

        currentRun.setCurrentNodeIndex(nodeIndex);
        return true;
    }

    /** The path rules the run's effects set, such as hidden planes or a free choice of node. */
    public PathUpdateContext pathContext() {
        PathUpdateContext pathCtx = new PathUpdateContext();
        RogueEffectComposite.INSTANCE.onPathUpdate(pathCtx, currentRun);
        return pathCtx;
    }

    public String enterText() {
        RoguePathNode node = currentRun.getCurrentNode();
        if (node == null) {
            return Localizer.getInstance().getMessage("lblRogueNoNodeAvailable");
        }
        return switch (node.getType()) {
            case PLANEBOUND -> {
                NodePlanebound nodePlanebound = (NodePlanebound) node;
                String planeName = pathContext().hidePlanes && !nodePlanebound.isRevealed()
                    ? "???" : nodePlanebound.getRoguePlanebound().planeName();
                yield Localizer.getInstance().getMessage("lblRogueEnterPlane", planeName);
            }
            case SANCTUM -> Localizer.getInstance().getMessage("lblRogueEnterSanctum");
            case BAZAAR -> Localizer.getInstance().getMessage("lblRogueEnterBazaar");
            case EVENT -> Localizer.getInstance().getMessage("lblRogueEnterEvent");
            case CHEST -> Localizer.getInstance().getMessage("lblRogueOpenChest");
        };
    }

    /** The Reroll Plane button: hidden when none are left, disabled with its reason when no other plane can come. */
    public record Reroll(int left, boolean enabled, String reason) { }

    public Reroll reroll() {
        int left = rerollsLeft();
        boolean enabled = left > 0 && !rerollCandidates().isEmpty();
        return new Reroll(left, enabled, Localizer.getInstance().getMessage(
            enabled ? "lblRogueRerollPlanebound" : "lblRogueNoAlternativePlanebound"));
    }

    /** 0 when the current node is not a plane or a match is on. */
    public int rerollsLeft() {
        if (currentRun == null || currentRun.getHostedMatch() != null
            || !(currentRun.getCurrentNode() instanceof NodePlanebound)) {
            return 0;
        }
        return pathContext().remainingPlaneboundRerolls;
    }

    public boolean rerollPlane() {
        if (rerollsLeft() <= 0) {
            return false;
        }

        List<RoguePlanebound> candidates = rerollCandidates();
        if (candidates.isEmpty()) {
            return false;
        }

        NodePlanebound nodePlanebound = (NodePlanebound) currentRun.getCurrentNode();
        PathUpdateContext pathCtx = pathContext();
        Collections.shuffle(candidates, MyRandom.getRandom());
        nodePlanebound.setRoguePlanebound(candidates.get(0));
        nodePlanebound.setRevealed(false);
        RogueEffectComposite.INSTANCE.onPathNodeReroll(pathCtx, currentRun);
        RogueIO.saveRun(currentRun);
        return true;
    }

    public List<RoguePlanebound> rerollCandidates() {
        if (currentRun == null || currentRun.getPath() == null
            || !(currentRun.getCurrentNode() instanceof NodePlanebound selectedNode)) {
            return List.of();
        }

        int currentRow = selectedNode.getRowIndex();
        Set<RoguePlanebound> excludedPlanebounds = new HashSet<>();
        for (RoguePathNode node : currentRun.getPath().getNodes()) {
            if (!(node instanceof NodePlanebound planebound)) {
                continue;
            }

            boolean isCurrentRow = node.getRowIndex() == currentRow;
            boolean isFutureRow = node.getRowIndex() > currentRow;
            boolean isRevealedPreviousRow = node.getRowIndex() < currentRow && planebound.isRevealed();
            if (isCurrentRow || isFutureRow || isRevealedPreviousRow) {
                excludedPlanebounds.add(planebound.getRoguePlanebound());
            }
        }

        int planeboundRow = currentRun.getPath().countPlaneboundRowsUpTo(currentRow);
        List<RoguePlanebound> candidates = new ArrayList<>(RogueConfig.loadPlanebounds());
        candidates.removeIf(planebound -> planebound.type() != selectedNode.getPlaneboundType()
            || planebound.minRow() > planeboundRow
            || excludedPlanebounds.contains(planebound));
        return candidates;
    }

    public void enter() {
        pendingBattle = null;
        enterNode();
        if (pendingBattle != null) {
            nodePlaneboundHelper.handlePlaneboundNode(pendingBattle, currentRun);
        }
    }

    /** The battle starts once everything else the node does is over. */
    void battle(NodePlanebound node) {
        pendingBattle = node;
    }

    void enterNode() {
        if (currentRun == null) {
            return;
        }

        RoguePathNode node = currentRun.getCurrentNode();

        if (node == null) {
            return;
        }

        switch (node.getType()) {
            case PLANEBOUND -> battle((NodePlanebound) node);
            case SANCTUM -> nodeSanctumHelper.handleSanctumNode((NodeSanctum) node, currentRun);
            case BAZAAR -> nodeBazaarHelper.handleBazaarNode((NodeBazaar) node, currentRun);
            case EVENT -> nodeEventHelper.handleEventNode((NodeEvent) node, currentRun);
            case CHEST -> nodeChestHelper.handleChestNode((NodeChest) node, currentRun);
        }
    }

    void completeSideNode(RoguePathNode node) {
        node.setCompleted(true);
        currentRun.nextNode();
        RogueStats.fireOnSideNodeCompleted(currentRun, RogueMetaProgress.getInstance());
        RogueIO.saveRun(currentRun);
        decisions.mapChanged(currentRun);
    }

    boolean checkSideNodeDefeat(String defeatedBy) {
        if (currentRun == null || currentRun.getCurrentLife() > 0) {
            return false;
        }

        DefeatContext defeatCtx = new DefeatContext();
        RogueEffectComposite.INSTANCE.onDefeat(defeatCtx, currentRun);
        if (defeatCtx.revived) {
            currentRun.setCurrentLife(defeatCtx.reviveLife);
            decisions.showResult(ResultKind.REVIVE,
                Localizer.getInstance().getMessage("lblRogueContinuumEngine"),
                Localizer.getInstance().getMessage("lblRogueContinuumRevive", defeatCtx.reviveLife),
                List.of());
            return false;
        }

        if (RogueWinLoseController.finalizeRunDefeat(currentRun, defeatedBy)) {
            decisions.showResult(ResultKind.SPARK, Localizer.getInstance().getMessage("lblRogueSparkReward"),
                Localizer.getInstance().getMessage("lblRogueSparkWon"), List.of());
        }
        showSideNodeDefeat(defeatedBy);
        currentRun = null;
        decisions.runEnded();
        return true;
    }

    private void showSideNodeDefeat(String defeatedBy) {
        String message = defeatedBy == null || defeatedBy.isEmpty()
            ? Localizer.getInstance().getMessage("lblRogueDefeated")
            : Localizer.getInstance().getMessage("lblRogueDefeatedBy", defeatedBy);
        decisions.showResult(ResultKind.DEFEAT, Localizer.getInstance().getMessage("lblRogueDefeat"),
            message, List.of());
    }

    /** Gives the rewards, or ends a lost run. What follows the result screen is in {@link #resultLeft}. */
    public RogueWinLoseController matchOver(GameView game, IWinLoseView<?> view) {
        RogueWinLoseController controller = new RogueWinLoseController(game, view, currentRun, decisions);
        try {
            controller.showRewards();
        } finally {
            nodePlaneboundHelper.restoreHuman();
        }
        return controller;
    }

    public String resultTitle(RogueWinLoseController controller) {
        Localizer localizer = Localizer.getInstance();
        if (currentRun != null && currentRun.getEndlessLevel() > 0 && currentRun.isRunWon()) {
            return localizer.getMessage("lblRogueRunWon");
        }
        if (controller.hasFailedRun()) {
            return localizer.getMessage("lblRogueRunLost");
        }
        if (!controller.hasWonMatch()) {
            return localizer.getMessage("lblRogueBattleLost");
        }
        return controller.hasWonRun() ? localizer.getMessage("lblRogueRunCompleted") : localizer.getMessage("lblRogueBattleWon");
    }

    /** Whether the player chooses between completing the run and Endless Mode. */
    public boolean offersEnding(RogueWinLoseController controller) {
        return controller.hasWonRun() && currentRun != null && !currentRun.isRunFailed();
    }

    public void resultLeft(RogueWinLoseController controller, boolean endless) {
        // Clear hosted match so new matches can be started
        if (currentRun != null) {
            currentRun.setHostedMatch(null);
        }

        if (currentRun != null && controller.hasWonMatch()) {
            for (NPCContext ctx : NPCEncounterComposite.INSTANCE.onAfterMatch(currentRun)) {
                decisions.chooseBoon(ctx, new ChoiceRerollContext());
            }
        }

        if (offersEnding(controller)) {
            if (endless) {
                goEndless();
            } else if (RogueWinLoseController.finalizeRunVictory(currentRun, null)) {
                decisions.showResult(ResultKind.SPARK, Localizer.getInstance().getMessage("lblRogueSparkReward"),
                    Localizer.getInstance().getMessage("lblRogueSparkWon"), List.of());
            }
        }

        if (currentRun != null && (currentRun.isRunFailed() || currentRun.isRunWon())) {
            // Run is over (won or lost); the client shows where the player goes next
            currentRun = null;
            decisions.runEnded();
        } else {
            // Continue run - return to map
            decisions.mapChanged(currentRun);
        }
    }

    public boolean goEndless() {
        if (currentRun == null || !currentRun.isRunComplete() || currentRun.isRunFailed()
            || currentRun.isRunWon()
            || !RogueMetaProgress.getInstance().isEndlessModeUnlocked()) {
            return false;
        }

        RogueMetaProgress progress = RogueMetaProgress.getInstance();
        currentRun.setEndlessLevel(currentRun.getEndlessLevel() + 1);
        currentRun.setCurrentNodeIndex(0);
        RoguePathGenerator.generateRandomPath(currentRun);
        runStartBoon(currentRun, progress);
        RogueIO.saveRun(currentRun);
        return true;
    }

    public void devNextNode() {
        if (currentRun == null) return;
        RoguePathNode node = currentRun.getCurrentNode();
        if (node == null) return;

        node.setCompleted(true);
        var progress = RogueMetaProgress.getInstance();
        if (node instanceof NodePlanebound nodePlanebound) {
            RogueStats.fireOnMatchCompleted(currentRun, progress, true);
            currentRun.addGold(nodePlanebound.getGoldReward());
            progress.addEchoes(nodePlanebound.getEchoReward());
        } else {
            RogueStats.fireOnSideNodeCompleted(currentRun, progress);
        }
        // Last node — treat as win
        if (currentRun.getCurrentNodeIndex() >= currentRun.getPath().getNodeCount() - 1) {
            devWinRun();
            return;
        }

        currentRun.nextNode();
        RogueIO.saveRun(currentRun);
        decisions.mapChanged(currentRun);
    }

    public void devWinRun() {
        if (currentRun == null) return;
        String commanderName = currentRun.getCurrentCommanderName();
        currentRun.setRunWon(true);
        currentRun.getRunTimer().stop();
        RogueMetaProgress progress = RogueMetaProgress.getInstance();
        progress.addEchoes(20);
        progress.addRunHistoryEntry(RogueRunHistoryEntry.fromRun(currentRun, "VICTORY", "[DEV]"));
        RogueStats.fireOnRunCompleted(currentRun, progress, true);
        RogueCommanderAchievements.getInstance().recordRunWon(commanderName);
        progress.recordDescensionWin(currentRun);
        RogueCommanderAchievements.getInstance().evaluateRunAchievements(currentRun);
        progress.notifyDescensionL1IfFirstWin(currentRun);
        RogueIO.saveRun(currentRun);
        currentRun = null;
        decisions.runEnded();
    }

    /** The run timer is stopped, also when the save fails. */
    public boolean save() {
        if (currentRun == null || currentRun.getHostedMatch() != null) {
            return false;
        }

        currentRun.getRunTimer().stop();
        try {
            RogueIO.saveRun(currentRun);
        } catch (RuntimeException e) {
            Logger.error(e, "Could not save Rogue Commander run before returning home");
            return false;
        }
        return true;
    }

    public void abandon() {
        if (currentRun == null || currentRun.getHostedMatch() != null) {
            return;
        }

        currentRun.getRunTimer().stop();
        RogueMetaProgress.getInstance().addRunHistoryEntry(
            RogueRunHistoryEntry.fromRun(currentRun, "ABANDONED", ""));
        RogueIO.deleteRun(currentRun);
        SoundSystem.instance.play(SoundEffectType.LoseDuel, SoundEffectType.LoseDuel.isSynced());
        currentRun = null;
        decisions.runEnded();
    }

    public void betweenRuns() {
        RogueMetaProgress progress = RogueMetaProgress.getInstance();
        progress.notifyEndlessModeUnlocked();
        for (NPCContext ctx : NPCEncounterComposite.INSTANCE.onBetweenRuns(progress)) {
            decisions.chooseBoon(ctx, new ChoiceRerollContext());
        }
    }

    public boolean begin(RogueStart start) {
        RogueMetaProgress progress = RogueMetaProgress.getInstance();
        if (!start.canBegin(progress)) {
            return false;
        }

        List<CustomRunModification> modifications = start.runModifications();
        boolean chosenOne = modifications.contains(CustomRunModification.CHOSEN_ONE);
        List<PaperCard> chosenCommanders = chosenOne
            ? decisions.chooseCommanders() : List.of();
        if (chosenOne && chosenCommanders.isEmpty()) {
            return false;
        }
        RogueDeck runDeck = createRunDeck(start.deck(), modifications, chosenCommanders);
        if (runDeck == null) {
            decisions.showMessage(Localizer.getInstance().getMessage("lblRogueCustomRun"),
                Localizer.getInstance().getMessage("lblRogueCouldNotGenerateDeck"));
            return false;
        }
        if (chosenOne) {
            runDeck.setAvatarIndex(decisions.choosePortrait(chosenCommanders.get(0), runDeck.getAvatarIndex()));
        }

        List<RogueRun> runsToAbandon = getContinuableRunsToAbandon();
        if (!runsToAbandon.isEmpty() && !decisions.confirmReplaceRun()) {
            return false;
        }
        for (RogueRun run : runsToAbandon) {
            run.getRunTimer().stop();
        }

        // Create new run and snapshot active Aetherworks
        RogueRun newRun = new RogueRun(runDeck);
        for (CustomRunModification modification : modifications) {
            newRun.addCustomRunModification(modification);
        }
        newRun.getRunTimer().start();
        starting = newRun;
        newRun.setDescensionLevel(start.descension());
        newRun.snapshotAetherEffects(progress);

        // Generate path and apply run start effects
        RoguePathGenerator.generateRandomPath(newRun);
        runStartBoon(newRun, progress);

        // Generate unique name for the run (used as filename)
        // Format: DeckName_EpochMillis; a custom run keeps only letters, digits, _ and - of its deck name
        String runName = (start.custom() ? runDeck.getName().replaceAll("[^a-zA-Z0-9_-]", "_")
            : runDeck.getName()) + "_" + System.currentTimeMillis();
        newRun.setName(runName);

        // Track meta progress
        RogueStats.fireOnRunStarted(newRun, progress);

        // Until the new run is on disk, a start that is cut short must leave the old one
        RogueIO.saveRun(newRun);
        abandonPreviousRuns(runsToAbandon, newRun, progress);

        currentRun = newRun;
        starting = null;
        decisions.runStarted(newRun);
        return true;
    }

    private RogueDeck createRunDeck(RogueDeck selectedDeck, List<CustomRunModification> modifications,
                                    List<PaperCard> chosenCommanders) {
        RogueDeck runDeck = selectedDeck;
        RuntimeException[] fromPlayer = new RuntimeException[1];
        try {
            for (CustomRunModification modification : modifications) {
                runDeck = modification.createRogueDeck(runDeck, chosenCommanders,
                    (deck, ctx) -> {
                        CardChoice choice = new CardChoice(CardChoiceKind.STARTING_DECK,
                            Localizer.getInstance().getMessage("lblRogueStartingDeck"), ctx.cardSelectionSubtitle,
                            ctx.candidateCards, new RogueRun(deck), ctx.cardSelectionMinCount,
                            ctx.cardSelectionMaxCount, ctx.buildDeckPreview);
                        try {
                            return decisions.chooseCards(choice);
                        } catch (RuntimeException e) {
                            fromPlayer[0] = e;
                            throw e;
                        }
                    });
                if (runDeck == null) {
                    return null;
                }
            }
            return runDeck;
        } catch (RuntimeException e) {
            if (e == fromPlayer[0]) {
                throw e;
            }
            Logger.error(e, "Could not generate a custom Rogue Commander deck");
            return null;
        }
    }

    private void abandonPreviousRuns(List<RogueRun> runsToAbandon, RogueRun newRun, RogueMetaProgress progress) {
        for (RogueRun run : runsToAbandon) {
            progress.addRunHistoryEntry(RogueRunHistoryEntry.fromRun(run, "ABANDONED", ""));
        }

        for (RogueRun old : RogueIO.loadAllRuns()) {
            if (!old.getName().equals(newRun.getName())) {
                RogueIO.deleteRun(old);
            }
        }
    }

    private List<RogueRun> getContinuableRunsToAbandon() {
        Map<String, RogueRun> runsByName = new LinkedHashMap<>();
        addContinuableRun(runsByName, currentRun);
        for (RogueRun run : RogueIO.loadContinuableRuns()) {
            addContinuableRun(runsByName, run);
        }
        return new ArrayList<>(runsByName.values());
    }

    private void addContinuableRun(Map<String, RogueRun> runsByName, RogueRun run) {
        if (!RogueIO.isContinuableRun(run)) {
            return;
        }
        String key = run.getName() != null ? run.getName() : "run@" + System.identityHashCode(run);
        runsByName.putIfAbsent(key, run);
    }

    private void runStartBoon(RogueRun newRun, RogueMetaProgress progress) {
        RogueEffectComposite.INSTANCE.onRunStart(newRun, ctx -> EffectResultHelper.handleTrigger(ctx, newRun, decisions));

        // Show NPC encounter dialogs (e.g. Tyvar offering npcEffect choices)
        for (NPCContext ctx : NPCEncounterComposite.INSTANCE.onRunStart(progress, newRun)) {
            NPCEffect chosen = showRunStartNpcDialog(progress, newRun, ctx);
            if (chosen == null) {
                continue;
            }

            EffectResultContext effectCtx = new EffectResultContext();
            chosen.applyEffect(newRun, effectCtx);
            if (chosen.getEffectType() != RogueEffect.EffectType.ONESHOT) {
                newRun.addNPCEffect(chosen);
            } else if (EffectResultHelper.handleTrigger(effectCtx, newRun, decisions)) {
                CodexHelper.recordAcquiredCards(newRun, effectCtx.addedCards);
                showNpcResultIfNeeded(chosen, effectCtx);
            }
        }
    }

    private NPCEffect showRunStartNpcDialog(RogueMetaProgress progress, RogueRun newRun, NPCContext ctx) {
        NPCContext effectiveCtx = maybeOverrideNpcChoices(ctx);
        Pick<NPCEffect> result;
        boolean rerollRequested;
        do {
            ChoiceRerollContext rerollCtx = new ChoiceRerollContext();
            RogueEffectComposite.INSTANCE.onBeforeNpcBoons(rerollCtx, newRun);
            CodexHelper.recordTraitChoices(effectiveCtx.choices().stream()
                .map(NPCContext.NPCChoice::npcEffect)
                .collect(Collectors.toList()));
            result = decisions.chooseBoon(effectiveCtx, rerollCtx);
            rerollRequested = result.reroll();

            if (rerollRequested) {
                RogueEffectComposite.INSTANCE.onChoiceReroll(rerollCtx, newRun);
                List<NPCContext> rerolledContexts = NPCEncounterComposite.INSTANCE.onRunStart(progress, newRun);
                if (!rerolledContexts.isEmpty()) {
                    effectiveCtx = maybeOverrideNpcChoices(rerolledContexts.get(0));
                }
            }
        } while (rerollRequested);

        NPCEffect choice = result.choice();
        CodexHelper.recordTraitAcquired(choice);
        return choice;
    }

    private NPCContext maybeOverrideNpcChoices(NPCContext ctx) {
        if (!ForgePreferences.DEV_MODE || ctx == null || ctx.choices().size() != 3) {
            return ctx;
        }
        if (ctx.choices().stream().anyMatch(choice -> choice == null || choice.npcEffect() == null)) {
            return ctx;
        }
        List<NPCEffect> preselectedEffects = ctx.choices().stream()
            .map(NPCContext.NPCChoice::npcEffect)
            .collect(Collectors.toList());
        List<NPCEffect> selected = decisions.devPick(Localizer.getInstance().getMessage("lblRogueDevNpcChoiceOverride"),
            NPCEffect.getEffectsForNpc(ctx.npc(), null), preselectedEffects, 3);

        List<NPCContext.NPCChoice> choices = new ArrayList<>(selected.size());
        for (NPCEffect effect : selected) {
            choices.add(new NPCContext.NPCChoice(effect.getDisplayName(), effect));
        }
        return new NPCContext(ctx.npc(), ctx.flavorTextChunks(), choices,
            ctx.displayNameOverride(), ctx.avatarIndexOverride());
    }

    private void showNpcResultIfNeeded(NPCEffect effect, EffectResultContext ctx) {
        List<Section> sections = buildNpcResultSections(effect, ctx);
        if (sections.isEmpty()) {
            return;
        }

        decisions.showResult(ResultKind.BOON,
            Localizer.getInstance().getMessage("lblRogueNpcRewardGained"), Localizer.getInstance().getMessage("lblRogueGainedNpcReward", effect.getDisplayName()), sections);
    }

    private List<Section> buildNpcResultSections(NPCEffect effect, EffectResultContext ctx) {
        Set<String> excludedNames = new HashSet<>(getPreviewedCardNames(effect));
        excludedNames.addAll(getCardNames(ctx.candidateCards));

        List<Section> sections = new ArrayList<>();
        List<PaperCard> removedCards = filterResultCards(ctx.removedCards, excludedNames);
        if (!removedCards.isEmpty()) {
            sections.add(new Section(Localizer.getInstance().getMessage("lblRogueCardsRemoved"), null, removedCards));
        }

        List<PaperCard> addedCards = filterResultCards(ctx.addedCards, excludedNames);
        if (!addedCards.isEmpty()) {
            sections.add(new Section(Localizer.getInstance().getMessage("lblRogueCardsAdded"), null, addedCards));
        }
        return sections;
    }

    private Set<String> getPreviewedCardNames(NPCEffect effect) {
        Set<String> cardNames = new HashSet<>();
        for (PreviewReference reference : effect.getPreviewReferences()) {
            if (reference.type() == PreviewReferenceType.CARD) {
                String cardName = TextHelper.extractCardNameFromReference(reference.token());
                if (StringUtils.isNotBlank(cardName)) {
                    cardNames.add(cardName);
                }
            }
        }
        return cardNames;
    }

    private Set<String> getCardNames(List<PaperCard> cards) {
        Set<String> cardNames = new HashSet<>();
        if (cards == null) {
            return cardNames;
        }

        for (PaperCard card : cards) {
            if (card != null) {
                cardNames.add(card.getName());
            }
        }
        return cardNames;
    }

    private List<PaperCard> filterResultCards(List<PaperCard> cards, Set<String> excludedNames) {
        if (cards == null || cards.isEmpty()) {
            return List.of();
        }

        List<PaperCard> resultCards = new ArrayList<>();
        for (PaperCard card : cards) {
            if (card != null && !excludedNames.contains(card.getName())) {
                resultCards.add(card);
            }
        }
        return resultCards;
    }
}
