package forge.screens.home.rogue;

import forge.util.Localizer;

import forge.ImageCache;
import forge.deck.Deck;
import forge.gamemodes.rogue.CodexHelper;
import forge.gamemodes.rogue.RogueConfig;
import forge.gamemodes.rogue.RogueDeck;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.RoguePlanebound;
import forge.gamemodes.rogue.RoguePlaneboundType;
import forge.gamemodes.rogue.TextHelper;
import forge.gamemodes.rogue.effect.RogueEffect;
import forge.gui.GuiBase;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.itemmanager.GroupDef;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.skin.FSkinProp;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.VHomeUI;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinImage;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.toolbox.FTabbedPane;
import forge.util.ImageFetcher;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Assembles Swing components for Rogue Commander Codex screen.
 */
public enum VSubmenuRogueCodex implements IVSubmenu<CSubmenuRogueCodex> {
  SINGLETON_INSTANCE;

  enum CodexTab {
    GLOBAL_STATS("lblRogueGlobalStats"),
    COMMANDERS("lblRogueCommanders"),
    PLANEBOUNDS("lblRoguePlanebounds"),
    TRAITS("lblRogueTraits");

    private final String labelKey;

    CodexTab(String labelKey) {
      this.labelKey = labelKey;
    }
  }

  private static final int SIDEBAR_WIDTH = 180;
  private static final int AVATAR_SIZE = 64;
  private static final int TOP_SECTION_GAP = 24;
  private static final int TOP_SECTION_WRAP_WIDTH = 700;

  private DragCell parentCell;
  private final Localizer localizer = Localizer.getInstance();
  private final DragTab tab = new DragTab(localizer.getMessage("lblRogueCodex"));
  private final FTabbedPane codexTabs = new FTabbedPane();
  private final Map<CodexTab, SkinnedPanel> tabPanels = new EnumMap<>(CodexTab.class);
  private CardUtil zoomUtil;
  private Consumer<CodexTab> tabSelectionCallback;
  private boolean updatingSelectedTab;

  private final FLabel lblTitle = new FLabel.Builder()
      .text(Localizer.getInstance().getMessage("lblRogueRogueCommanderCodex"))
      .fontAlign(SwingConstants.CENTER)
      .opaque(true)
      .fontSize(16)
      .build();

  private final FLabel lblRunsStarted = new FLabel.Builder().text(localizer.getMessage("lblRogueRunsStarted", 0)).fontSize(14).build();
  private final FLabel lblRunsCompleted = new FLabel.Builder().text(localizer.getMessage("lblRogueRunsCompleted", 0)).fontSize(14).build();
  private final FLabel lblRunsWon = new FLabel.Builder().text(localizer.getMessage("lblRogueRunsWon", 0)).fontSize(14).build();
  private final FLabel lblRunsLost = new FLabel.Builder().text(localizer.getMessage("lblRogueRunsLost", 0)).fontSize(14).build();
  private final FLabel lblTotalPlaytime = new FLabel.Builder().text(localizer.getMessage("lblRogueTotalPlaytime", "00:00:00")).fontSize(14).build();
  private final FLabel lblMatchesWon = new FLabel.Builder().text(localizer.getMessage("lblRogueMatchesWon", 0)).fontSize(14).build();
  private final FLabel lblMatchesLost = new FLabel.Builder().text(localizer.getMessage("lblRogueMatchesLost", 0)).fontSize(14).build();
  private final FLabel lblMaxLife = new FLabel.Builder().text(localizer.getMessage("lblRogueMaxLife", 0)).fontSize(14).build();
  private final FLabel lblMaxGold = new FLabel.Builder().text(localizer.getMessage("lblRogueMaxGold", 0)).fontSize(14).build();
  private final FLabel lblMaxCreatureTypes = new FLabel.Builder().text(localizer.getMessage("lblRogueMaxCreatureTypes", 0)).fontSize(14).build();
  private final FLabel lblMaxArtifacts = new FLabel.Builder().text(localizer.getMessage("lblRogueMaxArtifacts", 0)).fontSize(14).build();
  private final FLabel lblMaxSharedCreatureType = new FLabel.Builder().text(localizer.getMessage("lblRogueMaxSharedCreatureType", 0)).fontSize(14).build();
  private final FLabel lblMaxLegendaryPermanents = new FLabel.Builder().text(localizer.getMessage("lblRogueMaxLegendaryPermanents", 0)).fontSize(14).build();
  private final FLabel lblMaxNonbasicLands = new FLabel.Builder().text(localizer.getMessage("lblRogueMaxNonbasicLands", 0)).fontSize(14).build();

  private final FButton btnBack;
  private final FButton btnStatsBack;
  private final FButton btnReset;
  private final FButton btnResetTutorials;
  private final FButton btnDevUnlockCodex = new FButton(localizer.getMessage("lblRogueDevUnlockCodex"));

  VSubmenuRogueCodex() {
    lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

    for (CodexTab codexTab : CodexTab.values()) {
      SkinnedPanel tabPanel = new SkinnedPanel(new MigLayout("insets 0, gap 0"));
      tabPanel.setOpaque(false);
      tabPanels.put(codexTab, tabPanel);
      codexTabs.addTab(localizer.getMessage(codexTab.labelKey), tabPanel);
    }
    codexTabs.addChangeListener(e -> notifyTabSelection());

    btnBack = new FButton(localizer.getMessage("lblBack"));
    btnBack.setIcon(FSkin.getImage(FSkinProp.ICO_OPEN).resize(24, 24).getIcon());
    btnStatsBack = new FButton(localizer.getMessage("lblBack"));
    btnStatsBack.setIcon(FSkin.getImage(FSkinProp.ICO_OPEN).resize(24, 24).getIcon());

    btnReset = new FButton(localizer.getMessage("lblRogueResetProgress"));
    btnReset.setIcon(FSkin.getImage(FSkinProp.ICO_DELETE).resize(24, 24).getIcon());

    btnResetTutorials = new FButton(localizer.getMessage("lblRogueResetTutorials"));
    btnResetTutorials.setIcon(FSkin.getImage(FSkinProp.ICO_QUEST_BOOK).resize(24, 24).getIcon());
  }

  public void updateDisplay(int runsStarted, int runsCompleted, int runsWon, long totalPlaytimeMillis,
      int matchesWon, int matchesLost,
      int maxLife, int maxGold, int maxCreatureTypes, int maxArtifacts, int maxSharedCreatureType,
      int maxLegendaryPermanents, int maxNonbasicLands) {
    lblRunsStarted.setText(localizer.getMessage("lblRogueRunsStarted", runsStarted));
    lblRunsCompleted.setText(localizer.getMessage("lblRogueRunsCompleted", runsCompleted));
    lblRunsWon.setText(localizer.getMessage("lblRogueRunsWon", runsWon));
    lblRunsLost.setText(localizer.getMessage("lblRogueRunsLost", runsCompleted - runsWon));
    lblTotalPlaytime.setText(localizer.getMessage("lblRogueTotalPlaytime", TextHelper.formatHoursMinutesSeconds(totalPlaytimeMillis)));
    lblMatchesWon.setText(localizer.getMessage("lblRogueMatchesWon", matchesWon));
    lblMatchesLost.setText(localizer.getMessage("lblRogueMatchesLost", matchesLost));
    lblMaxLife.setText(localizer.getMessage("lblRogueMaxLife", maxLife));
    lblMaxGold.setText(localizer.getMessage("lblRogueMaxGold", maxGold));
    lblMaxCreatureTypes.setText(localizer.getMessage("lblRogueMaxCreatureTypes", maxCreatureTypes));
    lblMaxArtifacts.setText(localizer.getMessage("lblRogueMaxArtifacts", maxArtifacts));
    lblMaxSharedCreatureType.setText(localizer.getMessage("lblRogueMaxSharedCreatureType", maxSharedCreatureType));
    lblMaxLegendaryPermanents.setText(localizer.getMessage("lblRogueMaxLegendaryPermanents", maxLegendaryPermanents));
    lblMaxNonbasicLands.setText(localizer.getMessage("lblRogueMaxNonbasicLands", maxNonbasicLands));
  }

  public void showGlobalStats() {
    setActiveTab(CodexTab.GLOBAL_STATS);
    SkinnedPanel pnlContent = getContentPanel(CodexTab.GLOBAL_STATS);
    pnlContent.removeAll();
    pnlContent.setLayout(new MigLayout("insets 0, gap 0", "[grow]", "[grow]"));
    FScrollPane scroller = new FScrollPane(createStatsPanel(), false,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    pnlContent.add(scroller, "w 100%!, h 100%!");
    refreshContent();
  }

  public void showCommanders(List<RogueDeck> commanders, RogueDeck selectedCommander,
                             RogueMetaProgress progress, Consumer<RogueDeck> selectionCallback) {
    setActiveTab(CodexTab.COMMANDERS);
    SkinnedPanel pnlContent = getContentPanel(CodexTab.COMMANDERS);
    pnlContent.removeAll();
    pnlContent.setLayout(new MigLayout("insets 0, gap 10", "[" + SIDEBAR_WIDTH + "px!][grow]", "[grow]"));
    pnlContent.add(createSidebarScrollPane(createCommanderSidebar(commanders, selectedCommander, selectionCallback)),
        "growy");
    pnlContent.add(createCommanderDetails(selectedCommander, progress), "grow, push");
    refreshContent();
  }

  public void showPlanebounds(List<RoguePlanebound> planebounds, RoguePlanebound selectedPlanebound,
                              RogueMetaProgress progress, Consumer<RoguePlanebound> selectionCallback) {
    setActiveTab(CodexTab.PLANEBOUNDS);
    SkinnedPanel pnlContent = getContentPanel(CodexTab.PLANEBOUNDS);
    int sidebarScrollValue = getSidebarScrollValue(pnlContent);
    pnlContent.removeAll();
    pnlContent.setLayout(new MigLayout("insets 0, gap 10", "[" + SIDEBAR_WIDTH + "px!][grow]", "[grow]"));
    FScrollPane sidebarScroller = createSidebarScrollPane(createPlaneboundSidebar(planebounds, selectedPlanebound,
        progress, selectionCallback));
    pnlContent.add(sidebarScroller, "growy");
    pnlContent.add(createPlaneboundDetails(selectedPlanebound, progress), "grow, push");
    refreshContent();
    restoreSidebarScrollValue(sidebarScroller, sidebarScrollValue);
  }

  public void showTraits(Map<CodexHelper.TraitCategory, List<RogueEffect>> traitsByCategory,
                         RogueMetaProgress progress) {
    setActiveTab(CodexTab.TRAITS);
    SkinnedPanel pnlContent = getContentPanel(CodexTab.TRAITS);
    pnlContent.removeAll();
    pnlContent.setLayout(new MigLayout("insets 0, gap 0, wrap 1", "[grow]", "[][grow]"));

    JPanel sections = new CodexSectionsPanel(new MigLayout("insets 12, gap 0, wrap 1", "[grow]"));
    sections.setOpaque(false);

    List<CodexCardGrid.Entry> chestEntries = createTraitEntries(
        traitsByCategory.get(CodexHelper.TraitCategory.CHEST), progress);
    List<CodexCardGrid.Entry> npcEntries = createTraitEntries(
        traitsByCategory.get(CodexHelper.TraitCategory.NPC), progress);
    List<CodexCardGrid.Entry> eventEntries = createTraitEntries(
        traitsByCategory.get(CodexHelper.TraitCategory.EVENT), progress);
    List<CodexCardGrid.Entry> woundEntries = createTraitEntries(
        traitsByCategory.get(CodexHelper.TraitCategory.WOUND), progress);

    List<CodexCardGrid.Entry> traitEntries = new ArrayList<>();
    traitEntries.addAll(chestEntries);
    traitEntries.addAll(npcEntries);
    traitEntries.addAll(eventEntries);
    traitEntries.addAll(woundEntries);

    addTraitSection(sections, localizer.getMessage("lblRogueChest"), chestEntries);
    addTraitSection(sections, localizer.getMessage("lblRogueNpc"), npcEntries);
    addTraitSection(sections, localizer.getMessage("lblRogueEvent"), eventEntries);
    addTraitSection(sections, localizer.getMessage("lblRogueWound"), woundEntries);

    pnlContent.add(createTraitStatsHeader(traitEntries), "growx, gapbottom 16");
    pnlContent.add(createPanelScrollPane(sections), "grow, push");
    refreshContent();
  }

  void setTabSelectionCallback(Consumer<CodexTab> tabSelectionCallback) {
    this.tabSelectionCallback = tabSelectionCallback;
  }

  public CardUtil getZoomUtil() {
    if (zoomUtil == null) {
      Window window = SwingUtilities.getWindowAncestor(VHomeUI.SINGLETON_INSTANCE.getPnlDisplay());
      if (window == null) {
        return null;
      }
      zoomUtil = new CardUtil(window);
      zoomUtil.setupZoomOverlay();
    }
    return zoomUtil;
  }

  @Override
  public EMenuGroup getGroupEnum() {
    return EMenuGroup.ROGUE;
  }

  @Override
  public String getMenuTitle() {
    return Localizer.getInstance().getMessage("lblRogueCodex");
  }

  @Override
  public EDocID getItemEnum() {
    return EDocID.HOME_ROGUESTATS;
  }

  @Override
  public void populate() {
    VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();
    VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0, wrap"));

    VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(createHeaderPanel(), "w 98%!, h 34px!, gap 1% 0 15px 8px");
    VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(codexTabs, "w 98%!, h 100%-57px!, gap 1% 0 0 0");

    VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
    VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
  }

  private JPanel createHeaderPanel() {
    JPanel panel = new SkinnedPanel(new MigLayout("insets 0, gap 8", "[][grow]", "[grow]"));
    panel.setOpaque(false);
    panel.add(btnBack, "w 120px!, h 30px!");
    panel.add(lblTitle, "grow, push, h 30px!");
    return panel;
  }

  private JPanel createStatsPanel() {
    JPanel panel = new SkinnedPanel(new MigLayout("insets 20, gap 5, wrap 1", "[grow]", ""));
    panel.setOpaque(false);

    addSectionLabel(panel, localizer.getMessage("lblRogueRunStatistics"));
    panel.add(lblTotalPlaytime);
    panel.add(lblRunsStarted);
    panel.add(lblRunsCompleted);
    panel.add(lblRunsWon);
    panel.add(lblRunsLost, "gapbottom 20");

    addSectionLabel(panel, localizer.getMessage("lblRogueMatchStatistics"));
    panel.add(lblMatchesWon);
    panel.add(lblMatchesLost, "gapbottom 20");

    addSectionLabel(panel, localizer.getMessage("lblRogueMilestones"));
    panel.add(lblMaxLife);
    panel.add(lblMaxGold);
    panel.add(lblMaxCreatureTypes);
    panel.add(lblMaxArtifacts);
    panel.add(lblMaxSharedCreatureType);
    panel.add(lblMaxLegendaryPermanents);
    panel.add(lblMaxNonbasicLands, "gapbottom 20");

    JPanel buttonPanel = new SkinnedPanel(new MigLayout("insets 0, gap 10"));
    buttonPanel.setOpaque(false);
    buttonPanel.add(btnStatsBack, "w 180px!, h 40px!");
    buttonPanel.add(btnReset, "w 180px!, h 40px!");
    buttonPanel.add(btnResetTutorials, "w 180px!, h 40px!");
    if (ForgePreferences.DEV_MODE) {
      buttonPanel.add(btnDevUnlockCodex, "w 180px!, h 40px!");
    }
    panel.add(buttonPanel, "ax center");

    return panel;
  }

  private JPanel createCommanderSidebar(List<RogueDeck> commanders, RogueDeck selectedCommander,
                                        Consumer<RogueDeck> selectionCallback) {
    JPanel panel = createAvatarSidebarPanel();
    for (RogueDeck commander : commanders) {
      boolean unlocked = commander.isUnlocked();
      boolean selected = selectedCommander != null
          && selectedCommander.getCommanderCardName().equals(commander.getCommanderCardName());
      FLabel avatar = createAvatarLabel(unlocked ? commander.getAvatarIndex() : -1,
          unlocked ? commander.getName() : localizer.getMessage("lblRogueLockedCommander"), selected, unlocked);
      if (unlocked) {
        avatar.setCommand((Runnable) () -> selectionCallback.accept(commander));
      }
      panel.add(avatar, "w " + AVATAR_SIZE + "px!, h " + AVATAR_SIZE + "px!");
    }
    return panel;
  }

  private JPanel createPlaneboundSidebar(List<RoguePlanebound> planebounds, RoguePlanebound selectedPlanebound,
                                         RogueMetaProgress progress,
                                         Consumer<RoguePlanebound> selectionCallback) {
    JPanel panel = createAvatarSidebarPanel();
    for (RoguePlaneboundType type : RoguePlaneboundType.values()) {
      panel.add(new FLabel.Builder().text(type.name()).fontSize(12).fontStyle(Font.BOLD).build(),
          "newline, span 2, wrap, gapbottom 3");
      for (RoguePlanebound planebound : planebounds.stream().filter(p -> p.type() == type).toList()) {
        boolean encountered = progress.hasEncounteredPlanebound(planebound);
        boolean selected = selectedPlanebound != null
            && selectedPlanebound.deckPath().equals(planebound.deckPath());
        FLabel avatar = createAvatarLabel(encountered ? planebound.avatarIndex() : -1,
            encountered ? planebound.planeboundName() : localizer.getMessage("lblRogueUnknownPlanebound"), selected, encountered);
        if (encountered) {
          avatar.setCommand((Runnable) () -> selectionCallback.accept(planebound));
        }
        panel.add(avatar, "w " + AVATAR_SIZE + "px!, h " + AVATAR_SIZE + "px!");
      }
    }
    return panel;
  }

  private JPanel createAvatarSidebarPanel() {
    JPanel panel = new SkinnedPanel(new MigLayout("insets 8, gap 8, wrap 2", "[][]", ""));
    panel.setOpaque(false);
    return panel;
  }

  private FLabel createAvatarLabel(int avatarIndex, String tooltip, boolean selected, boolean enabled) {
    SkinImage image = avatarIndex >= 0 ? FSkin.getAvatars().get(RogueUIHelper.getAvailableAvatarIndex(avatarIndex)) : getQuestionAvatarImage();
    if (image == null) {
      image = FSkin.getIcon(FSkinProp.ICO_QUESTION);
    }
    FLabel label = new FLabel.Builder()
        .icon(image)
        .iconScaleFactor(0.95)
        .iconAlignX(SwingConstants.CENTER)
        .iconInBackground(true)
        .hoverable(enabled)
        .selectable(enabled)
        .selected(selected)
        .unhoveredAlpha(enabled ? 0.8f : 0.35f)
        .build();
    label.setToolTipText(tooltip);
    label.setEnabled(enabled);
    label.setPreferredSize(new Dimension(AVATAR_SIZE, AVATAR_SIZE));
    if (selected) {
      label.setBorder(new FSkin.LineSkinBorder(FSkin.getColor(FSkin.Colors.CLR_BORDERS).alphaColor(255), 3));
    }
    return label;
  }

  private SkinImage getQuestionAvatarImage() {
    return FSkin.getAvatars().get(125);
  }

  private FScrollPane createSidebarScrollPane(JPanel sidebar) {
    FScrollPane scroller = new FScrollPane(sidebar, false,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scroller.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 1));
    return scroller;
  }

  private int getSidebarScrollValue(JPanel panel) {
    for (Component component : panel.getComponents()) {
      if (component instanceof FScrollPane scroller) {
        return scroller.getVerticalScrollBar().getValue();
      }
    }
    return 0;
  }

  private void restoreSidebarScrollValue(FScrollPane scroller, int scrollValue) {
    if (scrollValue <= 0) {
      return;
    }
    SwingUtilities.invokeLater(() -> {
      int maxValue = scroller.getVerticalScrollBar().getMaximum()
          - scroller.getVerticalScrollBar().getVisibleAmount();
      scroller.getVerticalScrollBar().setValue(Math.min(scrollValue, Math.max(0, maxValue)));
    });
  }

  private JPanel createCommanderDetails(RogueDeck selectedCommander, RogueMetaProgress progress) {
    JPanel panel = new SkinnedPanel(new MigLayout("insets 0, gap 0, wrap 1", "[grow]", "[][grow]"));
    panel.setOpaque(false);
    if (selectedCommander == null) {
      panel.add(new FLabel.Builder().text(Localizer.getInstance().getMessage("lblRogueNoUnlockedCommanders")).fontSize(14).build(), "gap 12 0 12 0");
      return panel;
    }

    String commanderName = selectedCommander.getCommanderCardName();
    List<CodexCardGrid.Entry> nonMythicEntries = new ArrayList<>();
    List<CodexCardGrid.Entry> mythicEntries = new ArrayList<>();
    for (PaperCard card : selectedCommander.getRewardPoolCards()) {
      List<CodexCardGrid.Entry> entries = PaperCardPredicates.IS_MYTHIC_RARE.test(card)
          ? mythicEntries : nonMythicEntries;
      entries.add(createCommanderRewardEntry(commanderName, card, progress));
    }
    List<CodexCardGrid.Entry> startDeckEntries = new ArrayList<>();
    Deck startDeck = selectedCommander.getStartDeck();
    if (startDeck != null) {
      Set<String> displayedBasicLandNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
      for (PaperCard card : startDeck.getMain().toFlatList()) {
        if (card == null || card.getRules() == null) {
          continue;
        }
        if (card.getRules().getType().isBasicLand() && !displayedBasicLandNames.add(card.getName())) {
          continue;
        }
        startDeckEntries.add(new CodexCardGrid.Entry(card, card.getName(), card.getName(),
            CodexCardGrid.CardState.NORMAL));
      }
    }
    sortCodexCardEntries(nonMythicEntries);
    sortCodexCardEntries(mythicEntries);
    sortCodexCardEntries(startDeckEntries);
    panel.add(createCommanderStatsHeader(selectedCommander, progress, nonMythicEntries, mythicEntries),
        "growx, gapbottom 16");

    JPanel sections = new CodexSectionsPanel(new MigLayout("insets 0, gap 0, wrap 1", "[grow]"));
    sections.setOpaque(false);
    addCardGridSection(sections, localizer.getMessage("lblCommander"), createCommanderCardEntries(selectedCommander));
    addCardGridSection(sections, localizer.getMessage("lblRogueRewardPoolNonMythics"), nonMythicEntries);
    addCardGridSection(sections, localizer.getMessage("lblRogueRewardPoolMythics"), mythicEntries);
    addCardGridSection(sections, localizer.getMessage("lblRogueStartDeck"), startDeckEntries);
    panel.add(createPanelScrollPane(sections), "grow, push");
    return panel;
  }

  private CodexCardGrid.Entry createCommanderRewardEntry(String commanderName, PaperCard card,
                                                         RogueMetaProgress progress) {
    boolean acquired = progress.hasAcquiredCommanderRewardCard(commanderName, card);
    boolean seen = progress.hasSeenCommanderRewardCard(commanderName, card);
    CodexCardGrid.CardState state = acquired ? CodexCardGrid.CardState.NORMAL
        : seen ? CodexCardGrid.CardState.GREYED : CodexCardGrid.CardState.HIDDEN;
    return new CodexCardGrid.Entry(card, card.getName(),
        state == CodexCardGrid.CardState.HIDDEN ? localizer.getMessage("lblRogueUnknownCard") : card.getName(), state);
  }

  private JPanel createCommanderStatsHeader(RogueDeck selectedCommander, RogueMetaProgress progress,
                                            List<CodexCardGrid.Entry> nonMythicEntries,
                                            List<CodexCardGrid.Entry> mythicEntries) {
    JPanel panel = new SkinnedPanel(new MigLayout("insets 8, gap 12", "[][][][][][][]", ""));
    panel.setOpaque(false);
    String commanderName = selectedCommander.getCommanderCardName();
    List<CodexCardGrid.Entry> rewardEntries = new ArrayList<>(nonMythicEntries);
    rewardEntries.addAll(mythicEntries);
    panel.add(new FLabel.Builder().text(selectedCommander.getName()).fontSize(16).fontStyle(Font.BOLD).build());
    panel.add(new FLabel.Builder().text(localizer.getMessage("lblRogueCommanderRuns", progress.getRunsStartedWithCommander(commanderName))).fontSize(13).build());
    panel.add(new FLabel.Builder().text(localizer.getMessage("lblRogueCommanderWins", progress.getRunsWonWithCommander(commanderName))).fontSize(13).build());
    panel.add(new FLabel.Builder().text(localizer.getMessage("lblRogueMaxDescension", progress.getMaxDescensionWon(commanderName))).fontSize(13).build());
    panel.add(new FLabel.Builder().text(localizer.getMessage("lblRogueMaxEndless", progress.getMaxEndlessCompleted(commanderName))).fontSize(13).build());
    panel.add(new FLabel.Builder().text(localizer.getMessage("lblRogueSeenCards", countSeenCards(rewardEntries), rewardEntries.size())).fontSize(13).build());
    panel.add(new FLabel.Builder().text(localizer.getMessage("lblRogueAcquiredCards", countAcquiredCards(rewardEntries), rewardEntries.size())).fontSize(13).build());
    return panel;
  }

  private JPanel createPlaneboundDetails(RoguePlanebound selectedPlanebound, RogueMetaProgress progress) {
    JPanel panel = new SkinnedPanel(new MigLayout("insets 0, gap 0, wrap 1", "[grow]", "[][grow]"));
    panel.setOpaque(false);
    if (selectedPlanebound == null) {
      panel.add(new FLabel.Builder().text(Localizer.getInstance().getMessage("lblRogueEncounterAPlaneboundToUnlockItsDeck")).fontSize(14).build(),
          "gap 12 0 12 0");
      return panel;
    }

    List<CodexCardGrid.Entry> entries = new ArrayList<>();
    Deck deck = RogueConfig.loadPlaneboundDeck(selectedPlanebound);
    List<CodexCardGrid.Entry> commanderEntries = new ArrayList<>();
    if (deck != null) {
      for (PaperCard commander : deck.getCommanders()) {
        commanderEntries.add(createPlaneboundDeckEntry(selectedPlanebound, commander, progress));
      }
      Set<String> displayedBasicLandNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
      for (PaperCard card : deck.getAllCardsInASinglePool(false, false).toFlatList()) {
        if (card == null || card.getRules() == null) {
          continue;
        }
        if (card.getRules().getType().isBasicLand() && !displayedBasicLandNames.add(card.getName())) {
          continue;
        }
        entries.add(createPlaneboundDeckEntry(selectedPlanebound, card, progress));
      }
    }
    sortCodexCardEntries(commanderEntries);
    sortCodexCardEntries(entries);
    panel.add(createPlaneboundStatsHeader(selectedPlanebound, progress, entries),
        "growx, gapbottom 16");

    JPanel sections = new CodexSectionsPanel(new MigLayout("insets 0, gap 0, wrap 1", "[grow]"));
    sections.setOpaque(false);
    sections.add(createPlaneboundTopSections(selectedPlanebound, commanderEntries), "w 100%!, growx, gapbottom 20");
    addCardGridSection(sections, localizer.getMessage("lblDeck"), entries);
    panel.add(createPanelScrollPane(sections), "grow, push");
    return panel;
  }

  private JPanel createPlaneboundStatsHeader(RoguePlanebound selectedPlanebound,
                                             RogueMetaProgress progress,
                                             List<CodexCardGrid.Entry> deckEntries) {
    JPanel panel = new SkinnedPanel(new MigLayout("insets 8, gap 12", "[][][][]", ""));
    panel.setOpaque(false);
    panel.add(new FLabel.Builder()
        .text(selectedPlanebound.planeboundName() + " - " + selectedPlanebound.planeName())
        .fontSize(16)
        .fontStyle(Font.BOLD)
        .build());
    panel.add(new FLabel.Builder().text(localizer.getMessage("lblRoguePlaneboundSlain", progress.getPlaneboundWins(selectedPlanebound))
        + " times").fontSize(13).build());
    panel.add(new FLabel.Builder().text(localizer.getMessage("lblRoguePlaneboundDefeats", progress.getPlaneboundLosses(selectedPlanebound))
        + " times").fontSize(13).build());
    panel.add(new FLabel.Builder().text(localizer.getMessage("lblRogueSeenCards", countSeenCards(deckEntries), deckEntries.size())).fontSize(13).build());
    return panel;
  }

  private JPanel createPlaneboundTopSections(RoguePlanebound selectedPlanebound,
                                             List<CodexCardGrid.Entry> commanderEntries) {
    JPanel commanderSection = createCardGridSection(localizer.getMessage("lblCommander"), commanderEntries);
    PaperCard planeCard = getPlaneCard(selectedPlanebound);
    if (planeCard == null) {
      return commanderSection;
    }

    JPanel planeSection = createPlaneSection(planeCard);
    return new ResponsiveTopSectionsPanel(commanderSection, planeSection);
  }

  private JPanel createCardGridSection(String title, List<CodexCardGrid.Entry> entries) {
    CodexCardSectionPanel panel = new CodexCardSectionPanel();
    panel.setOpaque(false);
    addSectionLabel(panel, title);
    panel.addCardGrid(new CodexCardGrid(entries, this::getZoomUtil));
    return panel;
  }

  private JPanel createPlaneSection(PaperCard planeCard) {
    CodexPlaneSectionPanel panel = new CodexPlaneSectionPanel();
    panel.setOpaque(false);
    addSectionLabel(panel, localizer.getMessage("lblPlane"));
    panel.addPlanePanel(new CodexPlanePanel(planeCard, this::getZoomUtil));
    return panel;
  }

  private PaperCard getPlaneCard(RoguePlanebound planebound) {
    if (planebound == null || planebound.planeName() == null) {
      return null;
    }
    for (PaperCard card : RogueConfig.getAllPlanes().toFlatList()) {
      if (planebound.planeName().equalsIgnoreCase(card.getName())) {
        return card;
      }
    }
    return null;
  }

  private List<CodexCardGrid.Entry> createCommanderCardEntries(RogueDeck selectedCommander) {
    Deck startDeck = selectedCommander.getStartDeck();
    if (startDeck == null) {
      return List.of();
    }
    List<CodexCardGrid.Entry> entries = new ArrayList<>();
    for (PaperCard commander : startDeck.getCommanders()) {
      entries.add(new CodexCardGrid.Entry(commander, commander.getName(), commander.getName(),
          CodexCardGrid.CardState.NORMAL));
    }
    return entries;
  }

  private CodexCardGrid.Entry createPlaneboundDeckEntry(RoguePlanebound selectedPlanebound, PaperCard card,
                                                        RogueMetaProgress progress) {
    CodexCardGrid.CardState state = progress.hasSeenPlaneboundCard(selectedPlanebound, card)
        ? CodexCardGrid.CardState.NORMAL : CodexCardGrid.CardState.HIDDEN;
    return new CodexCardGrid.Entry(card, card.getName(),
        state == CodexCardGrid.CardState.HIDDEN ? localizer.getMessage("lblRogueUnknownCard") : card.getName(), state);
  }

  private JPanel createTraitStatsHeader(List<CodexCardGrid.Entry> traitEntries) {
    JPanel panel = new SkinnedPanel(new MigLayout("insets 8, gap 12", "[][]", ""));
    panel.setOpaque(false);
    panel.add(new FLabel.Builder().text(localizer.getMessage("lblRogueSeenTraits", countSeenCards(traitEntries), traitEntries.size())).fontSize(13).build());
    panel.add(new FLabel.Builder().text(localizer.getMessage("lblRogueAcquiredTraits", countAcquiredCards(traitEntries), traitEntries.size())).fontSize(13).build());
    return panel;
  }

  private List<CodexCardGrid.Entry> createTraitEntries(List<RogueEffect> effects, RogueMetaProgress progress) {
    if (effects == null) {
      effects = List.of();
    }
    List<CodexCardGrid.Entry> entries = new ArrayList<>();
    for (RogueEffect effect : effects) {
      PaperCard card = effect.getEffectCard();
      if (card == null) {
        continue;
      }
      boolean acquired = progress.hasAcquiredTrait(effect);
      boolean seen = progress.hasSeenTrait(effect);
      CodexCardGrid.CardState state = acquired ? CodexCardGrid.CardState.NORMAL
          : seen ? CodexCardGrid.CardState.GREYED : CodexCardGrid.CardState.HIDDEN;
      String label = getTraitLabel(effect, card);
      entries.add(new CodexCardGrid.Entry(card, label,
          state == CodexCardGrid.CardState.HIDDEN ? localizer.getMessage("lblRogueUnknownTrait") : label, state));
    }
    return entries;
  }

  private void addTraitSection(JPanel sections, String title, List<CodexCardGrid.Entry> entries) {
    addCardGridSection(sections, title, entries);
  }

  private String getTraitLabel(RogueEffect effect, PaperCard card) {
    if (card != null) {
      return card.getName();
    }
    String displayName = effect.getUIDisplayName();
    return displayName == null || displayName.isBlank() ? effect.getDisplayName() : displayName;
  }

  private void addCardGridSection(JPanel sections, String title, List<CodexCardGrid.Entry> entries) {
    sections.add(createCardGridSection(title, entries), "w 100%!, growx, gapbottom 20");
  }

  private int countSeenCards(List<CodexCardGrid.Entry> entries) {
    return countCards(entries, entry -> entry.state() != CodexCardGrid.CardState.HIDDEN);
  }

  private int countAcquiredCards(List<CodexCardGrid.Entry> entries) {
    return countCards(entries, entry -> entry.state() == CodexCardGrid.CardState.NORMAL);
  }

  private int countCards(List<CodexCardGrid.Entry> entries, Predicate<CodexCardGrid.Entry> filter) {
    int count = 0;
    for (CodexCardGrid.Entry entry : entries) {
      if (filter.test(entry)) {
        count++;
      }
    }
    return count;
  }

  private void sortCodexCardEntries(List<CodexCardGrid.Entry> entries) {
    entries.sort(Comparator
        .comparingInt((CodexCardGrid.Entry entry) -> getCodexCardTypeOrder(entry.card()))
        .thenComparing(entry -> entry.card() == null ? entry.label() : entry.card().getName(),
            String.CASE_INSENSITIVE_ORDER));
  }

  private int getCodexCardTypeOrder(PaperCard card) {
    if (card == null) {
      return 8;
    }
    int groupIndex = GroupDef.CARD_TYPE.getItemGroupIndex(card);
    return groupIndex < 0 ? 8 : groupIndex;
  }

  private FScrollPane createPanelScrollPane(JPanel panel) {
    return new FScrollPane(panel, false,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
  }

  private void addSectionLabel(JPanel panel, String text) {
    panel.add(new FLabel.Builder()
        .text(text)
        .fontSize(16)
        .fontStyle(Font.BOLD)
        .build(), "gapbottom 8");
  }

  private void setActiveTab(CodexTab activeTab) {
    int selectedIndex = activeTab.ordinal();
    if (codexTabs.getSelectedIndex() == selectedIndex) {
      return;
    }
    updatingSelectedTab = true;
    try {
      codexTabs.setSelectedIndex(selectedIndex);
    } finally {
      updatingSelectedTab = false;
    }
  }

  private void refreshContent() {
    codexTabs.revalidate();
    codexTabs.repaint();
    VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
    VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
  }

  private SkinnedPanel getContentPanel(CodexTab codexTab) {
    return tabPanels.get(codexTab);
  }

  private void notifyTabSelection() {
    if (updatingSelectedTab || tabSelectionCallback == null || codexTabs.getSelectedIndex() < 0) {
      return;
    }
    tabSelectionCallback.accept(CodexTab.values()[codexTabs.getSelectedIndex()]);
  }

  private static class CodexSectionsPanel extends SkinnedPanel implements Scrollable {
    private CodexSectionsPanel(MigLayout layout) {
      super(layout);
      setOpaque(false);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
      return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
      return 32;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
      return Math.max(32, visibleRect.height - 15);
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

  private abstract static class CodexContentSectionPanel<T extends Component> extends SkinnedPanel {
    private static final int LABEL_GAP = 8;

    private Component label;
    private T content;

    private CodexContentSectionPanel() {
      super(null);
      setOpaque(false);
    }

    protected void addContent(T content) {
      this.content = content;
      add(content);
    }

    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
      super.addImpl(comp, constraints, index);
      if (label == null) {
        label = comp;
      }
    }

    @Override
    public void doLayout() {
      int width = getWidth();
      int y = 0;
      if (label != null) {
        Dimension labelSize = label.getPreferredSize();
        label.setBounds(0, y, labelSize.width, labelSize.height);
        y += labelSize.height + LABEL_GAP;
      }
      if (content != null) {
        content.setBounds(0, y, width, getContentPreferredHeightForWidth(content, width));
      }
    }

    @Override
    public Dimension getPreferredSize() {
      int width = getWidth();
      if (width <= 0 && getParent() != null) {
        width = getParent().getWidth();
      }
      if (width <= 0) {
        width = getDefaultWidth();
      }
      int height = 0;
      if (label != null) {
        height += label.getPreferredSize().height + LABEL_GAP;
      }
      if (content != null) {
        height += getContentPreferredHeightForWidth(content, width);
      }
      return new Dimension(width, height);
    }

    protected abstract int getContentPreferredHeightForWidth(T content, int width);

    protected abstract int getDefaultWidth();
  }

  private static class CodexCardSectionPanel extends CodexContentSectionPanel<CodexCardGrid> {
    private void addCardGrid(CodexCardGrid cardGrid) {
      addContent(cardGrid);
    }

    @Override
    protected int getContentPreferredHeightForWidth(CodexCardGrid cardGrid, int width) {
      return cardGrid.getPreferredHeightForWidth(width);
    }

    @Override
    protected int getDefaultWidth() {
      return CodexCardGrid.getDefaultWidth();
    }
  }

  private static class ResponsiveTopSectionsPanel extends SkinnedPanel {
    private final Component leftSection;
    private final Component rightSection;

    private ResponsiveTopSectionsPanel(Component leftSection, Component rightSection) {
      super(null);
      this.leftSection = leftSection;
      this.rightSection = rightSection;
      setOpaque(false);
      add(leftSection);
      add(rightSection);
    }

    @Override
    public void doLayout() {
      int width = getWidth();
      if (isStacked(width)) {
        int leftHeight = getSectionHeight(leftSection, width);
        int rightHeight = getSectionHeight(rightSection, width);
        leftSection.setBounds(0, 0, width, leftHeight);
        rightSection.setBounds(0, leftHeight + TOP_SECTION_GAP, width, rightHeight);
      } else {
        int sectionWidth = (width - TOP_SECTION_GAP) / 2;
        int leftHeight = getSectionHeight(leftSection, sectionWidth);
        int rightHeight = getSectionHeight(rightSection, sectionWidth);
        leftSection.setBounds(0, 0, sectionWidth, leftHeight);
        rightSection.setBounds(sectionWidth + TOP_SECTION_GAP, 0, sectionWidth, rightHeight);
      }
    }

    @Override
    public Dimension getPreferredSize() {
      int width = getWidth();
      if (width <= 0 && getParent() != null) {
        width = getParent().getWidth();
      }
      if (width <= 0) {
        width = TOP_SECTION_WRAP_WIDTH;
      }

      if (isStacked(width)) {
        return new Dimension(width, getSectionHeight(leftSection, width) + TOP_SECTION_GAP
            + getSectionHeight(rightSection, width));
      }

      int sectionWidth = (width - TOP_SECTION_GAP) / 2;
      return new Dimension(width, Math.max(getSectionHeight(leftSection, sectionWidth),
          getSectionHeight(rightSection, sectionWidth)));
    }

    private boolean isStacked(int width) {
      return width < TOP_SECTION_WRAP_WIDTH;
    }

    private int getSectionHeight(Component section, int width) {
      section.setSize(width, Short.MAX_VALUE);
      return section.getPreferredSize().height;
    }
  }

  private static class CodexPlaneSectionPanel extends CodexContentSectionPanel<CodexPlanePanel> {
    private void addPlanePanel(CodexPlanePanel planePanel) {
      addContent(planePanel);
    }

    @Override
    protected int getContentPreferredHeightForWidth(CodexPlanePanel planePanel, int width) {
      return planePanel.getPreferredHeightForWidth(width);
    }

    @Override
    protected int getDefaultWidth() {
      return CodexPlanePanel.getDefaultWidth();
    }
  }

  private static class CodexPlanePanel extends SkinnedPanel implements ImageFetcher.Callback {
    private static final int BASE_WIDTH = 480;
    private static final float ASPECT_RATIO = 0.72f;
    private static final int CARD_GRID_BASE_WIDTH = 240;
    private static final int CARD_GRID_GAP = 15;
    private static final int CARD_GRID_PADDING = 15;

    private final PaperCard planeCard;
    private final Supplier<CardUtil> zoomUtilSupplier;
    private final PlaneImagePanel imagePanel = new PlaneImagePanel();
    private BufferedImage rotatedPlaneImage;

    private CodexPlanePanel(PaperCard planeCard, Supplier<CardUtil> zoomUtilSupplier) {
      super(null);
      this.planeCard = planeCard;
      this.zoomUtilSupplier = zoomUtilSupplier;
      setOpaque(false);
      add(imagePanel);
      updatePlaneImage();
    }

    @Override
    public void doLayout() {
      imagePanel.setBounds(getImageBounds());
    }

    private void showZoom() {
      CardUtil zoomUtil = zoomUtilSupplier == null ? null : zoomUtilSupplier.get();
      if (zoomUtil != null) {
        zoomUtil.showZoom(planeCard);
      }
    }

    private class PlaneImagePanel extends SkinnedPanel {
      private boolean hovered;

      private PlaneImagePanel() {
        super(null);
        setOpaque(false);
        addMouseListener(new MouseAdapter() {
          @Override
          public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON2) {
              showZoom();
            }
          }

          @Override
          public void mouseReleased(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON2) {
              CardUtil zoomUtil = zoomUtilSupplier == null ? null : zoomUtilSupplier.get();
              if (zoomUtil != null) {
                zoomUtil.closeZoom();
              }
            }
          }

          @Override
          public void mouseEntered(MouseEvent e) {
            hovered = true;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            repaint();
          }

          @Override
          public void mouseExited(MouseEvent e) {
            hovered = false;
            setCursor(Cursor.getDefaultCursor());
            repaint();
          }
        });
      }

      @Override
      public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g.create();
        if (rotatedPlaneImage != null) {
          g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
              RenderingHints.VALUE_INTERPOLATION_BILINEAR);
          g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
          g2d.drawImage(rotatedPlaneImage, 0, 0, getWidth(), getHeight(), null);
        }
        if (hovered) {
          g2d.setColor(new Color(255, 215, 0));
          g2d.setStroke(new BasicStroke(4));
          g2d.drawRect(3, 3, getWidth() - 6, getHeight() - 6);
        }
        g2d.dispose();
      }
    }

    @Override
    public Dimension getPreferredSize() {
      int width = getWidth();
      if (width <= 0 && getParent() != null) {
        width = getParent().getWidth();
      }
      if (width <= 0) {
        width = getDefaultWidth();
      }
      return new Dimension(width, getPreferredHeightForWidth(width));
    }

    private int getPreferredHeightForWidth(int width) {
      int planeWidth = getPlaneWidth(width, getStartX(width));
      return Math.round(planeWidth * ASPECT_RATIO);
    }

    private static int getDefaultWidth() {
      return BASE_WIDTH + CARD_GRID_PADDING * 2;
    }

    @Override
    public void onImageFetched() {
      updatePlaneImage();
      revalidate();
      repaint();
      imagePanel.repaint();
    }

    private Rectangle getImageBounds() {
      int startX = getStartX(getWidth());
      int planeWidth = getPlaneWidth(getWidth(), startX);
      int planeHeight = Math.round(planeWidth * ASPECT_RATIO);
      return new Rectangle(startX, 0, planeWidth, planeHeight);
    }

    private int getPlaneWidth(int width, int startX) {
      return Math.min(BASE_WIDTH, Math.max(1, width - startX - CARD_GRID_PADDING));
    }

    private int getStartX(int width) {
      int cardWidth = getCardGridCardWidth(width);
      int columns = getCardGridColumnCount(width);
      int rowWidth = columns * cardWidth + (columns - 1) * CARD_GRID_GAP;
      return Math.max(CARD_GRID_PADDING, (width - rowWidth) / 2);
    }

    private int getCardGridCardWidth(int width) {
      return Math.min(CARD_GRID_BASE_WIDTH, getAvailableCardGridWidth(width));
    }

    private int getCardGridColumnCount(int width) {
      int availableWidth = getAvailableCardGridWidth(width);
      if (availableWidth < CARD_GRID_BASE_WIDTH) {
        return 1;
      }
      return Math.max(1, (availableWidth + CARD_GRID_GAP)
          / (CARD_GRID_BASE_WIDTH + CARD_GRID_GAP));
    }

    private int getAvailableCardGridWidth(int width) {
      return Math.max(1, width - CARD_GRID_PADDING * 2);
    }

    private void updatePlaneImage() {
      Pair<BufferedImage, Boolean> imageInfo = ImageCache.getCardOriginalImageInfo(
          planeCard.getImageKey(false), true);
      BufferedImage image = imageInfo.getLeft();
      if (ImageCache.isDefaultImage(image) || imageInfo.getRight()) {
        GuiBase.getInterface().getImageFetcher().fetchImage(planeCard.getImageKey(false), this);
      }
      if (image != null) {
        rotatedPlaneImage = rotateImage90Clockwise(image);
      }
    }

    private BufferedImage rotateImage90Clockwise(BufferedImage image) {
      int imageType = image.getType() == BufferedImage.TYPE_CUSTOM
          ? BufferedImage.TYPE_INT_ARGB : image.getType();
      BufferedImage rotated = new BufferedImage(image.getHeight(), image.getWidth(), imageType);
      Graphics2D g2d = rotated.createGraphics();
      g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      AffineTransform transform = new AffineTransform();
      transform.translate(image.getHeight() / 2.0, image.getWidth() / 2.0);
      transform.rotate(Math.toRadians(90));
      transform.translate(-image.getWidth() / 2.0, -image.getHeight() / 2.0);
      g2d.drawImage(image, transform, null);
      g2d.dispose();
      return rotated;
    }
  }

  public JButton getBtnBack() {
    return btnBack;
  }

  public JButton getBtnStatsBack() {
    return btnStatsBack;
  }

  public JButton getBtnReset() {
    return btnReset;
  }

  public JButton getBtnResetTutorials() {
    return btnResetTutorials;
  }

  public JButton getBtnDevUnlockCodex() {
    return btnDevUnlockCodex;
  }

  @Override
  public EDocID getDocumentID() {
    return EDocID.HOME_ROGUESTATS;
  }

  @Override
  public DragTab getTabLabel() {
    return tab;
  }

  @Override
  public CSubmenuRogueCodex getLayoutControl() {
    return CSubmenuRogueCodex.SINGLETON_INSTANCE;
  }

  @Override
  @SuppressWarnings({"WeakerAccess", "java:S2039"})
  public void setParentCell(DragCell cell0) {
    this.parentCell = cell0;
  }

  @Override
  public DragCell getParentCell() {
    return parentCell;
  }
}
