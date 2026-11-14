package forge.screens.home.rogue;

import forge.gamemodes.rogue.effect.NPCEffect;
import forge.gamemodes.rogue.npc.NPCContext;
import forge.gamemodes.rogue.npc.NPCContext.NPCChoice;
import forge.toolbox.FCheckBox;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.util.Localizer;

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
        Localizer localizer = Localizer.getInstance();
        optionPane = RogueUIHelper.createChoiceOverridePane(
            localizer.getMessage("lblRogueDevNpcChoiceOverride"),
            localizer.getMessage("lblRogueDevOverrideNpcChoices"), lblInfo, checkBoxPanel);
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
        lblInfo.setText(Localizer.getInstance().getMessage("lblRogueNpcOverrideSelection", REQUIRED_SELECTIONS, selectedCount));
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
