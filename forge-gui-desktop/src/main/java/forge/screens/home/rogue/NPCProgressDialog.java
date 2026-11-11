package forge.screens.home.rogue;

import forge.util.Localizer;

import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.npc.NPC;
import forge.gamemodes.rogue.npc.NPCEncounter;
import forge.gamemodes.rogue.npc.GontiEncounter;
import forge.gamemodes.rogue.npc.HenzieEncounter;
import forge.gamemodes.rogue.npc.NarsetEncounter;
import forge.gamemodes.rogue.npc.TeferiEncounter;
import forge.gamemodes.rogue.npc.TyvarEncounter;
import forge.toolbox.FComboBox;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin.SkinnedPanel;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.SwingConstants;
import net.miginfocom.swing.MigLayout;

/**
 * Dev-only dialog for editing persistent NPC progression levels.
 */
public class NPCProgressDialog {

  private static final int DIALOG_WIDTH = 820;
  private static final int DIALOG_HEIGHT = 330;
  private static final int SAVE_OPTION = 0;

  private final MainPanel panel;
  private final Map<NPC, FComboBox<LevelOption>> levelInputs = new EnumMap<>(NPC.class);
  private FOptionPane optionPane;

  public NPCProgressDialog(RogueMetaProgress progress) {
    panel = new MainPanel();

    FLabel lblTitle = new FLabel.Builder()
        .text(Localizer.getInstance().getMessage("lblRogueSetNPCProgressLevels"))
        .fontSize(18)
        .fontStyle(Font.BOLD)
        .fontAlign(SwingConstants.CENTER)
        .build();

    FLabel lblDescription = new FLabel.Builder()
        .text(Localizer.getInstance().getMessage("lblRogueAdjustStoredNPCProgressionValuesForDevelopment"))
        .fontSize(12)
        .fontAlign(SwingConstants.CENTER)
        .build();

    panel.add(lblTitle, "w 100%!, h 36px!, ax center, gap 0 0 8px 0, wrap");
    panel.add(lblDescription, "w 100%!, ax center, gap 0 0 18px 0, wrap");

    for (NPC npc : NPC.values()) {
      int currentLevel = progress.getNPCLevel(npc.id);
      List<LevelOption> options = getOptionsForNpc(npc, currentLevel);
      if (options.isEmpty()) {
        continue;
      }
      FComboBox<LevelOption> comboBox = new FComboBox<>(options);
      comboBox.setSelectedIndex(0);
      levelInputs.put(npc, comboBox);

      panel.add(new FLabel.Builder().text(npc.name).fontSize(14).build(),
          "split 2, w 240px!, h 28px!");
      panel.add(comboBox, "w 520px!, h 28px!, wrap");
    }

    Dimension dialogSize = new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT);
    panel.setPreferredSize(dialogSize);
    panel.setMinimumSize(dialogSize);
  }

  public Map<NPC, Integer> show() {
    optionPane = new FOptionPane(
        null,
        "Dev NPC Progress",
        null,
        panel,
        List.of("Save", "Cancel"),
        SAVE_OPTION
    );

    panel.revalidate();
    panel.repaint();

    optionPane.setVisible(true);
    int result = optionPane.getResult();
    optionPane.dispose();

    if (result != SAVE_OPTION) {
      return null;
    }

    Map<NPC, Integer> updatedLevels = new EnumMap<>(NPC.class);
    for (Map.Entry<NPC, FComboBox<LevelOption>> entry : levelInputs.entrySet()) {
      updatedLevels.put(entry.getKey(), entry.getValue().getSelectedItem().level());
    }
    return updatedLevels;
  }

  private static List<LevelOption> getOptionsForNpc(NPC npc, int currentLevel) {
    NPCEncounter[] encounters = switch (npc) {
      case TYVAR -> TyvarEncounter.values();
      case GONTI -> GontiEncounter.values();
      case NARSET -> NarsetEncounter.values();
      case HENZIE -> HenzieEncounter.values();
      case TEFERI -> TeferiEncounter.values();
    };
    if (encounters.length == 0) {
      return List.of();
    }

    List<LevelOption> options = new ArrayList<>();
    options.add(new LevelOption(
        "Keep current level (" + currentLevel + " - " + getActiveEncounterName(encounters, currentLevel) + ")",
        currentLevel));
    boolean hasLevelZero = false;
    for (NPCEncounter encounter : encounters) {
      if (encounter.getRequiredLevel() < 0) {
        continue;
      }
      if (encounter.getRequiredLevel() == 0) {
        hasLevelZero = true;
      }
      options.add(new LevelOption(
          encounter.getRequiredLevel() + " - " + getEnumName(encounter), encounter.getRequiredLevel()));
    }
    if (!hasLevelZero) {
      options.add(1, new LevelOption("0 - NONE", 0));
    }
    return options;
  }

  private static String getEnumName(NPCEncounter encounter) {
    return ((Enum<?>) encounter).name();
  }

  private static String getActiveEncounterName(NPCEncounter[] encounters, int currentLevel) {
    NPCEncounter activeEncounter = null;
    for (NPCEncounter encounter : encounters) {
      int requiredLevel = encounter.getRequiredLevel();
      if (requiredLevel >= 0 && requiredLevel <= currentLevel
          && (activeEncounter == null || requiredLevel > activeEncounter.getRequiredLevel())) {
        activeEncounter = encounter;
      }
    }
    return activeEncounter == null ? "NONE" : getEnumName(activeEncounter);
  }

  private record LevelOption(String label, int level) {
    @Override
    public String toString() {
      return label;
    }
  }

  private static class MainPanel extends SkinnedPanel {

    private MainPanel() {
      super(new MigLayout("insets 20, gap 10, wrap 2", "[grow][520px!]", ""));
      setOpaque(false);
    }
  }
}
