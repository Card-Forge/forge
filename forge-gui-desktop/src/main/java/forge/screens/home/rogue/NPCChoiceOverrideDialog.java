package forge.screens.home.rogue;

import forge.gamemodes.rogue.effect.NPCEffect;
import forge.gamemodes.rogue.npc.NPCContext;
import forge.gamemodes.rogue.npc.NPCContext.NPCChoice;
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

class NPCChoiceOverrideDialog {
    private static final int REQUIRED_SELECTIONS = 3;

    private final NPCContext originalContext;
    private final List<EffectCheckBox> checkBoxes;
    private final SkinnedPanel checkBoxPanel;
    private final FLabel lblInfo;
    private FOptionPane optionPane;

    NPCChoiceOverrideDialog(NPCContext context) {
        originalContext = context;

        List<NPCEffect> pool = NPCEffect.getEffectsForNpc(context.npc(), null);
        List<NPCEffect> preselectedEffects = context.choices().stream()
            .map(NPCChoice::npcEffect)
            .toList();

        checkBoxes = new ArrayList<>();
        checkBoxPanel = new SkinnedPanel();
        checkBoxPanel.setOpaque(false);
        checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));
        for (NPCEffect effect : pool) {
            EffectCheckBox checkBox = new EffectCheckBox(effect, preselectedEffects.contains(effect));
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

    NPCContext show() {
        SkinnedPanel wrapper = new SkinnedPanel(new BorderLayout(0, 10));
        wrapper.setOpaque(false);

        FLabel lblTitle = new FLabel.Builder()
            .text("[DEV] Override NPC Choices")
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

        optionPane = new FOptionPane(null, "[DEV] NPC Choice Override", null, wrapper,
            List.of("OK", "Cancel"), 0);
        updateSelectionState();
        optionPane.setVisible(true);
        int result = optionPane.getResult();
        optionPane.dispose();

        if (result != 0) {
            return originalContext;
        }

        List<NPCChoice> choices = new ArrayList<>();
        for (EffectCheckBox checkBox : checkBoxes) {
            if (checkBox.isSelected()) {
                choices.add(new NPCChoice(checkBox.effect.getDisplayName(), checkBox.effect));
            }
        }
        return new NPCContext(originalContext.npc(), originalContext.flavorTextChunks(), choices,
            originalContext.displayNameOverride(), originalContext.avatarIndexOverride());
    }

    private void updateSelectionState() {
        int selectedCount = 0;
        for (EffectCheckBox checkBox : checkBoxes) {
            if (checkBox.isSelected()) {
                selectedCount++;
            }
        }
        lblInfo.setText("Select exactly 3 effects (" + selectedCount + " / " + REQUIRED_SELECTIONS
            + " selected). Current choices are preselected.");
        if (optionPane != null) {
            optionPane.setButtonEnabled(0, selectedCount == REQUIRED_SELECTIONS);
        }
    }

    private class EffectCheckBox extends FCheckBox {
        private final NPCEffect effect;

        private EffectCheckBox(NPCEffect effect0, boolean selected) {
            super(effect0.getDisplayName(), selected);
            effect = effect0;
        }
    }
}
