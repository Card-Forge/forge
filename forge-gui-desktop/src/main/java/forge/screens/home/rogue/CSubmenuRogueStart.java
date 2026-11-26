package forge.screens.home.rogue;

import forge.util.Localizer;

import forge.ImageCache;
import forge.ImageKeys;
import forge.gamemodes.rogue.*;
import forge.gamemodes.rogue.effect.*;
import forge.gamemodes.rogue.flow.RogueStart;
import forge.gamemodes.rogue.npc.NPC;
import forge.gui.GuiBase;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.localinstance.properties.ForgePreferences;
import forge.screens.home.CHomeUI;
import forge.toolbox.FOptionPane;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.SwingUtilities;

/**
 * Controls the Rogue Commander start screen. Handles commander selection via card carousel and new run
 * creation.
 */
public enum CSubmenuRogueStart implements ICDoc {
  SINGLETON_INSTANCE;
  private static final String HIDDEN_CARD_KEY = ImageKeys.getTokenKey(ImageKeys.HIDDEN_CARD);

  private final VSubmenuRogueStart view = VSubmenuRogueStart.SINGLETON_INSTANCE;
  private RogueDeck selectedDeck;
  private int selectedDescensionLevel = 0;
  private boolean updatingDescensionUi;
  private boolean updatingCustomModUi;

  @Override
  public void register() {
    // The start screen attaches its listeners in initialize().
  }

  @Override
  public void initialize() {
    view.getBtnBeginRun().addActionListener(e -> beginNewRun());
    view.getBtnStats().addActionListener(e -> openStats());
    view.getBtnAether().addActionListener(e -> openAether());
    view.getBtnHistory().addActionListener(e -> openHistory());
    view.getChkCustomRuns().addItemListener(e -> onCustomRunsToggled());
    for (CustomRunModification modification : CustomRunModification.values()) {
      view.getChkCustomModification(modification).addItemListener(
          e -> onCustomModificationToggled(modification));
    }
    view.getChkDescension().addItemListener(e -> onDescensionToggled());
    view.getBtnDescensionDown().addActionListener(e -> changeDescensionLevel(-1));
    view.getBtnDescensionUp().addActionListener(e -> changeDescensionLevel(1));
    view.getBtnCommanderPrevious().setCommand(() -> selectAdjacentCommander(-1));
    view.getBtnCommanderNext().setCommand(() -> selectAdjacentCommander(1));
    if (ForgePreferences.DEV_MODE) {
      view.getBtnDevUnlockAll().addActionListener(e -> devToggleUnlockAll());
      view.getBtnDevNPCProgress().addActionListener(e -> devEditNPCProgress());
    }
  }

  private void openStats() {
    CHomeUI.SINGLETON_INSTANCE.itemClick(EDocID.HOME_ROGUESTATS);
  }

  private void openAether() {
    CHomeUI.SINGLETON_INSTANCE.itemClick(EDocID.HOME_ROGUEAETHER);
  }

  private void openHistory() {
    CHomeUI.SINGLETON_INSTANCE.itemClick(EDocID.HOME_ROGUEHISTORY);
  }

  private void devToggleUnlockAll() {
    RogueMetaProgress progress = RogueMetaProgress.getInstance();
    boolean newState = !progress.isDevUnlockAll();
    progress.setDevUnlockAll(newState);
    view.getBtnDevUnlockAll().setText(Localizer.getInstance().getMessage(
        newState ? "lblRogueDevLockCommanders" : "lblRogueDevUnlockAll"));
    loadAvailableCommanders();
  }

  private void devEditNPCProgress() {
    Map<NPC, Integer> updatedLevels = new NPCProgressDialog(RogueMetaProgress.getInstance()).show();
    if (updatedLevels.isEmpty()) {
      return;
    }

    RogueMetaProgress progress = RogueMetaProgress.getInstance();
    for (Map.Entry<NPC, Integer> entry : updatedLevels.entrySet()) {
      progress.setNPCLevel(entry.getKey().id, entry.getValue());
    }

    FOptionPane.showMessageDialog(Localizer.getInstance().getMessage("lblRogueNpcProgressUpdated"), Localizer.getInstance().getMessage("lblRogueDevNpcProgressTitle"));
  }

  @Override
  public void update() {
    if (!RogueUIHelper.progressReadable()) {
      return;
    }
    loadAvailableCommanders();
    SwingUtilities.invokeLater(() -> {
      view.getBtnBeginRun().requestFocusInWindow();
      CSubmenuRogueMap.SINGLETON_INSTANCE.getFlow().betweenRuns();
      showTutorials();
    });
  }

  private void showTutorials() {
    RogueTutorialHelper.showIfNotSeen(
        RogueTutorialTriggers.onStartPage(RogueMetaProgress.getInstance()).toArray(new RogueTutorial[0]));
  }

  private void loadAvailableCommanders() {
    boolean customRunsUnlocked = RogueMetaProgress.getInstance().isCustomRunsUnlocked();
    if (!customRunsUnlocked) {
      view.getChkCustomRuns().setSelected(false);
    }
    view.getChkCustomRuns().setVisible(customRunsUnlocked);
    view.getPnlCustomRunLock().setVisible(!customRunsUnlocked);
    view.setCustomModificationsVisible(view.getChkCustomRuns().isSelected());

    boolean experimental = view.getChkCustomRuns().isSelected()
        && view.getChkCustomModification(CustomRunModification.EXPERIMENT).isSelected();
    String previousCommanderName = selectedDeck != null ? selectedDeck.getCommanderCardName() : null;
    int previousDescensionLevel = selectedDescensionLevel;

    // Clear existing commander panels
    view.getCommanderCarouselPanel().clear();

    for (RogueDeck deck : RogueStart.roster(experimental)) {
      CommanderCardPanel cardPanel = new CommanderCardPanel(deck, view);
      cardPanel.setSelectionCallback(this::onCommanderSelected);
      view.getCommanderCarouselPanel().addCommanderPanel(cardPanel);
    }

    requestHiddenCardImageForLockedPanels();
    restoreCommanderSelection(previousCommanderName, previousDescensionLevel);
    view.getBtnCommanderPrevious().setEnabled(view.getCommanderPanels().size() > 1);
    view.getBtnCommanderNext().setEnabled(view.getCommanderPanels().size() > 1);
    updateStartButtonState();

    // Update button states - locked until first run completed
    boolean hasCompletedRuns = RogueStart.tabsUnlocked(RogueMetaProgress.getInstance());
    view.getBtnAether().setEnabled(hasCompletedRuns);
    view.getBtnAether()
        .setToolTipText(hasCompletedRuns ? null : Localizer.getInstance().getMessage("lblRogueUnlockAetherHint"));
    view.getBtnHistory().setEnabled(hasCompletedRuns);
    view.getBtnHistory()
        .setToolTipText(hasCompletedRuns ? null : Localizer.getInstance().getMessage("lblRogueUnlockHistoryHint"));
    view.getBtnStats().setEnabled(hasCompletedRuns);
    view.getBtnStats()
        .setToolTipText(hasCompletedRuns ? null : Localizer.getInstance().getMessage("lblRogueUnlockCodexHint"));

    // Refresh layout
    view.getCommanderCarouselPanel().revalidate();
    view.getCommanderCarouselPanel().repaint();
  }

  private void restoreCommanderSelection(String previousCommanderName, int previousDescensionLevel) {
    List<CommanderCardPanel> panels = view.getCommanderPanels();
    if (panels.isEmpty()) {
      selectedDeck = null;
      updateDescensionVisibility(null, true);
      return;
    }

    CommanderCardPanel previousCommanderPanel = panels.stream()
        .filter(panel -> !panel.isLocked() && previousCommanderName != null
            && previousCommanderName.equals(panel.getCommander().getCommanderCardName()))
        .findFirst().orElse(null);
    CommanderCardPanel panelToSelect = previousCommanderPanel;
    if (panelToSelect == null) {
      panelToSelect = panels.stream().filter(panel -> !panel.isLocked()).findFirst().orElse(panels.get(0));
    }

    boolean locked = panelToSelect.isLocked();
    view.getCommanderCarouselPanel().centerOn(panelToSelect);
    panelToSelect.setSelected(!locked);
    panelToSelect.setHighlighted(locked);
    selectedDeck = locked ? null : panelToSelect.getCommander();
    selectedDescensionLevel = locked ? 0 : previousDescensionLevel;
    updateCommanderDetails(panelToSelect);
    updateDescensionVisibility(locked ? null : panelToSelect, previousCommanderPanel != null);
  }

  private void requestHiddenCardImageForLockedPanels() {
    boolean hasLockedPanels = view.getCommanderPanels().stream().anyMatch(CommanderCardPanel::isLocked);
    if (!hasLockedPanels) {
      return;
    }

    if (ImageCache.getOriginalImage(HIDDEN_CARD_KEY, false, null) != null) {
      refreshLockedCommanderPanels();
      return;
    }

    GuiBase.getInterface().getImageFetcher().fetchImage(HIDDEN_CARD_KEY, this::refreshLockedCommanderPanels);
  }

  private void refreshLockedCommanderPanels() {
    for (CommanderCardPanel panel : view.getCommanderPanels()) {
      panel.refreshHiddenCardImage();
    }
  }

  private void selectAdjacentCommander(int direction) {
    List<CommanderCardPanel> panels = view.getCommanderPanels();
    if (panels.isEmpty()) {
      return;
    }
    int centeredIndex = panels.indexOf(view.getCommanderCarouselPanel().getCenteredPanel());
    onCommanderSelected(panels.get(Math.floorMod(centeredIndex + direction, panels.size())));
  }

  private void onCommanderSelected(CommanderCardPanel clickedPanel) {
    if (clickedPanel == view.getCommanderCarouselPanel().getCenteredPanel()
        && (clickedPanel.isSelected() || clickedPanel.isHighlighted())) {
      return;
    }
    view.getCommanderCarouselPanel().centerOn(clickedPanel);
    // For locked commanders, just show details but don't select
    if (clickedPanel.isLocked()) {
      // Deselect and unhighlight all panels, then highlight clicked
      for (CommanderCardPanel panel : view.getCommanderPanels()) {
        panel.setSelected(false);
        panel.setHighlighted(panel == clickedPanel);
      }
      selectedDeck = null;
      updateCommanderDetails(clickedPanel);
      updateDescensionVisibility(null, true);
      updateStartButtonState();
      return;
    }

    // Clear highlights and deselect other panels (single-selection mode)
    for (CommanderCardPanel panel : view.getCommanderPanels()) {
      panel.setHighlighted(false);
      if (panel != clickedPanel) {
        panel.setSelected(false);
      }
    }

    clickedPanel.setSelected(true);
    selectedDeck = clickedPanel.getCommander();
    updateCommanderDetails(clickedPanel);
    updateDescensionVisibility(clickedPanel, view.getChkCustomRuns().isSelected());
    updateStartButtonState();
  }

  private void onCustomRunsToggled() {
    boolean custom = view.getChkCustomRuns().isSelected();
    boolean experimental = view.getChkCustomModification(
        CustomRunModification.EXPERIMENT).isSelected();
    if (!custom) {
      updatingCustomModUi = true;
      for (CustomRunModification modification : CustomRunModification.values()) {
        view.getChkCustomModification(modification).setSelected(false);
      }
      for (CustomRunModification modification : CustomRunModification.values()) {
        if (modification.isIncludedWithGeneratedCommander()) {
          view.getChkCustomModification(modification).setEnabled(true);
        }
      }
      updatingCustomModUi = false;
    }
    view.setCustomModificationsVisible(custom);
    if (experimental && !custom) {
      loadAvailableCommanders();
      return;
    }
    CommanderCardPanel selectedPanel = view.getCommanderPanels().stream()
        .filter(panel -> panel.getCommander() == selectedDeck && !panel.isLocked())
        .findFirst().orElse(null);
    updateDescensionVisibility(selectedPanel, true);
    updateStartButtonState();
  }

  private void onCustomModificationToggled(CustomRunModification modification) {
    if (updatingCustomModUi) {
      return;
    }
    boolean wasExperimental = view.getChkCustomModification(
        CustomRunModification.EXPERIMENT).isSelected();
    Set<CustomRunModification> on = RogueStart.toggle(selectedModifications(), modification,
        view.getChkCustomModification(modification).isSelected());
    updatingCustomModUi = true;
    for (CustomRunModification other : CustomRunModification.values()) {
      view.getChkCustomModification(other).setSelected(on.contains(other));
      if (other.isIncludedWithGeneratedCommander()) {
        view.getChkCustomModification(other).setEnabled(!RogueStart.isForced(on, other));
      }
    }
    boolean experimental = view.getChkCustomModification(
        CustomRunModification.EXPERIMENT).isSelected();
    updatingCustomModUi = false;
    if (modification == CustomRunModification.EXPERIMENT
        || wasExperimental != experimental) {
      loadAvailableCommanders();
    } else {
      updateStartButtonState();
    }
  }

  private boolean isGeneratedCommanderSelected() {
    return view.getChkRandomRogueCommander().isSelected() || view.getChkChooseRogueCommander().isSelected();
  }

  private void updateStartButtonState() {
    boolean ignoresSelectedCommander = view.getChkCustomRuns().isSelected() && isGeneratedCommanderSelected();
    for (CommanderCardPanel panel : view.getCommanderPanels()) {
      panel.setDimmed(ignoresSelectedCommander);
    }
    view.getBtnBeginRun().setEnabled(selectedStart().canBegin(RogueMetaProgress.getInstance()));
  }

  private void updateCommanderDetails(CommanderCardPanel panel) {
    RogueDeck deck = panel.getCommander();

    if (panel.isLocked()) {
      // Show unlock condition for locked commanders
      view.getLblCommanderName().setText("???");
      view.getLblDescriptionLabel().setText(Localizer.getInstance().getMessage("lblRogueUnlockLabel"));
      view.getLblDescriptionLock().setVisible(true);
      String unlockDesc = deck.getUnlockDescription();
      if (unlockDesc != null && !unlockDesc.isEmpty()) {
        view.getTxtDescription().setText(unlockDesc);
      } else {
        view.getTxtDescription().setText(Localizer.getInstance().getMessage("lblRogueLocked"));
      }
      // Hide theme row for locked commanders
      view.getLblThemeLabel().setVisible(false);
      view.getScrollTheme().setVisible(false);
    } else {
      // Show normal details for unlocked commanders
      view.getLblCommanderName().setText(deck.getCommanderCardName());
      view.getLblDescriptionLabel().setText(Localizer.getInstance().getMessage("lblRogueDescriptionLabel"));
      view.getLblDescriptionLock().setVisible(false);
      view.getTxtDescription().setText(deck.getDescription());
      view.getTxtTheme().setText(deck.getThemeDescription());
      // Show theme row for unlocked commanders
      view.getLblThemeLabel().setVisible(true);
      view.getScrollTheme().setVisible(true);
    }

    // Force UI refresh for text areas
    view.getLblDescriptionLabel().revalidate();
    view.getLblDescriptionLabel().repaint();
    view.getTxtDescription().revalidate();
    view.getTxtDescription().repaint();
    view.getTxtTheme().revalidate();
    view.getTxtTheme().repaint();
  }

  private void updateDescensionVisibility(CommanderCardPanel panel, boolean preserveDescension) {
    int previousDescensionLevel = preserveDescension ? selectedDescensionLevel : 0;
    selectedDescensionLevel = 0;
    updatingDescensionUi = true;
    view.getChkDescension().setSelected(false);
    updatingDescensionUi = false;
    view.getPnlDescensionLevel().setVisible(false);

    boolean custom = view.getChkCustomRuns().isSelected();
    if (!custom && (panel == null || panel.isLocked())) {
      view.getChkDescension().setVisible(false);
      view.getPnlDescensionLock().setVisible(false);
      return;
    }

    RogueMetaProgress progress = RogueMetaProgress.getInstance();
    RogueDeck deck = custom ? null : panel.getCommander();
    String lockText = RogueStart.descensionLockText(progress, deck, 0, custom);
    if (lockText != null) {
      view.getLblDescensionLockText().setText(lockText);
      view.getPnlDescensionLock().setVisible(true);
      view.getChkDescension().setVisible(false);
      return;
    }

    view.getPnlDescensionLock().setVisible(false);
    view.getChkDescension().setVisible(true);
    if (previousDescensionLevel > 0) {
      int maxUnlocked = custom ? DescensionLevel.getMaxLevel()
          : progress.getMaxDescensionUnlocked(deck.getCommanderCardName());
      selectedDescensionLevel = Math.min(previousDescensionLevel,
          Math.min(maxUnlocked, DescensionLevel.getMaxLevel()));
      updatingDescensionUi = true;
      view.getChkDescension().setSelected(true);
      updatingDescensionUi = false;
      view.getPnlDescensionLevel().setVisible(true);
      updateDescensionDisplay();
    }
  }

  private void onDescensionToggled() {
    if (updatingDescensionUi) {
      return;
    }
    if (view.getChkDescension().isSelected()) {
      selectedDescensionLevel = 1;
      view.getLblDescensionLock().setVisible(false);
      view.getPnlDescensionLevel().setVisible(true);
      updateDescensionDisplay();
    } else {
      selectedDescensionLevel = 0;
      view.getPnlDescensionLevel().setVisible(false);
      updateStartButtonState();
    }
  }

  private void changeDescensionLevel(int delta) {
    int newLevel = selectedDescensionLevel + delta;
    newLevel = Math.max(1, Math.min(DescensionLevel.getMaxLevel(), newLevel));
    selectedDescensionLevel = newLevel;
    updateDescensionDisplay();
  }

  private void updateDescensionDisplay() {
    boolean custom = view.getChkCustomRuns().isSelected();
    if (!custom && selectedDeck == null) return;
    String lockText = RogueStart.descensionLockText(RogueMetaProgress.getInstance(), selectedDeck,
        selectedDescensionLevel, custom);
    DescensionLevel dl = DescensionLevel.forLevel(selectedDescensionLevel);
    boolean locked = lockText != null;

    String levelName = dl != null
        ? Localizer.getInstance().getMessage("lblRogueNamedLevel", selectedDescensionLevel, dl.name)
        : Localizer.getInstance().getMessage("lblRogueLevel", selectedDescensionLevel);
    view.getLblDescensionLevel().setText(levelName);
    view.getLblDescensionLock().setVisible(locked);
    view.getBtnDescensionDown().setVisible(selectedDescensionLevel > 1);
    view.getBtnDescensionUp().setVisible(!locked && selectedDescensionLevel < DescensionLevel.getMaxLevel());

    view.getLblDescensionDesc().setText(locked ? lockText : dl != null ? dl.description : "");
    updateStartButtonState();
  }

  private void beginNewRun() {
    CSubmenuRogueMap.SINGLETON_INSTANCE.getFlow().begin(selectedStart());
  }

  private RogueStart selectedStart() {
    return new RogueStart(selectedDeck, view.getChkCustomRuns().isSelected(), selectedModifications(),
        selectedDescensionLevel);
  }

  private Set<CustomRunModification> selectedModifications() {
    Set<CustomRunModification> modifications = EnumSet.noneOf(CustomRunModification.class);
    for (CustomRunModification modification : CustomRunModification.values()) {
      if (view.getChkCustomModification(modification).isSelected()) {
        modifications.add(modification);
      }
    }
    return modifications;
  }
}
