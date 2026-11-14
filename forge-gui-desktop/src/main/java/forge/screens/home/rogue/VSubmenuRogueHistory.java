package forge.screens.home.rogue;

import forge.util.Localizer;

import forge.gamemodes.rogue.RogueRunHistoryEntry;
import forge.gamemodes.rogue.effect.DescensionLevel;
import forge.gamemodes.rogue.effect.EndlessMode;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.localinstance.skin.FSkinProp;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.VHomeUI;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FTextArea;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.miginfocom.swing.MigLayout;

/**
 * View for Rogue Commander run history screen.
 */
public enum VSubmenuRogueHistory implements IVSubmenu<CSubmenuRogueHistory> {
  SINGLETON_INSTANCE;

  private DragCell parentCell;
  private final Localizer localizer = Localizer.getInstance();
  private final DragTab tab = new DragTab(localizer.getMessage("lblRogueRunHistory"));

  private final FLabel lblTitle = new FLabel.Builder()
      .text(Localizer.getInstance().getMessage("lblRogueRogueCommanderRunHistory"))
      .fontAlign(SwingConstants.CENTER)
      .opaque(true)
      .fontSize(16)
      .build();

  private final ScrollablePanel pnlContent = new ScrollablePanel(new MigLayout("insets 10, gap 0, wrap, fillx"));
  private final FScrollPane scrollContent;
  private final FButton btnBack;
  private final FButton btnResetHistory;

  VSubmenuRogueHistory() {
    lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
    pnlContent.setOpaque(false);

    scrollContent = new FScrollPane(pnlContent, true,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    btnBack = new FButton(localizer.getMessage("lblBack"));
    btnBack.setIcon(FSkin.getImage(FSkinProp.ICO_OPEN).resize(24, 24).getIcon());

    btnResetHistory = new FButton(localizer.getMessage("lblRogueResetHistory"));
    btnResetHistory.setIcon(FSkin.getImage(FSkinProp.ICO_DELETE).resize(24, 24).getIcon());
  }

  @Override
  public EMenuGroup getGroupEnum() { return EMenuGroup.ROGUE; }

  @Override
  public String getMenuTitle() { return localizer.getMessage("lblRogueRunHistory"); }

  @Override
  public EDocID getItemEnum() { return EDocID.HOME_ROGUEHISTORY; }

  @Override
  public EDocID getDocumentID() { return EDocID.HOME_ROGUEHISTORY; }

  @Override
  public DragTab getTabLabel() { return tab; }

  @Override
  public CSubmenuRogueHistory getLayoutControl() { return CSubmenuRogueHistory.SINGLETON_INSTANCE; }

  @Override
  public void populate() {
    VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();
    VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0, wrap"));

    VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitle, "w 98%!, h 30px!, gap 1% 0 15px 15px");
    VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(scrollContent, "w 98%!, h 80%!, gap 1% 0 10px 10px");

    JPanel buttonPanel = new JPanel(new MigLayout("insets 0, gap 10"));
    buttonPanel.setOpaque(false);
    buttonPanel.add(btnBack, "w 180px!, h 40px!");
    buttonPanel.add(btnResetHistory, "w 180px!, h 40px!");
    VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(buttonPanel, "ax center, gap 0 0 10px 10px");

    VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
    VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
  }

  public void displayHistory(List<RogueRunHistoryEntry> entries, Consumer<RogueRunHistoryEntry> onViewDeck) {
    pnlContent.removeAll();

    if (entries == null || entries.isEmpty()) {
      FLabel lblEmpty = new FLabel.Builder()
          .text(Localizer.getInstance().getMessage("lblRogueNoCompletedRunsYet"))
          .fontSize(14)
          .fontAlign(SwingConstants.CENTER)
          .build();
      pnlContent.add(lblEmpty, "growx, ax center, gaptop 40px");
    } else {
      for (int i = entries.size() - 1; i >= 0; i--) {
        pnlContent.add(new RunHistoryCard(entries.get(i), onViewDeck), "growx, pushx, gapbottom 8");
      }
    }

    pnlContent.revalidate();
    pnlContent.repaint();
  }

  public JButton getBtnBack() { return btnBack; }
  public JButton getBtnResetHistory() { return btnResetHistory; }

  @Override
  @SuppressWarnings("java:S3066") // Required by IVDoc.
  public void setParentCell(DragCell cell0) { this.parentCell = cell0; }

  @Override
  public DragCell getParentCell() { return parentCell; }

  /**
   * Panel displaying a single run history entry.
   * Layout: avatar on left, info rows on right, all left aligned.
   */
  static class RunHistoryCard extends FSkin.SkinnedPanel {
    private static final String OUTCOME_VICTORY = "VICTORY";
    private static final String OUTCOME_DEFEAT = "DEFEAT";
    private static final int INSET = 10;
    private static final int AVATAR_SIZE = 60;
    private static final int AVATAR_GAP = 5;
    private static final int ROW_GAP = 2;
    private static final int MIN_BTN_HEIGHT = 26;
    private static final int INFO_COL_W = 220;   // Width of Descension / Effects column
    private static final int INFO_COL_GAP = 15;  // Gap between main content and info column

    private boolean isHovered = false;
    private final FLabel lblAvatar;
    private final FLabel lblName;
    private final FLabel lblOutcome;
    private final FLabel lblDetail;
    private final FLabel lblTimestamp;
    private final FTextArea txtPath;
    private final FLabel lblDescension; // null if descensionLevel == 0
    private final FLabel lblEndless;    // null if endlessLevel == 0
    private final FScrollPane scrollEffects; // null if no active permanent effects
    private final FButton btnViewDeck;

    RunHistoryCard(RogueRunHistoryEntry entry, Consumer<RogueRunHistoryEntry> onViewDeck) {
      super(null); // No layout manager - manual positioning
      setOpaque(true);
      setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

      java.awt.event.MouseAdapter hoverListener = new java.awt.event.MouseAdapter() {
        @Override
        public void mouseEntered(java.awt.event.MouseEvent e) {
          SwingUtilities.invokeLater(() -> setHovered(isMouseInsideCard()));
        }
        @Override
        public void mouseExited(java.awt.event.MouseEvent e) {
          SwingUtilities.invokeLater(() -> setHovered(isMouseInsideCard()));
        }
      };
      registerHoverTracking(this, hoverListener);

      lblAvatar = new FLabel.Builder().build();
      lblAvatar.setIcon(FSkin.getAvatars().get(RogueUIHelper.getAvailableAvatarIndex(entry.getAvatarIndex())));
      add(lblAvatar);

      lblName = new FLabel.Builder()
          .text(entry.getCommanderName())
          .fontSize(15)
          .fontStyle(Font.BOLD)
          .fontAlign(SwingConstants.LEFT)
          .build();
      add(lblName);

      lblOutcome = createOutcomeLabel(entry);
      add(lblOutcome);

      lblDetail = new FLabel.Builder().text(getOutcomeDetail(entry)).fontSize(12)
          .fontAlign(SwingConstants.LEFT).build();
      add(lblDetail);

      Localizer localizer = Localizer.getInstance();
      lblTimestamp = new FLabel.Builder()
          .text(localizer.getMessage("lblRogueHistoryTimestamp", entry.getTimestamp(), entry.getFormattedRunTime()))
          .fontSize(11)
          .fontAlign(SwingConstants.RIGHT)
          .build();
      add(lblTimestamp);

      if (!entry.getPath().isEmpty()) {
        txtPath = new FTextArea(localizer.getMessage("lblRogueHistoryPath", String.join(", ", entry.getPath())));
        txtPath.setEditable(false);
        txtPath.setLineWrap(true);
        txtPath.setWrapStyleWord(true);
        txtPath.setFocusable(false);
        txtPath.setOpaque(false);
        txtPath.setFont(FSkin.getFont(12));
        txtPath.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT).brighter());
        add(txtPath);
      } else {
        txtPath = null;
      }

      DescensionLevel descensionLevel = DescensionLevel.forLevel(entry.getDescensionLevel());

      if (descensionLevel != null) {
           lblDescension = new FLabel.Builder()
               .text(localizer.getMessage("lblRogueHistoryDescension", descensionLevel.level, descensionLevel.name))
               .fontSize(12).fontAlign(SwingConstants.LEFT).build();
           add(lblDescension);
      } else {
           lblDescension = null;
      }

      if (entry.getEndlessLevel() > 0) {
        lblEndless = new FLabel.Builder()
            .text(localizer.getMessage("lblRogueEndlessLevel", entry.getEndlessLevel()))
            .fontSize(12).fontAlign(SwingConstants.LEFT).build();
        lblEndless.setToolTipText(EndlessMode.getModifiersDescription(entry.getEndlessLevel()));
        add(lblEndless);
      } else {
        lblEndless = null;
      }

      List<RogueRunHistoryEntry.EffectSnapshot> effects = entry.getActiveEffects();
      if (!effects.isEmpty()) {
        scrollEffects = createEffectsScroller(effects);
        add(scrollEffects);
      } else {
        scrollEffects = null;
      }

      if (entry.getDeckSnapshot() != null) {
        btnViewDeck = RogueButtonHelper.createViewDeckButtonForHistory(() -> onViewDeck.accept(entry));
        add(btnViewDeck);
      } else {
        btnViewDeck = null;
      }

      for (Component component : getComponents()) {
        registerHoverTracking(component, hoverListener);
      }
    }

    private static FLabel createOutcomeLabel(RogueRunHistoryEntry entry) {
      String outcome = entry.getOutcome();
      String displayedOutcome = entry.getEndlessLevel() > 0 && OUTCOME_DEFEAT.equals(outcome)
          ? OUTCOME_VICTORY : outcome;
      Color outcomeColor = switch (displayedOutcome) {
        case OUTCOME_VICTORY -> new Color(0, 200, 0);
        case OUTCOME_DEFEAT -> new Color(220, 50, 50);
        default -> Color.GRAY;
      };
      Localizer localizer = Localizer.getInstance();
      FLabel label = new FLabel.Builder()
          .text(switch (displayedOutcome) {
            case OUTCOME_VICTORY -> localizer.getMessage("lblRogueHistoryVictory");
            case OUTCOME_DEFEAT -> localizer.getMessage("lblRogueHistoryDefeat");
            case "ABANDONED" -> localizer.getMessage("lblRogueHistoryAbandoned");
            default -> displayedOutcome;
          })
          .fontSize(13)
          .fontStyle(Font.BOLD)
          .fontAlign(SwingConstants.RIGHT)
          .build();
      label.setForeground(outcomeColor);
      return label;
    }

    private static String getOutcomeDetail(RogueRunHistoryEntry entry) {
      String key = switch (entry.getOutcome()) {
        case OUTCOME_VICTORY -> "lblRogueHistoryBossSlain";
        case OUTCOME_DEFEAT -> "lblRogueHistoryDefeatedBy";
        default -> null;
      };
      String opponent = entry.getBossOrDefeatedBy();
      if (key == null || opponent.isEmpty()) {
        return "";
      }
      return Localizer.getInstance().getMessage(key, opponent);
    }

    private static FScrollPane createEffectsScroller(List<RogueRunHistoryEntry.EffectSnapshot> effects) {
      JPanel pnlEffects = RogueUIHelper.createEffectPanel();
      RogueUIHelper.populateEffectPanelFromSnapshots(pnlEffects, effects);
      FScrollPane scroller = new FScrollPane(pnlEffects, false,
          ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
          ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      RogueUIHelper.resizeEffectScroller(scroller, pnlEffects);
      return scroller;
    }

    @Override
    public void doLayout() {
      int w = getWidth();
      LayoutMetrics metrics = getLayoutMetrics(w);
      int y = INSET;

      lblAvatar.setBounds(INSET, INSET, AVATAR_SIZE, AVATAR_SIZE);

      lblName.setBounds(metrics.contentX(), y, metrics.leftW(), metrics.rowH());
      lblOutcome.setBounds(metrics.rightSideX(), y, metrics.rightSideW(), metrics.rowH());
      y += metrics.rowH() + ROW_GAP;

      lblDetail.setBounds(metrics.contentX(), y, metrics.leftW(), metrics.rowH());
      lblTimestamp.setBounds(metrics.rightSideX(), y, metrics.rightSideW(), metrics.metadataH());
      y += metrics.metadataH() + ROW_GAP;

      int pathH = getPathHeight(metrics.leftW());
      if (txtPath != null) {
        txtPath.setBounds(metrics.contentX(), y, metrics.leftW(), pathH);
      }
      Dimension viewDeckSize = metrics.viewDeckSize();
      if (viewDeckSize != null) {
        int buttonY = y + Math.max(0, (pathH - viewDeckSize.height) / 2);
        btnViewDeck.setBounds(metrics.rightSideX(), buttonY, viewDeckSize.width, viewDeckSize.height);
      }
      y += getPathRowHeight(pathH, viewDeckSize);

      if (!metrics.hasInfoCol() && lblEndless != null) {
        lblEndless.setBounds(metrics.contentX(), y, metrics.leftW(), metrics.rowH());
        y += metrics.rowH() + ROW_GAP;
      }
      if (!metrics.hasInfoCol() && scrollEffects != null) {
        Dimension effectsSize = scrollEffects.getPreferredSize();
        scrollEffects.setBounds(metrics.contentX(), y,
            Math.max(0, Math.min(w - metrics.contentX() - INSET, effectsSize.width)), effectsSize.height);
        y += effectsSize.height;
      }

      int totalHeight = y + INSET;

      if (metrics.hasInfoCol()) {
        int infoY = INSET + metrics.rowH() + ROW_GAP;
        if (lblDescension != null) {
          lblDescension.setBounds(metrics.infoColX(), infoY, metrics.infoColW(), metrics.rowH());
          infoY += metrics.rowH() + ROW_GAP;
        }
        if (lblEndless != null) {
          lblEndless.setBounds(metrics.infoColX(), infoY, metrics.infoColW(), metrics.rowH());
          infoY += metrics.rowH() + ROW_GAP;
        }
        if (scrollEffects != null) {
          Dimension effectsSize = scrollEffects.getPreferredSize();
          scrollEffects.setBounds(metrics.infoColX(), infoY,
              Math.min(metrics.infoColW(), effectsSize.width), effectsSize.height);
          infoY += effectsSize.height;
        }
        totalHeight = Math.max(totalHeight, infoY + INSET);
      }

      setPreferredSize(new Dimension(w, totalHeight));
    }

    @Override
    public Dimension getPreferredSize() {
      int w = getParent() != null ? getParent().getWidth() : 500;
      LayoutMetrics metrics = getLayoutMetrics(w);
      int y = INSET;
      y += 20 + ROW_GAP;
      y += metrics.metadataH() + ROW_GAP;
      int pathH = getPathHeight(Math.max(metrics.leftW(), 100));
      y += getPathRowHeight(pathH, metrics.viewDeckSize());
      if (!metrics.hasInfoCol() && lblEndless != null) {
        y += 20 + ROW_GAP;
      }
      if (!metrics.hasInfoCol() && scrollEffects != null) {
        y += scrollEffects.getPreferredSize().height;
      }
      int totalHeight = y + INSET;
      if (metrics.hasInfoCol()) {
        int infoY = INSET + 20 + ROW_GAP;
        infoY += metrics.metadataH() + ROW_GAP;
        if (lblDescension != null) {
          infoY += 20 + ROW_GAP;
        }
        if (lblEndless != null) {
          infoY += 20 + ROW_GAP;
        }
        if (scrollEffects != null) {
          infoY += scrollEffects.getPreferredSize().height;
        }
        totalHeight = Math.max(totalHeight, infoY + INSET);
      }
      return new Dimension(w, totalHeight);
    }

    private int getPathHeight(int width) {
      if (txtPath == null) {
        return 0;
      }
      txtPath.setSize(width, Short.MAX_VALUE);
      return txtPath.getPreferredSize().height;
    }

    private int getPathRowHeight(int pathHeight, Dimension viewDeckSize) {
      if (txtPath == null && viewDeckSize == null) {
        return 0;
      }
      int buttonHeight = viewDeckSize == null ? 0 : viewDeckSize.height;
      return Math.max(pathHeight, buttonHeight) + ROW_GAP;
    }

    private LayoutMetrics getLayoutMetrics(int width) {
      int contentX = INSET + AVATAR_SIZE + AVATAR_GAP;
      int contentW = width - contentX - INSET;
      int rowH = 20;
      Dimension viewDeckSize = btnViewDeck != null
          ? RogueButtonHelper.getCompactButtonSize(btnViewDeck, 120, MIN_BTN_HEIGHT)
          : null;
      int buttonW = viewDeckSize != null ? viewDeckSize.width + 5 : 0;
      int metadataW = lblTimestamp.getPreferredSize().width;
      int rightSideW = Math.min(contentW, Math.max(buttonW, metadataW));
      int rightSideX = contentX + contentW - rightSideW;

      boolean hasInfoCol = (lblDescension != null || lblEndless != null || scrollEffects != null);
      int infoColWidth = scrollEffects == null ? INFO_COL_W
          : Math.max(INFO_COL_W, scrollEffects.getPreferredSize().width);
      int avail = contentW - rightSideW - infoColWidth - INFO_COL_GAP;
      if (hasInfoCol && avail < 100) {
        hasInfoCol = false;
      }

      int leftW = hasInfoCol ? Math.min(420, avail) : contentW - rightSideW;
      int infoColX = hasInfoCol ? contentX + leftW + INFO_COL_GAP : 0;
      int infoColW = hasInfoCol ? rightSideX - infoColX : 0;
      return new LayoutMetrics(contentX, rowH, viewDeckSize, rightSideW, rightSideX, hasInfoCol,
          leftW, infoColX, infoColW);
    }

    private void setHovered(boolean hovered) {
      if (isHovered != hovered) {
        isHovered = hovered;
        repaint();
      }
    }

    private boolean isMouseInsideCard() {
      if (!isShowing()) {
        return false;
      }
      if (MouseInfo.getPointerInfo() == null) {
        return false;
      }
      Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
      SwingUtilities.convertPointFromScreen(mouseLocation, this);
      return contains(mouseLocation);
    }

    private void registerHoverTracking(Component component, java.awt.event.MouseAdapter hoverListener) {
      component.addMouseListener(hoverListener);
      if (component instanceof Container container) {
        for (Component child : container.getComponents()) {
          registerHoverTracking(child, hoverListener);
        }
      }
    }

    private record LayoutMetrics(
        int contentX,
        int rowH,
        Dimension viewDeckSize,
        int rightSideW,
        int rightSideX,
        boolean hasInfoCol,
        int leftW,
        int infoColX,
        int infoColW) {
      int metadataH() {
        return rowH * 2;
      }
    }

    @Override
    public void paint(Graphics g) {
      super.paint(g);
      Graphics2D g2d = (Graphics2D) g;
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      if (isHovered) {
        g2d.setColor(new Color(180, 180, 180, 200));
        g2d.setStroke(new BasicStroke(3));
      } else {
        g2d.setColor(new Color(100, 100, 100, 150));
        g2d.setStroke(new BasicStroke(1));
      }
      g2d.drawRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 10, 10);
    }
  }

  /** JPanel that implements Scrollable to track viewport width, preventing horizontal scrolling. */
  private static class ScrollablePanel extends JPanel implements Scrollable {
    ScrollablePanel(LayoutManager layout) {
      super(layout);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
      return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
      return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
      return 64;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
      return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
      return false;
    }
  }
}
