package forge.screens.home.rogue;

import forge.gamemodes.rogue.CodexHelper;
import forge.gamemodes.rogue.RogueConfig;
import forge.gamemodes.rogue.RogueDeck;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.RoguePlanebound;
import forge.gamemodes.rogue.RogueTutorial;
import forge.gamemodes.rogue.effect.RogueEffect;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.localinstance.achievements.RogueCommanderAchievements;
import forge.localinstance.properties.ForgePreferences;
import forge.screens.home.CHomeUI;
import forge.toolbox.FOptionPane;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Controls the Rogue Commander Codex screen. Displays meta progression statistics and discovery from
 * RogueMetaProgress.
 */
public enum CSubmenuRogueCodex implements ICDoc {
  SINGLETON_INSTANCE;

  private final VSubmenuRogueCodex view = VSubmenuRogueCodex.SINGLETON_INSTANCE;
  private VSubmenuRogueCodex.CodexTab activeTab = VSubmenuRogueCodex.CodexTab.GLOBAL_STATS;
  private RogueDeck selectedCommander;
  private RoguePlanebound selectedPlanebound;

  @Override
  public void register() {
  }

  @Override
  public void initialize() {
    view.getBtnBack().addActionListener(e -> goBack());
    view.getBtnStatsBack().addActionListener(e -> goBack());
    view.getBtnReset().addActionListener(e -> confirmReset());
    view.getBtnResetTutorials().addActionListener(e -> confirmResetTutorials());
    if (ForgePreferences.DEV_MODE) {
      view.getBtnDevUnlockCodex().addActionListener(e -> devUnlockCodex());
    }
    view.setTabSelectionCallback(this::showTab);
  }

  @Override
  public void update() {
    RogueCommanderAchievements.instance.evaluateCodexAchievements(RogueMetaProgress.getInstance());
    RogueTutorialHelper.showIfNotSeen(RogueTutorial.CODEX, RogueTutorial.CARD_SELECTION_ZOOM);
    loadStatistics();
  }

  private void loadStatistics() {
    RogueMetaProgress progress = RogueMetaProgress.getInstance();

    view.updateDisplay(
        progress.getTotalRunsStarted(),
        progress.getTotalRunsCompleted(),
        progress.getTotalRunsWon(),
        progress.getTotalPlaytimeMillis(),
        progress.getTotalMatchesWon(),
        progress.getTotalMatchesLost(),
        progress.getMaxLifeInRun(),
        progress.getMaxGoldInRun(),
        progress.getMaxCreatureTypesInDeck(),
        progress.getMaxArtifactsInDeck(),
        progress.getMaxSharedCreatureTypeInDeck(),
        progress.getMaxLegendaryPermanentsInDeck(),
        progress.getMaxNonbasicLandsInDeck()
    );

    refreshActiveTab(progress);
  }

  private void showTab(VSubmenuRogueCodex.CodexTab tab) {
    activeTab = tab;
    loadStatistics();
  }

  private void refreshActiveTab(RogueMetaProgress progress) {
    switch (activeTab) {
      case GLOBAL_STATS -> view.showGlobalStats();
      case COMMANDERS -> {
        List<RogueDeck> commanders = loadCommanders();
        selectedCommander = resolveSelectedCommander(commanders);
        view.showCommanders(commanders, selectedCommander, progress, commander -> {
          selectedCommander = commander;
          activeTab = VSubmenuRogueCodex.CodexTab.COMMANDERS;
          refreshActiveTab(RogueMetaProgress.getInstance());
        });
      }
      case PLANEBOUNDS -> {
        List<RoguePlanebound> planebounds = loadPlanebounds();
        selectedPlanebound = resolveSelectedPlanebound(planebounds, progress);
        view.showPlanebounds(planebounds, selectedPlanebound, progress, planebound -> {
          selectedPlanebound = planebound;
          activeTab = VSubmenuRogueCodex.CodexTab.PLANEBOUNDS;
          refreshActiveTab(RogueMetaProgress.getInstance());
        });
      }
      case TRAITS -> view.showTraits(loadTraits(), progress);
    }
  }

  private List<RogueDeck> loadCommanders() {
    return RogueConfig.loadRogueDecks().stream()
        .filter(deck -> !deck.isExperimental())
        .sorted(Comparator.comparing(RogueDeck::getName, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  private RogueDeck resolveSelectedCommander(List<RogueDeck> commanders) {
    if (selectedCommander != null) {
      for (RogueDeck commander : commanders) {
        if (selectedCommander.getCommanderCardName().equals(commander.getCommanderCardName())
            && commander.isUnlocked()) {
          return commander;
        }
      }
    }
    return commanders.stream().filter(RogueDeck::isUnlocked).findFirst().orElse(null);
  }

  private List<RoguePlanebound> loadPlanebounds() {
    return RogueConfig.loadPlanebounds().stream()
        .sorted(Comparator.comparing(RoguePlanebound::planeboundName, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  private RoguePlanebound resolveSelectedPlanebound(List<RoguePlanebound> planebounds,
                                                    RogueMetaProgress progress) {
    if (selectedPlanebound != null) {
      for (RoguePlanebound planebound : planebounds) {
        if (selectedPlanebound.deckPath().equals(planebound.deckPath())
            && progress.hasEncounteredPlanebound(planebound)) {
          return planebound;
        }
      }
    }
    return planebounds.stream().filter(progress::hasEncounteredPlanebound).findFirst().orElse(null);
  }

  private Map<CodexHelper.TraitCategory, List<RogueEffect>> loadTraits() {
    return CodexHelper.getCodexTraitsByCategory();
  }

  private void goBack() {
    CHomeUI.SINGLETON_INSTANCE.itemClick(EDocID.HOME_ROGUESTART);
  }

  private void devUnlockCodex() {
    CodexHelper.unlockCodex();
    RogueCommanderAchievements.instance.evaluateCodexAchievements(RogueMetaProgress.getInstance());
    loadStatistics();
  }

  private void confirmReset() {
    boolean confirmed = FOptionPane.showConfirmDialog(
        "Are you sure you want to reset all Game progression?\nThis will delete all progress, history and stats.\nThis action cannot be undone.",
        "Reset Game Progression",
        "Reset",
        "Cancel",
        false
    );

    if (confirmed) {
      RogueMetaProgress.getInstance().reset();
      selectedCommander = null;
      selectedPlanebound = null;
      loadStatistics();
    }
  }

  private void confirmResetTutorials() {
    boolean confirmed = FOptionPane.showConfirmDialog(
        "Are you sure you want to reset all tutorials?\nThey will be shown again when you visit each screen.",
        "Reset Tutorials",
        "Reset",
        "Cancel",
        false
    );

    if (confirmed) {
      RogueMetaProgress.getInstance().resetTutorials();
    }
  }
}
