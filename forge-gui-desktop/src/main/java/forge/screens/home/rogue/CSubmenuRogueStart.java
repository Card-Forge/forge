package forge.screens.home.rogue;

import forge.util.Localizer;

import forge.ImageCache;
import forge.ImageKeys;
import forge.gamemodes.rogue.*;
import forge.gamemodes.rogue.effect.*;
import forge.gamemodes.rogue.npc.NPCContext;
import forge.gamemodes.rogue.npc.NPC;
import forge.gamemodes.rogue.npc.NPCEncounterComposite;
import forge.gamemodes.rogue.path.RoguePathGenerator;
import forge.gui.GuiBase;
import forge.gui.UiCommand;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences;
import forge.screens.home.CHomeUI;
import forge.screens.home.sanctioned.AvatarSelector;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    view.getBtnDevUnlockAll().setText(newState ? Localizer.getInstance().getMessage("lblRogueDevLockCommanders") : Localizer.getInstance().getMessage("lblRogueDevUnlockAll"));
    loadAvailableCommanders();
  }

  private void devEditNPCProgress() {
    Map<NPC, Integer> updatedLevels = new NPCProgressDialog(RogueMetaProgress.getInstance()).show();
    if (updatedLevels == null) {
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
    loadAvailableCommanders();
    SwingUtilities.invokeLater(() -> {
      view.getBtnBeginRun().requestFocusInWindow();
      RogueMetaProgress.getInstance().notifyEndlessModeUnlocked();
      showBetweenRunNpcDialogs();
      showTutorials();
    });
  }

  private void showTutorials() {

    var progress = RogueMetaProgress.getInstance();

    // Show Descension tutorial once when it becomes unlocked
    if (progress.isDescensionModeUnlocked()) {
      RogueTutorialHelper.showIfNotSeen(RogueTutorial.DESCENSION_UNLOCKED);
    }

    int highestDescensionWon = progress.getHighestDescensionWon();
    if (highestDescensionWon >= 1) {
      RogueTutorialHelper.showIfNotSeen(RogueTutorial.DESCENSION_LEVEL_1_WIN);
    }
  }

  private void showBetweenRunNpcDialogs() {
    RogueMetaProgress progress = RogueMetaProgress.getInstance();
    for (NPCContext ctx : NPCEncounterComposite.INSTANCE.onBetweenRuns(progress)) {
      new NPCDialog(ctx, new ChoiceRerollContext()).show();
    }
  }

  private void loadAvailableCommanders() {
    boolean customRunsUnlocked = RogueMetaProgress.getInstance().isCustomRunsUnlocked();
    if (!customRunsUnlocked) {
      view.getChkCustomRuns().setSelected(false);
    }
    view.getChkCustomRuns().setVisible(customRunsUnlocked);
    view.getPnlCustomRunLock().setVisible(!customRunsUnlocked);
    view.setCustomModificationsVisible(view.getChkCustomRuns().isSelected());

    List<RogueDeck> availableDecks = RogueConfig.loadRogueDecks();
    boolean experimental = view.getChkCustomRuns().isSelected()
        && view.getChkCustomModification(CustomRunModification.EXPERIMENT).isSelected();
    availableDecks.removeIf(deck -> deck.isExperimental() != experimental);
    String previousCommanderName = selectedDeck != null ? selectedDeck.getCommanderCardName() : null;
    int previousDescensionLevel = selectedDescensionLevel;

    // Sort commanders: unlocked first (alphabetically), then locked (alphabetically)
    availableDecks.sort(Comparator
        .comparing((RogueDeck d) -> !d.isUnlocked())  // false (unlocked) before true (locked)
        .thenComparing(RogueDeck::getName));

    // Clear existing commander panels
    view.getCommanderCarouselPanel().clear();

    // Create card panel for each commander
    CommanderCardPanel firstUnlockedPanel = null;
    CommanderCardPanel previousCommanderPanel = null;
    for (RogueDeck deck : availableDecks) {
      CommanderCardPanel cardPanel = new CommanderCardPanel(deck, view);

      // Set selection callback to update details and handle single-selection
      cardPanel.setSelectionCallback(this::onCommanderSelected);

      view.getCommanderCarouselPanel().addCommanderPanel(cardPanel);

      // Track first unlocked commander
      if (firstUnlockedPanel == null && !cardPanel.isLocked()) {
        firstUnlockedPanel = cardPanel;
      }
      if (previousCommanderPanel == null && !cardPanel.isLocked()
          && previousCommanderName != null
          && previousCommanderName.equals(cardPanel.getCommander().getCommanderCardName())) {
        previousCommanderPanel = cardPanel;
      }
    }

    requestHiddenCardImageForLockedPanels();

    // Restore previous commander selection when returning to this view; otherwise use first unlocked.
    CommanderCardPanel panelToSelect = previousCommanderPanel != null ? previousCommanderPanel
        : firstUnlockedPanel != null ? firstUnlockedPanel
        : view.getCommanderPanels().isEmpty() ? null : view.getCommanderPanels().get(0);
    if (panelToSelect != null) {
      view.getCommanderCarouselPanel().centerOn(panelToSelect);
      panelToSelect.setSelected(!panelToSelect.isLocked());
      panelToSelect.setHighlighted(panelToSelect.isLocked());
      selectedDeck = panelToSelect.isLocked() ? null : panelToSelect.getCommander();
      selectedDescensionLevel = panelToSelect.isLocked() ? 0 : previousDescensionLevel;
      updateCommanderDetails(panelToSelect);
      updateDescensionVisibility(panelToSelect.isLocked() ? null : panelToSelect,
          previousCommanderPanel != null);
    } else {
      selectedDeck = null;
      updateDescensionVisibility(null, true);
    }
    view.getBtnCommanderPrevious().setEnabled(view.getCommanderPanels().size() > 1);
    view.getBtnCommanderNext().setEnabled(view.getCommanderPanels().size() > 1);
    updateStartButtonState();

    // Update button states - locked until first run completed
    boolean hasCompletedRuns = RogueMetaProgress.getInstance().getTotalRunsCompleted() > 0;
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
    boolean wasGeneratedCommander = view.getChkRandomRogueCommander().isSelected()
        || view.getChkChooseRogueCommander().isSelected();
    updatingCustomModUi = true;
    if (view.getChkCustomModification(modification).isSelected()) {
      for (CustomRunModification other : CustomRunModification.values()) {
        if (modification.conflictsWith(other)) {
          view.getChkCustomModification(other).setSelected(false);
        }
      }
    }
    boolean generatedCommander = view.getChkRandomRogueCommander().isSelected()
        || view.getChkChooseRogueCommander().isSelected();
    for (CustomRunModification deckMod : CustomRunModification.values()) {
      if (!deckMod.isIncludedWithGeneratedCommander()) {
        continue;
      }
      if (generatedCommander || wasGeneratedCommander || modification.isGeneratedCommander()) {
        view.getChkCustomModification(deckMod).setSelected(generatedCommander);
      }
      view.getChkCustomModification(deckMod).setEnabled(!generatedCommander);
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

  private void updateStartButtonState() {
    boolean custom = view.getChkCustomRuns().isSelected();
    boolean ignoresSelectedCommander = custom && (view.getChkRandomRogueCommander().isSelected()
        || view.getChkChooseRogueCommander().isSelected());
    for (CommanderCardPanel panel : view.getCommanderPanels()) {
      panel.setDimmed(ignoresSelectedCommander);
    }
    boolean customAllowed = !custom || RogueMetaProgress.getInstance().isCustomRunsUnlocked();
    boolean hasAnyMod = false;
    for (CustomRunModification modification : CustomRunModification.values()) {
      hasAnyMod |= view.getChkCustomModification(modification).isSelected();
    }
    boolean hasDeck = custom ? hasAnyMod && (ignoresSelectedCommander || selectedDeck != null)
        : selectedDeck != null;
    boolean descensionAllowed = selectedDescensionLevel == 0
        || (RogueMetaProgress.getInstance().isDescensionModeUnlocked()
        && (custom || (selectedDeck != null && selectedDescensionLevel <= RogueMetaProgress
            .getInstance().getMaxDescensionUnlocked(selectedDeck.getCommanderCardName()))));
    view.getBtnBeginRun().setEnabled(customAllowed && hasDeck && descensionAllowed);
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
    if (!progress.isDescensionModeUnlocked()) {
      view.getLblDescensionLockText().setText(
          Localizer.getInstance().getMessage("lblRogueUnlockDescensionHint"));
      view.getPnlDescensionLock().setVisible(true);
      view.getChkDescension().setVisible(false);
      return;
    }

    if (custom) {
      view.getPnlDescensionLock().setVisible(false);
      view.getChkDescension().setVisible(true);
      if (previousDescensionLevel > 0) {
        selectedDescensionLevel = Math.min(previousDescensionLevel, DescensionLevel.getMaxLevel());
        updatingDescensionUi = true;
        view.getChkDescension().setSelected(true);
        updatingDescensionUi = false;
        view.getPnlDescensionLevel().setVisible(true);
        updateDescensionDisplay();
      }
      return;
    }

    String commanderName = panel.getCommander().getCommanderCardName();

    int maxUnlocked = progress.getMaxDescensionUnlocked(commanderName);
    if (maxUnlocked == 0) {
      view.getLblDescensionLockText().setText(
          Localizer.getInstance().getMessage("lblRogueUnlockCommanderDescensionHint", commanderName));
      view.getPnlDescensionLock().setVisible(true);
      view.getChkDescension().setVisible(false);
    } else {
      view.getPnlDescensionLock().setVisible(false);
      view.getChkDescension().setVisible(true);
      if (previousDescensionLevel > 0) {
        selectedDescensionLevel = Math.min(previousDescensionLevel,
            Math.min(maxUnlocked, DescensionLevel.getMaxLevel()));
        updatingDescensionUi = true;
        view.getChkDescension().setSelected(true);
        updatingDescensionUi = false;
        view.getPnlDescensionLevel().setVisible(true);
        updateDescensionDisplay();
      }
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
    String commanderName = selectedDeck != null ? selectedDeck.getCommanderCardName() : "";
    int maxUnlocked = custom ? DescensionLevel.getMaxLevel()
        : RogueMetaProgress.getInstance().getMaxDescensionUnlocked(commanderName);
    DescensionLevel dl = DescensionLevel.forLevel(selectedDescensionLevel);
    boolean locked = selectedDescensionLevel > maxUnlocked;

    String levelName = dl != null
        ? Localizer.getInstance().getMessage("lblRogueNamedLevel", selectedDescensionLevel, dl.name)
        : Localizer.getInstance().getMessage("lblRogueLevel", selectedDescensionLevel);
    view.getLblDescensionLevel().setText(levelName);
    view.getLblDescensionLock().setVisible(locked);
    view.getBtnDescensionDown().setVisible(selectedDescensionLevel > 1);
    view.getBtnDescensionUp().setVisible(!locked && selectedDescensionLevel < DescensionLevel.getMaxLevel());

    if (!locked) {
      view.getLblDescensionDesc().setText(dl != null ? dl.description : "");
      updateStartButtonState();
    } else {
      view.getLblDescensionDesc().setText(Localizer.getInstance().getMessage("lblRogueUnlockNextDescensionHint", commanderName, selectedDescensionLevel - 1, selectedDescensionLevel));
      updateStartButtonState();
    }
  }

  private void beginNewRun() {
    boolean custom = view.getChkCustomRuns().isSelected();
    boolean generatedCommander = view.getChkRandomRogueCommander().isSelected()
        || view.getChkChooseRogueCommander().isSelected();
    List<CustomRunModification> modifications = new ArrayList<>();
    if (custom) {
      for (CustomRunModification modification : CustomRunModification.values()) {
        if (view.getChkCustomModification(modification).isSelected()
            && (!generatedCommander || !modification.isIncludedWithGeneratedCommander())) {
          modifications.add(modification);
        }
      }
    }
    if ((custom && !RogueMetaProgress.getInstance().isCustomRunsUnlocked())
        || (selectedDescensionLevel > 0
        && !RogueMetaProgress.getInstance().isDescensionModeUnlocked())
        || (custom && (modifications.isEmpty() || (!generatedCommander && selectedDeck == null)))
        || (!custom && selectedDeck == null)) {
      return;
    }

    boolean chosenOne = modifications.contains(CustomRunModification.CHOSEN_ONE);
    PaperCard chosenCommander = chosenOne
        ? new CommanderSelectionDialog().show() : null;
    if (chosenOne && chosenCommander == null) {
      return;
    }
    RogueDeck runDeck = selectedDeck;
    try {
      for (CustomRunModification modification : modifications) {
        runDeck = modification.createRogueDeck(runDeck, chosenCommander,
            (deck, ctx) -> new CardSelectionDialog(Localizer.getInstance().getMessage("lblRogueStartingDeck"), ctx.cardSelectionSubtitle,
                ctx.candidateCards, new RogueRun(deck), ctx.cardSelectionMinCount,
                ctx.cardSelectionMaxCount, ctx.buildDeckPreview).show());
        if (runDeck == null) {
          break;
        }
      }
    } catch (RuntimeException e) {
      System.err.println("Could not generate a custom Rogue Commander deck: " + e);
      runDeck = null;
    }
    if (runDeck == null) {
      FOptionPane.showMessageDialog(Localizer.getInstance().getMessage("lblRogueCouldNotGenerateDeck"),
          Localizer.getInstance().getMessage("lblRogueCustomRun"));
      return;
    }
    if (chosenOne) {
      RogueDeck generatedDeck = runDeck;
      AvatarSelector avatarSelector = new AvatarSelector(chosenCommander.getName(),
          RogueUIHelper.getAvailableAvatarIndex(runDeck.getAvatarIndex()), List.of());
      for (FLabel avatar : avatarSelector.getSelectables()) {
        int avatarIndex = Integer.parseInt(avatar.getName().substring(11));
        if (RogueUIHelper.getAvailableAvatarIndex(avatarIndex) != avatarIndex) {
          avatar.setVisible(false);
          continue;
        }
        avatar.setCommand((UiCommand) () -> {
          generatedDeck.setAvatarIndex(avatarIndex);
          avatarSelector.setVisible(false);
        });
      }
      avatarSelector.setVisible(true);
      avatarSelector.dispose();
    }

    var progress = RogueMetaProgress.getInstance();

    List<RogueRun> runsToAbandon = getContinuableRunsToAbandon();
    if (!runsToAbandon.isEmpty()) {
      boolean confirmed = FOptionPane.showConfirmDialog(
          Localizer.getInstance().getMessage("lblRogueAbandonForNewRun"),
          Localizer.getInstance().getMessage("lblRogueAbandonCurrentRun"),
          Localizer.getInstance().getMessage("lblRogueStartNewRun"),
          Localizer.getInstance().getMessage("lblCancel"),
          false
      );
      if (!confirmed) return;

      for (RogueRun run : runsToAbandon) {
        run.getRunTimer().stop();
        progress.addRunHistoryEntry(
            RogueRunHistoryEntry.fromRun(run, "ABANDONED", ""));
      }
    }

    // Delete all old run save files — history is preserved in RogueMetaProgress
    for (RogueRun old : RogueIO.loadAllRuns()) {
      RogueIO.deleteRun(old);
    }

    // Create new run and snapshot active Aetherworks
    RogueRun newRun = new RogueRun(runDeck);
    for (CustomRunModification modification : modifications) {
      newRun.addCustomRunModification(modification);
    }
    newRun.getRunTimer().start();
    newRun.setDescensionLevel(selectedDescensionLevel);
    newRun.snapshotAetherEffects(progress);

    // Generate path and apply run start effects
    RoguePathGenerator.generateRandomPath(newRun);
    applyRunStartEffectsAndNpcBoon(newRun, progress);

    // Generate unique name for the run (used as filename)
    // Format: DeckName_Timestamp (e.g., "MeriaRogueCommander_12-11-25_143022")
    String runName = (custom ? runDeck.getName().replaceAll("[^a-zA-Z0-9_-]", "_")
        : runDeck.getName()) + "_" + System.currentTimeMillis();
    newRun.setName(runName);

    // Track meta progress
    RogueStats.fireOnRunStarted(newRun, progress);

    // Save the run
    RogueIO.saveRun(newRun);

    // Set as current run in the map controller
    CSubmenuRogueMap.SINGLETON_INSTANCE.setCurrentRun(newRun);


    // Navigate to the Rogue Map
    CHomeUI.SINGLETON_INSTANCE.itemClick(EDocID.HOME_ROGUEMAP);
  }

  void applyRunStartEffectsAndNpcBoon(RogueRun newRun, RogueMetaProgress progress) {
    RogueEffectComposite.INSTANCE.onRunStart(newRun, ctx -> EffectResultHelper.handleTrigger(ctx, newRun));

    // Show NPC encounter dialogs (e.g. Tyvar offering npcEffect choices)
    for (NPCContext ctx : NPCEncounterComposite.INSTANCE.onRunStart(progress, newRun)) {
      NPCEffect chosen = showRunStartNpcDialog(progress, newRun, ctx);
      if (chosen == null) {
        continue;
      }

      EffectResultContext effectCtx = new EffectResultContext();
      chosen.applyEffect(newRun, effectCtx);
      if (chosen.getEffectType() == RogueEffect.EffectType.ONESHOT) {
        if (!EffectResultHelper.handleTrigger(effectCtx, newRun)) {
          continue;
        }
        CodexHelper.recordAcquiredCards(newRun, effectCtx.addedCards);
        showNpcResultIfNeeded(chosen, effectCtx);
      } else {
        newRun.addNPCEffect(chosen);
      }
    }
  }

  private List<RogueRun> getContinuableRunsToAbandon() {
    Map<String, RogueRun> runsByName = new LinkedHashMap<>();
    addContinuableRun(runsByName, CSubmenuRogueMap.SINGLETON_INSTANCE.getCurrentRun());
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

  private NPCEffect showRunStartNpcDialog(RogueMetaProgress progress, RogueRun newRun, NPCContext ctx) {
    NPCContext effectiveCtx = maybeOverrideNpcChoices(ctx);
    NPCDialog.DialogResult result;
    boolean rerollRequested;
    do {
      ChoiceRerollContext rerollCtx = new ChoiceRerollContext();
      RogueEffectComposite.INSTANCE.onBeforeNpcBoons(rerollCtx, newRun);
      CodexHelper.recordTraitChoices(effectiveCtx.choices().stream()
          .map(NPCContext.NPCChoice::npcEffect)
          .toList());
      result = new NPCDialog(effectiveCtx, rerollCtx).show();
      rerollRequested = result.rerollRequested();

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
    return new NPCChoiceOverrideDialog(ctx).show();
  }

  private void showNpcResultIfNeeded(NPCEffect effect, EffectResultContext ctx) {
    List<NodeResultPanel.CardSection> sections = buildNpcResultSections(effect, ctx);
    if (sections.isEmpty()) {
      return;
    }

    CSubmenuRogueMap.SINGLETON_INSTANCE.showNodeResultDialog(
        Localizer.getInstance().getMessage("lblRogueNpcRewardGained"), Localizer.getInstance().getMessage("lblRogueGainedNpcReward", effect.getDisplayName()), sections);
  }

  private List<NodeResultPanel.CardSection> buildNpcResultSections(NPCEffect effect, EffectResultContext ctx) {
    Set<String> excludedNames = new HashSet<>(getPreviewedCardNames(effect));
    excludedNames.addAll(getCardNames(ctx.candidateCards));

    List<NodeResultPanel.CardSection> sections = new ArrayList<>();
    List<PaperCard> removedCards = filterResultCards(ctx.removedCards, excludedNames);
    if (!removedCards.isEmpty()) {
      sections.add(new NodeResultPanel.CardSection(Localizer.getInstance().getMessage("lblRogueCardsRemoved"), removedCards));
    }

    List<PaperCard> addedCards = filterResultCards(ctx.addedCards, excludedNames);
    if (!addedCards.isEmpty()) {
      sections.add(new NodeResultPanel.CardSection(Localizer.getInstance().getMessage("lblRogueCardsAdded"), addedCards));
    }
    return sections;
  }

  private Set<String> getPreviewedCardNames(NPCEffect effect) {
    Set<String> cardNames = new HashSet<>();
    for (PreviewReference reference : effect.getPreviewReferences()) {
      if (reference.type() == PreviewReferenceType.CARD) {
        String cardName = TextHelper.extractCardNameFromReference(reference.token());
        if (!cardName.isBlank()) {
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
