package forge.screens.home.rogue;

import javax.swing.SwingUtilities;
import forge.util.Localizer;

import forge.Singletons;
import forge.gamemodes.rogue.*;
import forge.gamemodes.rogue.effect.*;
import forge.gamemodes.rogue.path.*;
import forge.gui.framework.EDocID;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.localinstance.achievements.RogueCommanderAchievements;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.controllers.CEditorRogue;
import forge.screens.deckeditor.views.VCardCatalog;
import forge.screens.home.CHomeUI;
import forge.sound.SoundEffectType;
import forge.sound.SoundSystem;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.util.MyRandom;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.util.*;
import javax.swing.*;

/**
 * Controls the "rogue map" submenu in the home UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public enum CSubmenuRogueMap implements ICDoc {
  SINGLETON_INSTANCE;

  private final ActionListener actEnterNode = arg0 -> enterNode();
  private final ActionListener actEditDeck = arg0 -> editDeck();
  private final ActionListener actRerollPlane = arg0 -> rerollSelectedPlanebound();
  private final ActionListener actAbandonRun = arg0 -> abandonRun();
  private final VSubmenuRogueMap view = VSubmenuRogueMap.SINGLETON_INSTANCE;
  private final NodeBazaarHelper nodeBazaarHelper = new NodeBazaarHelper(this);
  private final NodeChestHelper nodeChestHelper = new NodeChestHelper(this);
  private final NodePlaneboundHelper nodePlaneboundHelper = new NodePlaneboundHelper();
  private final NodeSanctumHelper nodeSanctumHelper = new NodeSanctumHelper(this);
  private final NodeEffectResultHelper nodeEffectResultHelper =
      new NodeEffectResultHelper(this, nodeBazaarHelper, nodeChestHelper, nodeSanctumHelper, nodePlaneboundHelper);
  private final NodeEventHelper nodeEventHelper = new NodeEventHelper(this, nodeEffectResultHelper);

  CSubmenuRogueMap() {
    nodeChestHelper.setEffectResultHelper(nodeEffectResultHelper);
  }

  // Test run data (for MVP - will be replaced with proper loading later)
  private RogueRun currentRun;

  @Override
  public void update() {
    // If no run exists in memory, try to load from saved runs
    if (currentRun == null) {
      currentRun = RogueIO.loadMostRecentContinuableRun().orElse(null);
    }

    // If still no run (no saved runs or all completed/failed), navigate to start screen
    if (currentRun == null) {
      CHomeUI.SINGLETON_INSTANCE.itemClick(EDocID.HOME_ROGUESTART);
      return;
    }

    currentRun.getRunTimer().start();
    updateView();
    SwingUtilities.invokeLater(() -> {
      view.getBtnEnterNode().requestFocusInWindow();
      showTutorials();
    });
  }

  private void showTutorials() {
    RogueTutorialHelper.showIfNotSeen(RogueTutorial.MAP_NAVIGATION);
    if (currentRun != null && currentRun.getEndlessLevel() > 0) {
      RogueTutorialHelper.showIfNotSeen(RogueTutorial.ENDLESS_MODE);
    }

    if (currentRun == null || currentRun.getCurrentNode() == null) {
      return;
    }

    // Check if player just won their first battle and show post-battle tutorial if so
    List<RoguePathNode> completedNodes = currentRun.getPath().getNodes().stream()
        .filter(n -> n instanceof NodePlanebound && n.isCompleted())
        .toList();
    if (!completedNodes.isEmpty()) {
      RogueTutorialHelper.showIfNotSeen(RogueTutorial.POST_BATTLE);
    }

    // Check node types of current row and show battle tutorials if not seen yet
    int currentRow = currentRun.getCurrentNode().getRowIndex();
    for (RoguePathNode node : currentRun.getPath().getNodesInRow(currentRow)) {
      if (node instanceof NodePlanebound planebound
          && planebound.getPlaneboundType() == RoguePlaneboundType.ELITE) {
        RogueTutorialHelper.showIfNotSeen(RogueTutorial.ELITE_PLANEBOUND);
      } else if (node instanceof NodePlanebound) {
        RogueTutorialHelper.showIfNotSeen(RogueTutorial.PRE_BATTLE);
      }
    }
  }

  @Override
  public void register() {
    // TODO document why this method is empty
  }

  @Override
  public void initialize() {
    view.getBtnEnterNode().addActionListener(actEnterNode);
    view.getBtnEditDeck().addActionListener(actEditDeck);
    view.getBtnRerollPlane().addActionListener(actRerollPlane);
    view.getBtnAbandonRun().addActionListener(actAbandonRun);
    view.getBtnDevWinRun().addActionListener(e -> devWinRun());
    view.getBtnDevNextNode().addActionListener(e -> devNextNode());
    view.getPathVisualizer().setNodeClickHandler(this::handleNodeClick);
  }

  /**
   * Handle click on a node panel.
   */
  private void handleNodeClick(NodePanel panel) {
    if (currentRun == null) {
      return;
    }

    // Get the index of the clicked node
    int nodeIndex = view.getPathVisualizer().getNodePanels().indexOf(panel);
    if (nodeIndex < 0) {
      return;
    }

    RoguePathNode clickedNode = currentRun.getPath().getNode(nodeIndex);
    if (clickedNode == null) {
      return;
    }

    RoguePathNode currentNode = currentRun.getCurrentNode();
    if (currentNode == null) {
      return;
    }

    // Only allow selecting nodes in the current row
    if (clickedNode.getRowIndex() != currentNode.getRowIndex()) {
      return;
    }

    PathUpdateContext pathCtx = new PathUpdateContext();
    RogueEffectComposite.INSTANCE.onPathUpdate(pathCtx, currentRun);

    // Only allow selecting reachable nodes in the current row
    List<Integer> visibleInCurrentRow = currentRun.getPath()
        .getVisibleNodesInCurrentRow(currentNode.getRowIndex(), pathCtx);
    if (!visibleInCurrentRow.contains(nodeIndex)) {
      return; // Not reachable, ignore click
    }

    // Check if this is a multi-node row - only allow selection changes if multiple nodes
    List<RoguePathNode> currentRowNodes = currentRun.getPath()
        .getNodesInRow(currentNode.getRowIndex());
    if (currentRowNodes.size() == 1) {
      return; // Single node row - no selection needed, already auto-selected
    }

    // Allow clicking any reachable node in multi-node row to select it
    // Set as current node and update view
    currentRun.setCurrentNodeIndex(nodeIndex);
    updateViewWithSelection();
  }

  void updateView() {
    view.updateDisplay(currentRun);
    updateViewWithSelection();
  }

  /**
   * Update the view with selection state and button text.
   */
  private void updateViewWithSelection() {
    if (currentRun == null) {
      return;
    }

    // Update path visualizer selection
    view.getPathVisualizer().setSelectedNode(currentRun.getCurrentNodeIndex());

    boolean matchInProgress = currentRun.getHostedMatch() != null;
    view.getBtnAbandonRun().setEnabled(!matchInProgress);

    // Update button state based on current node
    RoguePathNode currentNode = currentRun.getCurrentNode();

    if (currentNode == null) {
      view.getBtnEnterNode().setEnabled(false);
      view.getBtnEnterNode().setText(Localizer.getInstance().getMessage("lblRogueNoNodeAvailable"));
      view.getBtnRerollPlane().setVisible(false);
      return;
    }

    PathUpdateContext pathCtx = new PathUpdateContext();
    RogueEffectComposite.INSTANCE.onPathUpdate(pathCtx, currentRun);

    // Disable button if match already in progress (prevents duplicate match tabs)
    view.getBtnEnterNode().setEnabled(!matchInProgress);
    view.getBtnEditDeck().setEnabled(!matchInProgress);
    view.getBtnEnterNode().setText(getEnterButtonText(currentNode, pathCtx));
    updatePlaneboundRerollButton(currentNode, matchInProgress, pathCtx);
  }

  /**
   * Get the appropriate button text for entering a node.
   */
  private String getEnterButtonText(RoguePathNode node, PathUpdateContext pathCtx) {
    if (node instanceof NodePlanebound nodePlanebound) {
      String planeName = pathCtx.hidePlanes && !nodePlanebound.isRevealed()
          ? "???" : nodePlanebound.getRoguePlanebound().planeName();
      return Localizer.getInstance().getMessage("lblRogueEnterPlane", planeName);
    } else if (node instanceof NodeSanctum) {
      return Localizer.getInstance().getMessage("lblRogueEnterSanctum");
    } else if (node instanceof NodeBazaar) {
      return Localizer.getInstance().getMessage("lblRogueEnterBazaar");
    } else if (node instanceof NodeEvent) {
      return Localizer.getInstance().getMessage("lblRogueEnterEvent");
    } else if (node instanceof NodeChest) {
      return Localizer.getInstance().getMessage("lblRogueOpenChest");
    } else {
      return Localizer.getInstance().getMessage("lblRogueEnterNode");
    }
  }

  private void updatePlaneboundRerollButton(RoguePathNode currentNode, boolean matchInProgress,
                                        PathUpdateContext pathCtx) {
    JButton button = view.getBtnRerollPlane();
    if (!(currentNode instanceof NodePlanebound nodePlanebound)
        || matchInProgress
        || pathCtx.remainingPlaneboundRerolls <= 0) {
      button.setVisible(false);
      button.setToolTipText(null);
    } else {
      button.setVisible(true);
      button.setText(Localizer.getInstance().getMessage("lblRogueRerollPlaneCount", pathCtx.remainingPlaneboundRerolls));
      boolean hasCandidates = !getPlaneboundRerollCandidates(nodePlanebound).isEmpty();
      button.setEnabled(hasCandidates);
      button.setToolTipText(hasCandidates
          ? Localizer.getInstance().getMessage("lblRogueRerollPlanebound")
          : Localizer.getInstance().getMessage("lblRogueNoAlternativePlanebound"));
    }

    if (button.getParent() != null) {
      button.getParent().revalidate();
      button.getParent().repaint();
    }
  }

  private void rerollSelectedPlanebound() {
    if (currentRun == null || currentRun.getHostedMatch() != null) {
      return;
    }

    RoguePathNode currentNode = currentRun.getCurrentNode();
    if (!(currentNode instanceof NodePlanebound nodePlanebound)) {
      return;
    }

    PathUpdateContext pathCtx = new PathUpdateContext();
    RogueEffectComposite.INSTANCE.onPathUpdate(pathCtx, currentRun);
    if (pathCtx.remainingPlaneboundRerolls <= 0) {
      updateViewWithSelection();
      return;
    }

    List<RoguePlanebound> candidates = getPlaneboundRerollCandidates(nodePlanebound);
    if (candidates.isEmpty()) {
      updateViewWithSelection();
      return;
    }

    Collections.shuffle(candidates, MyRandom.getRandom());
    nodePlanebound.setRoguePlanebound(candidates.get(0));
    nodePlanebound.setRevealed(false);
    RogueEffectComposite.INSTANCE.onPathNodeReroll(pathCtx, currentRun);
    RogueIO.saveRun(currentRun);
    updateView();
  }

  private List<RoguePlanebound> getPlaneboundRerollCandidates(NodePlanebound selectedNode) {
    if (currentRun == null || currentRun.getPath() == null || selectedNode == null) {
      return List.of();
    }

    RoguePathNode currentNode = currentRun.getCurrentNode();
    if (currentNode == null) {
      return List.of();
    }

    int currentRow = currentNode.getRowIndex();
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

    List<RoguePlanebound> candidates = new ArrayList<>(RogueConfig.loadPlanebounds());
    candidates.removeIf(planebound -> planebound.type() != selectedNode.getPlaneboundType()
        || excludedPlanebounds.contains(planebound));
    return candidates;
  }

  void enterNode() {
    if (currentRun == null) {
      return;
    }

    // Use current node
    RoguePathNode node = currentRun.getCurrentNode();

    if (node == null) {
      return;
    }

    // Handle different node types
    if (node instanceof NodePlanebound nodePlanebound) {
      nodePlaneboundHelper.handlePlaneboundNode(nodePlanebound, currentRun);
    } else if (node instanceof NodeSanctum nodeSanctum) {
      nodeSanctumHelper.handleSanctumNode(nodeSanctum, currentRun);
    } else if (node instanceof NodeBazaar nodeBazaar) {
      nodeBazaarHelper.handleBazaarNode(nodeBazaar, currentRun);
    } else if (node instanceof NodeEvent nodeEvent) {
      nodeEventHelper.handleEventNode(nodeEvent, currentRun);
    } else if (node instanceof NodeChest nodeChest) {
      nodeChestHelper.handleChestNode(nodeChest, currentRun);
    }
  }

  void completeSideNode(RoguePathNode node) {
    node.setCompleted(true);
    currentRun.nextNode();
    RogueStats.fireOnSideNodeCompleted(currentRun, RogueMetaProgress.getInstance());
    RogueIO.saveRun(currentRun);
    update();
  }

  boolean checkSideNodeDefeat(String defeatedBy) {
    if (currentRun == null || currentRun.getCurrentLife() > 0) {
      return false;
    }

    DefeatContext defeatCtx = new DefeatContext();
    RogueEffectComposite.INSTANCE.onDefeat(defeatCtx, currentRun);
    if (defeatCtx.revived) {
      currentRun.setCurrentLife(defeatCtx.reviveLife);
      FOptionPane.showMessageDialog(
          Localizer.getInstance().getMessage("lblRogueContinuumRevive", defeatCtx.reviveLife),
          Localizer.getInstance().getMessage("lblRogueContinuumEngine"));
      return false;
    }

    if (RogueWinLoseController.finalizeRunDefeat(currentRun, defeatedBy)) {
      FOptionPane.showMessageDialog(Localizer.getInstance().getMessage("lblRogueSparkWon"), Localizer.getInstance().getMessage("lblRogueSparkReward"));
    }
    showSideNodeDefeatDialog(defeatedBy);
    currentRun = null;
    CHomeUI.SINGLETON_INSTANCE.itemClick(EDocID.HOME_ROGUESTART);
    return true;
  }

  public void continueEndlessly() {
    if (currentRun == null || !currentRun.isRunComplete() || currentRun.isRunFailed()
        || currentRun.isRunWon()
        || !RogueMetaProgress.getInstance().isEndlessModeUnlocked()) {
      return;
    }

    RogueMetaProgress progress = RogueMetaProgress.getInstance();
    currentRun.setEndlessLevel(currentRun.getEndlessLevel() + 1);
    currentRun.setCurrentNodeIndex(0);
    RoguePathGenerator.generateRandomPath(currentRun);
    CSubmenuRogueStart.SINGLETON_INSTANCE.applyRunStartEffectsAndNpcBoon(currentRun, progress);
    RogueIO.saveRun(currentRun);
  }

  private void showSideNodeDefeatDialog(String defeatedBy) {
    String message = defeatedBy == null || defeatedBy.isEmpty()
        ? Localizer.getInstance().getMessage("lblRogueDefeated")
        : Localizer.getInstance().getMessage("lblRogueDefeatedBy", defeatedBy);
    NodeResultPanel resultPanel = new NodeResultPanel(message, List.of());
    FOptionPane optionPane = new FOptionPane(null, Localizer.getInstance().getMessage("lblRogueDefeat"), null, resultPanel, List.of(Localizer.getInstance().getMessage("lblOK")), 0);
    resultPanel.initZoom(optionPane);
    optionPane.setVisible(true);
    optionPane.dispose();
  }

  void showNodeResultDialog(String title, String message,
                                    List<NodeResultPanel.CardSection> sections) {
    showDefaultNodeResultDialog(title, message, sections, false);
  }

  void showEventResultDialog(String message, List<NodeResultPanel.CardSection> sections) {
    showDefaultNodeResultDialog(Localizer.getInstance().getMessage("lblRogueEventCompleted"), message, sections, true);
  }

  private void showDefaultNodeResultDialog(String title, String message,
                                           List<NodeResultPanel.CardSection> sections,
                                           boolean typewriterMessage) {
    boolean hasCardSections = sections.stream().anyMatch(
        section -> section.cards() != null && !section.cards().isEmpty());
    int minHeight = hasCardSections ? 700 : 0;
    showResultDialog(title, message, sections, 650, minHeight,
        NodeResultPanel.MessageAlignment.LEFT, typewriterMessage);
  }

  void showNodeResultDialog(String title, String message,
                            List<NodeResultPanel.CardSection> sections,
                            int minWidth, int minHeight,
                            NodeResultPanel.MessageAlignment messageAlignment) {
    showResultDialog(title, message, sections, minWidth, minHeight, messageAlignment, false);
  }

  private void showResultDialog(String title, String message,
                                List<NodeResultPanel.CardSection> sections,
                                int minWidth, int minHeight,
                                NodeResultPanel.MessageAlignment messageAlignment,
                                boolean typewriterMessage) {
    NodeResultPanel resultPanel = new NodeResultPanel(
        message, sections, minWidth, minHeight, messageAlignment, typewriterMessage);
    FScrollPane scrollPane = new FScrollPane(resultPanel, false,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    Dimension resultSize = resultPanel.getPreferredSize();
    GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
        .getDefaultScreenDevice().getDefaultConfiguration();
    Rectangle screenBounds = gc.getBounds();
    Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
    int maxDialogWidth = (int) ((screenBounds.width - screenInsets.left - screenInsets.right) * 0.9) - 50;
    int maxDialogHeight = (int) ((screenBounds.height - screenInsets.top - screenInsets.bottom) * 0.9) - 80;
    Dimension dialogSize = new Dimension(
        Math.min(resultSize.width + 30, maxDialogWidth),
        Math.min(resultSize.height, maxDialogHeight));

    FSkin.SkinnedPanel wrapper = new FSkin.SkinnedPanel(new BorderLayout());
    wrapper.setOpaque(false);
    wrapper.add(scrollPane, BorderLayout.CENTER);
    wrapper.setPreferredSize(dialogSize);
    wrapper.setMinimumSize(dialogSize);

    FOptionPane optionPane = new FOptionPane(null, title, null, wrapper, List.of(Localizer.getInstance().getMessage("lblOK")), 0);
    resultPanel.initZoom(optionPane);
    resultPanel.startTypewriter();
    optionPane.setVisible(true);
    resultPanel.stopTypewriter();
    optionPane.dispose();
  }

  public RogueRun getCurrentRun() {
    return currentRun;
  }

  public void setCurrentRun(RogueRun run) {
    this.currentRun = run;
  }

  private void devNextNode() {
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
    update();
  }

  private void devWinRun() {
    if (currentRun == null) return;
    String commanderName = currentRun.getCurrentCommanderName();
    currentRun.setRunWon(true);
    currentRun.getRunTimer().stop();
    RogueMetaProgress progress = RogueMetaProgress.getInstance();
    progress.addEchoes(20);
    progress.addRunHistoryEntry(RogueRunHistoryEntry.fromRun(currentRun, "VICTORY", "[DEV]"));
    RogueStats.fireOnRunCompleted(currentRun, progress, true);
    RogueCommanderAchievements.instance.recordRunWon(commanderName);
    progress.recordDescensionWin(currentRun);
    RogueCommanderAchievements.instance.evaluateRunAchievements(currentRun);
    progress.notifyDescensionL1IfFirstWin(currentRun);
    RogueIO.saveRun(currentRun);
    currentRun = null;
    CHomeUI.SINGLETON_INSTANCE.itemClick(EDocID.HOME_ROGUESTART);
  }

  private void abandonRun() {
    if (currentRun == null || currentRun.getHostedMatch() != null) {
      return;
    }

    boolean confirmed = FOptionPane.showConfirmDialog(
        Localizer.getInstance().getMessage("lblRogueAbandonRunConfirm"),
        Localizer.getInstance().getMessage("lblRogueAbandonRun"),
        Localizer.getInstance().getMessage("lblRogueAbandonRun"),
        Localizer.getInstance().getMessage("lblCancel"),
        false);
    if (!confirmed) {
      return;
    }

    currentRun.getRunTimer().stop();
    RogueMetaProgress.getInstance().addRunHistoryEntry(
        RogueRunHistoryEntry.fromRun(currentRun, "ABANDONED", ""));
    RogueIO.deleteRun(currentRun);
    SoundSystem.instance.play(SoundEffectType.LoseDuel, SoundEffectType.LoseDuel.isSynced());
    currentRun = null;
    CHomeUI.SINGLETON_INSTANCE.itemClick(EDocID.HOME_ROGUESTART);
  }

  public void editDeck() {
    if (currentRun == null || currentRun.getHostedMatch() != null) {
      return;
    }

    CDeckEditorUI deckEditor = CDeckEditorUI.SINGLETON_INSTANCE;
    deckEditor.setEditorController(new CEditorRogue(currentRun, FScreen.DECK_EDITOR_ROGUE,
        deckEditor.getCDetailPicture()));
    if (!Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_ROGUE)) {
      return;
    }

    SwingUtilities.invokeLater(() -> {
      VCardCatalog catalog = VCardCatalog.SINGLETON_INSTANCE;
      if (catalog.getParentCell() != null) {
        catalog.getParentCell().setSelected(catalog);
      }
      RogueTutorialHelper.showIfNotSeen(RogueTutorial.DECK_EDITOR);
    });
  }
}
