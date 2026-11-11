package forge.screens.home.rogue;

import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.effect.PathUpdateContext;
import forge.gamemodes.rogue.effect.RogueEffectComposite;
import forge.gamemodes.rogue.path.NodePlanebound;
import forge.gamemodes.rogue.path.RoguePath;
import forge.gamemodes.rogue.path.RoguePathNode;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedPanel;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Timer;

/**
 * Displays the entire path for a Rogue Commander run. Shows all nodes in a vertical linear
 * progression with connection lines.
 */
public class PathVisualizerPanel extends SkinnedPanel {

  private static final int NODE_SPACING = 40;  // Vertical space between rows
  private static final int HORIZONTAL_SPACING = 30;  // Horizontal space between nodes in same row
  private static final int PATH_LINE_WIDTH = 4;

  private final List<NodePanel> nodePanels;
  private int currentNodeIndex;
  private RogueRun currentRun;  // Store run for reachability checks
  private NodePanel.NodeClickHandler clickHandler;
  private Integer selectedPanelIndex;

  private record RowLayout(List<NodePanel> panels, List<Rectangle> bounds) {
  }

  public PathVisualizerPanel() {
    setLayout(null);
    setOpaque(false);
    this.nodePanels = new ArrayList<>();
    this.currentNodeIndex = -1;
    this.selectedPanelIndex = null;
  }

  /**
   * Update the display with a new run's path.
   *
   * @param run The current run data
   */
  public void updatePath(RogueRun run) {
    if (run == null) {
      clearPath();
      return;
    }

    RoguePath path = run.getPath();
    if (path == null || path.getNodes().isEmpty()) {
      clearPath();
      return;
    }

    // Store run for later use
    this.currentRun = run;

    // Clear existing panels
    removeAll();
    nodePanels.clear();

    // Find current node index and current row
    RoguePathNode currentNode = run.getCurrentNode();
    currentNodeIndex = path.getNodes().indexOf(currentNode);
    int currentRow = currentNode != null ? currentNode.getRowIndex() : 0;

    // Check wound effects (e.g. Wounded Eye hides planes)
    PathUpdateContext pathCtx = new PathUpdateContext();
    RogueEffectComposite.INSTANCE.onPathUpdate(pathCtx, run);

    // Get visible nodes in current row (reachable from last completed in previous row)
    List<Integer> visibleInCurrentRow = path.getVisibleNodesInCurrentRow(currentRow, pathCtx);
    List<Integer> visibleInFuturePlaneboundRows = path.getVisibleNodesInFuturePlaneboundRows(
        currentRow, pathCtx);

    // Create panels for each node
    List<RoguePathNode> nodes = path.getNodes();
    List<NodePlaneboundPanel> toReveal = new ArrayList<>();
    for (int i = 0; i < nodes.size(); i++) {
      RoguePathNode node = nodes.get(i);
      boolean wasAlreadyRevealed = node instanceof NodePlanebound planebound
          && planebound.isRevealed();

      // Face-down logic: only show nodes that were reachable in their row
      boolean isFaceDown;
      if (node.getRowIndex() < currentRow) {
        // Past row - only show nodes that were visible when that row was current
        List<Integer> visibleInThatRow = path.getVisibleNodesInCurrentRow(node.getRowIndex(),
            new PathUpdateContext());
        isFaceDown = !visibleInThatRow.contains(i);
      } else if (node.getRowIndex() == currentRow) {
        // Current row - show visible nodes
        isFaceDown = !visibleInCurrentRow.contains(i);
      } else {
        // Future rows - effects can reveal reachable Planebound rows
        isFaceDown = !visibleInFuturePlaneboundRows.contains(i);
      }

      // Wounded Eye: force planes in current and future rows to stay face-down
      if (pathCtx.hidePlanes && node instanceof NodePlanebound
          && node.getRowIndex() >= currentRow && !node.isCompleted()) {
        isFaceDown = true;
      }

      if (wasAlreadyRevealed) {
        isFaceDown = false;
      }

      boolean isCurrentRowReveal = node.getRowIndex() == currentRow
          && visibleInCurrentRow.contains(i);
      boolean isFutureReveal = node.getRowIndex() > currentRow
          && visibleInFuturePlaneboundRows.contains(i);
      boolean animateReveal = node instanceof NodePlanebound && !wasAlreadyRevealed && !isFaceDown
          && (isCurrentRowReveal || isFutureReveal);

      if (!isFaceDown && node instanceof NodePlanebound planebound) {
        planebound.setRevealed(true);
      }

      // Calculate planebound row count for life display
      int planeboundRowCount = path.countPlaneboundRowsUpTo(node.getRowIndex());

      NodePanel nodePanel = NodePanelFactory.createPanel(node, isFaceDown, planeboundRowCount,
          animateReveal);
      nodePanel.setClickHandler(this::handleNodeClick);
      nodePanels.add(nodePanel);
      add(nodePanel);

      if (animateReveal && nodePanel instanceof NodePlaneboundPanel) {
        toReveal.add((NodePlaneboundPanel) nodePanel);
      }
    }

    calculatePreferredSize();
    revalidate();
    repaint();

    // Trigger staggered flip animations
    if (!toReveal.isEmpty()) {
      final int[] revealIndex = {0};
      Timer revealTimer = new Timer(150, e -> {
        if (revealIndex[0] < toReveal.size()) {
          toReveal.get(revealIndex[0]).flipToReveal();
          revealIndex[0]++;
        } else {
          ((Timer) e.getSource()).stop();
        }
      });
      revealTimer.setInitialDelay(300); // Brief pause before first flip
      revealTimer.start();
    }
  }

  /**
   * Handle click on a node panel.
   */
  private void handleNodeClick(NodePanel panel) {
    if (clickHandler != null) {
      int index = nodePanels.indexOf(panel);
      if (index >= 0) {
        clickHandler.onNodeClicked(panel);
      }
    }
  }

  /**
   * Set the click handler for node panels.
   */
  public void setNodeClickHandler(NodePanel.NodeClickHandler handler) {
    this.clickHandler = handler;
  }

  /**
   * Set the selected node by index. Clears previous selection and highlights the new selected
   * node.
   */
  public void setSelectedNode(Integer nodeIndex) {
    // Clear previous selection
    if (selectedPanelIndex != null && selectedPanelIndex < nodePanels.size()) {
      nodePanels.get(selectedPanelIndex).setSelected(false);
    }

    // Set new selection
    selectedPanelIndex = nodeIndex;
    if (selectedPanelIndex != null && selectedPanelIndex < nodePanels.size()) {
      nodePanels.get(selectedPanelIndex).setSelected(true);
    }
  }

  /**
   * Clear all nodes from the display.
   */
  public void clearPath() {
    removeAll();
    nodePanels.clear();
    currentNodeIndex = -1;
    revalidate();
    repaint();
  }

  /**
   * Calculate the preferred size based on number of nodes.
   */
  private void calculatePreferredSize() {
    List<RowLayout> rowLayouts = calculateRowLayouts(0);
    if (rowLayouts.isEmpty()) {
      setPreferredSize(new Dimension(0, 0));
      return;
    }

    int maxX = 0;
    int maxY = 0;
    for (RowLayout rowLayout : rowLayouts) {
      for (Rectangle bound : rowLayout.bounds()) {
        maxX = Math.max(maxX, bound.x + bound.width);
        maxY = Math.max(maxY, bound.y + bound.height);
      }
    }
    setPreferredSize(new Dimension(maxX + 20, maxY + 20));
  }

  @Override
  public void doLayout() {
    List<RowLayout> rowLayouts = calculateRowLayouts(getWidth());
    for (RowLayout rowLayout : rowLayouts) {
      List<NodePanel> rowPanels = rowLayout.panels();
      List<Rectangle> bounds = rowLayout.bounds();
      for (int i = 0; i < rowPanels.size() && i < bounds.size(); i++) {
        rowPanels.get(i).setBounds(bounds.get(i));
      }
    }
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    if (nodePanels.isEmpty() || currentRun == null) {
      return;
    }

    RoguePath path = currentRun.getPath();
    if (path == null) {
      return;
    }

    Graphics2D g2d = (Graphics2D) g;
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // Draw connection lines to reachable nodes
    g2d.setColor(FSkin.getColor(FSkin.Colors.CLR_BORDERS).getColor());
    g2d.setStroke(new BasicStroke(PATH_LINE_WIDTH));

    List<RoguePathNode> nodes = path.getNodes();

    for (int i = 0; i < nodePanels.size(); i++) {
      RoguePathNode fromNode = nodes.get(i);
      List<Integer> reachableIndices = path.getReachableNodeIndices(i);

      if (reachableIndices.isEmpty()) {
        continue;
      }

      NodePanel fromPanel = nodePanels.get(i);

      for (Integer toIndex : reachableIndices) {
        if (toIndex >= nodePanels.size()) {
          continue;
        }

        NodePanel toPanel = nodePanels.get(toIndex);

        // Calculate line positions (center bottom of from to center top of to)
        int x1 = fromPanel.getX() + (fromPanel.getWidth() / 2);
        int y1 = fromPanel.getY() + fromPanel.getHeight();
        int x2 = toPanel.getX() + (toPanel.getWidth() / 2);
        int y2 = toPanel.getY();

        // Draw the connecting line
        // Use curved line for diagonal connections
        if (Math.abs(x2 - x1) > 10) {
          drawCurvedLine(g2d, x1, y1, x2, y2);
        } else {
          g2d.drawLine(x1, y1, x2, y2);
        }

        // Draw arrow at the end
        drawArrow(g2d, x1, y1, x2, y2);
      }
    }
  }

  /**
   * Draw a curved line (cubic Bezier curve) from (x1, y1) to (x2, y2).
   */
  private void drawCurvedLine(Graphics2D g2d, int x1, int y1, int x2, int y2) {
    // Control points for cubic Bezier curve
    // Place control points vertically between start and end
    int midY = (y1 + y2) / 2;
    int ctrl1X = x1;
    int ctrl1Y = midY;
    int ctrl2X = x2;
    int ctrl2Y = midY;

    java.awt.geom.CubicCurve2D curve = new java.awt.geom.CubicCurve2D.Float(
        x1, y1,
        ctrl1X, ctrl1Y,
        ctrl2X, ctrl2Y,
        x2, y2
    );

    g2d.draw(curve);
  }

  /**
   * Draw an arrow pointing from (x1, y1) to (x2, y2).
   */
  private void drawArrow(Graphics2D g2d, int x1, int y1, int x2, int y2) {
    int arrowSize = 10;

    // Calculate angle
    double angle = Math.atan2(y2 - y1, x2 - x1);

    // Arrow head points
    int x3 = (int) (x2 - arrowSize * Math.cos(angle - Math.PI / 6));
    int y3 = (int) (y2 - arrowSize * Math.sin(angle - Math.PI / 6));
    int x4 = (int) (x2 - arrowSize * Math.cos(angle + Math.PI / 6));
    int y4 = (int) (y2 - arrowSize * Math.sin(angle + Math.PI / 6));

    // Draw arrow head
    g2d.drawLine(x2, y2, x3, y3);
    g2d.drawLine(x2, y2, x4, y4);
  }

  /**
   * Get all node panels.
   */
  public List<NodePanel> getNodePanels() {
    return nodePanels;
  }

  /**
   * Get the current node index.
   */
  public int getCurrentNodeIndex() {
    return currentNodeIndex;
  }

  private List<RowLayout> calculateRowLayouts(int availableWidth) {
    if (nodePanels.isEmpty() || currentRun == null || currentRun.getPath() == null) {
      return new ArrayList<>();
    }

    RoguePath path = currentRun.getPath();
    List<RoguePathNode> nodes = path.getNodes();
    Map<Integer, List<Integer>> rowIndices = new HashMap<>();
    for (int i = 0; i < nodePanels.size() && i < nodes.size(); i++) {
      rowIndices.computeIfAbsent(nodes.get(i).getRowIndex(), k -> new ArrayList<>()).add(i);
    }

    int maxRow = path.getMaxRow();
    int maxPackedRowWidth = 0;
    int y = 20;

    List<Integer> rows = new ArrayList<>();
    Map<Integer, Integer> rowY = new HashMap<>();
    Map<Integer, Boolean> sideRows = new HashMap<>();
    Map<Integer, List<NodePanel>> rowPanels = new HashMap<>();

    for (int row = 0; row <= maxRow; row++) {
      List<Integer> indices = rowIndices.get(row);
      if (indices == null || indices.isEmpty()) {
        continue;
      }

      indices.sort(Comparator.comparingInt(i -> nodes.get(i).getColumnIndex()));

      int rowHeight = 0;
      int rowWidth = 0;
      boolean sideRow = true;
      List<NodePanel> panels = new ArrayList<>();

      for (Integer index : indices) {
        NodePanel panel = nodePanels.get(index);
        panels.add(panel);
        rowHeight = Math.max(rowHeight, panel.getPreferredSize().height);
        rowWidth += panel.getPreferredSize().width;
        sideRow &= nodes.get(index).isSideNode();
      }
      rowWidth += (panels.size() - 1) * HORIZONTAL_SPACING;

      rows.add(row);
      rowY.put(row, y);
      sideRows.put(row, sideRow);
      rowPanels.put(row, panels);
      maxPackedRowWidth = Math.max(maxPackedRowWidth, rowWidth);
      y += rowHeight + NODE_SPACING;
    }

    int layoutWidth = availableWidth > 0 ? Math.max(availableWidth, maxPackedRowWidth + 40)
        : maxPackedRowWidth + 40;

    Map<Integer, List<Rectangle>> packedBounds = new HashMap<>();
    for (Integer row : rows) {
      packedBounds.put(row, createPackedBounds(rowPanels.get(row), rowY.get(row), layoutWidth));
    }

    Map<Integer, Rectangle> planeBoundsByIndex = new HashMap<>();
    for (Integer row : rows) {
      if (Boolean.TRUE.equals(sideRows.get(row))) {
        continue;
      }
      List<Integer> indices = rowIndices.get(row);
      List<Rectangle> bounds = packedBounds.get(row);
      for (int i = 0; i < indices.size() && i < bounds.size(); i++) {
        planeBoundsByIndex.put(indices.get(i), bounds.get(i));
      }
    }

    List<RowLayout> rowLayouts = new ArrayList<>();
    for (Integer row : rows) {
      List<NodePanel> panels = rowPanels.get(row);
      List<Rectangle> rowPackedBounds = packedBounds.get(row);
      List<Rectangle> bounds;

      if (!Boolean.TRUE.equals(sideRows.get(row))) {
        bounds = rowPackedBounds;
      } else {
        bounds = createSideRowBounds(
            row,
            panels,
            rowIndices,
            planeBoundsByIndex,
            rowPackedBounds
        );
      }

      rowLayouts.add(new RowLayout(panels, bounds));
    }

    return rowLayouts;
  }

  private List<Rectangle> createPackedBounds(List<NodePanel> rowPanels, int y, int layoutWidth) {
    List<Rectangle> bounds = new ArrayList<>();
    int totalWidth = 0;
    for (NodePanel panel : rowPanels) {
      totalWidth += panel.getPreferredSize().width;
    }
    totalWidth += (rowPanels.size() - 1) * HORIZONTAL_SPACING;

    int startX = (layoutWidth - totalWidth) / 2;
    for (NodePanel panel : rowPanels) {
      int panelWidth = panel.getPreferredSize().width;
      int panelHeight = panel.getPreferredSize().height;
      bounds.add(new Rectangle(startX, y, panelWidth, panelHeight));
      startX += panelWidth + HORIZONTAL_SPACING;
    }
    return bounds;
  }

  private List<Rectangle> createSideRowBounds(int row, List<NodePanel> rowPanels,
                                              Map<Integer, List<Integer>> rowIndices,
                                              Map<Integer, Rectangle> planeBoundsByIndex,
                                              List<Rectangle> packedBounds) {
    if (rowPanels.isEmpty()) {
      return new ArrayList<>();
    }

    int sideCount = rowPanels.size();
    List<Double> sideCenters = getSideCenters(sideCount,
        getRowPlaneCenters(row - 1, rowIndices, planeBoundsByIndex));
    if (sideCenters.isEmpty()) {
      sideCenters = getSideCenters(sideCount,
          getRowPlaneCenters(row + 1, rowIndices, planeBoundsByIndex));
    }
    if (sideCenters.isEmpty()) {
      return packedBounds;
    }

    List<Rectangle> bounds = new ArrayList<>();
    int y = packedBounds.isEmpty() ? 0 : packedBounds.get(0).y;

    for (int i = 0; i < rowPanels.size(); i++) {
      NodePanel panel = rowPanels.get(i);
      double centerX = sideCenters.get(i);
      int x = (int) Math.round(centerX - (panel.getPreferredSize().width / 2.0));
      bounds.add(new Rectangle(x, y, panel.getPreferredSize().width, panel.getPreferredSize().height));
    }
    return bounds;
  }

  private List<Double> getRowPlaneCenters(int row, Map<Integer, List<Integer>> rowIndices,
                                          Map<Integer, Rectangle> planeBoundsByIndex) {
    List<Double> centers = new ArrayList<>();
    List<Integer> indices = rowIndices.get(row);
    if (indices == null || indices.isEmpty()) {
      return centers;
    }

    for (Integer index : indices) {
      Rectangle bound = planeBoundsByIndex.get(index);
      if (bound != null) {
        centers.add(bound.getX() + (bound.getWidth() / 2.0));
      }
    }
    return centers;
  }

  private List<Double> getSideCenters(int sideCount, List<Double> planeCenters) {
    if (planeCenters.isEmpty()) {
      return new ArrayList<>();
    }

    List<Double> anchors = getSideAnchors(sideCount, planeCenters);
    if (anchors.isEmpty()) {
      return new ArrayList<>();
    }
    if (sideCount == anchors.size()) {
      return anchors;
    }
    if (sideCount < anchors.size()) {
      int start = (anchors.size() - sideCount) / 2;
      return new ArrayList<>(anchors.subList(start, start + sideCount));
    }

    double spacing = getAverageCenterSpacing(anchors);
    if (spacing <= 0) {
      spacing = getAverageCenterSpacing(planeCenters);
    }
    int extraCount = sideCount - anchors.size();
    if (spacing <= 0 || extraCount % 2 != 0) {
      return new ArrayList<>();
    }

    int extraPerSide = extraCount / 2;
    List<Double> centers = new ArrayList<>(sideCount);
    double left = anchors.get(0);
    for (int i = extraPerSide; i >= 1; i--) {
      centers.add(left - (i * spacing));
    }
    centers.addAll(anchors);
    double right = anchors.get(anchors.size() - 1);
    for (int i = 1; i <= extraPerSide; i++) {
      centers.add(right + (i * spacing));
    }
    return centers;
  }

  private List<Double> getSideAnchors(int sideCount, List<Double> planeCenters) {
    if (planeCenters.size() == 1 || sideCount % 2 == planeCenters.size() % 2) {
      return new ArrayList<>(planeCenters);
    }

    List<Double> anchors = new ArrayList<>();
    for (int i = 1; i < planeCenters.size(); i++) {
      anchors.add((planeCenters.get(i - 1) + planeCenters.get(i)) / 2.0);
    }
    return anchors;
  }

  private double getAverageCenterSpacing(List<Double> centers) {
    if (centers.size() < 2) {
      return 0;
    }

    double totalSpacing = 0;
    for (int i = 1; i < centers.size(); i++) {
      totalSpacing += centers.get(i) - centers.get(i - 1);
    }
    return totalSpacing / (centers.size() - 1);
  }
}
