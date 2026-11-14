package forge.screens.home.rogue;

import forge.gamemodes.rogue.effect.ChestEffect;
import forge.toolbox.FCheckBox;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.util.Localizer;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.SwingConstants;

class ChestChoiceOverrideDialog {
    private static final int REQUIRED_SELECTIONS = 2;

    private final List<ChestEffect> originalChoices;
    private final List<EffectCheckBox> checkBoxes;
    private final SkinnedPanel checkBoxPanel;
    private final FLabel lblInfo;
    private FOptionPane optionPane;

    ChestChoiceOverrideDialog(List<ChestEffect> choices) {
        originalChoices = choices;

        checkBoxes = new ArrayList<>();
        checkBoxPanel = new SkinnedPanel();
        checkBoxPanel.setOpaque(false);
        checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));
        for (ChestEffect effect : ChestEffect.values()) {
            EffectCheckBox checkBox = new EffectCheckBox(effect, choices.contains(effect));
            checkBox.addActionListener(e -> updateSelectionState());
            checkBoxes.add(checkBox);
            checkBoxPanel.add(checkBox);
        }

        lblInfo = new FLabel.Builder()
            .text("")
            .fontSize(14)
            .fontAlign(SwingConstants.CENTER)
            .build();
    }

    List<ChestEffect> show() {
        Localizer localizer = Localizer.getInstance();
        optionPane = RogueUIHelper.createChoiceOverridePane(
            localizer.getMessage("lblRogueDevChestLootOverride"),
            localizer.getMessage("lblRogueDevOverrideChestLoot"), lblInfo, checkBoxPanel);
        updateSelectionState();
        optionPane.setVisible(true);
        int result = optionPane.getResult();
        optionPane.dispose();

        if (result != 0) {
            return originalChoices;
        }

        List<ChestEffect> choices = new ArrayList<>();
        for (EffectCheckBox checkBox : checkBoxes) {
            if (checkBox.isSelected()) {
                choices.add(checkBox.effect);
            }
        }
        return choices;
    }

    private void updateSelectionState() {
        int selectedCount = 0;
        for (EffectCheckBox checkBox : checkBoxes) {
            if (checkBox.isSelected()) {
                selectedCount++;
            }
        }
        lblInfo.setText(Localizer.getInstance().getMessage("lblRogueChestOverrideSelection", REQUIRED_SELECTIONS, selectedCount));
        if (optionPane != null) {
            optionPane.setButtonEnabled(0, selectedCount == REQUIRED_SELECTIONS);
        }
    }

    private static class EffectCheckBox extends FCheckBox {
        private final ChestEffect effect;

        private EffectCheckBox(ChestEffect effect0, boolean selected) {
            super(effect0.getDisplayName(), selected);
            effect = effect0;
        }
    }
}
