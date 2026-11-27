package forge.screens.home.rogue;

import forge.util.Localizer;

import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.npc.NPC;
import forge.gamemodes.rogue.npc.NPCEncounterComposite;
import forge.gamemodes.rogue.npc.NPCEncounterComposite.LevelChoice;
import forge.toolbox.FComboBox;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin.SkinnedPanel;
import java.awt.Dimension;
import java.awt.Font;
import java.util.*;
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
  private final Map<NPC, FComboBox<LevelChoice>> levelInputs = new EnumMap<>(NPC.class);

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
      List<LevelChoice> options = NPCEncounterComposite.levelChoices(npc, currentLevel);
      if (options.isEmpty()) {
        continue;
      }
      FComboBox<LevelChoice> comboBox = new FComboBox<>(options);
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
    FOptionPane optionPane = new FOptionPane(
        null,
        Localizer.getInstance().getMessage("lblRogueDevNpcProgressTitle"),
        null,
        panel,
        List.of(Localizer.getInstance().getMessage("lblSave"), Localizer.getInstance().getMessage("lblCancel")),
        SAVE_OPTION
    );

    panel.revalidate();
    panel.repaint();

    optionPane.setVisible(true);
    int result = optionPane.getResult();
    optionPane.dispose();

    if (result != SAVE_OPTION) {
      return Map.of();
    }

    Map<NPC, Integer> updatedLevels = new EnumMap<>(NPC.class);
    for (Map.Entry<NPC, FComboBox<LevelChoice>> entry : levelInputs.entrySet()) {
      updatedLevels.put(entry.getKey(), Objects.requireNonNull(entry.getValue().getSelectedItem()).level());
    }
    return updatedLevels;
  }

  private static class MainPanel extends SkinnedPanel {

    private MainPanel() {
      super(new MigLayout("insets 20, gap 10, wrap 2", "[grow][520px!]", ""));
      setOpaque(false);
    }
  }
}
