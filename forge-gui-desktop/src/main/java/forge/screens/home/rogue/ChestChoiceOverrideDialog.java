package forge.screens.home.rogue;

import forge.gamemodes.rogue.effect.ChestEffect;
import forge.toolbox.FCheckBox;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin.SkinnedPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
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
        SkinnedPanel wrapper = new SkinnedPanel(new BorderLayout(0, 10));
        wrapper.setOpaque(false);

        FLabel lblTitle = new FLabel.Builder()
            .text("[DEV] Override Chest Loot")
            .fontSize(16)
            .fontStyle(Font.BOLD)
            .fontAlign(SwingConstants.CENTER)
            .build();

        SkinnedPanel header = new SkinnedPanel(new GridLayout(2, 1));
        header.setOpaque(false);
        header.add(lblTitle);
        header.add(lblInfo);

        wrapper.add(header, BorderLayout.NORTH);
        wrapper.add(new FScrollPane(checkBoxPanel, true), BorderLayout.CENTER);
        wrapper.setPreferredSize(new Dimension(420, 420));
        wrapper.setMinimumSize(new Dimension(420, 420));

        optionPane = new FOptionPane(null, "[DEV] Chest Loot Override", null, wrapper,
            List.of("OK", "Cancel"), 0);
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
        lblInfo.setText("Select exactly 2 effects (" + selectedCount + " / " + REQUIRED_SELECTIONS
            + " selected). Current loot choices are preselected.");
        if (optionPane != null) {
            optionPane.setButtonEnabled(0, selectedCount == REQUIRED_SELECTIONS);
        }
    }

    private class EffectCheckBox extends FCheckBox {
        private final ChestEffect effect;

        private EffectCheckBox(ChestEffect effect0, boolean selected) {
            super(effect0.getDisplayName(), selected);
            effect = effect0;
        }
    }
}
