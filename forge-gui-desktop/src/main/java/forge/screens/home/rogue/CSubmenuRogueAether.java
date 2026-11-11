package forge.screens.home.rogue;

import forge.gamemodes.rogue.AetherUpgrade;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.RogueTutorial;
import forge.gamemodes.rogue.effect.AetherEffect;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.localinstance.properties.ForgePreferences;
import forge.screens.home.CHomeUI;
import forge.sound.SoundEffectType;
import forge.sound.SoundSystem;
import forge.toolbox.FOptionPane;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Controls the Aether screen for managing permanent upgrades (Aetherworks).
 */
public enum CSubmenuRogueAether implements ICDoc {
  SINGLETON_INSTANCE;

  private final VSubmenuRogueAether view = VSubmenuRogueAether.SINGLETON_INSTANCE;
  private boolean initialized = false;

  @Override
  public void register() {
  }

  @Override
  public void initialize() {
    // Guard against multiple initialization (would cause duplicate listeners)
    if (initialized) {
      return;
    }
    initialized = true;

    view.getBtnBack().addActionListener(e -> goBack());
    view.getBtnResetAetherworks().addActionListener(e -> confirmResetAetherworks());
    view.getUpgradeCard().getBtnUpgrade().addActionListener(e -> purchaseNextUpgrade());

    if (ForgePreferences.DEV_MODE) {
      view.getBtnDevMaxAether().addActionListener(e -> devMaxAether());
      view.getBtnDevGainEchoes().addActionListener(e -> {
        RogueMetaProgress.getInstance().addEchoes(10);
        refreshDisplay();
      });
      view.getBtnDevGainSparks().addActionListener(e -> {
        RogueMetaProgress.getInstance().addSparks(10);
        refreshDisplay();
      });
    }

    // Setup listeners for each Aetherwork panel
    for (Map.Entry<AetherEffect, VSubmenuRogueAether.AetherworkPanel> entry
        : view.getAetherworkPanels()
        .entrySet()) {
      AetherEffect type = entry.getKey();
      VSubmenuRogueAether.AetherworkPanel panel = entry.getValue();

      panel.getBtnUpgrade().addActionListener(e -> upgradeAetherwork(type));
      // Panel click toggles active state (when unlocked)
      panel.setToggleCallback(p -> toggleAetherworkActive(p.getAetherEffect(), !p.isActive()));
    }
  }

  @Override
  public void update() {
    refreshDisplay();
    RogueTutorialHelper.showIfNotSeen(RogueTutorial.AETHER);
  }

  private void refreshDisplay() {
    RogueMetaProgress progress = RogueMetaProgress.getInstance();

    Map<AetherEffect, Integer> effectRanks = new EnumMap<>(AetherEffect.class);
    for (AetherEffect type : AetherEffect.values()) {
      effectRanks.put(type, progress.getAetherEffectRank(type));
    }

    Set<AetherEffect> activeEffects = progress.getActiveAetherEffects();

    view.updateDisplay(
        progress.getTotalEchoes(),
        progress.getTotalSparks(),
        progress.isDescensionModeUnlocked(),
        progress.getActiveAetherEffectCount(),
        progress.getAetherUpgradeLevel(),
        effectRanks,
        activeEffects
    );
  }

  private void purchaseNextUpgrade() {
    RogueMetaProgress progress = RogueMetaProgress.getInstance();
    int nextLevel = progress.getAetherUpgradeLevel() + 1;
    if (progress.purchaseAetherUpgrade(nextLevel)) {
      SoundSystem.instance.play(SoundEffectType.Artifact, SoundEffectType.Artifact.isSynced());
      // Re-populate to show newly unlocked Aetherworks
      view.populate();
      refreshDisplay();
      AetherUpgrade u = AetherUpgrade.forLevel(nextLevel);
      if (u != null) {
        FOptionPane.showMessageDialog(u.name + " unlocked!\n" + u.description, "Aether Upgrade Unlocked");
      }
    }
  }

  private void upgradeAetherwork(AetherEffect type) {
    RogueMetaProgress progress = RogueMetaProgress.getInstance();
    int rankBefore = progress.getAetherEffectRank(type);
    if (progress.upgradeAetherEffect(type)) {
      SoundSystem.instance.play(SoundEffectType.Artifact, SoundEffectType.Artifact.isSynced());
      // Auto-activate the Aetherwork when first built (rank goes from 0 to 1)
      if (rankBefore == 0 && progress.getAetherEffectRank(type) == 1) {
        progress.activateAetherEffect(type);
      }
      refreshDisplay();
    }
  }

  private void toggleAetherworkActive(AetherEffect type, boolean active) {
    RogueMetaProgress progress = RogueMetaProgress.getInstance();
    if (active) {
      progress.activateAetherEffect(type);
    } else {
      progress.deactivateAetherEffect(type);
    }
    refreshDisplay();
  }

  private void devMaxAether() {
    RogueMetaProgress progress = RogueMetaProgress.getInstance();
    int maxLevel = AetherUpgrade.getMaxLevel();
    boolean isMaxed = progress.getAetherUpgradeLevel() >= maxLevel;
    progress.setAetherUpgradeLevel(isMaxed ? 0 : maxLevel);
    view.getBtnDevMaxAether().setText(isMaxed ? "[Dev] Max Aether" : "[Dev] Reset Aether");
    view.populate();
    refreshDisplay();
  }

  private void goBack() {
    CHomeUI.SINGLETON_INSTANCE.itemClick(EDocID.HOME_ROGUESTART);
  }

  private void confirmResetAetherworks() {
    boolean confirmed = FOptionPane.showConfirmDialog(
        "Are you sure you want to reset all Aetherworks?\nAll spent Echoes will be refunded.",
        "Reset Aetherworks",
        "Reset",
        "Cancel",
        false
    );

    if (confirmed) {
      int refunded = RogueMetaProgress.getInstance().resetAetherEffects();
      refreshDisplay();
      if (refunded > 0) {
        FOptionPane.showMessageDialog("Refunded " + refunded + " Echoes.", "Aetherworks Reset");
      }
    }
  }
}
