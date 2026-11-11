package forge.screens.home.rogue;

import forge.gamemodes.rogue.effect.CustomRunModification;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.skin.FSkinProp;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.VHomeUI;
import forge.toolbox.*;
import forge.util.Localizer;
import forge.view.arcane.CardPanel;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import net.miginfocom.swing.MigLayout;

/**
 * Assembles Swing components for Rogue Commander start screen. Allows player to select a commander
 * visually and begin a new run.
 */
public enum VSubmenuRogueStart implements IVSubmenu<CSubmenuRogueStart> {
  SINGLETON_INSTANCE;
  final Localizer localizer = Localizer.getInstance();

  // Card display constants
  private static final int CENTER_CARD_WIDTH = 250;
  private static final int NEAR_CARD_WIDTH = 200;
  private static final int FAR_CARD_WIDTH = 170;
  private static final int OUTER_CARD_WIDTH = 140;
  private static final int CARD_SPACING = 15;
  private static final int CARD_PADDING = 15;
  private static final int ACTION_BUTTON_WIDTH = 150;
  private static final int ACTION_BUTTON_GAP = 10;
  private static final int COMMANDER_CAROUSEL_HEIGHT =
      Math.round(CENTER_CARD_WIDTH * CardPanel.ASPECT_RATIO) + 2 * CARD_PADDING + 20;

  // Fields used with interface IVDoc
  private DragCell parentCell;
  private final DragTab tab = new DragTab(localizer.getMessage("lblRogueStartNewRun"));

  private final FLabel lblTitle = new FLabel.Builder()
      .text(Localizer.getInstance().getMessage("lblRoguePickYourRogueCommander"))
      .opaque(true)
      .fontSize(16)
      .build();

  // Commander carousel
  private final List<CommanderCardPanel> commanderPanels = new ArrayList<>();
  private final CommanderCarouselPanel pnlCommanderCarousel = new CommanderCarouselPanel();
  private final FButton btnCommanderPrevious = new FButton("<");
  private final FButton btnCommanderNext = new FButton(">");
  private CardUtil zoomUtil; // Lazily initialized on first use

  // Commander details
  private final FLabel lblCommanderName = new FLabel.Builder()
      .text("")
      .fontSize(18)
      .fontAlign(SwingConstants.LEFT)
      .build();

  private final FLabel lblDescriptionLabel = new FLabel.Builder()
      .text(localizer.getMessage("lblRogueDescriptionLabel"))
      .fontSize(14)
      .build();

  private final FLabel lblDescriptionLock = new FLabel.Builder()
      .icon(FSkin.getImage(FSkinProp.ICO_LOCK).resize(14, 14)).build();

  private final FLabel lblThemeLabel = new FLabel.Builder()
      .text(localizer.getMessage("lblRogueThemesLabel"))
      .fontSize(14)
      .build();

  private final FTextArea txtDescription = new FTextArea("");
  private final FTextArea txtTheme = new FTextArea("");
  private FScrollPane scrollDescription;
  private FScrollPane scrollTheme;

  // Action buttons
  private final FButton btnBeginRun;
  private final FButton btnStats;
  private final FButton btnAether;
  private final FButton btnHistory;

  // Dev buttons
  private final FButton btnDevUnlockAll = new FButton(localizer.getMessage("lblRogueDevUnlockAll"));
  private final FButton btnDevNPCProgress = new FButton(localizer.getMessage("lblRogueDevNpcProgress"));

  // Run configuration
  private final FCheckBox chkCustomRuns = new FCheckBox(localizer.getMessage("lblRogueCustomRun"));
  private final FCheckBox chkRandomRogueCommander = new FCheckBox(localizer.getMessage("lblRogueMania"));
  private final FCheckBox chkChooseRogueCommander = new FCheckBox(localizer.getMessage("lblRogueChosenOne"));
  private final FLabel lblCustomRunHint = new FLabel.Builder()
      .text(localizer.getMessage("lblRogueCustomRunLimitations"))
      .fontSize(12).build();
  private FScrollPane scrollCustomModifications;
  private JPanel pnlDetails;
  private JPanel descriptionHeader;
  private JPanel pnlActions;
  private int actionColumns;
  private int lastDisplayWidth = -1;
  private int lastViewportHeight = -1;
  private final ComponentAdapter resizeListener = new ComponentAdapter() {
    @Override public void componentResized(ComponentEvent e) {
      VHomeUI.PnlDisplay display = VHomeUI.SINGLETON_INSTANCE.getPnlDisplay();
      if (display.getParent() == null) {
        return;
      }
      int width = display.getWidth();
      int height = display.getParent().getHeight();
      if (width == lastDisplayWidth && height == lastViewportHeight) {
        return;
      }
      lastDisplayWidth = width;
      lastViewportHeight = height;
      SwingUtilities.invokeLater(VSubmenuRogueStart.this::updateResponsiveLayout);
    }
  };
  private final ComponentAdapter visibilityListener = new ComponentAdapter() {
    @Override public void componentShown(ComponentEvent e) {
      SwingUtilities.invokeLater(VSubmenuRogueStart.this::updateResponsiveLayout);
    }
    @Override public void componentHidden(ComponentEvent e) {
      SwingUtilities.invokeLater(VSubmenuRogueStart.this::updateResponsiveLayout);
    }
  };
  private final Map<CustomRunModification, FCheckBox> customModificationChecks =
      new EnumMap<>(CustomRunModification.class);
  private JPanel pnlCustomRunLock;

  // Descension UI
  private final FCheckBox chkDescension = new FCheckBox(localizer.getMessage("lblRogueDescensionMode"));
  private final FButton btnDescensionDown = new FButton("<");
  private final FLabel lblDescensionLock = new FLabel.Builder()
      .icon(FSkin.getImage(FSkinProp.ICO_LOCK).resize(20, 20)).build();
  private final FLabel lblDescensionLevel = new FLabel.Builder().text("").fontSize(14).build();
  private final FButton btnDescensionUp = new FButton(">");
  private final FLabel lblDescensionDesc = new FLabel.Builder().text("").fontSize(12).build();
  private JPanel pnlDescensionLevel;
  private JPanel pnlDescensionLock;
  private final FLabel lblDescensionLockText = new FLabel.Builder().text("").fontSize(12).build();

  VSubmenuRogueStart() {
    // Setup buttons with icons (matching Path View style)
    btnBeginRun = new FButton(localizer.getMessage("lblRogueStartRun"));
    btnBeginRun.setIcon(FSkin.getImage(FSkinProp.ICO_ALPHASTRIKE).resize(24, 24).getIcon());

    btnStats = new FButton(localizer.getMessage("lblRogueCodex"));
    btnStats.setIcon(FSkin.getImage(FSkinProp.ICO_QUEST_BOOK).resize(24, 24).getIcon());

    btnAether = new FButton(localizer.getMessage("lblRogueAether"));
    btnAether.setIcon(FSkin.getImage(FSkinProp.ICO_QUEST_GOLD).resize(24, 24).getIcon());

    btnHistory = new FButton(localizer.getMessage("lblRogueHistory"));
    btnHistory.setIcon(FSkin.getImage(FSkinProp.ICO_QUEST_NOTES).resize(24, 24).getIcon());

    lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
    btnCommanderPrevious.setFocusable(false);
    btnCommanderNext.setFocusable(false);
    btnCommanderPrevious.setToolTipText(localizer.getMessage("lblRoguePreviousCommander"));
    btnCommanderNext.setToolTipText(localizer.getMessage("lblRogueNextCommander"));
    chkDescension.addComponentListener(visibilityListener);
    chkDescension.addItemListener(e -> SwingUtilities.invokeLater(this::updateResponsiveLayout));
    pnlCommanderCarousel.add(btnCommanderPrevious);
    pnlCommanderCarousel.add(btnCommanderNext);

    // Setup description text areas
    txtDescription.setOpaque(true);
    txtDescription.setEditable(false);
    txtDescription.setLineWrap(true);
    txtDescription.setWrapStyleWord(true);
    txtDescription.setFocusable(false);
    txtDescription.setFont(FSkin.getFont(14));
    txtDescription.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
    txtDescription.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

    txtTheme.setOpaque(true);
    txtTheme.setEditable(false);
    txtTheme.setLineWrap(true);
    txtTheme.setWrapStyleWord(true);
    txtTheme.setFocusable(false);
    txtTheme.setFont(FSkin.getFont(14));
    txtTheme.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
    txtTheme.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

    for (CustomRunModification modification : CustomRunModification.values()) {
      FCheckBox check = switch (modification) {
        case MANIA -> chkRandomRogueCommander;
        case CHOSEN_ONE -> chkChooseRogueCommander;
        default -> new FCheckBox(modification.getDisplayName());
      };
      customModificationChecks.put(modification, check);
    }
  }

  @Override
  public EDocID getItemEnum() {
    return EDocID.HOME_ROGUESTART;
  }

  @Override
  public EDocID getDocumentID() {
    return EDocID.HOME_ROGUESTART;
  }

  @Override
  public DragTab getTabLabel() {
    return tab;
  }

  @Override
  public EMenuGroup getGroupEnum() {
    return EMenuGroup.ROGUE;
  }

  @Override
  public String getMenuTitle() {
    return Localizer.getInstance().getMessage("lblRogueStartNewRun");
  }

  @Override
  public CSubmenuRogueStart getLayoutControl() {
    return CSubmenuRogueStart.SINGLETON_INSTANCE;
  }

  @Override
  public void populate() {
    VHomeUI.PnlDisplay display = VHomeUI.SINGLETON_INSTANCE.getPnlDisplay();
    display.removeComponentListener(resizeListener);
    display.removeAll();
    display.setLayout(new MigLayout("insets 0, gap 0, wrap"));

    display.add(lblTitle, "w 98%!, h 30px!, gap 1% 0 15px 15px");
    display.add(pnlCommanderCarousel,
        "w 98%!, h " + COMMANDER_CAROUSEL_HEIGHT + "px!, gap 1% 0 15px 15px");

    // Commander details panel - right after carousel
    pnlDetails = createDetailsPanel();
    display.add(pnlDetails, "w 98%!, gap 1% 0 0 15px");
    display.addComponentListener(resizeListener);
    lastDisplayWidth = display.getWidth();
    lastViewportHeight = display.getParent() != null ? display.getParent().getHeight() : -1;
    actionColumns = 0;

    display.repaintSelf();
    display.revalidate();
    SwingUtilities.invokeLater(this::updateResponsiveLayout);
  }

  private JPanel createDetailsPanel() {
    final String compactRowLayout = "insets 0, gap 5";
    final String growHorizontally = "growx";
    JPanel panel = new JPanel();
    panel.setOpaque(false);
    panel.setLayout(new MigLayout("insets 0, gap 0, wrap 2", "[150px][grow]"));

    // Row 0: Commander name
    panel.add(new FLabel.Builder().text(localizer.getMessage("lblRogueCommanderLabel")).fontSize(14).build(),
        "cell 0 0, alignx left, aligny top");
    panel.add(lblCommanderName, "cell 1 0, alignx left, growx");

    // Row 1: Description (label can change to "Unlock:" for locked commanders)
    descriptionHeader = new JPanel(new MigLayout("insets 0, gap 4"));
    descriptionHeader.setOpaque(false);
    lblDescriptionLock.setVisible(false);
    descriptionHeader.add(lblDescriptionLock, "w 14px!, h 14px!, aligny center, hidemode 3");
    descriptionHeader.add(lblDescriptionLabel, "aligny center");
    panel.add(descriptionHeader, "cell 0 1, alignx left, aligny top, gaptop 10px");
    scrollDescription = new FScrollPane(txtDescription, false,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    panel.add(scrollDescription, "cell 1 1, growx, h 60px!, gaptop 10px");

    // Row 2: Theme (hidden for locked commanders)
    panel.add(lblThemeLabel, "cell 0 2, alignx left, aligny top, gaptop 10px");
    scrollTheme = new FScrollPane(txtTheme, false,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollTheme.addComponentListener(visibilityListener);
    panel.add(scrollTheme, "cell 1 2, growx, h 40px!, gaptop 10px");

    // Rows 3-4: Custom run modifications
    chkCustomRuns.setVisible(false);
    panel.add(chkCustomRuns, "cell 0 3, span 2, alignx left, gaptop 10px, hidemode 3");
    pnlCustomRunLock = new JPanel(new MigLayout(compactRowLayout));
    pnlCustomRunLock.setOpaque(false);
    pnlCustomRunLock.add(
        new FLabel.Builder().icon(FSkin.getImage(FSkinProp.ICO_LOCK).resize(20, 20)).build(),
        "w 20px!, h 20px!");
    pnlCustomRunLock.add(new FLabel.Builder()
        .text(Localizer.getInstance().getMessage("lblRogueCustomRunWin2RunsWithAnyCommanderToUnlock")).fontSize(12).build(),
        growHorizontally);
    pnlCustomRunLock.setVisible(false);
    pnlCustomRunLock.addComponentListener(visibilityListener);
    panel.add(pnlCustomRunLock, "cell 0 3, span 2, growx, gaptop 10px, hidemode 3");
    FSkin.SkinnedPanel pnlCustomModifications = new ModificationsPanel(
        new MigLayout("insets 10 12 10 12, gapx 4, gapy 8, wrap 2, fillx", "[pref!][grow,fill]"));
    pnlCustomModifications.setOpaque(true);
    pnlCustomModifications.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
    pnlCustomModifications.add(lblCustomRunHint, "span 2");
    for (CustomRunModification modification : CustomRunModification.values()) {
      pnlCustomModifications.add(customModificationChecks.get(modification), "aligny top");
      pnlCustomModifications.add(new FLabel.Builder()
          .text("<html>" + modification.getRawDescription() + "</html>")
          .fontSize(12).fontAlign(SwingConstants.LEFT).build(), "growx, wmin 0");
    }
    scrollCustomModifications = new FScrollPane(pnlCustomModifications, true,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollCustomModifications.getVerticalScrollBar().setUnitIncrement(20);
    scrollCustomModifications.setVisible(chkCustomRuns.isSelected());
    panel.add(scrollCustomModifications, "cell 0 4, span 2, growx, h 240px!, gaptop 5px, hidemode 3");

    // Row 5: Descension checkbox (unlocked) OR global lock indicator (locked)
    chkDescension.setVisible(false);
    panel.add(chkDescension, "cell 0 5, span 2, alignx left, gaptop 10px, hidemode 3");

    pnlDescensionLock = new JPanel(new MigLayout(compactRowLayout));
    pnlDescensionLock.setOpaque(false);
    pnlDescensionLock.add(
        new FLabel.Builder().icon(FSkin.getImage(FSkinProp.ICO_LOCK).resize(20, 20)).build(),
        "w 20px!, h 20px!");
    pnlDescensionLock.add(lblDescensionLockText, growHorizontally);
    pnlDescensionLock.setVisible(false);
    pnlDescensionLock.addComponentListener(visibilityListener);
    panel.add(pnlDescensionLock, "cell 0 5, span 2, growx, gaptop 10px, hidemode 3");

    // Row 6: Descension level selector (hidden by default)
    pnlDescensionLevel = new JPanel(new MigLayout(compactRowLayout + ", wrap 1"));
    pnlDescensionLevel.setOpaque(false);
    JPanel arrowRow = new JPanel(new MigLayout(compactRowLayout));
    arrowRow.setOpaque(false);
    arrowRow.add(btnDescensionDown, "w 50px!, h 30px!");
    arrowRow.add(lblDescensionLock, "w 20px!, h 20px!, hidemode 3");
    arrowRow.add(lblDescensionLevel, growHorizontally);
    arrowRow.add(btnDescensionUp, "w 50px!, h 30px!");
    pnlDescensionLevel.add(arrowRow, growHorizontally);
    pnlDescensionLevel.add(lblDescensionDesc, growHorizontally);
    pnlDescensionLevel.setVisible(false);
    pnlDescensionLevel.addComponentListener(visibilityListener);
    panel.add(pnlDescensionLevel, "cell 0 6, span 2, growx, gaptop 10px, hidemode 3");

    // Row 7: Buttons
    pnlActions = new JPanel(new MigLayout("insets 0, gap " + ACTION_BUTTON_GAP));
    pnlActions.setOpaque(false);
    addActionButtons();
    panel.add(pnlActions, "cell 0 7, span 2, alignx center, gaptop 20px");

    return panel;
  }

  private void addActionButtons() {
    final String buttonSize = "w " + ACTION_BUTTON_WIDTH + "px!, h 40px!";
    pnlActions.add(btnBeginRun, buttonSize);
    pnlActions.add(btnAether, buttonSize);
    pnlActions.add(btnHistory, buttonSize);
    pnlActions.add(btnStats, buttonSize);
    if (ForgePreferences.DEV_MODE) {
      pnlActions.add(btnDevUnlockAll, buttonSize);
      pnlActions.add(btnDevNPCProgress, buttonSize);
    }
  }

  private static int getActionColumnCount(int availableWidth) {
    int maxColumns = Math.max(1,
        (availableWidth + ACTION_BUTTON_GAP) / (ACTION_BUTTON_WIDTH + ACTION_BUTTON_GAP));
    if (ForgePreferences.DEV_MODE) {
      if (maxColumns >= 6) {
        return 6;
      }
      if (maxColumns >= 3) {
        return 3;
      }
    } else if (maxColumns >= 4) {
      return 4;
    }
    return maxColumns >= 2 ? 2 : 1;
  }

  private void updateResponsiveLayout() {
    VHomeUI.PnlDisplay display = VHomeUI.SINGLETON_INSTANCE.getPnlDisplay();
    if (pnlDetails == null || pnlDetails.getParent() != display || display.getParent() == null
        || display.getWidth() <= 0 || display.getHeight() <= 0) {
      return;
    }

    int availableWidth = Math.round(display.getWidth() * 0.98f);
    int columns = getActionColumnCount(availableWidth);
    if (columns != actionColumns) {
      actionColumns = columns;
      pnlActions.removeAll();
      pnlActions.setLayout(new MigLayout("insets 0, gap " + ACTION_BUTTON_GAP + ", wrap " + columns));
      addActionButtons();
    }

    int availableHeight = display.getParent().getHeight() - display.getY();
    boolean expanded = scrollCustomModifications.isVisible();
    if (expanded) {
      scrollCustomModifications.setVisible(false);
    }
    int budget;
    boolean compact;
    try {
      budget = measureHeightBudget(display, availableHeight, false);
      compact = budget < (expanded ? 148 : 48);
      if (compact) {
        budget = measureHeightBudget(display, availableHeight, true);
      }
    } finally {
      if (expanded) {
        scrollCustomModifications.setVisible(true);
      }
    }

    int targetCustom = expanded
        ? Math.max(100, Math.min(240, Math.round(Math.max(0, budget) * 240f
            / (COMMANDER_CAROUSEL_HEIGHT + 240))))
        : 240;
    int targetCarousel = Math.max(48, Math.min(COMMANDER_CAROUSEL_HEIGHT,
        budget - (expanded ? targetCustom : 0)));
    applyResponsiveSizes(targetCarousel, targetCustom, compact);
    pnlDetails.revalidate();
    display.revalidate();
    display.repaintSelf();
  }

  private int measureHeightBudget(VHomeUI.PnlDisplay display, int availableHeight, boolean compact) {
    applyResponsiveSizes(COMMANDER_CAROUSEL_HEIGHT, 240, compact);
    int fixedHeight = display.getPreferredSize().height - COMMANDER_CAROUSEL_HEIGHT;
    return availableHeight - fixedHeight;
  }

  private void applyResponsiveSizes(int targetCarousel, int targetCustom, boolean compact) {
    int outerGap = compact ? 0 : 15;
    int rowGap = compact ? 0 : 10;
    MigLayout displayLayout = (MigLayout) VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().getLayout();
    displayLayout.setComponentConstraints(lblTitle,
        "w 98%!, h 30px!, gap 1% 0 " + outerGap + "px " + outerGap + "px");
    displayLayout.setComponentConstraints(pnlCommanderCarousel,
        "w 98%!, h " + targetCarousel + "px!, gap 1% 0 "
            + outerGap + "px " + outerGap + "px");
    displayLayout.setComponentConstraints(pnlDetails,
        "w 98%!, gap 1% 0 0 " + outerGap + "px");

    MigLayout detailsLayout = (MigLayout) pnlDetails.getLayout();
    String rowTop = ", gaptop " + rowGap + "px";
    String hiddenRowTop = rowTop + ", hidemode 3";
    int descriptionHeight = compact ? 24 : 60;
    int themeHeight = compact ? 24 : 40;

    detailsLayout.setComponentConstraints(descriptionHeader,
        "cell 0 1, alignx left, aligny top" + rowTop);
    detailsLayout.setComponentConstraints(scrollDescription,
        "cell 1 1, growx, h " + descriptionHeight + "px!" + rowTop);
    detailsLayout.setComponentConstraints(lblThemeLabel,
        "cell 0 2, alignx left, aligny top" + rowTop);
    detailsLayout.setComponentConstraints(scrollTheme,
        "cell 1 2, growx, h " + themeHeight + "px!" + rowTop);

    detailsLayout.setComponentConstraints(chkCustomRuns,
        "cell 0 3, span 2, alignx left" + hiddenRowTop);
    detailsLayout.setComponentConstraints(pnlCustomRunLock,
        "cell 0 3, span 2, growx" + hiddenRowTop);
    detailsLayout.setComponentConstraints(scrollCustomModifications,
        "cell 0 4, span 2, growx, h " + targetCustom + "px!, gaptop "
            + (compact ? 0 : 5) + "px, hidemode 3");

    detailsLayout.setComponentConstraints(chkDescension,
        "cell 0 5, span 2, alignx left" + hiddenRowTop);
    detailsLayout.setComponentConstraints(pnlDescensionLock,
        "cell 0 5, span 2, growx" + hiddenRowTop);
    detailsLayout.setComponentConstraints(pnlDescensionLevel,
        "cell 0 6, span 2, growx" + hiddenRowTop);
    detailsLayout.setComponentConstraints(pnlActions,
        "cell 0 7, span 2, alignx center, gaptop " + (compact ? 0 : 20) + "px");
  }

  void setCustomModificationsVisible(boolean visible) {
    scrollCustomModifications.setVisible(visible);
    pnlDetails.revalidate();
    SwingUtilities.invokeLater(this::updateResponsiveLayout);
  }

  @Override
  @SuppressWarnings("java:S3066") // Required by IVDoc.
  public void setParentCell(DragCell cell0) {
    this.parentCell = cell0;
  }

  @Override
  public DragCell getParentCell() {
    return this.parentCell;
  }

  // Getters for controller access
  public FLabel getLblCommanderName() {
    return lblCommanderName;
  }

  public FLabel getLblDescriptionLabel() {
    return lblDescriptionLabel;
  }

  public FLabel getLblDescriptionLock() {
    return lblDescriptionLock;
  }

  public FTextArea getTxtDescription() {
    return txtDescription;
  }

  public FTextArea getTxtTheme() {
    return txtTheme;
  }

  public FLabel getLblThemeLabel() {
    return lblThemeLabel;
  }

  public FScrollPane getScrollTheme() {
    return scrollTheme;
  }

  public JButton getBtnBeginRun() {
    return btnBeginRun;
  }

  public JButton getBtnStats() {
    return btnStats;
  }

  public JButton getBtnAether() {
    return btnAether;
  }

  public JButton getBtnHistory() {
    return btnHistory;
  }

  public JButton getBtnDevUnlockAll() {
    return btnDevUnlockAll;
  }

  public JButton getBtnDevNPCProgress() {
    return btnDevNPCProgress;
  }

  public JCheckBox getChkDescension() {
    return chkDescension;
  }

  public JCheckBox getChkCustomRuns() {
    return chkCustomRuns;
  }

  public JCheckBox getChkRandomRogueCommander() {
    return chkRandomRogueCommander;
  }

  public JCheckBox getChkChooseRogueCommander() {
    return chkChooseRogueCommander;
  }

  public FCheckBox getChkCustomModification(CustomRunModification modification) {
    return customModificationChecks.get(modification);
  }

  public JPanel getPnlCustomRunLock() {
    return pnlCustomRunLock;
  }

  public FButton getBtnDescensionDown() {
    return btnDescensionDown;
  }

  public FLabel getLblDescensionLevel() {
    return lblDescensionLevel;
  }

  public FButton getBtnDescensionUp() {
    return btnDescensionUp;
  }

  public FLabel getLblDescensionLock() {
    return lblDescensionLock;
  }

  public FLabel getLblDescensionDesc() {
    return lblDescensionDesc;
  }

  public JPanel getPnlDescensionLevel() {
    return pnlDescensionLevel;
  }

  public JPanel getPnlDescensionLock() {
    return pnlDescensionLock;
  }

  public FLabel getLblDescensionLockText() {
    return lblDescensionLockText;
  }

  public List<CommanderCardPanel> getCommanderPanels() {
    return commanderPanels;
  }

  public CommanderCarouselPanel getCommanderCarouselPanel() {
    return pnlCommanderCarousel;
  }

  public FButton getBtnCommanderPrevious() {
    return btnCommanderPrevious;
  }

  public FButton getBtnCommanderNext() {
    return btnCommanderNext;
  }

  /**
   * Get the zoom utility, creating it lazily if needed. This ensures the window hierarchy is ready
   * when zoom is first used.
   */
  public CardUtil getZoomUtil() {
    if (zoomUtil == null) {
      Window window = SwingUtilities.getWindowAncestor(pnlCommanderCarousel);
      if (window != null) {
        zoomUtil = new CardUtil(window);
        zoomUtil.setupZoomOverlay();
      }
    }
    return zoomUtil;
  }

  private static class ModificationsPanel extends FSkin.SkinnedPanel implements Scrollable {
    ModificationsPanel(MigLayout layout) {
      super(layout);
    }

    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) { return 20; }
    @Override public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) {
      return Math.max(20, r.height - 20);
    }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }
  }

  /** Displays the centered commander and the neighboring cards that fit completely. */
  public class CommanderCarouselPanel extends FSkin.SkinnedPanel {
    private CommanderCardPanel centeredPanel;

    public CommanderCarouselPanel() {
      super(null);
      setOpaque(false);
    }

    public void clear() {
      centeredPanel = null;
      for (CommanderCardPanel panel : commanderPanels) {
        remove(panel);
      }
      commanderPanels.clear();
    }

    public void addCommanderPanel(CommanderCardPanel panel) {
      commanderPanels.add(panel);
      add(panel);
    }

    public CommanderCardPanel getCenteredPanel() {
      return centeredPanel;
    }

    public void centerOn(CommanderCardPanel panel) {
      if (!commanderPanels.contains(panel)) {
        return;
      }
      centeredPanel = panel;
      doLayout();
      repaint();
    }

    @Override
    public void doLayout() {
      int count = commanderPanels.size();
      int centeredIndex = commanderPanels.indexOf(centeredPanel);
      if (centeredIndex < 0) {
        for (CommanderCardPanel panel : commanderPanels) {
          panel.setVisible(false);
        }
        btnCommanderPrevious.setVisible(false);
        btnCommanderNext.setVisible(false);
        return;
      }

      CarouselCardSizes sizes = getCardSizes();
      int threeCardsWidth = sizes.centerWidth() + 2 * sizes.nearWidth()
          + 2 * CARD_SPACING + 2 * CARD_PADDING;
      int radius = getVisibleRadius(count, sizes, threeCardsWidth);
      boolean showSecondCard = count == 2 && getWidth() >= threeCardsWidth;
      int centerLine = getHeight() / 2 + Math.round(10 * sizes.scale());
      int centerHeight = Math.round(sizes.centerWidth() * CardPanel.ASPECT_RATIO);
      int arrowWidth = 50;
      int arrowHeight = 30;
      int arrowY = Math.max(0, Math.min(getHeight() - arrowHeight,
          centerLine - centerHeight / 2 + centerHeight - arrowHeight));
      if (radius > 0 || showSecondCard) {
        int nearHeight = Math.round(sizes.nearWidth() * CardPanel.ASPECT_RATIO);
        int nearBottom = centerLine - nearHeight / 2 - Math.round(20 * sizes.scale()) + nearHeight;
        arrowY = Math.min(getHeight() - arrowHeight, Math.max(arrowY, nearBottom + 2));
        // Hide neighboring cards if the arrows would overlap them.
        if (arrowY < nearBottom + 2) {
          radius = 0;
          showSecondCard = false;
        }
      }
      layoutCards(centeredIndex, -radius, showSecondCard ? 1 : radius, centerLine, sizes);

      Rectangle centerBounds = centeredPanel.getBounds();
      btnCommanderPrevious.setBounds(Math.max(0, centerBounds.x - arrowWidth - 8),
          arrowY, arrowWidth, arrowHeight);
      btnCommanderNext.setBounds(Math.min(getWidth() - arrowWidth,
          centerBounds.x + centerBounds.width + 8), arrowY, arrowWidth, arrowHeight);
      btnCommanderPrevious.setVisible(true);
      btnCommanderNext.setVisible(true);
    }

    private CarouselCardSizes getCardSizes() {
      int centerWidth = Math.max(1, Math.min(CENTER_CARD_WIDTH,
          Math.round((getHeight() - 2 * CARD_PADDING - 20) / CardPanel.ASPECT_RATIO)));
      float scale = centerWidth / (float) CENTER_CARD_WIDTH;
      return new CarouselCardSizes(centerWidth,
          Math.max(1, Math.round(NEAR_CARD_WIDTH * scale)),
          Math.max(1, Math.round(FAR_CARD_WIDTH * scale)),
          Math.max(1, Math.round(OUTER_CARD_WIDTH * scale)), scale);
    }

    private int getVisibleRadius(int count, CarouselCardSizes sizes, int threeCardsWidth) {
      int fiveCardsWidth = threeCardsWidth + 2 * sizes.farWidth() + 2 * CARD_SPACING;
      int sevenCardsWidth = fiveCardsWidth + 2 * sizes.outerWidth() + 2 * CARD_SPACING;
      int minSideWidth = 80;
      if (count >= 7 && sizes.outerWidth() >= minSideWidth && getWidth() >= sevenCardsWidth) {
        return 3;
      }
      if (count >= 5 && sizes.farWidth() >= minSideWidth && getWidth() >= fiveCardsWidth) {
        return 2;
      }
      if (count >= 3 && sizes.nearWidth() >= minSideWidth && getWidth() >= threeCardsWidth) {
        return 1;
      }
      return 0;
    }

    private void layoutCards(int centeredIndex, int firstOffset, int lastOffset, int centerLine,
        CarouselCardSizes sizes) {
      int count = commanderPanels.size();
      boolean[] visible = new boolean[count];
      int x = (getWidth() - sizes.centerWidth()) / 2;
      // Start at the leftmost card, then advance by each card's width and the gap.
      for (int offset = firstOffset; offset < 0; offset++) {
        x -= sizes.widthAtDistance(-offset) + CARD_SPACING;
      }
      for (int offset = firstOffset; offset <= lastOffset; offset++) {
        int index = Math.floorMod(centeredIndex + offset, count);
        int distance = Math.abs(offset);
        int width = sizes.widthAtDistance(distance);
        int height = Math.round(width * CardPanel.ASPECT_RATIO);
        CommanderCardPanel panel = commanderPanels.get(index);
        panel.setBounds(x, centerLine - height / 2 - Math.round(distance * 20 * sizes.scale()),
            width, height);
        visible[index] = true;
        x += width + CARD_SPACING;
      }
      for (int i = 0; i < count; i++) {
        CommanderCardPanel panel = commanderPanels.get(i);
        if (panel.isVisible() != visible[i]) {
          panel.setVisible(visible[i]);
        }
      }
    }

    private record CarouselCardSizes(int centerWidth, int nearWidth, int farWidth, int outerWidth,
                                     float scale) {
      private int widthAtDistance(int distance) {
        return switch (distance) {
          case 0 -> centerWidth;
          case 1 -> nearWidth;
          case 2 -> farWidth;
          default -> outerWidth;
        };
      }
    }
  }
}
